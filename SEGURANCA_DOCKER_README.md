# Segurança em Contêineres Docker - Conexão de Sorte

## Visão Geral

Este documento descreve as melhorias de segurança implementadas nos arquivos de configuração Docker do projeto Conexão de Sorte, seguindo as melhores práticas de segurança para contêineres.

## Melhorias Implementadas

### 1. Remoção de Segredos Expostos

#### Dockerfile
- Removidos ARGs e ENVs que continham segredos (senhas, chaves, tokens)
- Implementado suporte a BuildKit secrets para injeção segura de credenciais durante o build
- Adicionados comentários explicativos sobre o uso correto de segredos

#### docker-compose.yaml
- Substituídas variáveis de ambiente com segredos por referências a arquivos de secrets
- Implementada seção `secrets` para definir fontes de segredos
- Atualizada configuração de healthcheck para usar arquivos de secrets

#### GitHub Actions Workflow
- Implementado uso de Docker Buildx com suporte a secrets
- Melhorado uso de OIDC para autenticação segura no Azure
- Adicionada limpeza de arquivos temporários de secrets

### 2. Melhorias de Metadados e Rastreabilidade

- Adicionados labels OCI padronizados (`org.opencontainers.image.*`)
- Incluídas informações de build, versão e origem
- Adicionados labels específicos da aplicação

## Como Usar

### Configuração de Secrets Locais

Para desenvolvimento local, execute o script de configuração de secrets:

```bash
./scripts/setup-local-secrets.sh
```

Este script irá:
1. Criar o diretório `./secrets/` (já incluído no .gitignore)
2. Gerar senhas aleatórias para todos os serviços
3. Criar arquivos de secrets com permissões restritas

### Build com BuildKit

Para construir a imagem usando BuildKit e secrets:

```bash
DOCKER_BUILDKIT=1 docker build \
  --secret id=db_username,src=./secrets/db_username.txt \
  --secret id=db_password,src=./secrets/db_password.txt \
  --secret id=redis_password,src=./secrets/redis_password.txt \
  --secret id=jwt_secret,src=./secrets/jwt_secret.txt \
  --secret id=jwt_signing_key,src=./secrets/jwt_signing_key.txt \
  --secret id=jwt_verification_key,src=./secrets/jwt_verification_key.txt \
  -t conexao-de-sorte/auth-service:latest .
```

### Execução com Docker Compose

Para iniciar os serviços com Docker Compose:

```bash
# Primeiro, configure os secrets locais
./scripts/setup-local-secrets.sh

# Inicie os serviços
docker-compose up -d
```

## Boas Práticas de Segurança

1. **Nunca comite segredos** no repositório Git
2. **Nunca use ARG ou ENV** para segredos no Dockerfile
3. **Sempre use BuildKit secrets** para injetar segredos durante o build
4. **Sempre use Docker Compose secrets** ou variáveis de ambiente em runtime
5. **Verifique regularmente** por segredos expostos usando ferramentas como GitLeaks

## Verificação de Segurança

Para verificar se há segredos expostos nos arquivos Docker:

```bash
# Verificar Dockerfile
grep -E "(PASSWORD|SECRET|KEY|TOKEN|ENCRYPTION|JWT|SSL)" Dockerfile

# Verificar docker-compose.yaml
grep -E "(PASSWORD|SECRET|KEY|TOKEN|ENCRYPTION|JWT|SSL)" docker-compose.yaml
```

## Referências

- [Docker BuildKit Secrets](https://docs.docker.com/engine/reference/commandline/buildx_build/#secret)
- [Docker Compose Secrets](https://docs.docker.com/compose/use-secrets/)
- [OCI Image Spec](https://github.com/opencontainers/image-spec/blob/main/annotations.md)
- [GitHub Actions OIDC](https://docs.github.com/pt/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-cloud-providers)