package br.tec.facilitaservicos.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequisicaoValidar2FADTO(
        @NotNull Long usuarioId,
        @NotBlank String canal,
        @NotBlank String codigo
) {}

