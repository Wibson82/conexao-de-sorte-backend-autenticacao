package br.tec.facilitaservicos.autenticacao.aplicacao.servico;

import br.tec.facilitaservicos.autenticacao.dominio.entidade.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Serviço reativo para validação de senhas com sistema de pontuação detalhado.
 * Implementa verificações robustas de segurança compatível com Java 24.
 * 
 * Migrado de backend-original-2 para Spring WebFlux.
 */
@Service
public class ServicoValidacaoSenha {
    private static final Logger logger = LoggerFactory.getLogger(ServicoValidacaoSenha.class);

    // Constantes para validação de senha
    private static final int SENHA_TAMANHO_MINIMO = 8;
    private static final int SENHA_TAMANHO_MAXIMO = 64;
    private static final int PONTUACAO_MAXIMA_COMPRIMENTO = 40;
    private static final int BLOQUEIO_TEMPO_PADRAO_MINUTOS = 15;

    // Padrões de validação com Pattern - Thread-safe
    private static final Pattern PATTERN_LETRAS_MAIUSCULAS = Pattern.compile("[A-Z]");
    private static final Pattern PATTERN_LETRAS_MINUSCULAS = Pattern.compile("[a-z]");
    private static final Pattern PATTERN_NUMEROS = Pattern.compile("\\d");
    private static final Pattern PATTERN_CARACTERES_ESPECIAIS = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern PATTERN_COMPLETO = Pattern.compile(
            "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9])(?=\\S+$).{" + 
            SENHA_TAMANHO_MINIMO + "," + SENHA_TAMANHO_MAXIMO + "}$");

    private final PasswordEncoder passwordEncoder;

    // Configurações externalizadas
    @Value("${conexao-de-sorte.seguranca.senha.dias-expiracao:90}")
    private int diasExpiracao;

    @Value("${conexao-de-sorte.seguranca.senha.historico-tamanho:6}")
    private int tamanhoHistorico;

    @Value("${conexao-de-sorte.seguranca.senha.pontuacao-minima:70}")
    private int pontuacaoMinima;

