# Exemplos de Configuração para Diferentes Ambientes

## Visão Geral

Este guia fornece exemplos práticos de configuração para diferentes ambientes usando GitHub Actions com OIDC e Azure Key Vault.

## 1. Estrutura de Ambientes

### 1.1 Ambientes Recomendados

```
desenvolvimento (dev)
├── Branch: develop
├── Servidor: dev.exemplo.com
└── Recursos limitados

staging (stg)
├── Branch: staging
├── Servidor: staging.exemplo.com
└── Espelho da produção

produção (prod)
├── Branch: main
├── Servidor: exemplo.com
└── Recursos completos
```

## 2. Configuração do GitHub Actions por Ambiente

### 2.1 Workflow para Desenvolvimento

```yaml
name: Deploy Development

on:
  push:
    branches: [develop]
  workflow_dispatch:

env:
  ENVIRONMENT: dev
  AZURE_CLIENT_ID: ${{ secrets.AZURE_CLIENT_ID_DEV }}
  AZURE_TENANT_ID: ${{ secrets.AZURE_TENANT_ID }}
  AZURE_SUBSCRIPTION_ID: ${{ secrets.AZURE_SUBSCRIPTION_ID_DEV }}
  KEY_VAULT_NAME: kv-projeto-dev
  REGISTRY: ghcr.io
  IMAGE_NAME: projeto/auth-service

jobs:
  deploy-dev:
    runs-on: [self-hosted, dev]
    environment: development
    permissions:
      id-token: write
      contents: read
      packages: write
    
    steps:
      - name: Login Azure via OIDC
        uses: azure/login@v1
        with:
          client-id: ${{ env.AZURE_CLIENT_ID }}
          tenant-id: ${{ env.AZURE_TENANT_ID }}
          subscription-id: ${{ env.AZURE_SUBSCRIPTION_ID }}
      
      - name: Deploy to Development
        run: |
          docker-compose -f docker-compose.dev.yml up -d
```

### 2.2 Workflow para Staging

```yaml
name: Deploy Staging

on:
  push:
    branches: [staging]
  workflow_dispatch:

env:
  ENVIRONMENT: stg
  AZURE_CLIENT_ID: ${{ secrets.AZURE_CLIENT_ID_STG }}
  AZURE_TENANT_ID: ${{ secrets.AZURE_TENANT_ID }}
  AZURE_SUBSCRIPTION_ID: ${{ secrets.AZURE_SUBSCRIPTION_ID_STG }}
  KEY_VAULT_NAME: kv-projeto-stg
  REGISTRY: ghcr.io
  IMAGE_NAME: projeto/auth-service

jobs:
  deploy-staging:
    runs-on: [self-hosted, staging]
    environment: staging
    permissions:
      id-token: write
      contents: read
      packages: write
    
    steps:
      - name: Login Azure via OIDC
        uses: azure/login@v1
        with:
          client-id: ${{ env.AZURE_CLIENT_ID }}
          tenant-id: ${{ env.AZURE_TENANT_ID }}
          subscription-id: ${{ env.AZURE_SUBSCRIPTION_ID }}
      
      - name: Run Integration Tests
        run: |
          ./scripts/integration-tests.sh
      
      - name: Deploy to Staging
        run: |
          docker-compose -f docker-compose.staging.yml up -d
```

### 2.3 Workflow para Produção

```yaml
name: Deploy Production

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  ENVIRONMENT: prod
  AZURE_CLIENT_ID: ${{ secrets.AZURE_CLIENT_ID_PROD }}
  AZURE_TENANT_ID: ${{ secrets.AZURE_TENANT_ID }}
  AZURE_SUBSCRIPTION_ID: ${{ secrets.AZURE_SUBSCRIPTION_ID_PROD }}
  KEY_VAULT_NAME: kv-projeto-prod
  REGISTRY: ghcr.io
  IMAGE_NAME: projeto/auth-service

jobs:
  deploy-production:
    runs-on: [self-hosted, production]
    environment: production
    permissions:
      id-token: write
      contents: read
      packages: write
    
    steps:
      - name: Login Azure via OIDC
        uses: azure/login@v1
        with:
          client-id: ${{ env.AZURE_CLIENT_ID }}
          tenant-id: ${{ env.AZURE_TENANT_ID }}
          subscription-id: ${{ env.AZURE_SUBSCRIPTION_ID }}
      
      - name: Backup Current Version
        run: |
          ./scripts/backup-production.sh
      
      - name: Deploy to Production
        run: |
          docker-compose -f docker-compose.prod.yml up -d
      
      - name: Health Check
        run: |
          ./scripts/health-check.sh
      
      - name: Rollback on Failure
        if: failure()
        run: |
          ./scripts/rollback.sh
```

## 3. Configuração de Segredos por Ambiente

### 3.1 Azure Key Vault - Desenvolvimento

