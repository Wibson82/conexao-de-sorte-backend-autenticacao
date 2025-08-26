package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisi��o de login.
 * Record imut�vel seguindo padr�es reativos.
 */
@Schema(description = "Dados para requisi��o de login")
public record RequisicaoLoginDTO(
    
    @Schema(description = "Nome de usu�rio ou email", example = "usuario@exemplo.com")
    @NotBlank(message = "Usu�rio � obrigat�rio")
    @Size(min = 3, max = 255, message = "Usu�rio deve ter entre 3 e 255 caracteres")
    @JsonProperty("username")
    String usuario,
    
    @Schema(description = "Senha do usu�rio", example = "senha123")
    @NotBlank(message = "Senha � obrigat�ria")
    @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
    @JsonProperty("password")
    String senha
) {
    
    /**
     * Valida��o adicional no construtor compacto
     */
    public RequisicaoLoginDTO {
        // Trim dos valores de entrada
        usuario = usuario != null ? usuario.trim() : null;
        senha = senha != null ? senha.trim() : null;
        
        // Valida��es b�sicas adicionais
        if (usuario != null && usuario.isBlank()) {
            throw new IllegalArgumentException("Usu�rio n�o pode estar vazio");
        }
        if (senha != null && senha.isBlank()) {
            throw new IllegalArgumentException("Senha n�o pode estar vazia");
        }
    }
    
    /**
     * M�todo auxiliar para logs seguros (sem expor a senha)
     */
    public String toSecureString() {
        return String.format("RequisicaoLoginDTO{usuario='%s', senha='[PROTEGIDA]'}", usuario);
    }
    
    @Override
    public String toString() {
        return toSecureString();
    }
}