package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para diagnóstico de usuário.
 */
public record DiagnosticoUsuarioDTO(
    boolean existe,
    boolean bloqueado,
    int tentativasRecentes,
    LocalDateTime ultimaTentativa,
    LocalDateTime ultimoSucesso,
    List<String> bloqueios,
    String status
) {}