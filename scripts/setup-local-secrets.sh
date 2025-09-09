#!/bin/bash

# Script para criar arquivos de secrets locais para uso com Docker Compose
# Autor: Equipe Conexão de Sorte
# Data: $(date +"%d/%m/%Y")

set -e

# Cores para output
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
BLUE="\033[0;34m"
NC="\033[0m" # No Color

# Diretório de secrets
SECRETS_DIR="./secrets"

# Função para criar diretório de secrets se não existir
create_secrets_dir() {
  if [ ! -d "$SECRETS_DIR" ]; then
    echo -e "${BLUE}Criando diretório de secrets...${NC}"
    mkdir -p "$SECRETS_DIR"
    chmod 700 "$SECRETS_DIR"
  fi
}

# Função para gerar senha aleatória
generate_random_password() {
  local length=${1:-32}
  LC_ALL=C tr -dc 'A-Za-z0-9!@#$%^&*()_+?><~' < /dev/urandom | head -c "$length"
}

# Função para criar arquivo de secret
create_secret_file() {
  local secret_name=$1
  local secret_value=$2
  local secret_file="$SECRETS_DIR/${secret_name}.txt"
  
  # Verificar se o arquivo já existe
  if [ -f "$secret_file" ]; then
    echo -e "${YELLOW}Arquivo $secret_file já existe. Deseja sobrescrever? (s/n)${NC}"
    read -r answer
    if [ "$answer" != "s" ]; then
      echo -e "${BLUE}Mantendo arquivo existente.${NC}"
      return
    fi
  fi
  
  # Criar arquivo com permissões restritas
  echo -n "$secret_value" > "$secret_file"
  chmod 600 "$secret_file"
  echo -e "${GREEN}Secret $secret_name criado com sucesso.${NC}"
}

# Função principal
main() {
  echo -e "${BLUE}=== Configuração de Secrets Locais para Docker Compose ===${NC}"
  
  create_secrets_dir
  
  # MySQL secrets
  echo -e "\n${YELLOW}Configurando secrets do MySQL...${NC}"
  create_secret_file "mysql_root_password" "$(generate_random_password 16)"
  create_secret_file "mysql_password" "$(generate_random_password 16)"
  
  # Database credentials
  echo -e "\n${YELLOW}Configurando credenciais do banco de dados...${NC}"
  create_secret_file "db_username" "conexao_sorte"
  create_secret_file "db_password" "$(generate_random_password 16)"
  
  # Redis password
  echo -e "\n${YELLOW}Configurando senha do Redis...${NC}"
  create_secret_file "redis_password" "$(generate_random_password 16)"
  
  # JWT secrets
  echo -e "\n${YELLOW}Configurando secrets JWT...${NC}"
  create_secret_file "jwt_secret" "$(generate_random_password 64)"
  create_secret_file "jwt_signing_key" "$(generate_random_password 64)"
  create_secret_file "jwt_verification_key" "$(generate_random_password 64)"
  create_secret_file "jwt_key_id" "$(uuidgen 2>/dev/null || echo "$(date +%s)-$(generate_random_password 8)")"
  
  # Encryption keys
  echo -e "\n${YELLOW}Configurando chaves de criptografia...${NC}"
  create_secret_file "encryption_master_key" "$(generate_random_password 32)"
  create_secret_file "encryption_backup_key" "$(generate_random_password 32)"
  
  # SSL keystore password
  echo -e "\n${YELLOW}Configurando senha do keystore SSL...${NC}"
  create_secret_file "ssl_keystore_password" "$(generate_random_password 16)"
  
  echo -e "\n${GREEN}Todos os secrets foram configurados com sucesso!${NC}"
  echo -e "${YELLOW}IMPORTANTE: Nunca compartilhe ou comite estes arquivos no repositório.${NC}"
  echo -e "${YELLOW}Os arquivos estão localizados em: $SECRETS_DIR${NC}"
  echo -e "${BLUE}Adicione o diretório $SECRETS_DIR ao .gitignore para evitar commits acidentais.${NC}"
}

# Executar função principal
main