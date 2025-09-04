package br.tec.facilitaservicos.autenticacao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.autenticacao.dto.Resposta2FADTO;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

/**
 * Serviço de autenticação de dois fatores (2FA).
 */
@Service
public class TwoFactorService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorService.class);
    
    private static final String REDIS_2FA_PREFIX = "2fa:code:";
    private static final int CODE_LENGTH = 6;
    private static final int DEFAULT_TTL_MINUTES = 5;
    private final SecureRandom secureRandom = new SecureRandom();
    
    private final ReactiveStringRedisTemplate redisTemplate;
    private final KeyVaultService keyVaultService;
    
    public TwoFactorService(ReactiveStringRedisTemplate redisTemplate, 
                           KeyVaultService keyVaultService) {
        this.redisTemplate = redisTemplate;
        this.keyVaultService = keyVaultService;
    }
    
    /**
     * Gera código 2FA para usuário.
     */
    public Mono<Resposta2FADTO> gerarCodigo(String usuarioId, String canal) {
        logger.info("Gerando código 2FA para usuário: {} canal: {}", usuarioId, canal);
        
        return Mono.fromCallable(() -> {
            String codigo = generateSecureCode();
            String redisKey = REDIS_2FA_PREFIX + usuarioId;
            
            return Map.of("codigo", codigo, "key", redisKey);
        })
        .flatMap(data -> {
            String codigo = (String) data.get("codigo");
            String redisKey = (String) data.get("key");
            
            // Salvar código no Redis com TTL
            return redisTemplate.opsForValue()
                .set(redisKey, codigo, Duration.ofMinutes(DEFAULT_TTL_MINUTES))
                .then(Mono.fromCallable(() -> {
                    String codigoMasked = maskCode(codigo);
                    logger.info("Código 2FA gerado com sucesso para usuário: {}", usuarioId);
                    return Resposta2FADTO.sucesso(codigoMasked, DEFAULT_TTL_MINUTES * 60, canal);
                }));
        })
        .onErrorResume(throwable -> {
            logger.error("Erro ao gerar código 2FA para usuário: {}", usuarioId, throwable);
            return Mono.just(Resposta2FADTO.falha());
        });
    }
    
    /**
     * Verifica código 2FA do usuário.
     */
    public Mono<Boolean> verificarCodigo(String usuarioId, String codigo) {
        logger.info("Verificando código 2FA para usuário: {}", usuarioId);
        
        String redisKey = REDIS_2FA_PREFIX + usuarioId;
        
        return redisTemplate.opsForValue()
            .get(redisKey)
            .flatMap(storedCode -> {
                boolean valido = codigo.equals(storedCode);
                
                if (valido) {
                    // Remove código após verificação bem-sucedida
                    return redisTemplate.delete(redisKey)
                        .then(Mono.fromCallable(() -> {
                            logger.info("Código 2FA verificado com sucesso para usuário: {}", usuarioId);
                            return true;
                        }));
                } else {
                    logger.warn("Código 2FA inválido para usuário: {}", usuarioId);
                    return Mono.just(false);
                }
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                logger.warn("Código 2FA expirado ou não encontrado para usuário: {}", usuarioId);
                return false;
            }))
            .onErrorResume(throwable -> {
                logger.error("Erro ao verificar código 2FA para usuário: {}", usuarioId, throwable);
                return Mono.just(false);
            });
    }
    
    /**
     * Desabilita 2FA para usuário (admin only).
     */
    public Mono<Void> desabilitar2FA(String usuarioId) {
        logger.info("Desabilitando 2FA para usuário: {}", usuarioId);
        
        String redisKey = REDIS_2FA_PREFIX + usuarioId;
        
        return redisTemplate.delete(redisKey)
            .then(Mono.<Void>fromRunnable(() -> 
                logger.info("2FA desabilitado com sucesso para usuário: {}", usuarioId)))
            .onErrorResume(throwable -> {
                logger.error("Erro ao desabilitar 2FA para usuário: {}", usuarioId, throwable);
                return Mono.empty();
            });
    }
    
    private String generateSecureCode() {
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d", code);
    }
    
    private String maskCode(String code) {
        if (code.length() < 6) return "****";
        return code.substring(0, 2) + "****";
    }
    
    // Methods required by controllers
    public Mono<Resposta2FADTO> generateCode(Long userId, String channel) {
        return gerarCodigo(userId.toString(), channel);
    }
    
    public Mono<Boolean> verifyCode(Long userId, String code, String channel) {
        return verificarCodigo(userId.toString(), code);
    }
    
    public Mono<Void> disable(Long userId) {
        return desabilitar2FA(userId.toString());
    }
    
    public Mono<String> cachePing() {
        return redisTemplate.opsForValue()
            .set("ping:2fa", "pong", Duration.ofSeconds(5))
            .then(redisTemplate.opsForValue().get("ping:2fa"))
            .defaultIfEmpty("TIMEOUT");
    }
}