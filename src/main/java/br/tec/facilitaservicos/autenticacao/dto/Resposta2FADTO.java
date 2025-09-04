package br.tec.facilitaservicos.autenticacao.dto;

/**
 * DTO para resposta de geração de código 2FA.
 */
public record Resposta2FADTO(
    String codigoMasked,
    int ttlSegundos,
    String canal,
    boolean sucesso
) {
    public static Resposta2FADTO sucesso(String codigoMasked, int ttl, String canal) {
        return new Resposta2FADTO(codigoMasked, ttl, canal, true);
    }
    
    public static Resposta2FADTO falha() {
        return new Resposta2FADTO(null, 0, null, false);
    }
}