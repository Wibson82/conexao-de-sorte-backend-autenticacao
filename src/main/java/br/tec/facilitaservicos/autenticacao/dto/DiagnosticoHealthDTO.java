package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para diagnóstico completo de health.
 */
public record DiagnosticoHealthDTO(
    String status,
    LocalDateTime timestamp,
    Map<String, Object> cache,
    Map<String, Object> database,
    Map<String, Object> keyVault,
    long upTimeSegundos,
    String versao
) {}