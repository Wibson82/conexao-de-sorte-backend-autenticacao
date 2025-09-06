# Manual de Deploy com GitHub OIDC e Azure Key Vault

## 📋 Visão Geral
Este manual documenta a implementação de pipeline CI/CD usando GitHub Actions com OIDC (OpenID Connect) para autenticação keyless no Azure e injeção segura de segredos do Azure Key Vault em imagens Docker.

## 🎯 Objetivo
Garantir deploy seguro e automatizado em produção com:
- Autenticação sem segredos estáticos (OIDC)
- Gerenciamento centralizado de segredos no Azure Key Vault
- Imagens Docker pré-configuradas com variáveis de ambiente

## ⚙️ Pré-requisitos

### Azure Resources
- **Azure Subscription** com permissões adequadas
- **Azure Key Vault** configurado com os segredos necessários
- **Azure Container Registry (ACR)** para armazenar imagens Docker
- **Azure Resource Group** para organização dos recursos

### Permissões Azure RBAC
- **Contributor** no Resource Group para deploy
- **Key Vault Secrets User** para leitura de segredos
- **AcrPush** no Container Registry

### GitHub Configuration
- **GitHub Secrets** (apenas para configuração inicial):
  - `AZURE_CLIENT_ID`: Client ID da Azure App Registration
  - `AZURE_TENANT_ID`: Azure Tenant ID
  - `AZURE_SUBSCRIPTION_ID`: Azure Subscription ID

### Azure Key Vault Secrets
Segredos obrigatórios para aplicação:
- `DATABASE-URL` - Connection string do banco de dados
- `DATABASE-USERNAME` - Usuário do banco
- `DATABASE-PASSWORD` - Senha do banco  
- `REDIS-URL` - URL de conexão Redis
- `JWT-SECRET` - Secret para tokens JWT
- `JWT-REFRESH-SECRET` - Secret para refresh tokens
- `ENCRYPTION-KEY` - Chave de criptografia
- `ENCRYPTION-IV` - IV para criptografia

## 🏗️ Estrutura do Pipeline CI/CD

### Workflow Location
```
.github/workflows/ci-cd.yml
```

### Jobs Principais

#### 1. Test Runner
- Executa testes unitários e de integração
- Verifica qualidade do código
- Gera relatórios de cobertura

#### 2. Build and Test
- Compila a aplicação
- Executa análise estática
- Valida dependências

#### 3. Build Image
- Constrói imagem Docker multi-stage
- Otimiza tamanho da imagem final
- Aplica security best practices

#### 4. Sign Image (OIDC Keyless)
- **Autenticação OIDC** com Azure
- Assinatura digital da imagem
- Geração de SBOM (Software Bill of Materials)
- Upload de attestations

#### 5. Deploy Production
- **Login OIDC** no Azure
- **Carregamento de segredos** do Key Vault
- Deploy para ambiente production
- Configuração Traefik e labels

## 🔐 Configuração OIDC

### Azure App Registration
1. Criar App Registration no Azure AD
2. Configurar Federated Credentials:
   - Issuer: `https://token.actions.githubusercontent.com`
   - Subject: `repo:organizacao/repo:ref:refs/heads/main`
   - Audience: `api://AzureADTokenExchange`

### GitHub Workflow Permissions
```yaml
permissions:
  id-token: write   # Para OIDC authentication
  contents: read    # Para checkout do código
```

## 🚀 Implementação Passo a Passo

### 1. Configuração do Ambiente
```bash
# Clone e setup inicial
git clone <repository>
cd project-directory

# Configure environment variables
cp .env.example .env
# Edite .env com valores de desenvolvimento
```

### 2. Azure Infrastructure Setup
```bash
# Login na Azure
az login

# Criar Resource Group
az group create --name myResourceGroup --location eastus

# Criar Key Vault
az keyvault create --name myKeyVault --resource-group myResourceGroup

# Adicionar segredos ao Key Vault
az keyvault secret set --vault-name myKeyVault --name DATABASE-URL --value "jdbc:postgresql://localhost:5432/mydb"
# Repetir para todos os segredos necessários
```

### 3. GitHub Secrets Configuration
```bash
# Configurar secrets iniciais no GitHub
gh secret set AZURE_CLIENT_ID -b "<client-id>"
gh secret set AZURE_TENANT_ID -b "<tenant-id>"
gh secret set AZURE_SUBSCRIPTION_ID -b "<subscription-id>"
```

