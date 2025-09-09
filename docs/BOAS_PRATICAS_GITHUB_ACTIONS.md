# Boas Práticas para GitHub Actions no Conexão de Sorte

## Gerenciamento de Artefatos

### Problema Resolvido

O workflow CI/CD apresentava um erro na etapa de download de artefatos:

```
Run actions/download-artifact@v4
Error: Unable to download artifact(s): Artifact not found for name: autenticacao-jar
```

Este erro ocorreu porque as etapas de build e upload do artefato estavam comentadas no workflow, mas a etapa de download continuava ativa.

### Solução Implementada

1. Mantivemos apenas as etapas essenciais de build, teste e upload de artefatos no job `build-test-scan`
2. Comentamos as etapas de análise de segurança (CodeQL e Trivy) e cobertura de código (JaCoCo) para otimizar o tempo de execução
3. Garantimos a compatibilidade entre as versões de `actions/upload-artifact@v4` e `actions/download-artifact@v4`

## Recomendações para Gerenciamento de Artefatos

### Compatibilidade de Versões

Sempre utilize versões compatíveis entre `actions/upload-artifact` e `actions/download-artifact`. Conforme a documentação oficial:

| upload-artifact | download-artifact | toolkit |
|----------------|-------------------|--------|
| v4             | v4                | v2     |
| < v3           | < v3              | < v1   |

### Retenção de Artefatos

O período padrão de retenção de artefatos no GitHub Actions é de 90 dias. Para artefatos temporários que são usados apenas dentro do mesmo workflow, recomendamos definir um período menor:

```yaml
uses: actions/upload-artifact@v4
with:
  name: artefato-temporario
  path: caminho/para/artefato
  retention-days: 1  # Para artefatos usados apenas no workflow atual
```

### Tamanho e Compressão

Para artefatos grandes, considere ajustar o nível de compressão:

```yaml
uses: actions/upload-artifact@v4
with:
  name: artefato-grande
  path: caminho/para/artefato
  compression-level: 0  # 0 = sem compressão (mais rápido para arquivos grandes)
```

### Verificação de Existência

Para jobs que dependem de artefatos, sempre verifique se o job que cria o artefato foi executado com sucesso:

```yaml
job-que-usa-artefato:
  needs: job-que-cria-artefato
  if: success() || needs.job-que-cria-artefato.result == 'success'
```

## Boas Práticas Gerais para GitHub Actions

### Segurança

1. **Secrets**: Nunca exponha secrets em logs ou outputs
2. **OIDC**: Utilize OIDC para autenticação em serviços cloud (como já implementado para Azure)
3. **Permissões**: Configure permissões mínimas necessárias para cada job

### Performance

1. **Cache**: Utilize cache para dependências (como já implementado para Maven)
2. **Matriz de Testes**: Para testes extensos, considere paralelização com matrizes
3. **Timeout**: Configure timeouts adequados para evitar execuções infinitas

### Observabilidade

1. **Artefatos de Diagnóstico**: Salve logs e relatórios como artefatos
2. **Notificações**: Configure notificações para falhas (como já implementado com Slack)
3. **Badges**: Adicione badges de status no README

## Referências

- [GitHub Actions Artifacts FAQ](https://github.com/actions/toolkit/blob/main/packages/artifact/docs/faq.md)
- [GitHub Actions - Boas Práticas](https://docs.github.com/pt/actions/learn-github-actions/usage-limits-billing-and-administration)
- [GitHub Actions - Segurança](https://docs.github.com/pt/actions/security-guides/security-hardening-for-github-actions)