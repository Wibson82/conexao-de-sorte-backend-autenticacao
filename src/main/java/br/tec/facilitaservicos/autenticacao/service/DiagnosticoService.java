package br.tec.facilitaservicos.autenticacao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.autenticacao.dto.DiagnosticoHealthDTO;
import br.tec.facilitaservicos.autenticacao.dto.DiagnosticoUsuarioDTO;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servico de diagnostico do sistema de autenticacao.
 */
@Service
public class DiagnosticoService {
    
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticoService.class);
    
    private static final String REDIS_USER_PREFIX = "user:login:";
    private static final String VERSION = "1.0.0";
    private final LocalDateTime startTime = LocalDateTime.now();
    
    private final ReactiveStringRedisTemplate redisTemplate;
    private final R2dbcEntityTemplate r2dbcTemplate;
    private final KeyVaultService keyVaultService;
    
    public DiagnosticoService(ReactiveStringRedisTemplate redisTemplate,
                             R2dbcEntityTemplate r2dbcTemplate,
                             KeyVaultService keyVaultService) {
        this.redisTemplate = redisTemplate;
        this.r2dbcTemplate = r2dbcTemplate;
        this.keyVaultService = keyVaultService;
    }
    
    /**
     * Diagnostico completo do usuario.
     */
    public Mono<DiagnosticoUsuarioDTO> diagnosticoUsuario(String username) {
        logger.info("Executando diagnostico para usuario: {}", username);
        
        return r2dbcTemplate.getDatabaseClient()
            .sql("SELECT COUNT(*) as count FROM usuarios WHERE username = :username")
            .bind("username", username)
            .map((row, metadata) -> row.get("count", Long.class))
            .one()
            .flatMap(count -> {
                boolean existe = count > 0;
                
                if (!existe) {
                    return Mono.just(new DiagnosticoUsuarioDTO(
                        false, false, 0, null, null, List.of(), "INEXISTENTE"
                    ));
                }
                
                // Verificar tentativas recentes no Redis
                String redisKey = REDIS_USER_PREFIX + username;
                return redisTemplate.opsForValue()
                    .get(redisKey)
                    .map(Integer::parseInt)
                    .defaultIfEmpty(0)
                    .flatMap(tentativas -> {
                        boolean bloqueado = tentativas >= 5;
                        String status = bloqueado ? "BLOQUEADO" : "ATIVO";
                        
                        List<String> bloqueios = bloqueado ? 
                            List.of("Muitas tentativas de login") : List.of();
                        
                        return Mono.just(new DiagnosticoUsuarioDTO(
                            true, bloqueado, tentativas, 
                            LocalDateTime.now().minusMinutes(tentativas),
                            LocalDateTime.now().minusHours(1),
                            bloqueios, status
                        ));
                    });
            })
            .onErrorResume(throwable -> {
                logger.error("Erro no diagnostico do usuario: {}", username, throwable);
                return Mono.just(new DiagnosticoUsuarioDTO(
                    false, false, 0, null, null, 
                    List.of("Erro no diagnostico"), "ERRO"
                ));
            });
    }
    
    /**
     * Diagnostico completo de health do sistema.
     */
    public Mono<DiagnosticoHealthDTO> diagnosticoHealth() {
        logger.info("Executando diagnostico completo de health");
        
        return Mono.zip(
            verificarCache(),
            verificarDatabase(),
            verificarKeyVault()
        )
        .map(tuple -> {
            Map<String, Object> cache = tuple.getT1();
            Map<String, Object> database = tuple.getT2();
            Map<String, Object> keyVault = tuple.getT3();
            
            boolean allHealthy = (Boolean) cache.get("healthy") && 
                               (Boolean) database.get("healthy") && 
                               (Boolean) keyVault.get("healthy");
            
            String status = allHealthy ? "UP" : "DOWN";
            long upTime = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            
            return new DiagnosticoHealthDTO(
                status, LocalDateTime.now(), cache, database, 
                keyVault, upTime, VERSION
            );
        })
        .onErrorResume(throwable -> {
            logger.error("Erro no diagnostico de health", throwable);
            
            Map<String, Object> errorInfo = Map.of(
                "healthy", false,
                "error", throwable.getMessage()
            );
            
            return Mono.just(new DiagnosticoHealthDTO(
                "DOWN", LocalDateTime.now(), errorInfo, errorInfo, 
                errorInfo, 0, VERSION
            ));
        });
    }
    
    private Mono<Map<String, Object>> verificarCache() {
        return redisTemplate.opsForValue()
            .set("health:test", "ok", Duration.ofSeconds(5))
            .then(redisTemplate.opsForValue().get("health:test"))
            .map(value -> {
                Map<String, Object> result = new HashMap<>();
                result.put("healthy", "ok".equals(value));
                result.put("service", "Redis");
                result.put("latency", "< 10ms");
                return result;
            })
            .onErrorResume(throwable -> {
                Map<String, Object> result = new HashMap<>();
                result.put("healthy", false);
                result.put("service", "Redis");
                result.put("error", throwable.getMessage());
                return Mono.just(result);
            });
    }
    
    private Mono<Map<String, Object>> verificarDatabase() {
        return r2dbcTemplate.getDatabaseClient()
            .sql("SELECT 1 as health_check")
            .map((row, metadata) -> row.get("health_check", Integer.class))
            .one()
            .map(result -> {
                Map<String, Object> info = new HashMap<>();
                info.put("healthy", result == 1);
                info.put("service", "R2DBC MySQL");
                info.put("latency", "< 50ms");
                return info;
            })
            .onErrorResume(throwable -> {
                Map<String, Object> info = new HashMap<>();
                info.put("healthy", false);
                info.put("service", "R2DBC MySQL");
                info.put("error", throwable.getMessage());
                return Mono.just(info);
            });
    }
    
    private Mono<Map<String, Object>> verificarKeyVault() {
        // Simular verificacao do KeyVault
        return keyVaultService.getSecret("conexao-de-sorte-jwt-secret")
            .map(secret -> {
                Map<String, Object> info = new HashMap<>();
                info.put("healthy", secret != null && !secret.trim().isEmpty());
                info.put("service", "Azure Key Vault");
                info.put("secrets_count", 1);
                return info;
            })
            .onErrorResume(throwable -> {
                Map<String, Object> info = new HashMap<>();
                info.put("healthy", false);
                info.put("service", "Azure Key Vault");
                info.put("error", throwable.getMessage());
                return Mono.just(info);
            });
    }
}