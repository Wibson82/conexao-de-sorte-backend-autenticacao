package br.tec.facilitaservicos.autenticacao.configuracao;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * 🛡️ R2DBC URL NORMALIZER - PROGRAMAÇÃO DEFENSIVA 
 * ============================================================================
 * 
 * Componente de programação defensiva que automaticamente converte URLs JDBC
 * para formato R2DBC compatível, evitando falhas em produção.
 * 
 * PROBLEMA RESOLVIDO:
 * - Azure Key Vault pode conter URLs JDBC (jdbc:mysql://)
 * - R2DBC requer URLs no formato r2dbc (r2dbc:mysql://)
 * - Conversão automática e transparente
 * 
 * CONVERSÕES SUPORTADAS:
 * - jdbc:mysql:// -> r2dbc:mysql://
 * - jdbc:postgresql:// -> r2dbc:postgresql://
 * - jdbc:h2:// -> r2dbc:h2://
 * - jdbc:mariadb:// -> r2dbc:mariadb://
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Component
public class R2dbcUrlNormalizer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(R2dbcUrlNormalizer.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Propriedades R2DBC monitoradas
    private static final String SPRING_R2DBC_URL = "spring.r2dbc.url";
    private static final String CUSTOM_R2DBC_URL = "conexao-de-sorte-database-r2dbc-url";

    // Prefixos de protocolo
    private static final String JDBC_PREFIX = "jdbc:";
    private static final String R2DBC_PREFIX = "r2dbc:";

    // Padrões de conversão JDBC -> R2DBC
    private static final Pattern JDBC_MYSQL_PATTERN = Pattern.compile("^jdbc:mysql://");
    private static final Pattern JDBC_POSTGRESQL_PATTERN = Pattern.compile("^jdbc:postgresql://");
    private static final Pattern JDBC_H2_PATTERN = Pattern.compile("^jdbc:h2://");
    private static final Pattern JDBC_MARIADB_PATTERN = Pattern.compile("^jdbc:mariadb://");

    // Substituições R2DBC
    private static final String R2DBC_MYSQL_REPLACEMENT = "r2dbc:mysql://";
    private static final String R2DBC_POSTGRESQL_REPLACEMENT = "r2dbc:postgresql://";
    private static final String R2DBC_H2_REPLACEMENT = "r2dbc:h2://";
    private static final String R2DBC_MARIADB_REPLACEMENT = "r2dbc:mariadb://";

    // Padrão para mascarar senhas
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("://([^:]+):([^@]+)@");
    private static final String PASSWORD_MASK = "://$1:****@";

    // Mensagens de log
    private static final String LOG_CONVERSION_APPLIED = "🛡️ R2DBC URL Normalizer - Conversão automática aplicada:";
    private static final String LOG_PROPERTY = "   Propriedade: {}";
    private static final String LOG_ORIGINAL = "   Original: {}";
    private static final String LOG_CONVERTED = "   Convertida: {}";
    private static final String LOG_SUCCESS = "✅ R2DBC URL Normalizer - {} propriedades convertidas com sucesso";
    private static final String LOG_NO_CONVERSION = "🔍 R2DBC URL Normalizer - Nenhuma conversão necessária";
    private static final String LOG_GENERIC_CONVERSION = "⚠️ Conversão genérica aplicada para: {}";
    private static final String LOG_DRIVER_WARNING = "   Verifique se o driver R2DBC está disponível para este banco";

    // Nome da fonte de propriedades
    private static final String PROPERTY_SOURCE_NAME = "r2dbcUrlNormalizer";

    // Valores padrão
    private static final String NULL_VALUE = "null";
    
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // Programação defensiva: validação do evento
        if (event == null) {
            logger.warn("❌ Evento ApplicationEnvironmentPreparedEvent nulo recebido");
            return;
        }

        ConfigurableEnvironment environment = event.getEnvironment();

        // Programação defensiva: validação do environment
        if (environment == null) {
            logger.warn("❌ Environment nulo no evento ApplicationEnvironmentPreparedEvent");
            return;
        }

        // Propriedades que precisam ser verificadas e convertidas
        String[] r2dbcProperties = {
            SPRING_R2DBC_URL,
            CUSTOM_R2DBC_URL
        };

        Map<String, Object> normalizedProperties = new HashMap<>();
        boolean hasChanges = false;

        for (String property : r2dbcProperties) {
            // Programação defensiva: validação da propriedade
            if (property == null || property.trim().isEmpty()) {
                logger.warn("❌ Propriedade nula ou vazia encontrada na lista de propriedades R2DBC");
                continue;
            }

            try {
                String originalValue = environment.getProperty(property);

                if (originalValue != null && isJdbcUrl(originalValue)) {
                    String convertedValue = convertJdbcToR2dbc(originalValue);

                    // Programação defensiva: validação da conversão
                    if (convertedValue != null && !originalValue.equals(convertedValue)) {
                        normalizedProperties.put(property, convertedValue);
                        hasChanges = true;

                        logger.info(LOG_CONVERSION_APPLIED);
                        logger.info(LOG_PROPERTY, property);
                        logger.info(LOG_ORIGINAL, maskUrl(originalValue));
                        logger.info(LOG_CONVERTED, maskUrl(convertedValue));
                    }
                }
            } catch (Exception e) {
                logger.error("❌ Erro ao processar propriedade {}: {}", property, e.getMessage(), e);
            }
        }

        // Adicionar propriedades convertidas se houver mudanças
        if (hasChanges) {
            try {
                environment.getPropertySources().addFirst(
                    new MapPropertySource(PROPERTY_SOURCE_NAME, normalizedProperties)
                );

                logger.info(LOG_SUCCESS, normalizedProperties.size());
            } catch (Exception e) {
                logger.error("❌ Erro ao adicionar propriedades convertidas ao environment: {}", e.getMessage(), e);
            }
        } else {
            logger.debug(LOG_NO_CONVERSION);
        }
    }
    
    /**
     * 🔍 Verifica se a URL é uma URL JDBC que precisa ser convertida.
     * Implementa programação defensiva para ambiente de produção.
     */
    private boolean isJdbcUrl(String url) {
        // Programação defensiva: validação de entrada
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            String trimmedUrl = url.trim();
            return trimmedUrl.startsWith(JDBC_PREFIX) && !trimmedUrl.startsWith(R2DBC_PREFIX);
        } catch (Exception e) {
            logger.warn("❌ Erro ao verificar se URL é JDBC: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 🔄 Converte URL JDBC para formato R2DBC compatível.
     * Implementa programação defensiva para ambiente de produção.
     */
    private String convertJdbcToR2dbc(String jdbcUrl) {
        // Programação defensiva: validação de entrada
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return jdbcUrl;
        }

        try {
            String converted = jdbcUrl.trim();

            // MySQL
            if (JDBC_MYSQL_PATTERN.matcher(converted).find()) {
                converted = JDBC_MYSQL_PATTERN.matcher(converted).replaceFirst(R2DBC_MYSQL_REPLACEMENT);
            }
            // PostgreSQL
            else if (JDBC_POSTGRESQL_PATTERN.matcher(converted).find()) {
                converted = JDBC_POSTGRESQL_PATTERN.matcher(converted).replaceFirst(R2DBC_POSTGRESQL_REPLACEMENT);
            }
            // H2
            else if (JDBC_H2_PATTERN.matcher(converted).find()) {
                converted = JDBC_H2_PATTERN.matcher(converted).replaceFirst(R2DBC_H2_REPLACEMENT);
            }
            // MariaDB
            else if (JDBC_MARIADB_PATTERN.matcher(converted).find()) {
                converted = JDBC_MARIADB_PATTERN.matcher(converted).replaceFirst(R2DBC_MARIADB_REPLACEMENT);
            }
            // Conversão genérica para outros drivers
            else if (converted.startsWith(JDBC_PREFIX)) {
                converted = converted.replaceFirst("^" + Pattern.quote(JDBC_PREFIX), R2DBC_PREFIX);
                logger.warn(LOG_GENERIC_CONVERSION, maskUrl(jdbcUrl));
                logger.warn(LOG_DRIVER_WARNING);
            }

            return converted;
        } catch (Exception e) {
            logger.error("❌ Erro ao converter URL JDBC para R2DBC: {}", e.getMessage(), e);
            return jdbcUrl; // Retorna URL original em caso de erro
        }
    }
    
    /**
     * 🔒 Mascara informações sensíveis da URL para logs.
     * Implementa programação defensiva para ambiente de produção.
     */
    private String maskUrl(String url) {
        // Programação defensiva: validação de entrada
        if (url == null) {
            return NULL_VALUE;
        }

        if (url.trim().isEmpty()) {
            return url;
        }

        try {
            // Mascarar senha se presente: user:password@host -> user:****@host
            return PASSWORD_PATTERN.matcher(url).replaceAll(PASSWORD_MASK);
        } catch (Exception e) {
            logger.warn("❌ Erro ao mascarar URL: {}", e.getMessage());
            // Em caso de erro, retorna uma versão segura
            return "[URL_MASCARADA_POR_ERRO]";
        }
    }
}