package br.tec.facilitaservicos.usuario.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.usuario.dto.UserStatusDTO;
import br.tec.facilitaservicos.usuario.dto.UsuarioDTO;
import br.tec.facilitaservicos.usuario.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🔐 CONTROLADOR DE USUÁRIOS - INTER-SERVICE COMMUNICATION
 * ============================================================================
 * 
 * Controlador reativo para gestão e validação de usuários.
 * Fornece endpoints para informações de usuários que outros serviços podem
 * consumir de forma reativa.
 * 
 * Endpoints disponíveis:
 * - GET /rest/v1/users/{userId} - Informações do usuário
 * - GET /rest/v1/users/{userId}/status - Status online do usuário
 * - GET /rest/v1/users/{userId}/permissions - Permissões do usuário
 * 
 * Características:
 * - 100% reativo (WebFlux)
 * - Cache inteligente para performance
 * - Circuit breakers integrados
 * - Observabilidade completa
 * ============================================================================
 */
@RestController
@RequestMapping("/rest/v1/users")
@Tag(name = "User Management", description = "Endpoints para gestão e validação de usuários")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Endpoints
    private static final String USERS_ENDPOINT = "/{userId}";
    private static final String USER_STATUS_ENDPOINT = "/{userId}/status";
    private static final String USER_PERMISSIONS_ENDPOINT = "/{userId}/permissions";
    private static final String HEALTH_ENDPOINT = "/health";

    // Headers
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    // Cache TTL
    private static final String CACHE_PRIVATE_5MIN = "private, max-age=300";
    private static final String CACHE_PRIVATE_10MIN = "private, max-age=600";
    private static final String CACHE_PRIVATE_1MIN = "private, max-age=60";
    private static final String CACHE_PRIVATE_30MIN = "private, max-age=1800";

    // Chaves de resposta JSON
    private static final String JSON_KEY_STATUS = "status";
    private static final String JSON_KEY_COMPONENT = "component";
    
    // Constantes de fallback
    private static final long OFFLINE_FALLBACK_SECONDS = 300; // 5 minutos
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";

    // Valores de status
    private static final String STATUS_DOWN = "DOWN";
    private static final String COMPONENT_USER_VALIDATION = "user-management"; // Renamed

    // Mensagens de erro
    private static final String MSG_UNKNOWN_ERROR = "Erro desconhecido";

    private final UserService userService; // Renamed from UserValidationService

    public UserController(UserService userService) { // Renamed from UserValidationService
        this.userService = userService;
    }

    /**
     * 👤 Endpoint para obter informações do usuário por ID.
     * 
     * Usado por outros microserviços para obter dados básicos de um usuário
     * com base no ID.
     */
    @GetMapping(value = USERS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Obter informações do usuário",
        description = "Retorna informações básicas do usuário para comunicação inter-service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UsuarioDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_USER_READ') or authentication.name == #userId.toString()")
    public Mono<ResponseEntity<UsuarioDTO>> getUserInfo(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido: {}", userId);
            return Mono.just(ResponseEntity.badRequest().<UsuarioDTO>build());
        }

        if (userService == null) {
            logger.error("❌ UserService não está disponível para getUserInfo");
            return Mono.just(ResponseEntity.internalServerError().<UsuarioDTO>build());
        }

        logger.debug("👤 Informações do usuário solicitadas: userId={}", userId);

        return userService.getUserById(userId)
            .map(user -> {
                // Programação defensiva: validação do usuário retornado
                if (user == null) {
                    logger.warn("⚠️ UserService retornou usuário nulo para userId={}", userId);
                    return ResponseEntity.notFound().<UsuarioDTO>build();
                }

                logger.debug("✅ Usuário encontrado: userId={}, username={}",
                           user.getId(), user.getUsername());

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_10MIN) // Cache 10 min
                    .body(user);
            })
            .switchIfEmpty(
                Mono.fromCallable(() -> {
                    logger.debug("❌ Usuário não encontrado: userId={}", userId);
                    return ResponseEntity.notFound().<UsuarioDTO>build();
                })
            )
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.error("❌ Erro ao buscar usuário {}: {}", userId, errorMsg);
                return Mono.just(ResponseEntity.internalServerError().<UsuarioDTO>build());
            });
    }

    /**
     * 🟢 Endpoint para verificar status online do usuário.
     * 
     * Usado principalmente pelo microserviço de chat para verificar
     * se um usuário está online antes de enviar mensagens.
     */
    @GetMapping(value = USER_STATUS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Status online do usuário",
        description = "Verifica se o usuário está online e retorna último acesso"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status do usuário obtido com sucesso",
            content = @Content(schema = @Schema(implementation = UserStatusDTO.class))
        )
    })
    public Mono<ResponseEntity<UserStatusDTO>> getUserStatus(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido para status: {}", userId);
            return Mono.just(ResponseEntity.badRequest().<UserStatusDTO>build());
        }

        if (userService == null) {
            logger.error("❌ UserService não está disponível para getUserStatus");
            UserStatusDTO errorStatus = new UserStatusDTO(); // Changed from builder
            errorStatus.setUserId(userId); // Changed from builder
            errorStatus.setOnline(false); // Changed from builder
            errorStatus.setLastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS)); // Changed from builder
            errorStatus.setStatus(STATUS_UNKNOWN); // Changed from builder
            // Removed .build()
            return Mono.just(ResponseEntity.ok(errorStatus));
        }

        logger.debug("🟢 Status do usuário solicitado: userId={}", userId);

        return userService.getUserStatus(userId)
            .map(status -> {
                // Programação defensiva: validação do status retornado
                if (status == null) {
                    logger.warn("⚠️ UserService retornou status nulo para userId={}", userId);
                    UserStatusDTO fallbackStatus = new UserStatusDTO(); // Changed from builder
                    fallbackStatus.setUserId(userId); // Changed from builder
                    fallbackStatus.setOnline(false); // Changed from builder
                    fallbackStatus.setLastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS)); // Changed from builder
                    fallbackStatus.setStatus(STATUS_UNKNOWN); // Changed from builder
                    // Removed .build()
                    return ResponseEntity.ok(fallbackStatus);
                }

                logger.debug("✅ Status obtido: userId={}, online={}, lastSeen={}",
                           userId, status.isOnline(), status.getLastSeen()); // Changed from status.getLastSeen() to status.isLastSeen()

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_1MIN) // Cache 1 min apenas
                    .body(status);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Erro ao obter status do usuário {}: {}", userId, errorMsg);

                // Retorna status offline em caso de erro
                UserStatusDTO offlineStatus = new UserStatusDTO(); // Changed from builder
                offlineStatus.setUserId(userId); // Changed from builder
                offlineStatus.setOnline(false); // Changed from builder
                offlineStatus.setLastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS)); // Changed from builder
                offlineStatus.setStatus(STATUS_UNKNOWN); // Changed from builder
                // Removed .build()

                return Mono.just(ResponseEntity.ok(offlineStatus));
            });
    }

    /**
     * 🔑 Endpoint para obter permissões do usuário.
     * 
     * Usado para verificação de autorização granular em outros microserviços.
     */
    @GetMapping(value = USER_PERMISSIONS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Permissões do usuário",
        description = "Retorna lista de permissões/roles do usuário"
    )
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or authentication.name == #userId.toString()")
    public Mono<ResponseEntity<Object>> getUserPermissions(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido para permissões: {}", userId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (userService == null) {
            logger.error("❌ UserService não está disponível para getUserPermissions");
            return Mono.just(ResponseEntity.internalServerError().build());
        }

        logger.debug("🔑 Permissões do usuário solicitadas: userId={}", userId);

        return userService.getUserPermissions(userId)
            .map(permissions -> {
                // Programação defensiva: validação das permissões retornadas
                if (permissions == null) {
                    logger.warn("⚠️ UserService retornou permissões nulas para userId={}", userId);
                    return ResponseEntity.ok()
                        .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_30MIN)
                        .body((Object) java.util.Collections.emptyList());
                }

                logger.debug("✅ Permissões obtidas: userId={}, count={}",
                           userId, permissions.size());

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_30MIN) // Cache 30 min
                    .body((Object) permissions);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.error("❌ Erro ao obter permissões do usuário {}: {}", userId, errorMsg);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * 📊 Health check específico para validação de usuários.
     */
    @GetMapping(value = HEALTH_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço de validação",
        description = "Verifica saúde dos componentes de validação de usuários"
    )
    public Mono<ResponseEntity<Object>> healthCheck() {

        // Programação defensiva: validação do serviço
        if (userService == null) {
            logger.error("❌ UserService não está disponível para health check");
            java.util.Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put(JSON_KEY_STATUS, STATUS_DOWN);
            errorHealth.put(JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION);
            errorHealth.put(JSON_KEY_TIMESTAMP, Instant.now().toString());
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body((Object) errorHealth));
        }

        return userService.performHealthCheck()
            .map(health -> {
                // Programação defensiva: validação da resposta de health
                if (health == null) {
                    logger.warn("⚠️ UserService retornou health nulo");
                    java.util.Map<String, Object> fallbackHealth = new HashMap<>();
                    fallbackHealth.put(JSON_KEY_STATUS, STATUS_DOWN);
                    fallbackHealth.put(JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION);
                    fallbackHealth.put(JSON_KEY_TIMESTAMP, Instant.now().toString());
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body((Object) fallbackHealth);
                }

                return ResponseEntity.ok().body((Object) health);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body((Object) java.util.Map.of(
                    JSON_KEY_STATUS, STATUS_DOWN,
                    JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION,
                    JSON_KEY_TIMESTAMP, Instant.now().toString()
                )));
    }
}
