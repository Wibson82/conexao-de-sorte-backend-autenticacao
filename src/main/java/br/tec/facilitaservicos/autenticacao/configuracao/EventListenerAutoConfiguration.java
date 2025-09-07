package br.tec.facilitaservicos.autenticacao.configuracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * 🛡️ AUTO-CONFIGURAÇÃO DE EVENT LISTENERS - PROGRAMAÇÃO DEFENSIVA
 * ============================================================================
 * 
 * Garante que os ApplicationListeners essenciais sejam registrados mesmo
 * se o mecanismo spring.factories falhar por algum motivo.
 * 
 * Esta é uma medida de programação defensiva para ambiente de produção,
 * garantindo que componentes críticos como R2dbcUrlNormalizer sempre
 * sejam carregados.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
public class EventListenerAutoConfiguration {

    /**
     * 🛡️ Registra R2dbcUrlNormalizer como bean se não existir.
     * 
     * Esta configuração defensiva garante que o normalizer sempre
     * seja disponível, mesmo se o spring.factories não for processado.
     */
    @Bean
    @ConditionalOnMissingBean(R2dbcUrlNormalizer.class)
    public R2dbcUrlNormalizer r2dbcUrlNormalizer() {
        return new R2dbcUrlNormalizer();
    }
}