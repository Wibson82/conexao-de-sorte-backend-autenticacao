package br.tec.facilitaservicos.usuario.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import br.tec.facilitaservicos.usuario.dto.TokenValidationResponseDTO;
import br.tec.facilitaservicos.usuario.dto.UserStatusDTO;
import br.tec.facilitaservicos.usuario.dto.UsuarioDTO;
import br.tec.facilitaservicos.usuario.entity.Usuario;
import br.tec.facilitaservicos.usuario.repository.UsuarioRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🔐 SERVIÇO DE USUÁRIOS
 * ============================================================================
 * 
 * Serviço responsável por fornecer informações de usuários para outros
 * microserviços de forma otimizada, com cache e resilience patterns.
 * 
 * Características:
 * - Cache inteligente para reduzir latência
 * - Circuit breakers para resiliência
 * - Tracking de sessões/status online
 * - Otimizado para alta performance
 * ============================================================================
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UsuarioRepository usuarioRepository;
    
    // Cache em memória para status online (complementar ao Redis)
    private final ConcurrentHashMap<Long, UserSessionInfo> userSessions = new ConcurrentHashMap<>();

    public UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * 👤 Busca informações do usuário por ID com cache.
     */
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "user-lookup", fallbackMethod = "fallbackGetUser")
    @Retry(name = "user-lookup")
    public Mono<UsuarioDTO> getUserById(Long userId) {
        logger.debug("👤 Buscando usuário: userId={}", userId);
        
        return usuarioRepository.findById(userId)
            .map(UsuarioDTO::from)
            .doOnSuccess(user -> {
                if (user != null) {
                    logger.debug("✅ Usuário encontrado: {}", user.toLogString());
                } else {
                    logger.debug("❌ Usuário não encontrado: userId={}", userId);
                }
            })
            .timeout(Duration.ofSeconds(2));
    }

    /**
     * 🟢 Obtém status online do usuário.
     */
    @CircuitBreaker(name = "user-status", fallbackMethod = "fallbackGetUserStatus")
    public Mono<UserStatusDTO> getUserStatus(Long userId) {
        logger.debug("🟢 Verificando status do usuário: userId={}", userId);
        
        UserSessionInfo sessionInfo = userSessions.get(userId);
        
        if (sessionInfo != null && sessionInfo.isActive()) {
            return Mono.just(UserStatusDTO.online(
                userId,
                sessionInfo.getStatusMessage(),
                sessionInfo.getDevice(),
                sessionInfo.getIpAddress()
            ));
        }
        
        // Buscar última atividade no banco se não estiver em sessão ativa
        return usuarioRepository.findById(userId)
            .map(usuario -> new UserStatusDTO(
                userId,
                false,
                usuario.getUltimoLogin() != null ? 
                    usuario.getUltimoLogin().atZone(java.time.ZoneOffset.UTC).toInstant() : 
                    Instant.now().minusSeconds(3600),
                null, null, null, null)) // Added nulls for other fields
            .switchIfEmpty(Mono.just(new UserStatusDTO(userId, false, Instant.now().minusSeconds(3600), null, null, null, null))) // Changed from builder
            .timeout(Duration.ofSeconds(1));
    }

    /**
     * 🔑 Obtém permissões do usuário.
     */
    @Cacheable(value = "user-permissions", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "user-permissions", fallbackMethod = "fallbackGetUserPermissions")
    public Mono<Map<String, Object>> getUserPermissions(Long userId) {
        logger.debug("🔑 Obtendo permissões do usuário: userId={}", userId);
        
        return usuarioRepository.findById(userId)
            .map(usuario -> Map.<String, Object>of(
                "userId", usuario.getId(),
                "roles", usuario.getRoles(),
                "permissions", usuario.getPermissoes(),
                "isAdmin", usuario.getRoles().contains("ROLE_ADMIN"),
                "isPremium", usuario.getRoles().contains("ROLE_PREMIUM"),
                "canChat", usuario.getPermissoes().contains("chat:send"),
                "canModerate", usuario.getPermissoes().contains("chat:moderate")
            ))
            .timeout(Duration.ofSeconds(2));
    }

    /**
     * 📊 Realiza health check dos componentes de validação.
     */
    public Mono<Map<String, Object>> performHealthCheck() {
        Instant startTime = Instant.now();
        
        return usuarioRepository.count()
            .map(userCount -> {
                Duration responseTime = Duration.between(startTime, Instant.now());
                
                return Map.<String, Object>of(
                    "status", "UP",
                    "component", "user-service", // Renamed component
                    "checks", Map.of(
                        "database", Map.of(
                            "status", "UP",
                            "responseTime", responseTime.toMillis() + "ms",
                            "userCount", userCount
                        ),
                        "cache", Map.of(
                            "status", "UP",
                            "activeSessions", userSessions.size()
                        ),
                        "jwt", Map.of(
                            "status", "UP",
                            "provider", "available"
                        )
                    ),
                    "timestamp", Instant.now().toString()
                );
            })
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(Map.of(
                "status", "DOWN",
                "component", "user-service", // Renamed component
                "error", "Health check failed",
                "timestamp", Instant.now().toString()
            ));
    }

    /**
     * 🔄 Atualiza status online do usuário.
     */
    public void updateUserOnlineStatus(Long userId, String username) {
        updateUserOnlineStatus(userId, username, null, null, null);
    }

    /**
     * 🔄 Atualiza status online do usuário com informações detalhadas.
     */
    public void updateUserOnlineStatus(Long userId, String username, String statusMessage, 
                                     String device, String ipAddress) {
        UserSessionInfo sessionInfo = new UserSessionInfo(
            userId,
            username,
            statusMessage,
            device,
            ipAddress,
            Instant.now()
        );
            
        userSessions.put(userId, sessionInfo);
        
        logger.debug("🔄 Status online atualizado: userId={}, device={}", userId, device);
    }

    /**
     * 🚪 Marca usuário como offline.
     */
    public void markUserOffline(Long userId) {
        userSessions.remove(userId);
        logger.debug("🚪 Usuário marcado como offline: userId={}", userId);
    }

    /**
     * 🧹 Limpa sessões expiradas (executado periodicamente).
     */
    public void cleanExpiredSessions() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(15)); // 15 minutos de inatividade
        
        int removed = 0;
        var iterator = userSessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().getLastActivity().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.info("🧹 Limpeza de sessões: {} sessões expiradas removidas", removed);
        }
    }

    // Métodos de fallback para resilience

    public Mono<TokenValidationResponseDTO> fallbackValidateToken(String token, Exception ex) {
        logger.warn("🔴 Fallback ativado para validação de token: {}", ex.getMessage());
        return Mono.just(TokenValidationResponseDTO.invalid("Token validation service temporarily unavailable"));
    }

    public Mono<UsuarioDTO> fallbackGetUser(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para busca de usuário {}: {}", userId, ex.getMessage());
        return Mono.just(UsuarioDTO.anonimo(userId));
    }

    public Mono<UserStatusDTO> fallbackGetUserStatus(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para status do usuário {}: {}", userId, ex.getMessage());
        return Mono.just(new UserStatusDTO(userId, false, Instant.now().minusSeconds(3600), null, null, null, null));
    }

    public Mono<Map<String, Object>> fallbackGetUserPermissions(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para permissões do usuário {}: {}", userId, ex.getMessage());
        return Mono.just(Map.of(
            "userId", userId,
            "roles", List.of("ROLE_USER"),
            "permissions", List.of("basic:access"),
            "isAdmin", false,
            "isPremium", false,
            "canChat", true,
            "canModerate", false
        ));
    }

    /**
     * Classe interna para informações de sessão do usuário.
     */
    private static class UserSessionInfo {
        private final Long userId;
        private final String username;
        private final String statusMessage;
        private final String device;
        private final String ipAddress;
        private final Instant lastActivity;

        // Manual constructor (replaces @Builder)
        public UserSessionInfo(Long userId, String username, String statusMessage, String device, String ipAddress, Instant lastActivity) {
            this.userId = userId;
            this.username = username;
            this.statusMessage = statusMessage;
            this.device = device;
            this.ipAddress = ipAddress;
            this.lastActivity = lastActivity;
        }

        public boolean isActive() {
            return lastActivity.isAfter(Instant.now().minus(Duration.ofMinutes(5)));
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getStatusMessage() { return statusMessage; }
        public String getDevice() { return device; }
        public String getIpAddress() { return ipAddress; }
        public Instant getLastActivity() { return lastActivity; }
    }
}