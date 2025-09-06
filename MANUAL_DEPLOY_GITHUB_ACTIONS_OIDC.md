# 🚀 Manual de Deploy com GitHub Actions e OIDC

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Pré-requisitos](#pré-requisitos)
3. [Configuração do Azure OIDC](#configuração-do-azure-oidc)
4. [Configuração do Azure Key Vault](#configuração-do-azure-key-vault)
5. [Estrutura do Pipeline](#estrutura-do-pipeline)
6. [Configuração do Dockerfile](#configuração-do-dockerfile)
7. [Segredos e Variáveis](#segredos-e-variáveis)
8. [Deploy em Produção](#deploy-em-produção)
9. [Monitoramento e Troubleshooting](#monitoramento-e-troubleshooting)
10. [Checklist de Implementação](#checklist-de-implementação)

---

## 🎯 Visão Geral

Este manual documenta como implementar um pipeline de CI/CD completo usando GitHub Actions com autenticação OIDC (OpenID Connect) para deploy seguro de microserviços. O pipeline inclui:

- ✅ **Autenticação sem senhas** via OIDC
- 🔐 **Gestão segura de segredos** com Azure Key Vault
- 🐳 **Build e assinatura de imagens Docker**
- 🚀 **Deploy automatizado** em self-hosted runners
- 📊 **Monitoramento e notificações**

---

## 📋 Pré-requisitos

### Infraestrutura Necessária

- **Azure Subscription** com permissões de administrador
- **GitHub Repository** com Actions habilitado
- **Self-hosted Runner** configurado (opcional, mas recomendado)
- **Azure Key Vault** para armazenamento de segredos
- **Container Registry** (GitHub Container Registry - GHCR)

### Ferramentas Requeridas

```bash
# Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Cosign (para assinatura de imagens)
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign
```

---

## 🔐 Configuração do Azure OIDC

### 1. Criar Service Principal

```bash
# Definir variáveis
SUBSCRIPTION_ID="sua-subscription-id"
RESOURCE_GROUP="rg-conexao-de-sorte"
APP_NAME="github-actions-conexao-sorte"
REPO="usuario/repositorio"

# Criar Service Principal
az ad sp create-for-rbac \
  --name "$APP_NAME" \
  --role contributor \
  --scopes "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP" \
  --sdk-auth
```

### 2. Configurar Federated Credentials

```bash
# Obter Application ID
APP_ID=$(az ad app list --display-name "$APP_NAME" --query '[0].appId' -o tsv)

# Criar federated credential para branch main
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-main",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:'$REPO':ref:refs/heads/main",
    "description": "GitHub Actions - Main Branch",
    "audiences": ["api://AzureADTokenExchange"]
  }'

# Criar federated credential para Pull Requests
az ad app federated-credential create \
  --id $APP_ID \
  --parameters '{
    "name": "github-pr",
    "issuer": "https://token.actions.githubusercontent.com",
    "subject": "repo:'$REPO':pull_request",
    "description": "GitHub Actions - Pull Requests",
    "audiences": ["api://AzureADTokenExchange"]
  }'
```

### 3. Obter Informações para GitHub Secrets

```bash
# Obter informações necessárias
echo "AZURE_CLIENT_ID: $APP_ID"
echo "AZURE_TENANT_ID: $(az account show --query tenantId -o tsv)"
echo "AZURE_SUBSCRIPTION_ID: $SUBSCRIPTION_ID"
```

---

## 🗝️ Configuração do Azure Key Vault

### 1. Criar Key Vault

```bash
KEYVAULT_NAME="kv-conexao-de-sorte"
LOCATION="brazilsouth"

# Criar Key Vault
az keyvault create \
  --name $KEYVAULT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --enable-rbac-authorization

# Dar permissões ao Service Principal
az role assignment create \
  --role "Key Vault Secrets Officer" \
  --assignee $APP_ID \
  --scope "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.KeyVault/vaults/$KEYVAULT_NAME"
```

### 2. Adicionar Segredos

```bash
# Database secrets
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-database-r2dbc-url" --value "r2dbc:mysql://mysql-server:3306/database_name"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-database-username" --value "db_user"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-database-password" --value "db_password"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-database-jdbc-url" --value "jdbc:mysql://mysql-server:3306/database_name"

# Redis secrets
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-redis-host" --value "redis-server"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-redis-port" --value "6379"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-redis-password" --value "redis_password"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-redis-database" --value "0"

# JWT secrets
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-secret" --value "jwt_secret_key"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-key-id" --value "jwt_key_id"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-signing-key" --value "jwt_signing_key"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-verification-key" --value "jwt_verification_key"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-issuer" --value "https://auth.conexaodesorte.com.br"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-jwks-uri" --value "https://auth.conexaodesorte.com.br/.well-known/jwks.json"

# Encryption secrets
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-encryption-master-key" --value "encryption_master_key"

# JWT Keys (RSA)
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-privatekey" --value "$(cat private_key.pem)"
az keyvault secret set --vault-name $KEYVAULT_NAME --name "conexao-de-sorte-jwt-publickey" --value "$(cat public_key.pem)"
```

---

## 🏗️ Estrutura do Pipeline

### 1. Arquivo de Workflow Base

Crie o arquivo `.github/workflows/ci-cd.yml`:

```yaml
name: 🔐 Microservice - CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:
    inputs:
      environment:
        description: 'Destino do deploy'
        required: false
        default: 'production'
        type: choice
        options: [staging, production]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}
  SERVICE_NAME: seu-servico
  TZ: America/Sao_Paulo

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}-${{ github.event_name == 'workflow_dispatch' && inputs.environment || 'production' }}
  cancel-in-progress: false

jobs:
  # Job de testes
  test-runner:
    runs-on: [self-hosted, seu-runner-label]
    name: 🧪 Test Self-hosted Runner
    steps:
      - uses: actions/checkout@v4
      - name: Test runner connectivity
        run: |
          echo "🎉 Self-hosted runner está funcionando!"
          echo "🏃 Runner: $(hostname)"
          echo "📅 Data: $(date)"
          echo "👤 Usuário: $(whoami)"
          echo "📁 Diretório: $(pwd)"

  # Job de build e testes
  build-and-test:
    needs: test-runner
    runs-on: self-hosted
    name: 🏗️ Build and Test
    steps:
      - uses: actions/checkout@v4
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'  # ou sua versão preferida
          distribution: 'temurin'
          cache: maven
      
      - name: Build application
        run: ./mvnw clean package -DskipTests
      
      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: microservice-jar
          path: target/*.jar
          retention-days: 7

  # Job de build da imagem Docker
  build-image:
    runs-on: self-hosted
    name: 🐳 Build Docker Image
    needs: build-and-test
    permissions:
      id-token: write
      contents: read
      packages: write
      attestations: write
    outputs:
      image-digest: ${{ steps.build.outputs.digest }}
      image-name-lower: ${{ steps.image_name.outputs.image_name_lower }}
      image-tags: ${{ steps.meta.outputs.tags }}
    steps:
      - uses: actions/checkout@v4

      - name: Compute lowercase image name
        id: image_name
        run: |
          set -euo pipefail
          REPO='${{ env.IMAGE_NAME }}'
          echo "image_name_lower=$(echo "$REPO" | tr '[:upper:]' '[:lower:]')" >> "$GITHUB_OUTPUT"

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
        with:
          driver-opts: network=host

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ steps.image_name.outputs.image_name_lower }}
          tags: |
            type=ref,event=pr
            type=raw,value=latest,enable={{is_default_branch}}
            type=raw,value={{date 'YYYY-MM-DD-HHmmss'}}-{{sha}},enable={{is_default_branch}}
          labels: |
            org.opencontainers.image.title=Seu Microservice
            org.opencontainers.image.description=Descrição do seu microservice
            org.opencontainers.image.service=${{ env.SERVICE_NAME }}

      - name: Build and push image
        id: build
        uses: docker/build-push-action@v5
        with:
          context: .
          platforms: linux/amd64
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          sbom: true
          provenance: mode=max
          cache-from: type=gha
          cache-to: type=gha,mode=max
          network: host
          build-args: |
            SERVICE_NAME=${{ env.SERVICE_NAME }}
            BUILD_DATE=${{ github.event.head_commit.timestamp || github.run_id }}
            VCS_REF=${{ github.sha }}
            VERSION=1.0.0

  # Job de assinatura da imagem
  sign-image:
    runs-on: self-hosted
    name: ✍️ Sign & Verify (100% OIDC keyless)
    needs: build-image
    permissions:
      id-token: write
      contents: read
      packages: write
      attestations: write
    steps:
      - name: Verify OIDC token availability
        run: |
          echo "🔍 Verifying OIDC token is available..."
          if [ -z "${ACTIONS_ID_TOKEN_REQUEST_TOKEN:-}" ]; then
            echo "❌ OIDC token not available"
            exit 1
          fi
          echo "✅ OIDC token is available"
          
      - name: Resolve image digest ref
        id: ref
        run: |
          set -euo pipefail
          IMG="ghcr.io/${{ needs.build-image.outputs.image-name-lower }}@${{ needs.build-image.outputs.image-digest }}"
          echo "lower=$(echo "$IMG" | tr '[:upper:]' '[:lower:]')" >> "$GITHUB_OUTPUT"

      - name: Install cosign
        uses: sigstore/cosign-installer@v3
        with:
          cosign-release: v2.5.3

      - name: Login GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Pull digest
        run: docker pull "${{ steps.ref.outputs.lower }}"

      - name: Sign (keyless OIDC)
        env:
          COSIGN_EXPERIMENTAL: "1"
        run: |
          echo "🔐 Signing container image with OIDC keyless signature..."
          cosign sign --yes "${{ steps.ref.outputs.lower }}"

      - name: Verify OIDC signature
        env:
          COSIGN_EXPERIMENTAL: "1"
        run: |
          echo "✅ Verifying OIDC keyless signature..."
          cosign verify \
            --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
            --certificate-identity-regexp "^https://github.com/${{ github.repository }}/\.github/workflows/.*@refs/heads/main$" \
            "${{ steps.ref.outputs.lower }}"
          echo "🎉 Container image signature verified successfully!"

  # Job de deploy
  deploy-production:
    needs: [build-image, sign-image]
    runs-on: [self-hosted, seu-runner-label]
    if: github.event_name == 'push' || (github.event_name == 'workflow_dispatch' && inputs.environment == 'production')
    name: 🌟 Deploy to Production (Self-hosted)
    permissions:
      id-token: write
      contents: read
      packages: read
    steps:
      - uses: actions/checkout@v4

      - name: Azure Login (OIDC)
        uses: azure/login@v2
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

      - name: Load Azure Key Vault secrets
        run: |
          set -euo pipefail
          # Extract vault name from AZURE_KEYVAULT_ENDPOINT
          VAULT_ENDPOINT="${{ secrets.AZURE_KEYVAULT_ENDPOINT }}"
          if [[ "$VAULT_ENDPOINT" =~ https://([^.]+)\.vault\.azure\.net ]]; then
            VAULT="${BASH_REMATCH[1]}"
          else
            echo "ERROR: Invalid AZURE_KEYVAULT_ENDPOINT format: $VAULT_ENDPOINT" >&2
            exit 1
          fi
          
          echo "Loading secrets from Azure Key Vault: $VAULT" >&2
          echo "VAULT=$VAULT" >> "$GITHUB_ENV"
          
          get() { 
            echo "Fetching secret: $1" >&2
            SECRET_VALUE=$(az keyvault secret show --vault-name "$VAULT" --name "$1" --query value -o tsv 2>/dev/null)
            if [ $? -ne 0 ] || [ -z "$SECRET_VALUE" ]; then
              echo "ERROR: Secret $1 not found in Azure Key Vault $VAULT" >&2
              exit 1
            fi
            echo "$SECRET_VALUE"
          }
          
          # Carregar seus segredos específicos aqui
          echo "DB_URL=$(get seu-database-url)" >> $GITHUB_ENV
          echo "DB_USERNAME=$(get seu-database-username)" >> $GITHUB_ENV
          echo "DB_PASSWORD=$(get seu-database-password)" >> $GITHUB_ENV
          
          echo "Success: Azure Key Vault secrets loaded successfully" >&2

      - name: 🧹 Clean old containers
        run: |
          set -euo pipefail
          echo "🧹 Cleaning old containers..."
          
          if docker ps -a --format '{{.Names}}' | grep -q '^seu-microservice$'; then
            echo '🛑 Stopping container: seu-microservice'
            docker stop 'seu-microservice' || true
            echo '🗑️ Removing container: seu-microservice'
            docker rm 'seu-microservice' || true
          else
            echo '✅ No containers found'
          fi
          
          echo "✅ Container cleanup completed"

      - name: 🚀 Deploy microservice
        run: |
          set -euo pipefail
          export TZ=America/Sao_Paulo
          
          IMAGE_DIGEST="${{ needs.build-image.outputs.image-digest }}"
          IMAGE_NAME_LOWER="${{ needs.build-image.outputs.image-name-lower }}"
          FULL_IMAGE="ghcr.io/${IMAGE_NAME_LOWER}@${IMAGE_DIGEST}"
          
          echo "🚀 Starting deployment..."
          echo "   - Service: seu-microservice"
          echo "   - Image: ${FULL_IMAGE}"
          
          echo "🔑 Logging in to GHCR..."
          echo '${{ secrets.GITHUB_TOKEN }}' | docker login ghcr.io -u '${{ github.actor }}' --password-stdin
          
          echo "📥 Pulling image: ${FULL_IMAGE}"
          docker pull "${FULL_IMAGE}"
          
          echo "🌐 Creating Docker network if it doesn't exist..."
          docker network create conexao-network 2>/dev/null || true
          
          echo "🚀 Starting microservice container..."
          docker run -d \
            --name "seu-microservice" \
            --network conexao-network \
            --restart unless-stopped \
            -p "8080:8080" \
            --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
            --health-interval=30s \
            --health-timeout=10s \
            --health-retries=5 \
            --health-start-period=60s \
            -e SPRING_PROFILES_ACTIVE="prod,azure" \
            -e ENVIRONMENT="production" \
            -e SERVER_PORT="8080" \
            -e TZ="America/Sao_Paulo" \
            -e AZURE_CLIENT_ID="${{ secrets.AZURE_CLIENT_ID }}" \
            -e AZURE_TENANT_ID="${{ secrets.AZURE_TENANT_ID }}" \
            -e AZURE_KEYVAULT_ENDPOINT="${{ secrets.AZURE_KEYVAULT_ENDPOINT }}" \
            -e AZURE_KEYVAULT_ENABLED="true" \
            "${FULL_IMAGE}"
          
          echo "✅ Container deployed successfully"
          echo "🔍 Waiting for service to be ready..."
          
          # Wait for health check to pass (max 3 minutes)
          TIMEOUT=180
          ELAPSED=0
          while [ $ELAPSED -lt $TIMEOUT ]; do
            if docker exec seu-microservice curl -f -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
              echo "✅ Service is ready and healthy!"
              break
            fi
            echo "⏳ Waiting for service readiness... ($ELAPSED/$TIMEOUT seconds)"
            sleep 10
            ELAPSED=$((ELAPSED + 10))
          done
          
          if [ $ELAPSED -ge $TIMEOUT ]; then
            echo "❌ Service failed to become ready within $TIMEOUT seconds"
            echo "🔍 Container logs:"
            docker logs seu-microservice --tail 50
            exit 1
          fi
          
          echo "🔍 Final container status:"
          docker ps --filter name=seu-microservice --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
          
          echo "🎉 Deployment completed successfully - Service is healthy!"
```

---

## 🐳 Configuração do Dockerfile

### Dockerfile Otimizado

```dockerfile
# Multi-stage build para otimização
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

# Metadados
LABEL maintainer="Sua Empresa <tech@suaempresa.com>"
LABEL description="Seu Microservice - Build Stage"
LABEL version="1.0.0"

# Variáveis de build
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

WORKDIR /build

# Copiar arquivos de configuração Maven (cache layer)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download de dependências (layer cacheável)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copiar código fonte
COPY src/ src/

# Build da aplicação
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B \
    -Dspring-boot.build-image.pullPolicy=IF_NOT_PRESENT \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.optimize=true

# === ESTÁGIO 2: RUNTIME ===
FROM eclipse-temurin:21-jre-alpine AS runtime

# Instalar dependências do sistema
RUN apk add --no-cache \
    tzdata \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Configurar timezone
ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Criar diretório de logs
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app/logs

# Copiar JAR da aplicação
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Mudar para usuário não-root
USER appuser:appgroup

# Labels para metadata
LABEL org.opencontainers.image.title="Seu Microservice"
LABEL org.opencontainers.image.description="Descrição do seu microservice"
LABEL org.opencontainers.image.version=${VERSION}
LABEL org.opencontainers.image.created=${BUILD_DATE}
LABEL org.opencontainers.image.revision=${VCS_REF}
LABEL org.opencontainers.image.vendor="Sua Empresa"
LABEL org.opencontainers.image.licenses="MIT"

# Comando de inicialização
ENTRYPOINT ["dumb-init", "--", "java", "-jar", "app.jar"]
```

---

## 🔐 Segredos e Variáveis

### GitHub Secrets Necessários

Configure os seguintes secrets no seu repositório GitHub:

```bash
# Azure OIDC
AZURE_CLIENT_ID=<app-id-do-service-principal>
AZURE_TENANT_ID=<tenant-id-do-azure>
AZURE_SUBSCRIPTION_ID=<subscription-id-do-azure>
AZURE_KEYVAULT_ENDPOINT=https://seu-keyvault.vault.azure.net/

# Opcional: Notificações
SLACK_WEBHOOK_URL=<webhook-url-do-slack>
SNYK_TOKEN=<token-do-snyk>
```

### Variáveis de Ambiente no Container

```yaml
# Exemplo de variáveis para o container
environment:
  - SPRING_PROFILES_ACTIVE=prod,azure
  - ENVIRONMENT=production
  - SERVER_PORT=8080
  - TZ=America/Sao_Paulo
  - AZURE_CLIENT_ID=${AZURE_CLIENT_ID}
  - AZURE_TENANT_ID=${AZURE_TENANT_ID}
  - AZURE_KEYVAULT_ENDPOINT=${AZURE_KEYVAULT_ENDPOINT}
  - AZURE_KEYVAULT_ENABLED=true
```

---

## 🚀 Deploy em Produção

### Self-hosted Runner Setup

```bash
# Instalar runner
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# Configurar runner
./config.sh --url https://github.com/usuario/repositorio --token SEU_TOKEN --labels seu-runner-label

# Instalar como serviço
sudo ./svc.sh install
sudo ./svc.sh start
```

### Configuração de Rede Docker

```bash
# Criar rede para microserviços
docker network create conexao-network

# Verificar rede
docker network ls
docker network inspect conexao-network
```

---

## 📊 Monitoramento e Troubleshooting

### Comandos Úteis

```bash
# Verificar status do container
docker ps --filter name=seu-microservice

# Ver logs do container
docker logs seu-microservice --tail 100 -f

# Verificar health check
docker inspect seu-microservice | jq '.[0].State.Health'

# Testar conectividade
curl -f http://localhost:8080/actuator/health

# Verificar assinatura da imagem
cosign verify --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
  --certificate-identity-regexp "^https://github.com/usuario/repositorio/\.github/workflows/.*@refs/heads/main$" \
  ghcr.io/usuario/repositorio@sha256:digest
```

### Troubleshooting Comum

#### 1. Erro de OIDC Token
```bash
# Verificar se OIDC está habilitado
echo $ACTIONS_ID_TOKEN_REQUEST_TOKEN

# Verificar permissões do workflow
# Certifique-se de ter: id-token: write
```

#### 2. Erro de Azure Key Vault
```bash
# Testar acesso ao Key Vault
az keyvault secret list --vault-name seu-keyvault

# Verificar permissões RBAC
az role assignment list --assignee $AZURE_CLIENT_ID --scope /subscriptions/$AZURE_SUBSCRIPTION_ID
```

#### 3. Erro de Container Registry
```bash
# Verificar login no GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_ACTOR --password-stdin

# Verificar permissões do token
# Certifique-se de ter: packages: write
```

---

## ✅ Checklist de Implementação

### Pré-Deploy
- [ ] Azure Service Principal criado
- [ ] Federated Credentials configurados
- [ ] Azure Key Vault criado e configurado
- [ ] Segredos adicionados ao Key Vault
- [ ] GitHub Secrets configurados
- [ ] Self-hosted Runner configurado
- [ ] Dockerfile otimizado
- [ ] Workflow de CI/CD criado

### Pós-Deploy
- [ ] Pipeline executado com sucesso
- [ ] Imagem Docker assinada
- [ ] Container em execução
- [ ] Health checks passando
- [ ] Logs sem erros
- [ ] Monitoramento configurado
- [ ] Notificações funcionando

### Segurança
- [ ] Usuário não-root no container
- [ ] Segredos não expostos em logs
- [ ] Imagem assinada com Cosign
- [ ] Vulnerabilidades verificadas
- [ ] Permissões mínimas aplicadas
- [ ] HTTPS configurado
- [ ] Firewall configurado

---

## 🎯 Próximos Passos

1. **Implementar Staging Environment**
   - Criar ambiente de staging
   - Configurar deploy automático para PRs

2. **Adicionar Testes Automatizados**
   - Testes unitários
   - Testes de integração
   - Testes de segurança

3. **Melhorar Observabilidade**
   - Métricas com Prometheus
   - Logs centralizados
   - Alertas automatizados

4. **Implementar GitOps**
   - ArgoCD ou Flux
   - Configuração declarativa
   - Rollback automático

---

## 📚 Referências

- [GitHub Actions OIDC](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/about-security-hardening-with-openid-connect)
- [Azure Workload Identity](https://docs.microsoft.com/en-us/azure/active-directory/develop/workload-identity-federation)
- [Cosign Documentation](https://docs.sigstore.dev/cosign/overview/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Production Ready](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**📝 Nota:** Este manual foi baseado na análise do pipeline do microserviço de autenticação do projeto Conexão de Sorte. Adapte as configurações conforme suas necessidades específicas.

**🔒 Segurança:** Sempre revise e teste as configurações de segurança antes de usar em produção.

**🚀 Deploy Responsável:** Implemente gradualmente e monitore cada etapa do processo.