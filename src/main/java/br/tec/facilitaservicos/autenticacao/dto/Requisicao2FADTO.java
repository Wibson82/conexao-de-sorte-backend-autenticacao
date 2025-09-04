package br.tec.facilitaservicos.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para requisições de 2FA (Two-Factor Authentication).
 */
public record Requisicao2FADTO(
    @NotBlank(message = "usuarioId é obrigatório")
    String usuarioId,
    
    @Pattern(regexp = "email|sms", message = "canal deve ser 'email' ou 'sms'")
    String canal
) {}