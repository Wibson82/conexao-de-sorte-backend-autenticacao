package br.tec.facilitaservicos.autenticacao.configuracao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;

import java.time.Duration;

/**
 * ============================================================================
 * 🛡️ STARTUP RESILIENCE CONFIGURATION - AUTENTICACAO
 * ============================================================================
 * 
 * Configuração defensiva para evitar travamentos durante startup da aplicação.
 * 
 * PROBLEMAS RESOLVIDOS:
 * - Timeouts de conexão com database durante startup
 * - Travamentos em configuração de beans R2DBC
 * - Timeout de health checks durante deployment
 * 
 * MEDIDAS DEFENSIVAS:
 * - Connection timeouts reduzidos
 * - Pool configurations otimizadas para startup rápido
 * - Fallbacks para conectividade falha
 * 
 * @author Sistema de Resilience - Autenticacao
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConditionalOnProperty(value = "app.resilience.startup.enabled", havingValue = "true", matchIfMissing = true)
public class StartupResilienceConfiguration {

    /**
     * 🛡️ Configuração otimizada de Connection Factory com timeouts reduzidos
     */
    @Bean
    @ConditionalOnProperty(value = "spring.r2dbc.url")
    public ConnectionFactory connectionFactory() {
        
        String url = System.getProperty("spring.r2dbc.url", System.getenv("SPRING_R2DBC_URL"));
        if (url == null) {
            // Fallback para URL padrão se não configurada
            url = "r2dbc:mysql://localhost:3306/conexao_auth";
        }
        
        ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(url)
            .mutate()
            // Timeouts otimizados para startup rápido
            .option(ConnectionFactoryOptions.CONNECT_TIMEOUT, Duration.ofSeconds(10))
            .option(ConnectionFactoryOptions.STATEMENT_TIMEOUT, Duration.ofSeconds(30))
            .build();

        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        
        // Pool configuration otimizada para startup
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
            .initialSize(1)  // Reduzido para startup mais rápido
            .maxSize(5)      // Reduzido para startup mais rápido
            .maxIdleTime(Duration.ofMinutes(5))
            .maxAcquireTime(Duration.ofSeconds(10))  // Timeout rápido para evitar travamentos
            .maxCreateConnectionTime(Duration.ofSeconds(10))
            .validationQuery("SELECT 1")
            .build();

        return new ConnectionPool(poolConfig);
    }
}