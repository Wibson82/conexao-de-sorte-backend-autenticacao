# 🔐 Guia do Projeto: Autenticação
## Microserviço Core de Segurança

> **🎯 Contexto**: Este é o microserviço de autenticação da plataforma Conexão de Sorte, responsável por JWT, OAuth2, gestão de usuários e autorização. É um serviço crítico que todos os outros dependem.

---

## 📋 INFORMAÇÕES DO PROJETO

### **Identificação:**
- **Nome**: conexao-de-sorte-backend-autenticacao  
- **Porta**: 8081
- **Rede Principal**: conexao-network-swarm
- **Database**: conexao_sorte_auth (MySQL 8.0)
- **Runner**: `[self-hosted, Linux, X64, conexao, conexao-de-sorte-backend-autenticacao]`

### **Tecnologias Específicas:**
- Spring Boot 3.5.5 + Spring WebFlux (reativo)
- Spring Security + OAuth2 Resource Server
- R2DBC MySQL (persistência reativa)
- Redis (cache JWT + sessions)
- Azure Key Vault (secrets management)

---

## 🗄️ ESTRUTURA DO BANCO DE DADOS

### **Database**: `conexao_sorte_auth`

#### **Tabelas:**
1. **`usuarios`** - Usuários principais
2. **`endereco`** - Endereços de usuários (1:N)
3. **`papel`** - Roles/papéis do sistema  
4. **`usuario_papel`** - Relacionamento N:M
5. **`refresh_tokens`** - Tokens JWT refresh

#### **Relacionamentos:**
- usuarios ← endereco (1:N)
- usuarios ← usuario_papel ← papel (N:M)

### **Configuração R2DBC:**
```yaml
r2dbc:
  url: r2dbc:mysql://mysql-proxy:6033/conexao_sorte_auth
  pool:
    initial-size: 1
    max-size: 10
```

---

## 🔐 SECRETS ESPECÍFICOS

### **Azure Key Vault Secrets Utilizados:**
```yaml
# JWT Configuration
conexao-de-sorte-jwt-secret
conexao-de-sorte-jwt-privateKey  
conexao-de-sorte-jwt-publicKey
conexao-de-sorte-jwt-signing-key
conexao-de-sorte-jwt-verification-key
conexao-de-sorte-jwt-issuer
conexao-de-sorte-jwt-jwks-uri
conexao-de-sorte-jwt-key-id

# Database
conexao-de-sorte-database-r2dbc-url
conexao-de-sorte-database-username
conexao-de-sorte-database-password

# Redis Cache
conexao-de-sorte-redis-host
conexao-de-sorte-redis-password
conexao-de-sorte-redis-port

# Session & Security
conexao-de-sorte-session-secret
conexao-de-sorte-cors-allowed-origins
conexao-de-sorte-cors-allow-credentials
```

### **Cache Redis Específico:**
```yaml
redis:
  database: 0
  cache-names:
    - rsa-private-key
    - rsa-public-key  
    - key-id
    - valid-tokens
    - jwks
```

---

## 🌐 INTEGRAÇÃO DE REDE

