# 🔐 Guia de Configuração de Segredos no Azure Key Vault

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Estrutura de Nomenclatura](#estrutura-de-nomenclatura)
3. [Segredos por Categoria](#segredos-por-categoria)
4. [Scripts de Configuração](#scripts-de-configuração)
5. [Validação e Testes](#validação-e-testes)
6. [Rotação de Segredos](#rotação-de-segredos)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Visão Geral

Este guia documenta a configuração completa de segredos no Azure Key Vault para microserviços, baseado na implementação do projeto Conexão de Sorte. O Key Vault centraliza o gerenciamento de segredos, certificados e chaves criptográficas.

### Benefícios
- ✅ **Centralização** de segredos
- 🔒 **Criptografia** automática
- 📊 **Auditoria** completa
- 🔄 **Rotação** automatizada
- 🚫 **Eliminação** de hardcoding

---

## 📝 Estrutura de Nomenclatura

### Padrão de Nomenclatura
```
{projeto}-{categoria}-{subcategoria}-{nome}
```

### Exemplos
```bash
# Database
conexao-de-sorte-database-r2dbc-url
conexao-de-sorte-database-username
conexao-de-sorte-database-password
conexao-de-sorte-database-jdbc-url

# Redis
conexao-de-sorte-redis-host
conexao-de-sorte-redis-port
conexao-de-sorte-redis-password
conexao-de-sorte-redis-database

# JWT
conexao-de-sorte-jwt-secret
conexao-de-sorte-jwt-key-id
conexao-de-sorte-jwt-signing-key
conexao-de-sorte-jwt-verification-key
conexao-de-sorte-jwt-issuer
conexao-de-sorte-jwt-jwks-uri
conexao-de-sorte-jwt-privatekey
conexao-de-sorte-jwt-publickey

# Encryption
conexao-de-sorte-encryption-master-key
```

---

## 🗂️ Segredos por Categoria

### 1. Database (MySQL/PostgreSQL)

```bash
# Configuração R2DBC (Reactive)
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-database-r2dbc-url" \
  --value "r2dbc:mysql://mysql-server.database.azure.com:3306/conexao_sorte_auth?sslMode=REQUIRED&useUnicode=true&characterEncoding=UTF-8"

# Configuração JDBC (Flyway)
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-database-jdbc-url" \
  --value "jdbc:mysql://mysql-server.database.azure.com:3306/conexao_sorte_auth?useSSL=true&requireSSL=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Sao_Paulo"

# Credenciais
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-database-username" \
  --value "conexao_sorte_user"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-database-password" \
  --value "$(openssl rand -base64 32)"
```

### 2. Redis Cache

```bash
# Configuração Redis
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-redis-host" \
  --value "redis-server.redis.cache.windows.net"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-redis-port" \
  --value "6380"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-redis-password" \
  --value "$(az redis list-keys --name redis-server --resource-group rg-conexao-de-sorte --query primaryKey -o tsv)"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-redis-database" \
  --value "0"

# SSL Configuration (se necessário)
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-redis-ssl-enabled" \
  --value "true"
```

### 3. JWT e OAuth2

```bash
# Gerar chaves RSA para JWT
openssl genrsa -out jwt_private.pem 2048
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem

# JWT Private Key
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-privatekey" \
  --value "$(cat jwt_private.pem)"

# JWT Public Key
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-publickey" \
  --value "$(cat jwt_public.pem)"

# JWT Configuration
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-secret" \
  --value "$(openssl rand -base64 64)"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-key-id" \
  --value "conexao-sorte-key-$(date +%Y%m%d)"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-issuer" \
  --value "https://auth.conexaodesorte.com.br"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-jwks-uri" \
  --value "https://auth.conexaodesorte.com.br/.well-known/jwks.json"

# Signing and Verification Keys (HMAC)
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-signing-key" \
  --value "$(openssl rand -base64 64)"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-jwt-verification-key" \
  --value "$(openssl rand -base64 64)"

# Cleanup
rm jwt_private.pem jwt_public.pem
```

### 4. Encryption

```bash
# Master Encryption Key
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-encryption-master-key" \
  --value "$(openssl rand -base64 32)"

# AES Keys para diferentes propósitos
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-encryption-data-key" \
  --value "$(openssl rand -base64 32)"

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-encryption-session-key" \
  --value "$(openssl rand -base64 32)"
```

### 5. External APIs

```bash
# API Keys de terceiros
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-api-payment-key" \
  --value "pk_live_..."

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-api-sms-key" \
  --value "api_key_..."

az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-api-email-key" \
  --value "sg_key_..."
```

### 6. Monitoring e Observabilidade

```bash
# Application Insights
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-appinsights-connection-string" \
  --value "InstrumentationKey=..."

# Slack Notifications
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-slack-webhook-url" \
  --value "https://hooks.slack.com/services/..."

# Snyk Security
az keyvault secret set \
  --vault-name "$KEYVAULT_NAME" \
  --name "conexao-de-sorte-snyk-token" \
  --value "snyk_token_..."
```

---

## 🔧 Scripts de Configuração

### Script Completo de Setup

```bash
#!/bin/bash
set -euo pipefail

# Configurações
KEYVAULT_NAME="kv-conexao-de-sorte"
RESOURCE_GROUP="rg-conexao-de-sorte"
LOCATION="brazilsouth"
PROJECT_PREFIX="conexao-de-sorte"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
    exit 1
}

# Função para criar segredo
create_secret() {
    local name="$1"
    local value="$2"
    local description="${3:-}"
    
    log "Creating secret: $name"
    
    if az keyvault secret set \
        --vault-name "$KEYVAULT_NAME" \
        --name "$name" \
        --value "$value" \
        --description "$description" \
        --output none 2>/dev/null; then
        success "Secret '$name' created successfully"
    else
        error "Failed to create secret '$name'"
    fi
}

# Função para gerar senha segura
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Função para gerar chave AES
generate_aes_key() {
    openssl rand -base64 32
}

main() {
    log "Starting Azure Key Vault secrets setup for $PROJECT_PREFIX"
    
    # Verificar se está logado no Azure
    if ! az account show >/dev/null 2>&1; then
        error "Not logged in to Azure. Run 'az login' first."
    fi
    
    # Verificar se o Key Vault existe
    if ! az keyvault show --name "$KEYVAULT_NAME" >/dev/null 2>&1; then
        error "Key Vault '$KEYVAULT_NAME' not found. Create it first."
    fi
    
    log "Setting up database secrets..."
    create_secret "${PROJECT_PREFIX}-database-r2dbc-url" \
        "r2dbc:mysql://mysql-server:3306/conexao_sorte_auth" \
        "R2DBC connection URL for reactive database access"
    
    create_secret "${PROJECT_PREFIX}-database-jdbc-url" \
        "jdbc:mysql://mysql-server:3306/conexao_sorte_auth?useSSL=true&serverTimezone=America/Sao_Paulo" \
        "JDBC connection URL for Flyway migrations"
    
    create_secret "${PROJECT_PREFIX}-database-username" \
        "conexao_sorte_user" \
        "Database username"
    
    create_secret "${PROJECT_PREFIX}-database-password" \
        "$(generate_password)" \
        "Database password"
    
    log "Setting up Redis secrets..."
    create_secret "${PROJECT_PREFIX}-redis-host" \
        "redis-server" \
        "Redis server hostname"
    
    create_secret "${PROJECT_PREFIX}-redis-port" \
        "6379" \
        "Redis server port"
    
    create_secret "${PROJECT_PREFIX}-redis-password" \
        "$(generate_password)" \
        "Redis authentication password"
    
    create_secret "${PROJECT_PREFIX}-redis-database" \
        "0" \
        "Redis database number"
    
    log "Setting up JWT secrets..."
    
    # Gerar chaves RSA temporárias
    TEMP_DIR=$(mktemp -d)
    openssl genrsa -out "$TEMP_DIR/jwt_private.pem" 2048 2>/dev/null
    openssl rsa -in "$TEMP_DIR/jwt_private.pem" -pubout -out "$TEMP_DIR/jwt_public.pem" 2>/dev/null
    
    create_secret "${PROJECT_PREFIX}-jwt-privatekey" \
        "$(cat "$TEMP_DIR/jwt_private.pem")" \
        "JWT RSA private key for token signing"
    
    create_secret "${PROJECT_PREFIX}-jwt-publickey" \
        "$(cat "$TEMP_DIR/jwt_public.pem")" \
        "JWT RSA public key for token verification"
    
    create_secret "${PROJECT_PREFIX}-jwt-secret" \
        "$(generate_aes_key)" \
        "JWT HMAC secret key"
    
    create_secret "${PROJECT_PREFIX}-jwt-key-id" \
        "conexao-sorte-key-$(date +%Y%m%d)" \
        "JWT key identifier"
    
    create_secret "${PROJECT_PREFIX}-jwt-issuer" \
        "https://auth.conexaodesorte.com.br" \
        "JWT token issuer URL"
    
    create_secret "${PROJECT_PREFIX}-jwt-jwks-uri" \
        "https://auth.conexaodesorte.com.br/.well-known/jwks.json" \
        "JWKS endpoint URL"
    
    create_secret "${PROJECT_PREFIX}-jwt-signing-key" \
        "$(generate_aes_key)" \
        "JWT HMAC signing key"
    
    create_secret "${PROJECT_PREFIX}-jwt-verification-key" \
        "$(generate_aes_key)" \
        "JWT HMAC verification key"
    
    # Cleanup
    rm -rf "$TEMP_DIR"
    
    log "Setting up encryption secrets..."
    create_secret "${PROJECT_PREFIX}-encryption-master-key" \
        "$(generate_aes_key)" \
        "Master encryption key for data protection"
    
    log "Setting up monitoring secrets..."
    create_secret "${PROJECT_PREFIX}-appinsights-connection-string" \
        "InstrumentationKey=00000000-0000-0000-0000-000000000000" \
        "Application Insights connection string"
    
    success "All secrets have been created successfully in Key Vault: $KEYVAULT_NAME"
    
    log "Listing created secrets:"
    az keyvault secret list --vault-name "$KEYVAULT_NAME" \
        --query "[?starts_with(name, '$PROJECT_PREFIX')].{Name:name, Created:attributes.created}" \
        --output table
}

# Executar apenas se chamado diretamente
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
```

### Script de Validação

```bash
#!/bin/bash
set -euo pipefail

KEYVAULT_NAME="kv-conexao-de-sorte"
PROJECT_PREFIX="conexao-de-sorte"

# Lista de segredos obrigatórios
REQUIRED_SECRETS=(
    "${PROJECT_PREFIX}-database-r2dbc-url"
    "${PROJECT_PREFIX}-database-username"
    "${PROJECT_PREFIX}-database-password"
    "${PROJECT_PREFIX}-database-jdbc-url"
    "${PROJECT_PREFIX}-redis-host"
    "${PROJECT_PREFIX}-redis-port"
    "${PROJECT_PREFIX}-redis-password"
    "${PROJECT_PREFIX}-redis-database"
    "${PROJECT_PREFIX}-jwt-secret"
    "${PROJECT_PREFIX}-jwt-key-id"
    "${PROJECT_PREFIX}-jwt-signing-key"
    "${PROJECT_PREFIX}-jwt-verification-key"
    "${PROJECT_PREFIX}-jwt-issuer"
    "${PROJECT_PREFIX}-jwt-jwks-uri"
    "${PROJECT_PREFIX}-jwt-privatekey"
    "${PROJECT_PREFIX}-jwt-publickey"
    "${PROJECT_PREFIX}-encryption-master-key"
)

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

success() {
    echo "✅ $1"
}

error() {
    echo "❌ $1" >&2
}

warn() {
    echo "⚠️ $1"
}

validate_secrets() {
    log "Validating secrets in Key Vault: $KEYVAULT_NAME"
    
    local missing_secrets=()
    local total_secrets=${#REQUIRED_SECRETS[@]}
    local found_secrets=0
    
    for secret in "${REQUIRED_SECRETS[@]}"; do
        if az keyvault secret show --vault-name "$KEYVAULT_NAME" --name "$secret" >/dev/null 2>&1; then
            success "Secret found: $secret"
            ((found_secrets++))
        else
            error "Secret missing: $secret"
            missing_secrets+=("$secret")
        fi
    done
    
    echo
    log "Validation Summary:"
    echo "   Found: $found_secrets/$total_secrets secrets"
    
    if [ ${#missing_secrets[@]} -eq 0 ]; then
        success "All required secrets are present!"
        return 0
    else
        error "Missing ${#missing_secrets[@]} required secrets:"
        for secret in "${missing_secrets[@]}"; do
            echo "     - $secret"
        done
        return 1
    fi
}

test_secret_access() {
    log "Testing secret access..."
    
    # Testar acesso a um segredo específico
    local test_secret="${PROJECT_PREFIX}-database-username"
    
    if value=$(az keyvault secret show --vault-name "$KEYVAULT_NAME" --name "$test_secret" --query value -o tsv 2>/dev/null); then
        if [ -n "$value" ] && [ "$value" != "null" ]; then
            success "Secret access test passed"
            return 0
        else
            error "Secret value is empty or null"
            return 1
        fi
    else
        error "Failed to access secret: $test_secret"
        return 1
    fi
}

main() {
    if ! az account show >/dev/null 2>&1; then
        error "Not logged in to Azure. Run 'az login' first."
        exit 1
    fi
    
    if ! validate_secrets; then
        exit 1
    fi
    
    if ! test_secret_access; then
        exit 1
    fi
    
    success "All validations passed!"
}

main "$@"
```

---

## 🔄 Rotação de Segredos

### Script de Rotação Automática

```bash
#!/bin/bash
set -euo pipefail

KEYVAULT_NAME="kv-conexao-de-sorte"
PROJECT_PREFIX="conexao-de-sorte"

# Segredos que podem ser rotacionados automaticamente
ROTATABLE_SECRETS=(
    "${PROJECT_PREFIX}-jwt-secret"
    "${PROJECT_PREFIX}-jwt-signing-key"
    "${PROJECT_PREFIX}-jwt-verification-key"
    "${PROJECT_PREFIX}-encryption-master-key"
)

rotate_secret() {
    local secret_name="$1"
    local new_value
    
    echo "Rotating secret: $secret_name"
    
    # Gerar novo valor
    new_value=$(openssl rand -base64 32)
    
    # Criar nova versão do segredo
    if az keyvault secret set \
        --vault-name "$KEYVAULT_NAME" \
        --name "$secret_name" \
        --value "$new_value" \
        --output none; then
        echo "✅ Secret rotated successfully: $secret_name"
    else
        echo "❌ Failed to rotate secret: $secret_name"
        return 1
    fi
}

rotate_jwt_keys() {
    echo "Rotating JWT RSA keys..."
    
    # Gerar novas chaves RSA
    TEMP_DIR=$(mktemp -d)
    openssl genrsa -out "$TEMP_DIR/jwt_private.pem" 2048 2>/dev/null
    openssl rsa -in "$TEMP_DIR/jwt_private.pem" -pubout -out "$TEMP_DIR/jwt_public.pem" 2>/dev/null
    
    # Atualizar chaves no Key Vault
    az keyvault secret set \
        --vault-name "$KEYVAULT_NAME" \
        --name "${PROJECT_PREFIX}-jwt-privatekey" \
        --value "$(cat "$TEMP_DIR/jwt_private.pem")" \
        --output none
    
    az keyvault secret set \
        --vault-name "$KEYVAULT_NAME" \
        --name "${PROJECT_PREFIX}-jwt-publickey" \
        --value "$(cat "$TEMP_DIR/jwt_public.pem")" \
        --output none
    
    # Atualizar Key ID
    az keyvault secret set \
        --vault-name "$KEYVAULT_NAME" \
        --name "${PROJECT_PREFIX}-jwt-key-id" \
        --value "conexao-sorte-key-$(date +%Y%m%d-%H%M%S)" \
        --output none
    
    # Cleanup
    rm -rf "$TEMP_DIR"
    
    echo "✅ JWT keys rotated successfully"
}

main() {
    echo "Starting secret rotation for Key Vault: $KEYVAULT_NAME"
    
    # Rotacionar segredos simples
    for secret in "${ROTATABLE_SECRETS[@]}"; do
        rotate_secret "$secret"
    done
    
    # Rotacionar chaves JWT
    rotate_jwt_keys
    
    echo "✅ Secret rotation completed"
    echo "⚠️ Remember to restart applications to pick up new secrets"
}

main "$@"
```

---

## 🧪 Validação e Testes

### Teste de Conectividade

```bash
#!/bin/bash
# test-keyvault-connectivity.sh

KEYVAULT_NAME="kv-conexao-de-sorte"

test_keyvault_access() {
    echo "Testing Key Vault access..."
    
    # Testar listagem de segredos
    if az keyvault secret list --vault-name "$KEYVAULT_NAME" --query "[0].name" -o tsv >/dev/null 2>&1; then
        echo "✅ Key Vault access: OK"
    else
        echo "❌ Key Vault access: FAILED"
        return 1
    fi
    
    # Testar leitura de segredo específico
    if az keyvault secret show --vault-name "$KEYVAULT_NAME" --name "conexao-de-sorte-database-username" --query value -o tsv >/dev/null 2>&1; then
        echo "✅ Secret read access: OK"
    else
        echo "❌ Secret read access: FAILED"
        return 1
    fi
    
    return 0
}

test_keyvault_access
```

### Teste de Aplicação

```java
// KeyVaultConfigTest.java
@SpringBootTest
@TestPropertySource(properties = {
    "azure.keyvault.enabled=true",
    "azure.keyvault.uri=${AZURE_KEYVAULT_ENDPOINT}"
})
class KeyVaultConfigTest {
    
    @Value("${conexao-de-sorte-database-username}")
    private String dbUsername;
    
    @Value("${conexao-de-sorte-jwt-issuer}")
    private String jwtIssuer;
    
    @Test
    void testKeyVaultSecretsLoaded() {
        assertThat(dbUsername).isNotNull().isNotEmpty();
        assertThat(jwtIssuer).isNotNull().startsWith("https://");
    }
}
```

---

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. Erro de Permissão
```bash
# Verificar permissões RBAC
az role assignment list --assignee $AZURE_CLIENT_ID --scope /subscriptions/$AZURE_SUBSCRIPTION_ID

# Adicionar permissão se necessário
az role assignment create \
  --role "Key Vault Secrets Officer" \
  --assignee $AZURE_CLIENT_ID \
  --scope "/subscriptions/$AZURE_SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.KeyVault/vaults/$KEYVAULT_NAME"
```

#### 2. Segredo Não Encontrado
```bash
# Listar todos os segredos
az keyvault secret list --vault-name $KEYVAULT_NAME --query "[].name" -o table

# Verificar nome exato do segredo
az keyvault secret list --vault-name $KEYVAULT_NAME --query "[?contains(name, 'database')].name" -o table
```

#### 3. Valor de Segredo Vazio
```bash
# Verificar valor do segredo
az keyvault secret show --vault-name $KEYVAULT_NAME --name "secret-name" --query value -o tsv

# Verificar histórico de versões
az keyvault secret list-versions --vault-name $KEYVAULT_NAME --name "secret-name"
```

#### 4. Problema de Rede
```bash
# Testar conectividade
nslookup $KEYVAULT_NAME.vault.azure.net
curl -I https://$KEYVAULT_NAME.vault.azure.net

# Verificar firewall do Key Vault
az keyvault network-rule list --name $KEYVAULT_NAME
```

### Logs de Debug

```bash
# Habilitar logs detalhados do Azure CLI
export AZURE_CLI_DIAGNOSTICS_TELEMETRY=off
az keyvault secret show --vault-name $KEYVAULT_NAME --name "secret-name" --debug
```

---

## 📊 Monitoramento

### Métricas Importantes

- **Secret Access Count**: Número de acessos aos segredos
- **Failed Requests**: Tentativas de acesso negadas
- **Secret Rotation**: Frequência de rotação de segredos
- **Vault Availability**: Disponibilidade do Key Vault

### Alertas Recomendados

```json
{
  "alertRules": [
    {
      "name": "KeyVault-HighFailureRate",
      "condition": "Failed requests > 10 in 5 minutes",
      "action": "Send notification to ops team"
    },
    {
      "name": "KeyVault-UnauthorizedAccess",
      "condition": "Unauthorized access attempts > 5 in 1 minute",
      "action": "Send security alert"
    },
    {
      "name": "KeyVault-SecretExpiration",
      "condition": "Secret expires in 30 days",
      "action": "Send rotation reminder"
    }
  ]
}
```

---

## 🎯 Próximos Passos

1. **Implementar Rotação Automática**
   - Configurar Azure Functions para rotação
   - Implementar notificações de rotação

2. **Melhorar Segurança**
   - Implementar HSM (Hardware Security Module)
   - Configurar Private Endpoints

3. **Automatizar Backup**
   - Backup automático de segredos
   - Disaster recovery procedures

4. **Implementar Compliance**
   - Auditoria de acesso
   - Relatórios de conformidade

---

**📝 Nota:** Este guia foi baseado na implementação real do projeto Conexão de Sorte. Adapte os nomes e valores conforme sua necessidade.

**🔒 Segurança:** Nunca exponha segredos em logs, código ou documentação. Use sempre o Key Vault para armazenamento seguro.

**🔄 Manutenção:** Implemente rotação regular de segredos e monitore o acesso continuamente.