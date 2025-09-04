package br.tec.facilitaservicos.autenticacao.dto;

public record Resposta2FAGeradoDTO(String codigoMasked, int ttlSeconds) {
    public static Resposta2FAGeradoDTO of(String masked, int ttl) {
        return new Resposta2FAGeradoDTO(masked, ttl);
    }
}

