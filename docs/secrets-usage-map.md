# 🔐 Mapa de Uso de Segredos - Autenticação

## 📋 Resumo Executivo

Este documento mapeia **exatamente** quais segredos são necessários por cada job/serviço do microserviço de autenticação, seguindo o **princípio do mínimo** conforme especificado no prompt de auditoria.

---

## 🎯 Segredos por Job/Contexto

### **Job: build_and_deploy_backend**

#### **Segredos Mínimos Necessários:**

```yaml
# Azure Key Vault - Lista Explícita (SEM curingas)
secrets: |
  conexao-de-sorte-database-r2dbc-url
  conexao-de-sorte-database-jdbc-url
  conexao-de-sorte-database-username
  conexao-de-sorte-database-password
  conexao-de-sorte-redis-host
  conexao-de-sorte-redis-port
  conexao-de-sorte-redis-password
  conexao-de-sorte-redis-database
  conexao-de-sorte-jwt-secret
  conexao-de-sorte-jwt-signing-key
  conexao-de-sorte-jwt-verification-key
  conexao-de-sorte-jwt-key-id
  conexao-de-sorte-jwt-issuer
  conexao-de-sorte-jwt-privateKey
  conexao-de-sorte-jwt-publicKey
  conexao-de-sorte-cors-allowed-origins
  conexao-de-sorte-cors-allow-credentials
  conexao-de-sorte-encryption-master-key
  conexao-de-sorte-server-port
```

#### **Justificativa por Segredo:**

| Segredo | Uso | Arquivo de Configuração |
|---------|-----|------------------------|
| `conexao-de-sorte-database-r2dbc-url` | Conexão R2DBC com MySQL | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-database-jdbc-url` | Conexão JDBC para Flyway | `application.yml` |
| `conexao-de-sorte-database-username` | Usuário do banco | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-database-password` | Senha do banco | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-redis-host` | Host do Redis | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-redis-port` | Porta do Redis | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-redis-password` | Senha do Redis | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-redis-database` | Database do Redis | `application.yml`, `application-azure.yml` |
| `conexao-de-sorte-jwt-secret` | Chave secreta JWT | `application.yml`, `application-fallback.yml` |
| `conexao-de-sorte-jwt-signing-key` | Chave de assinatura JWT | `application-azure.yml`, `application-fallback.yml` |
| `conexao-de-sorte-jwt-verification-key` | Chave de verificação JWT | `application-azure.yml`, `application-fallback.yml` |
| `conexao-de-sorte-jwt-key-id` | ID da chave JWT | `application-azure.yml`, `application-fallback.yml` |
| `conexao-de-sorte-jwt-issuer` | Emissor JWT | `application.yml`, `application-fallback.yml` |
| `conexao-de-sorte-jwt-privateKey` | Chave privada JWT | `AZURE_KEYVAULT_SECRETS_GUIDE.md` |
| `conexao-de-sorte-jwt-publicKey` | Chave pública JWT | `AZURE_KEYVAULT_SECRETS_GUIDE.md` |
| `conexao-de-sorte-cors-allowed-origins` | Origens CORS permitidas | `SecurityConfig.java` |
| `conexao-de-sorte-cors-allow-credentials` | Credenciais CORS | `SecurityConfig.java` |
| `conexao-de-sorte-encryption-master-key` | Chave mestra de criptografia | `application-azure.yml` |
| `conexao-de-sorte-server-port` | Porta do servidor | `application.yml` |

---

## 🚫 Segredos NÃO Necessários

### **Removidos da Lista Original:**

- ❌ `conexao-de-sorte-alerting-webhook-secret` - Não usado neste microserviço
- ❌ `conexao-de-sorte-api-rate-limit-key` - Rate limiting configurado via properties
- ❌ `conexao-de-sorte-auth-service-url` - Este É o serviço de auth
- ❌ `conexao-de-sorte-backup-encryption-key` - Não implementado
- ❌ `conexao-de-sorte-database-host` - Incluído na URL R2DBC
- ❌ `conexao-de-sorte-database-port` - Incluído na URL R2DBC
- ❌ `conexao-de-sorte-database-url` - Usa R2DBC e JDBC específicos
- ❌ `conexao-de-sorte-ssl-*` - SSL não implementado neste microserviço
- ❌ Todos os segredos de Kafka, RabbitMQ, Traefik - Não usados

---

## 🔒 Política de Mascaramento

### **Segredos que DEVEM ser mascarados:**

```bash
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-database-password }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-redis-password }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-secret }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-signing-key }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-verification-key }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-jwt-privateKey }}"
echo ::add-mask::"${{ steps.kv.outputs.conexao-de-sorte-encryption-master-key }}"
```

---

## 📊 Estatísticas

- **Total de segredos necessários:** 19
- **Segredos de banco:** 4
- **Segredos de Redis:** 4
- **Segredos JWT:** 8
- **Segredos CORS:** 2
- **Segredos de criptografia:** 1
- **Segredos removidos da lista original:** 30+

---

## ✅ Validação

Este mapeamento foi validado contra:

1. ✅ `application.yml` - Configurações principais
2. ✅ `application-azure.yml` - Configurações Azure
3. ✅ `application-fallback.yml` - Configurações de fallback
4. ✅ `docker-compose.yml` - Variáveis de ambiente
5. ✅ `SecurityConfig.java` - Configurações CORS
6. ✅ Código fonte Java - Uso efetivo dos segredos

**Data da última validação:** $(date '+%Y-%m-%d %H:%M:%S')