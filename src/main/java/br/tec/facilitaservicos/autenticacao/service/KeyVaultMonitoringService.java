package br.tec.facilitaservicos.autenticacao.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de monitoramento para Azure Key Vault.
 * Implementa logs estruturados, métricas e alertas para acesso aos segredos.
 */
@Service
public class KeyVaultMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(KeyVaultMonitoringService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT.KEYVAULT");

    // Métricas
    private final Counter secretAccessCounter;
    private final Counter secretErrorCounter;
    private final Timer secretAccessTimer;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    
    // Cache de estatísticas
    private final Map<String, SecretAccessStats> secretStats = new ConcurrentHashMap<>();
    
    public KeyVaultMonitoringService(MeterRegistry meterRegistry) {
        this.secretAccessCounter = Counter.builder("keyvault.secret.access")
            .description("Número de acessos aos segredos do Key Vault")
            .register(meterRegistry);
            
        this.secretErrorCounter = Counter.builder("keyvault.secret.error")
            .description("Número de erros ao acessar segredos do Key Vault")
            .register(meterRegistry);
            
        this.secretAccessTimer = Timer.builder("keyvault.secret.access.duration")
            .description("Tempo de acesso aos segredos do Key Vault")
            .register(meterRegistry);
            
        this.cacheHitCounter = Counter.builder("keyvault.cache.hit")
            .description("Número de cache hits para segredos")
            .register(meterRegistry);
            
        this.cacheMissCounter = Counter.builder("keyvault.cache.miss")
            .description("Número de cache misses para segredos")
            .register(meterRegistry);
    }
    
    /**
     * Monitora acesso a segredo com logs estruturados e métricas.
     */
    public <T> Mono<T> monitorSecretAccess(String secretName, String operation, Mono<T> secretMono) {
        Instant startTime = Instant.now();
        String correlationId = generateCorrelationId();
        
        // Configurar MDC para logs estruturados
        MDC.put("correlationId", correlationId);
        MDC.put("secretName", secretName);
        MDC.put("operation", operation);
        MDC.put("timestamp", startTime.toString());
        
        return secretMono
            .doOnSubscribe(subscription -> {
                logSecretAccessStart(secretName, operation, correlationId);
                secretAccessCounter.increment();
            })
            .doOnSuccess(result -> {
                Duration duration = Duration.between(startTime, Instant.now());
                secretAccessTimer.record(duration);
                
                if (result != null) {
                    logSecretAccessSuccess(secretName, operation, correlationId, duration);
                    updateSecretStats(secretName, true, duration);
                } else {
                    logSecretAccessEmpty(secretName, operation, correlationId, duration);
                }
            })
            .doOnError(error -> {
                Duration duration = Duration.between(startTime, Instant.now());
                secretErrorCounter.increment();
                logSecretAccessError(secretName, operation, correlationId, duration, error);
                updateSecretStats(secretName, false, duration);
            })
            .doFinally(signalType -> {
                MDC.clear();
            });
    }
    
    /**
     * Registra cache hit.
     */
    public void recordCacheHit(String secretName) {
        cacheHitCounter.increment();
        logger.debug("🎯 Cache HIT para secret: {}", secretName);
    }
    
    /**
     * Registra cache miss.
     */
    public void recordCacheMiss(String secretName) {
        cacheMissCounter.increment();
        logger.debug("❌ Cache MISS para secret: {}", secretName);
    }
    
    /**
     * Obtém estatísticas de acesso a um segredo.
     */
    public SecretAccessStats getSecretStats(String secretName) {
        return secretStats.getOrDefault(secretName, new SecretAccessStats());
    }
    
    /**
     * Obtém estatísticas gerais do Key Vault.
     */
    public KeyVaultStats getGeneralStats() {
        return new KeyVaultStats(
            secretAccessCounter.count(),
            secretErrorCounter.count(),
            cacheHitCounter.count(),
            cacheMissCounter.count(),
            secretStats.size()
        );
    }
    
    // Métodos privados de logging
    
    private void logSecretAccessStart(String secretName, String operation, String correlationId) {
        auditLogger.info("🔐 KEYVAULT_ACCESS_START: secret={}, operation={}, correlationId={}", 
            secretName, operation, correlationId);
    }
    
    private void logSecretAccessSuccess(String secretName, String operation, String correlationId, Duration duration) {
        auditLogger.info("✅ KEYVAULT_ACCESS_SUCCESS: secret={}, operation={}, correlationId={}, duration={}ms", 
            secretName, operation, correlationId, duration.toMillis());
    }
    
    private void logSecretAccessEmpty(String secretName, String operation, String correlationId, Duration duration) {
        auditLogger.warn("⚠️ KEYVAULT_ACCESS_EMPTY: secret={}, operation={}, correlationId={}, duration={}ms", 
            secretName, operation, correlationId, duration.toMillis());
    }
    
    private void logSecretAccessError(String secretName, String operation, String correlationId, Duration duration, Throwable error) {
        auditLogger.error("❌ KEYVAULT_ACCESS_ERROR: secret={}, operation={}, correlationId={}, duration={}ms, error={}", 
            secretName, operation, correlationId, duration.toMillis(), error.getMessage());
    }
    
    private void updateSecretStats(String secretName, boolean success, Duration duration) {
        secretStats.compute(secretName, (key, stats) -> {
            if (stats == null) {
                stats = new SecretAccessStats();
            }
            stats.recordAccess(success, duration);
            return stats;
        });
    }
    
    private String generateCorrelationId() {
        return "kv-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    // Classes de estatísticas
    
    public static class SecretAccessStats {
        private long totalAccesses = 0;
        private long successfulAccesses = 0;
        private long failedAccesses = 0;
        private Duration totalDuration = Duration.ZERO;
        private Duration maxDuration = Duration.ZERO;
        private Duration minDuration = Duration.ofDays(1); // Valor alto inicial
        
        public synchronized void recordAccess(boolean success, Duration duration) {
            totalAccesses++;
            if (success) {
                successfulAccesses++;
            } else {
                failedAccesses++;
            }
            
            totalDuration = totalDuration.plus(duration);
            if (duration.compareTo(maxDuration) > 0) {
                maxDuration = duration;
            }
            if (duration.compareTo(minDuration) < 0) {
                minDuration = duration;
            }
        }
        
        public double getSuccessRate() {
            return totalAccesses > 0 ? (double) successfulAccesses / totalAccesses : 0.0;
        }
        
        public Duration getAverageDuration() {
            return totalAccesses > 0 ? totalDuration.dividedBy(totalAccesses) : Duration.ZERO;
        }
        
        // Getters
        public long getTotalAccesses() { return totalAccesses; }
        public long getSuccessfulAccesses() { return successfulAccesses; }
        public long getFailedAccesses() { return failedAccesses; }
        public Duration getMaxDuration() { return maxDuration; }
        public Duration getMinDuration() { return minDuration.equals(Duration.ofDays(1)) ? Duration.ZERO : minDuration; }
    }
    
    public static class KeyVaultStats {
        private final double totalAccesses;
        private final double totalErrors;
        private final double cacheHits;
        private final double cacheMisses;
        private final int uniqueSecrets;
        
        public KeyVaultStats(double totalAccesses, double totalErrors, double cacheHits, double cacheMisses, int uniqueSecrets) {
            this.totalAccesses = totalAccesses;
            this.totalErrors = totalErrors;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.uniqueSecrets = uniqueSecrets;
        }
        
        public double getErrorRate() {
            return totalAccesses > 0 ? totalErrors / totalAccesses : 0.0;
        }
        
        public double getCacheHitRate() {
            double totalCacheRequests = cacheHits + cacheMisses;
            return totalCacheRequests > 0 ? cacheHits / totalCacheRequests : 0.0;
        }
        
        // Getters
        public double getTotalAccesses() { return totalAccesses; }
        public double getTotalErrors() { return totalErrors; }
        public double getCacheHits() { return cacheHits; }
        public double getCacheMisses() { return cacheMisses; }
        public int getUniqueSecrets() { return uniqueSecrets; }
    }
}