package br.tec.facilitaservicos.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para verificação de código 2FA.
 */
public record Verificacao2FADTO(
    @NotBlank(message = "usuarioId é obrigatório")
    String usuarioId,
    
    @NotBlank(message = "codigo é obrigatório")
    @Pattern(regexp = "\\d{6}", message = "código deve ter 6 dígitos")
    String codigo
) {}