    public ServicoValidacaoSenha(@NonNull PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, 
                "Password encoder não pode ser nulo");
    }

    /**
     * Valida a força da senha de forma reativa com mensagens detalhadas.
     *
     * @param senha Senha a ser validada
     * @return Mono<Void> que completa com sucesso ou erro
     */
    public Mono<Void> validarForcaSenha(@NonNull String senha) {
        return Mono.fromRunnable(() -> {
            if (senha.length() < SENHA_TAMANHO_MINIMO) {
                throw new IllegalArgumentException(
                    "A senha deve ter no mínimo " + SENHA_TAMANHO_MINIMO + " caracteres");
            }

            if (senha.length() > SENHA_TAMANHO_MAXIMO) {
                throw new IllegalArgumentException(
                    "A senha deve ter no máximo " + SENHA_TAMANHO_MAXIMO + " caracteres");
            }

            int pontuacao = calcularPontuacaoSenha(senha);
            if (pontuacao < pontuacaoMinima) {
                throw new IllegalArgumentException(
                    "Senha muito fraca (pontuação: " + pontuacao + "/" + pontuacaoMinima + 
                    "). Use combinações mais complexas.");
            }

            // Validações específicas com mensagens detalhadas
            validarRequisitosEspecificos(senha);
            logger.debug("Senha validada com sucesso - pontuação: {}", pontuacao);
        });
    }

    /**
     * Verifica de forma reativa se a senha atende às políticas de segurança.
     *
     * @param senha Senha a ser verificada
     * @return Mono<Boolean> true se a senha atender às políticas
     */
    public Mono<Boolean> senhaAtendePoliticas(@NonNull String senha) {
        return Mono.fromCallable(() -> {
            if (senha.isEmpty() || 
                senha.length() < SENHA_TAMANHO_MINIMO || 
                senha.length() > SENHA_TAMANHO_MAXIMO) {
                return false;
            }

            return contemLetrasMaiusculas(senha) &&
                   contemLetrasMinusculas(senha) &&
                   contemNumeros(senha) &&
                   contemCaracteresEspeciais(senha) &&
                   calcularPontuacaoSenha(senha) >= pontuacaoMinima;
        });
    }

    /**
     * Verifica de forma reativa se a senha do usuário expirou.
     *
     * @param usuario Usuário a verificar
     * @return Mono<Boolean> true se a senha estiver expirada
     */
    public Mono<Boolean> senhaExpirada(@NonNull Usuario usuario) {
        return Mono.fromCallable(() -> {
            if (usuario.getUltimaAlteracaoSenha() == null) {
                return true;
            }

            return LocalDateTime.now().isAfter(
                    usuario.getUltimaAlteracaoSenha().plusDays(diasExpiracao));
        });
    }

    /**
     * Calcula a pontuação de força da senha com sistema detalhado de 0-100 pontos.
     *
     * @param senha Senha a ser avaliada
     * @return Pontuação entre 0 e 100
     */
    public int calcularPontuacaoSenha(@NonNull String senha) {
        if (senha.isEmpty()) {
            return 0;
        }

        int pontuacao = 0;

        // Pontuação baseada no comprimento (máximo 40 pontos)
        pontuacao += Math.min(senha.length() * 4, PONTUACAO_MAXIMA_COMPRIMENTO);

        // Critérios de complexidade (10 pontos cada)
        pontuacao += contemNumeros(senha) ? 10 : 0;
        pontuacao += contemLetrasMinusculas(senha) ? 10 : 0;
        pontuacao += contemLetrasMaiusculas(senha) ? 10 : 0;
        pontuacao += contemCaracteresEspeciais(senha) ? BLOQUEIO_TEMPO_PADRAO_MINUTOS : 0;

        // Bônus por atender a todos os critérios
        if (PATTERN_COMPLETO.matcher(senha).matches()) {
            pontuacao += BLOQUEIO_TEMPO_PADRAO_MINUTOS;
        }

        // Limitar pontuação máxima
        return Math.min(pontuacao, 100);
    }

    /**
     * Verifica força da senha com detalhes.
     *
     * @param senha Senha a ser analisada
     * @return ResultadoForcaSenha com pontuação e sugestões
     */
    public Mono<ResultadoForcaSenha> analisarForcaSenha(@NonNull String senha) {
        return Mono.fromCallable(() -> {
            int pontuacao = calcularPontuacaoSenha(senha);
            
            var resultado = ResultadoForcaSenha.builder()
                    .pontuacao(pontuacao)
                    .forcaSenha(classificarForcaSenha(pontuacao))
                    .contemMaiusculas(contemLetrasMaiusculas(senha))
                    .contemMinusculas(contemLetrasMinusculas(senha))
                    .contemNumeros(contemNumeros(senha))
                    .contemEspeciais(contemCaracteresEspeciais(senha))
                    .tamanhoAdequado(senha.length() >= SENHA_TAMANHO_MINIMO && 
                                   senha.length() <= SENHA_TAMANHO_MAXIMO)
                    .build();

            resultado.setSugestoes(gerarSugestoesMelhoria(senha, resultado));
            return resultado;
        });
    }

    /**
     * Valida requisitos específicos da senha com mensagens individualizadas.
     */
    private void validarRequisitosEspecificos(@NonNull String senha) {
        if (!contemLetrasMaiusculas(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve conter pelo menos uma letra maiúscula");
        }
        if (!contemLetrasMinusculas(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve conter pelo menos uma letra minúscula");
        }
        if (!contemNumeros(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve conter pelo menos um número");
        }
        if (!contemCaracteresEspeciais(senha)) {
            throw new IllegalArgumentException(
                    "A senha deve conter pelo menos um caractere especial");
        }
    }

    private ForcaSenha classificarForcaSenha(int pontuacao) {
        if (pontuacao < 30) return ForcaSenha.MUITO_FRACA;
        if (pontuacao < 50) return ForcaSenha.FRACA;
        if (pontuacao < 70) return ForcaSenha.MEDIA;
        if (pontuacao < 90) return ForcaSenha.FORTE;
        return ForcaSenha.MUITO_FORTE;
    }

    private java.util.List<String> gerarSugestoesMelhoria(String senha, ResultadoForcaSenha resultado) {
        var sugestoes = new java.util.ArrayList<String>();

        if (senha.length() < SENHA_TAMANHO_MINIMO) {
            sugestoes.add("Aumentar o comprimento para pelo menos " + SENHA_TAMANHO_MINIMO + " caracteres");
        }
        if (!resultado.isContemMaiusculas()) {
            sugestoes.add("Adicionar letras maiúsculas (A-Z)");
        }
        if (!resultado.isContemMinusculas()) {
            sugestoes.add("Adicionar letras minúsculas (a-z)");
        }
        if (!resultado.isContemNumeros()) {
            sugestoes.add("Adicionar números (0-9)");
        }
        if (!resultado.isContemEspeciais()) {
            sugestoes.add("Adicionar caracteres especiais (!@#$%^&*)");
        }
        if (senha.length() > 12) {
            sugestoes.add("Considere usar frases longas com espaços ou hífen");
        }

        return sugestoes;
    }

    // Métodos auxiliares de verificação - otimizados para performance
    private boolean contemLetrasMaiusculas(@NonNull String senha) {
        return PATTERN_LETRAS_MAIUSCULAS.matcher(senha).find();
    }

    private boolean contemLetrasMinusculas(@NonNull String senha) {
        return PATTERN_LETRAS_MINUSCULAS.matcher(senha).find();
    }

    private boolean contemNumeros(@NonNull String senha) {
        return PATTERN_NUMEROS.matcher(senha).find();
    }

    private boolean contemCaracteresEspeciais(@NonNull String senha) {
        return PATTERN_CARACTERES_ESPECIAIS.matcher(senha).find();
    }

    // DTOs internos
    public enum ForcaSenha {
        MUITO_FRACA, FRACA, MEDIA, FORTE, MUITO_FORTE
    }

    public static class ResultadoForcaSenha {
        private int pontuacao;
        private ForcaSenha forcaSenha;
        private boolean contemMaiusculas;
        private boolean contemMinusculas;
        private boolean contemNumeros;
        private boolean contemEspeciais;
        private boolean tamanhoAdequado;
        private java.util.List<String> sugestoes;

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public int getPontuacao() { return pontuacao; }
        public ForcaSenha getForcaSenha() { return forcaSenha; }
        public boolean isContemMaiusculas() { return contemMaiusculas; }
        public boolean isContemMinusculas() { return contemMinusculas; }
        public boolean isContemNumeros() { return contemNumeros; }
        public boolean isContemEspeciais() { return contemEspeciais; }
        public boolean isTamanhoAdequado() { return tamanhoAdequado; }
        public java.util.List<String> getSugestoes() { return sugestoes; }

        public void setSugestoes(java.util.List<String> sugestoes) {
            this.sugestoes = sugestoes;
        }

        public static class Builder {
            private final ResultadoForcaSenha resultado = new ResultadoForcaSenha();

            public Builder pontuacao(int pontuacao) {
                resultado.pontuacao = pontuacao;
                return this;
            }

            public Builder forcaSenha(ForcaSenha forcaSenha) {
                resultado.forcaSenha = forcaSenha;
                return this;
            }

            public Builder contemMaiusculas(boolean contemMaiusculas) {
                resultado.contemMaiusculas = contemMaiusculas;
                return this;
            }

            public Builder contemMinusculas(boolean contemMinusculas) {
                resultado.contemMinusculas = contemMinusculas;
                return this;
            }

            public Builder contemNumeros(boolean contemNumeros) {
                resultado.contemNumeros = contemNumeros;
                return this;
            }

            public Builder contemEspeciais(boolean contemEspeciais) {
                resultado.contemEspeciais = contemEspeciais;
                return this;
            }

            public Builder tamanhoAdequado(boolean tamanhoAdequado) {
                resultado.tamanhoAdequado = tamanhoAdequado;
                return this;
            }

            public ResultadoForcaSenha build() {
                return resultado;
            }
        }
    }
}