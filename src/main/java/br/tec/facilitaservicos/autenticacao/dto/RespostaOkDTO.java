package br.tec.facilitaservicos.autenticacao.dto;

public record RespostaOkDTO(boolean ok) {
    public static RespostaOkDTO sucesso() { return new RespostaOkDTO(true); }
    public static RespostaOkDTO falha() { return new RespostaOkDTO(false); }
}

