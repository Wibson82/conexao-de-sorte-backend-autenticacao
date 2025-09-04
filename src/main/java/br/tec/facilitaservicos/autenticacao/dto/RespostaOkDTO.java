package br.tec.facilitaservicos.autenticacao.dto;

public record RespostaOkDTO(boolean ok) {
    public static RespostaOkDTO ok() { return new RespostaOkDTO(true); }
    public static RespostaOkDTO fail() { return new RespostaOkDTO(false); }
}

