#!/bin/bash

# Script para validar nomes de imagens GHCR
# Uso: ./validate-ghcr-images.sh

set -euo pipefail

echo "🔍 Validando nomes de imagens no GHCR..."

# Definir o repositório correto
REPO_OWNER="wibson82"
REPO_NAME="conexao-de-sorte-backend-autenticacao"
CORRECT_IMAGE_BASE="ghcr.io/${REPO_OWNER}/${REPO_NAME}"

echo "📋 Nome base esperado: ${CORRECT_IMAGE_BASE}"

# Verificar arquivos Docker Compose
echo "🐳 Verificando docker-compose.yml..."
if grep -q "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" docker-compose.yml 2>/dev/null; then
    echo "❌ ERRO: Nome duplicado encontrado em docker-compose.yml"
    grep "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" docker-compose.yml
    exit 1
elif grep -q "${CORRECT_IMAGE_BASE}" docker-compose.yml 2>/dev/null; then
    echo "✅ docker-compose.yml: Nome correto"
else
    echo "⚠️  docker-compose.yml: Nenhuma referência GHCR encontrada"
fi

echo "🐳 Verificando docker-compose-microservices.yml..."
if grep -q "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" docker-compose-microservices.yml 2>/dev/null; then
    echo "❌ ERRO: Nome duplicado encontrado em docker-compose-microservices.yml"
    grep "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" docker-compose-microservices.yml
    exit 1
elif grep -q "${CORRECT_IMAGE_BASE}" docker-compose-microservices.yml 2>/dev/null; then
    echo "✅ docker-compose-microservices.yml: Nome correto"
else
    echo "⚠️  docker-compose-microservices.yml: Nenhuma referência GHCR encontrada"
fi

# Verificar GitHub Actions
echo "🔧 Verificando .github/workflows/ci-cd.yml..."
if grep -q "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" .github/workflows/ci-cd.yml 2>/dev/null; then
    echo "❌ ERRO: Nome duplicado encontrado em ci-cd.yml"
    grep "ghcr.io.*/${REPO_NAME}/${REPO_NAME}" .github/workflows/ci-cd.yml
    exit 1
elif grep -q "ghcr.io/\$REPO_LOWER:\${TIMESTAMP}" .github/workflows/ci-cd.yml 2>/dev/null; then
    echo "✅ ci-cd.yml: Configuração correta usando \$REPO_LOWER"
else
    echo "⚠️  ci-cd.yml: Configuração de imagem não encontrada"
fi

echo ""
echo "🎉 Validação concluída!"
echo "📖 Nome correto para usar: ${CORRECT_IMAGE_BASE}:latest"
echo "📖 Para uma tag específica: ${CORRECT_IMAGE_BASE}:14-09-2025-07-43"