### 4. Dockerfile Best Practices
```dockerfile
# Use multi-stage build
FROM maven:3.8-openjdk-17 AS builder
# ... build steps ...

FROM eclipse-temurin:17-jre

# Create non-root user
RUN addgroup --system --gid 1000 appgroup && \
    adduser --system --uid 1000 --gid 1000 appuser

# Copy application
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar /app/app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 5. CI/CD Pipeline Template
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: myregistry.azurecr.io
  IMAGE_NAME: my-app

permissions:
  id-token: write
  contents: read

jobs:
  test-runner:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Run Tests
      run: mvn test
      
  build-image:
    needs: test-runner
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Docker Build
      run: docker build -t $IMAGE_NAME .
      
  sign-image:
    needs: build-image
    runs-on: ubuntu-latest
    steps:
    - name: OIDC Login to Azure
      uses: azure/login@v1
      with:
        client-id: ${{ secrets.AZURE_CLIENT_ID }}
        tenant-id: ${{ secrets.AZURE_TENANT_ID }}
        subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
        
    - name: Sign Image with Notary
      uses: notary/notary-action@v1
      with:
        image: $IMAGE_NAME
        
  deploy-production:
    needs: sign-image
    runs-on: self-hosted
    environment: production
    steps:
    - name: OIDC Azure Login
      uses: azure/login@v1
      with:
        client-id: ${{ secrets.AZURE_CLIENT_ID }}
        tenant-id: ${{ secrets.AZURE_TENANT_ID }}
        subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
        
    - name: Load Secrets from Key Vault
      uses: azure/keyvault-action@v1
      with:
        keyvault: myKeyVault
        secrets: |
          DATABASE-URL,
          DATABASE-USERNAME,
          DATABASE-PASSWORD,
          REDIS-URL,
          JWT-SECRET,
          JWT-REFRESH-SECRET,
          ENCRYPTION-KEY,
          ENCRYPTION-IV
        
    - name: Deploy Application
      run: |
        # Cleanup old containers
        docker rm -f auth-microservice || true
        
        # Run new container with secrets
        docker run -d \
          --name auth-microservice \
          --network traefik-public \
          -p 8080:8080 \
          -e DATABASE_URL="${{ secrets.DATABASE-URL }}" \
          -e DATABASE_USERNAME="${{ secrets.DATABASE-USERNAME }}" \
          -e DATABASE_PASSWORD="${{ secrets.DATABASE-PASSWORD }}" \
          -e REDIS_URL="${{ secrets.REDIS-URL }}" \
          -e JWT_SECRET="${{ secrets.JWT-SECRET }}" \
          -e JWT_REFRESH_SECRET="${{ secrets.JWT-REFRESH-SECRET }}" \
          -e ENCRYPTION_KEY="${{ secrets.ENCRYPTION-KEY }}" \
          -e ENCRYPTION_IV="${{ secrets.ENCRYPTION-IV }}" \
          --label "traefik.enable=true" \
          --label "traefik.http.routers.auth-microservice.rule=Host(\`auth.example.com\`)" \
          --label "traefik.http.services.auth-microservice.loadbalancer.server.port=8080" \
          $REGISTRY/$IMAGE_NAME:latest
```

## 🔍 Troubleshooting Comum

### Erro: "OIDC token exchange failed"
- Verificar federated credentials na Azure App Registration
- Validar subject pattern no GitHub workflow
- Confirmar audience configuration

### Erro: "Key Vault access denied"
- Verificar RBAC permissions no Azure
- Confirmar que a identity tem permissão Key Vault Secrets User
- Validar nome do Key Vault no workflow

### Erro: "Image signing failed"
- Validar Notary configuration
- Verificar permissões ACRPush no Container Registry

### Erro: "Container startup failed"
- Verificar variáveis de ambiente injetadas
- Validar formatos dos segredos (especialmente connection strings)
- Checar logs do container: `docker logs auth-microservice`

## 📊 Monitoring e Logs

### Health Checks
```bash
# Verificar saúde da aplicação
curl http://localhost:8080/actuator/health

# Verificar logs do container
docker logs auth-microservice --tail 50

# Monitorar recursos
docker stats auth-microservice
```

### Azure Monitor
- Configurar Azure Monitor para containers
- Habilitar logs e métricas no Container Insights
- Configurar alertas para erro rates e resource usage

## 🔒 Security Best Practices

### OIDC Configuration
- Use specific subject patterns (evite wildcards amplos)
- Configure audience corretamente
- Regularmente revise federated credentials

### Key Vault Management
- Rotação regular de segredos
- Access reviews periódicas
- Backup e recovery procedures

### Container Security
- Use distroless ou minimal base images
- Regularmente atualize dependências
- Scan images for vulnerabilities
- Use non-root users dentro dos containers

## 📈 Performance Optimization

### Image Size Reduction
- Multi-stage builds
- .dockerignore bem configurado
- Remover arquivos desnecessários
- Use layer caching effectively

### Startup Time Optimization
- Configure appropriate JVM options
- Use Spring Boot's lazy initialization onde apropriado
- Optimize database connection pooling

## 🚨 Emergency Procedures

### Rollback Manual
```bash
# Parar container atual
docker stop auth-microservice

# Rodar versão anterior
docker run -d \
  --name auth-microservice-backup \
  --network traefik-public \
  -p 8080:8080 \
  # ... variáveis de ambiente ...
  myregistry.azurecr.io/my-app:previous-version
```

### Database Recovery
- Ter backups automatizados configurados
- Testar restore procedures regularmente
- Documentar emergency contact procedures

## ✅ Checklist de Implementação

- [ ] Azure Resources criados (Key Vault, ACR, Resource Group)
- [ ] Azure App Registration com federated credentials
- [ ] GitHub Secrets configurados (CLIENT_ID, TENANT_ID, SUBSCRIPTION_ID)
- [ ] Azure Key Vault preenchido com todos os segredos necessários
- [ ] RBAC permissions configuradas corretamente
- [ ] Dockerfile otimizado e seguro
- [ ] CI/CD workflow implementado (.github/workflows/ci-cd.yml)
- [ ] Environment-specific configurations
- [ ] Health checks implementados
- [ ] Monitoring e alerting configurados
- [ ] Documentation completa
- [ ] Testes de deploy em staging environment
- [ ] Rollback procedures documentados
- [ ] Team training realizado

## 📚 Recursos Adicionais

- [Azure OIDC Documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/workload-identity-federation)
- [GitHub OIDC Guide](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/about-security-hardening-with-openid-connect)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [Spring Boot Production Ready](https://spring.io/guides/gs/production-ready/)

## 🔄 Versionamento do Manual

| Versão | Data       | Alterações                         | Autor         |
|--------|------------|------------------------------------|---------------|
| 1.0    | 2024-02-01 | Versão inicial                     | DevOps Team   |
| 1.1    | 2024-02-15 | Adicionado troubleshooting section | DevOps Team   |

---

*Este manual deve ser revisado e atualizado regularmente conforme novas best practices e ferramentas são disponibilizadas.*