```bash
# Segredos para desenvolvimento
az keyvault secret set --vault-name "kv-projeto-dev" --name "db-host" --value "dev-mysql.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-dev" --name "db-username" --value "dev_user"
az keyvault secret set --vault-name "kv-projeto-dev" --name "db-password" --value "dev_password_123"
az keyvault secret set --vault-name "kv-projeto-dev" --name "redis-host" --value "dev-redis.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-dev" --name "jwt-secret" --value "dev_jwt_secret_key"
az keyvault secret set --vault-name "kv-projeto-dev" --name "api-key" --value "dev_api_key_123"
```

### 3.2 Azure Key Vault - Staging

```bash
# Segredos para staging
az keyvault secret set --vault-name "kv-projeto-stg" --name "db-host" --value "stg-mysql.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-stg" --name "db-username" --value "stg_user"
az keyvault secret set --vault-name "kv-projeto-stg" --name "db-password" --value "stg_secure_password_456"
az keyvault secret set --vault-name "kv-projeto-stg" --name "redis-host" --value "stg-redis.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-stg" --name "jwt-secret" --value "stg_jwt_secret_key_complex"
az keyvault secret set --vault-name "kv-projeto-stg" --name "api-key" --value "stg_api_key_456"
```

### 3.3 Azure Key Vault - Produção

```bash
# Segredos para produção
az keyvault secret set --vault-name "kv-projeto-prod" --name "db-host" --value "prod-mysql.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-prod" --name "db-username" --value "prod_user"
az keyvault secret set --vault-name "kv-projeto-prod" --name "db-password" --value "$(openssl rand -base64 32)"
az keyvault secret set --vault-name "kv-projeto-prod" --name "redis-host" --value "prod-redis.exemplo.com"
az keyvault secret set --vault-name "kv-projeto-prod" --name "jwt-secret" --value "$(openssl rand -base64 64)"
az keyvault secret set --vault-name "kv-projeto-prod" --name "api-key" --value "$(openssl rand -base64 32)"
```

## 4. Docker Compose por Ambiente

### 4.1 docker-compose.dev.yml

```yaml
version: '3.8'

services:
  auth-service-dev:
    image: ghcr.io/projeto/auth-service:dev-latest
    container_name: auth-service-dev
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SERVER_PORT=8080
      - LOGGING_LEVEL_ROOT=DEBUG
    ports:
      - "8080:8080"
    networks:
      - dev-network
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.auth-dev.rule=Host(`dev-auth.exemplo.com`)"
      - "traefik.http.services.auth-dev.loadbalancer.server.port=8080"

networks:
  dev-network:
    external: true
```

### 4.2 docker-compose.staging.yml

```yaml
version: '3.8'

services:
  auth-service-staging:
    image: ghcr.io/projeto/auth-service:staging-latest
    container_name: auth-service-staging
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - SERVER_PORT=8080
      - LOGGING_LEVEL_ROOT=INFO
    ports:
      - "8080:8080"
    networks:
      - staging-network
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.auth-staging.rule=Host(`staging-auth.exemplo.com`)"
      - "traefik.http.services.auth-staging.loadbalancer.server.port=8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  staging-network:
    external: true
```

### 4.3 docker-compose.prod.yml

```yaml
version: '3.8'

services:
  auth-service-prod:
    image: ghcr.io/projeto/auth-service:prod-latest
    container_name: auth-service-prod
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_PORT=8080
      - LOGGING_LEVEL_ROOT=WARN
      - JVM_OPTS=-Xmx2g -Xms1g
    ports:
      - "8080:8080"
    networks:
      - prod-network
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.auth-prod.rule=Host(`auth.exemplo.com`)"
      - "traefik.http.services.auth-prod.loadbalancer.server.port=8080"
      - "traefik.http.routers.auth-prod.tls=true"
      - "traefik.http.routers.auth-prod.tls.certresolver=letsencrypt"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'

networks:
  prod-network:
    external: true
```

## 5. Scripts de Configuração por Ambiente

### 5.1 setup-secrets-dev.sh

```bash
#!/bin/bash
set -e

echo "🔧 Configurando segredos para desenvolvimento..."

# Configurações específicas do ambiente
KEY_VAULT_NAME="kv-projeto-dev"
SECRETS_DIR="/app/secrets"
ENVIRONMENT="dev"

# Login no Azure via OIDC
echo "🔐 Fazendo login no Azure..."
az login --service-principal \
  --username "$AZURE_CLIENT_ID" \
  --tenant "$AZURE_TENANT_ID" \
  --federated-token "$AZURE_FEDERATED_TOKEN"

# Criar diretório de segredos
mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"

# Recuperar segredos do Key Vault
echo "📥 Recuperando segredos do Azure Key Vault..."

# Segredos de banco de dados
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "db-host" --query "value" -o tsv > "$SECRETS_DIR/db_host"
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "db-username" --query "value" -o tsv > "$SECRETS_DIR/db_username"
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "db-password" --query "value" -o tsv > "$SECRETS_DIR/db_password"

# Segredos de Redis
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "redis-host" --query "value" -o tsv > "$SECRETS_DIR/redis_host"

# Segredos de aplicação
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "jwt-secret" --query "value" -o tsv > "$SECRETS_DIR/jwt_secret"
az keyvault secret show --vault-name "$KEY_VAULT_NAME" --name "api-key" --query "value" -o tsv > "$SECRETS_DIR/api_key"

# Definir permissões
chmod 600 "$SECRETS_DIR"/*
chown app:app "$SECRETS_DIR"/*

echo "✅ Segredos configurados com sucesso para $ENVIRONMENT!"
```