### **Comunicação Entrada (Server):**
- **Gateway** → Autenticação (rotas /auth/*)
- **Frontend** → Autenticação (login/logout)
- **Todos os microserviços** → Validação JWT

### **Comunicação Saída (Client):**
- Autenticação → **Auditoria** (eventos de login/logout)
- Autenticação → **Notificações** (avisos de segurança)

### **Portas e Endpoints:**
```yaml
server.port: 8081

# Endpoints principais:
POST /auth/login
POST /auth/refresh  
POST /auth/logout
GET  /auth/userinfo
GET  /auth/jwks
GET  /actuator/health
```

---

## 🔗 DEPENDÊNCIAS CRÍTICAS

### **Serviços Dependentes (Upstream):**
1. **MySQL** (`mysql-proxy:6033`) - Persistência principal
2. **Redis** (`conexao-redis:6379`) - Cache distribuído
3. **Azure Key Vault** - Secrets management

### **Serviços Consumidores (Downstream):**
- **TODOS os microserviços** dependem para validação JWT
- **Gateway** - Roteamento seguro
- **Frontend** - Autenticação de usuários

### **Ordem de Deploy:**
```
1. MySQL + Redis (infrastructure)
2. Autenticação (core security)  
3. Outros microserviços (dependem de auth)
```

---

## 🚨 ESPECIFICIDADES DE SEGURANÇA

### **JWT Configuration:**
- **Algorithm**: RS256 (RSA + SHA256)
- **Key Rotation**: Suportada via JWKS
- **Expiration**: Access 15min, Refresh 7 dias
- **Issuer**: Validação obrigatória

### **Rate Limiting:**
- **Login**: 5 tentativas/minuto por IP
- **Token Refresh**: 10/minuto por usuário  
- **JWKS**: 100/minuto (cacheado)

### **Security Headers:**
```yaml
security.headers:
  frame-options: DENY
  content-type-options: nosniff
  referrer-policy: strict-origin-when-cross-origin
  x-permitted-cross-domain-policies: none
```

---

## 📊 MÉTRICAS ESPECÍFICAS

### **Custom Metrics:**
- `auth_login_attempts_total{result}` - Tentativas de login
- `auth_token_validations_total{result}` - Validações JWT
- `auth_cache_hits_total{type}` - Cache hits por tipo
- `auth_user_sessions_active` - Sessões ativas
- `auth_jwt_generation_duration` - Tempo geração JWT

### **Alertas Configurados:**
- Login failure rate > 10%
- JWT validation errors > 5%  
- Cache miss rate > 30%
- Response time P95 > 500ms

---

## 🔧 CONFIGURAÇÕES ESPECÍFICAS

### **Application Properties:**
```yaml
# JWT
jwt:
  rsa:
    private-key: ${conexao-de-sorte-jwt-privateKey}
    public-key: ${conexao-de-sorte-jwt-publicKey}
  issuer: ${conexao-de-sorte-jwt-issuer}
  access-token-expiration: PT15M
  refresh-token-expiration: P7D

# OAuth2 Resource Server
spring.security.oauth2.resourceserver:
  jwt:
    issuer-uri: ${jwt.issuer}
    jwk-set-uri: ${conexao-de-sorte-jwt-jwks-uri}

# CORS
cors:
  allowed-origins: ${conexao-de-sorte-cors-allowed-origins}
  allow-credentials: ${conexao-de-sorte-cors-allow-credentials}
```

### **Docker Compose Específico:**
```yaml
services:
  auth-service:
    container_name: conexao-auth
    hostname: conexao-auth
    networks:
      - conexao-network-swarm
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.auth.rule=PathPrefix(`/auth`)"
      - "traefik.http.services.auth.loadbalancer.server.port=8081"
```

---

## 🧪 TESTES E VALIDAÇÕES

### **Health Checks:**
```bash
# Health principal
curl -f http://localhost:8081/actuator/health

# Database connectivity
curl -f http://localhost:8081/actuator/health/db

# Redis connectivity  
curl -f http://localhost:8081/actuator/health/redis

# Custom readiness
curl -f http://localhost:8081/actuator/ready
```

### **Smoke Tests Pós-Deploy:**
```bash
# 1. Login test
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# 2. JWKS endpoint
curl -f http://localhost:8081/auth/jwks

# 3. Token validation
curl -f http://localhost:8081/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

---

## ⚠️ TROUBLESHOOTING

### **Problema: JWT Inválido**
```bash
# Verificar secrets
az keyvault secret show --vault-name kv-conexao-de-sorte --name conexao-de-sorte-jwt-privateKey

# Verificar cache JWKS
redis-cli -a $REDIS_PASS GET "jwks"

# Logs específicos
docker service logs conexao-auth | grep JWT
```

### **Problema: Database Connection**
```bash
# Verificar R2DBC pool
curl http://localhost:8081/actuator/metrics/r2dbc.pool.connections

# Test query
mysql -h mysql-proxy -P 6033 -u $DB_USER -p$DB_PASS -e "SELECT 1" conexao_sorte_auth
```

### **Problema: Cache Redis**
```bash
# Verificar conectividade
redis-cli -h conexao-redis -a $REDIS_PASS ping

# Verificar cache entries
redis-cli -h conexao-redis -a $REDIS_PASS KEYS "*jwt*"
```

---

## 📋 CHECKLIST PRÉ-DEPLOY

### **Configuração:**
- [ ] Secrets JWT configurados no Key Vault
- [ ] Database `conexao_sorte_auth` criado
- [ ] Redis cache configurado (database 0)
- [ ] CORS origins configuradas

### **Pipeline:**
- [ ] Tests unitários passando
- [ ] Security scan aprovado  
- [ ] Docker Compose validado
- [ ] OIDC secrets configurados no repo

### **Dependências:**
- [ ] MySQL funcionando (mysql-proxy:6033)
- [ ] Redis funcionando (conexao-redis:6379)
- [ ] Azure Key Vault acessível
- [ ] Rede conexao-network-swarm criada

---

## 🔄 DISASTER RECOVERY

### **Backup Crítico:**
- Database `conexao_sorte_auth`
- Redis cache (tokens ativos)
- JWT private/public keys

### **Recovery Order:**
1. Restore MySQL database
2. Restore Redis cache (opcional)
3. Verify JWT keys em Key Vault
4. Deploy service
5. Smoke test login flow

### **Fallback Strategy:**
- JWT keys podem ser regeneradas (invalidará tokens ativos)
- Cache pode ser reconstruído (performance impact)
- Database deve ser restaurado (dados críticos)

---

## 💡 OPERATIONAL NOTES

### **Performance Optimization:**
- Connection pooling configurado para R2DBC
- Cache Redis para JWT validation
- Query optimization com índices adequados
- Load balancing horizontal ready

### **Security Best Practices:**
- JWT rotation policy (3600s expiration)
- Password hashing com bcrypt (strength 12)
- Rate limiting para endpoints críticos
- Audit logs para todas operações sensíveis

### **Monitoramento 24/7:**
- Login success/failure rates
- JWT validation performance
- Cache hit ratios
- Database connection health
- Security breach attempts

---

**📅 Última Atualização**: Setembro 2025  
**🏷️ Versão**: 1.0  
**🔐 Criticidade**: ALTA - Serviço crítico para toda plataforma