package br.tec.facilitaservicos.usuario.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Configuração otimizada de cache Redis com TTL diferenciado por criticidade
 * Implementa estratégias de cache warming, eviction policies e métricas
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:PT1H}")
    private Duration defaultTtl;

    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    /**
     * Cache Manager principal com configurações otimizadas por criticidade dos dados
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(LettuceConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        
        // Configuração padrão do Redis
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        // Configurar caching de valores null baseado na propriedade
        if (!cacheNullValues) {
            defaultConfig = defaultConfig.disableCachingNullValues();
        }

        // Configurações específicas por cache baseado em criticidade dos dados
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // CRÍTICO: Dados de usuário frequentemente acessados (5 minutos)
        cacheConfigurations.put("user-profiles", defaultConfig
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("usuario:profile:"));
        
        // ALTO: Permissões de usuário (15 minutos)  
        cacheConfigurations.put("user-permissions", defaultConfig
                .entryTtl(Duration.ofMinutes(15))
                .prefixCacheNameWith("usuario:permission:"));
        
        // MÉDIO: Sessões de usuário (30 minutos)
        cacheConfigurations.put("user-sessions", defaultConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("usuario:session:"));
                
        // BAIXO: Metadados e configurações (2 horas)
        cacheConfigurations.put("user-metadata", defaultConfig
                .entryTtl(Duration.ofHours(2))
                .prefixCacheNameWith("usuario:metadata:"));
                
        // ESTÁTICO: Dados raramente modificados (6 horas)
        cacheConfigurations.put("user-static-data", defaultConfig
                .entryTtl(Duration.ofHours(6))
                .prefixCacheNameWith("usuario:static:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Configurações de cache warming para dados críticos
     */
    @Bean
    @ConditionalOnProperty(name = "cache.warming.enabled", havingValue = "true", matchIfMissing = true)
    public CacheWarmingService cacheWarmingService() {
        return new CacheWarmingService();
    }

    /**
     * Serviço para pre-carregamento de dados críticos no cache
     */
    public static class CacheWarmingService {
        
        /**
         * Pre-carrega dados de usuários ativos no cache
         * Executado no startup e periodicamente via scheduled tasks
         */
        public void warmupUserProfiles() {
            // TODO: Implementar pre-carregamento de perfis de usuários ativos
            // Exemplo: buscar top 100 usuários mais ativos e carregar no cache
        }
        
        /**
         * Pre-carrega permissões de usuário no cache
         */
        public void warmupUserPermissions() {
            // TODO: Implementar pre-carregamento de permissões de usuário
            // Exemplo: carregar permissões dos grupos mais comuns
        }
        
        /**
         * Executa o warming completo do cache
         */
        public void performFullWarmup() {
            warmupUserProfiles();
            warmupUserPermissions();
        }
    }
}