### 5.2 health-check.sh

```bash
#!/bin/bash
set -e

ENVIRONMENT=${ENVIRONMENT:-"prod"}
SERVICE_URL=${SERVICE_URL:-"http://localhost:8080"}
MAX_RETRIES=30
RETRY_INTERVAL=10

echo "🏥 Iniciando health check para ambiente: $ENVIRONMENT"

for i in $(seq 1 $MAX_RETRIES); do
    echo "Tentativa $i/$MAX_RETRIES..."
    
    if curl -f -s "$SERVICE_URL/actuator/health" > /dev/null; then
        echo "✅ Serviço está saudável!"
        
        # Verificações adicionais para produção
        if [ "$ENVIRONMENT" = "prod" ]; then
            echo "🔍 Executando verificações adicionais de produção..."
            
            # Verificar métricas
            if curl -f -s "$SERVICE_URL/actuator/metrics" > /dev/null; then
                echo "✅ Métricas disponíveis"
            else
                echo "❌ Métricas não disponíveis"
                exit 1
            fi
            
            # Verificar conectividade com banco
            if curl -f -s "$SERVICE_URL/actuator/health/db" | grep -q '"status":"UP"'; then
                echo "✅ Banco de dados conectado"
            else
                echo "❌ Problema na conectividade com banco"
                exit 1
            fi
        fi
        
        exit 0
    fi
    
    echo "⏳ Aguardando $RETRY_INTERVAL segundos..."
    sleep $RETRY_INTERVAL
done

echo "❌ Health check falhou após $MAX_RETRIES tentativas"
exit 1
```

## 6. Configuração de Runners por Ambiente

### 6.1 Runner de Desenvolvimento

```yaml
# .github/workflows/setup-dev-runner.yml
name: Setup Development Runner

on:
  workflow_dispatch:

jobs:
  setup-dev-runner:
    runs-on: [self-hosted, dev]
    steps:
      - name: Install Dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y docker.io docker-compose
          sudo usermod -aG docker $USER
      
      - name: Configure Development Environment
        run: |
          echo "ENVIRONMENT=dev" >> ~/.bashrc
          echo "LOG_LEVEL=DEBUG" >> ~/.bashrc
```

### 6.2 Runner de Produção

```yaml
# .github/workflows/setup-prod-runner.yml
name: Setup Production Runner

on:
  workflow_dispatch:

jobs:
  setup-prod-runner:
    runs-on: [self-hosted, production]
    steps:
      - name: Install Dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y docker.io docker-compose
          sudo usermod -aG docker $USER
      
      - name: Configure Production Environment
        run: |
          echo "ENVIRONMENT=prod" >> ~/.bashrc
          echo "LOG_LEVEL=WARN" >> ~/.bashrc
          
      - name: Setup Monitoring
        run: |
          # Instalar agentes de monitoramento
          curl -sSL https://get.datadoghq.com/install.sh | bash
```

## 7. Variáveis de Ambiente por Ambiente

### 7.1 Desenvolvimento

```bash
# .env.dev
ENVIRONMENT=dev
SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_ROOT=DEBUG
SERVER_PORT=8080
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
SPRING_JPA_SHOW_SQL=true
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### 7.2 Staging

```bash
# .env.staging
ENVIRONMENT=staging
SPRING_PROFILES_ACTIVE=staging
LOGGING_LEVEL_ROOT=INFO
SERVER_PORT=8080
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,info
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

### 7.3 Produção

```bash
# .env.prod
ENVIRONMENT=prod
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_ROOT=WARN
SERVER_PORT=8080
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics
SPRING_JPA_SHOW_SQL=false
SPRING_JPA_HIBERNATE_DDL_AUTO=none
JVM_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

## 8. Checklist de Configuração

### 8.1 Para Cada Ambiente

- [ ] Criar Azure Key Vault específico
- [ ] Configurar OIDC no GitHub
- [ ] Definir secrets no GitHub
- [ ] Criar docker-compose específico
- [ ] Configurar runner self-hosted
- [ ] Testar pipeline completo
- [ ] Configurar monitoramento
- [ ] Documentar URLs e acessos

### 8.2 Validação Final

- [ ] Deploy automático funciona
- [ ] Segredos são injetados corretamente
- [ ] Health checks passam
- [ ] Logs estão sendo gerados
- [ ] Métricas estão disponíveis
- [ ] Rollback funciona (produção)

## 9. Próximos Passos

1. **Implementar Monitoramento**: Configure alertas e dashboards
2. **Automatizar Testes**: Adicione testes de integração e E2E
3. **Configurar Backup**: Implemente estratégia de backup
4. **Documentar Runbooks**: Crie guias de troubleshooting
5. **Treinar Equipe**: Capacite a equipe nos novos processos

---

**Nota**: Adapte os exemplos conforme sua infraestrutura e necessidades específicas.