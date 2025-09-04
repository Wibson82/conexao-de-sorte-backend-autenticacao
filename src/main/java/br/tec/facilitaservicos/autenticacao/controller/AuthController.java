package br.tec.facilitaservicos.autenticacao.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import br.tec.facilitaservicos.autenticacao.dto.RequisicaoIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoLoginDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoRefreshDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaTokenDTO;
import br.tec.facilitaservicos.autenticacao.dto.Requisicao2FADTO;
import br.tec.facilitaservicos.autenticacao.dto.Resposta2FADTO;
import br.tec.facilitaservicos.autenticacao.dto.Verificacao2FADTO;
import br.tec.facilitaservicos.autenticacao.dto.DiagnosticoUsuarioDTO;
import br.tec.facilitaservicos.autenticacao.dto.DiagnosticoHealthDTO;
import br.tec.facilitaservicos.autenticacao.service.AuthService;
import br.tec.facilitaservicos.autenticacao.service.TwoFactorService;
import br.tec.facilitaservicos.autenticacao.service.DiagnosticoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * 🔐 CONTROLADOR DE AUTENTICAÇÃO - ENTERPRISE GRADE WEBFLUX
 * ============================================================================
 * 
 * Controlador reativo para autenticação OAuth2/OpenID Connect.
 * Implementa endpoints seguros e reativos para:
 * - Geração de tokens (login)
 * - Renovação de tokens (refresh)
 * - Introspecção de tokens (validação)
 * - Revogação de tokens (logout)
 * 
 * Características:
 * - 100% reativo (WebFlux)
 * - Conformidade com RFCs OAuth2/OpenID Connect
 * - Rate limiting integrado
 * - Headers de segurança apropriados
 * - Auditoria e observabilidade
 * - Tratamento de erros padronizado
 * 
 * ============================================================================
 */
@RestController
@RequestMapping("/rest/v1/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação OAuth2/OpenID Connect")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Endpoints
    private static final String TOKEN_ENDPOINT = "/token";
    private static final String REFRESH_ENDPOINT = "/refresh";
    private static final String INTROSPECT_ENDPOINT = "/introspect";
    private static final String REVOKE_ENDPOINT = "/revoke";
    private static final String HEALTH_ENDPOINT = "/health";

    // Headers de segurança
    private static final String CACHE_CONTROL_NO_STORE = "no-store";
    private static final String PRAGMA_NO_CACHE = "no-cache";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String HEADER_PRAGMA = "Pragma";

    // Headers de cliente
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";

    // Chaves de resposta JSON
    private static final String JSON_KEY_ERROR = "error";
    private static final String JSON_KEY_ERROR_DESCRIPTION = "error_description";
    private static final String JSON_KEY_STATUS = "status";
    private static final String JSON_KEY_SERVICE = "service";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";
    private static final String JSON_KEY_REVOKED = "revoked";
    private static final String JSON_KEY_MESSAGE = "message";

    // Valores de erro OAuth2
    private static final String ERROR_INVALID_GRANT = "invalid_grant";
    private static final String ERROR_INVALID_REQUEST = "invalid_request";
    private static final String ERROR_SERVER_ERROR = "server_error";

    // Mensagens de erro
    private static final String MSG_INVALID_CREDENTIALS = "Credenciais inválidas";
    private static final String MSG_INVALID_REFRESH_TOKEN = "Refresh token inválido ou expirado";
    private static final String MSG_TOKEN_REVOKED = "Token revogado com sucesso";
    private static final String MSG_TOKEN_REVOKED_SIMPLE = "Token revogado";
    private static final String MSG_SERVICE_UNAVAILABLE = "Serviço temporariamente indisponível";
    private static final String MSG_UNKNOWN_ERROR = "Erro desconhecido";
    private static final String MSG_UNKNOWN_USER = "unknown";

    // Valores de status
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String SERVICE_NAME = "authentication";
    private static final String CLIENT_IP_UNKNOWN = "unknown";

    // Separadores
    private static final String COMMA_SEPARATOR = ",";

    private final AuthService authService;
    private final TwoFactorService twoFactorService;
    private final DiagnosticoService diagnosticoService;
    
    public AuthController(AuthService authService, 
                         TwoFactorService twoFactorService,
                         DiagnosticoService diagnosticoService) {
        this.authService = authService;
        this.twoFactorService = twoFactorService;
        this.diagnosticoService = diagnosticoService;
    }

    /**
     * 🛡️ Método helper para criar respostas de erro padronizadas.
     * Implementa programação defensiva para ambiente de produção.
     */
    private ResponseEntity<Object> createErrorResponse(HttpStatus status, String error, String description) {
        if (error == null || error.trim().isEmpty()) {
            error = ERROR_SERVER_ERROR;
        }
        if (description == null || description.trim().isEmpty()) {
            description = MSG_SERVICE_UNAVAILABLE;
        }

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put(JSON_KEY_ERROR, error);
        errorBody.put(JSON_KEY_ERROR_DESCRIPTION, description);

        return ResponseEntity.status(status)
            .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
            .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
            .body(errorBody);
    }

    /**
     * 🛡️ Método helper para criar respostas de sucesso padronizadas.
     */
    private ResponseEntity<Object> createSuccessResponse(Object body) {
        if (body == null) {
            body = Map.of(JSON_KEY_MESSAGE, "Operação realizada com sucesso");
        }

        return ResponseEntity.ok()
            .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
            .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
            .body(body);
    }
    
    /**
     * 🔐 Endpoint de autenticação (geração de token).
     * 
     * Implementa o fluxo OAuth2 Resource Owner Password Credentials Grant.
     */
    @PostMapping(value = TOKEN_ENDPOINT,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Autenticação de usuário",
        description = "Realiza autenticação e retorna access token e refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaTokenDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Conta bloqueada",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Muitas tentativas de login",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Mono<ResponseEntity<Object>> token(
            @Valid @RequestBody RequisicaoLoginDTO loginRequest,
            ServerWebExchange exchange) {

        // Programação defensiva: validação de parâmetros
        if (loginRequest == null) {
            logger.warn("❌ Requisição de login nula recebida");
            return Mono.just(createErrorResponse(HttpStatus.BAD_REQUEST, ERROR_INVALID_REQUEST, "Dados de login obrigatórios"));
        }

        if (authService == null) {
            logger.error("❌ AuthService não está disponível");
            return Mono.just(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_SERVER_ERROR, MSG_SERVICE_UNAVAILABLE));
        }

        String clientIp = getClientIp(exchange);
        String userAgent = getUserAgent(exchange);

        // Programação defensiva: validação de dados extraídos
        String safeUsuario = loginRequest.usuario() != null ? loginRequest.usuario() : MSG_UNKNOWN_USER;

        logger.info("🔐 Tentativa de autenticação: usuario={}, ip={}", safeUsuario, clientIp);

        return authService.authenticate(loginRequest, clientIp, userAgent)
            .map(tokenResponse -> {
                if (tokenResponse == null) {
                    logger.warn("⚠️ AuthService retornou resposta nula para usuario={}", safeUsuario);
                    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_SERVER_ERROR, MSG_SERVICE_UNAVAILABLE);
                }

                logger.info("✅ Autenticação bem-sucedida: usuario={}, ip={}", safeUsuario, clientIp);
                return createSuccessResponse(tokenResponse);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Falha na autenticação: usuario={}, ip={}, erro={}", safeUsuario, clientIp, errorMsg);

                return handleAuthError(throwable);
            });
    }
    
    /**
     * 🔄 Endpoint de renovação de token.
     * 
     * Implementa a renovação de access token usando refresh token.
     */
    @PostMapping(value = REFRESH_ENDPOINT,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Renovação de token de acesso",
        description = "Renova o access token usando refresh token válido"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaTokenDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Refresh token inválido ou malformado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token expirado ou revogado"
        )
    })
    public Mono<ResponseEntity<Object>> refresh(
            @Valid @RequestBody RequisicaoRefreshDTO refreshRequest,
            ServerWebExchange exchange) {

        // Programação defensiva: validação de parâmetros
        if (refreshRequest == null) {
            logger.warn("❌ Requisição de refresh nula recebida");
            return Mono.just(createErrorResponse(HttpStatus.BAD_REQUEST, ERROR_INVALID_REQUEST, "Refresh token obrigatório"));
        }

        if (authService == null) {
            logger.error("❌ AuthService não está disponível para refresh");
            return Mono.just(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_SERVER_ERROR, MSG_SERVICE_UNAVAILABLE));
        }

        String clientIp = getClientIp(exchange);
        String userAgent = getUserAgent(exchange);

        logger.info("🔄 Tentativa de renovação de token: ip={}", clientIp);

        return authService.refresh(refreshRequest, clientIp, userAgent)
            .map(tokenResponse -> {
                if (tokenResponse == null) {
                    logger.warn("⚠️ AuthService retornou resposta nula para refresh: ip={}", clientIp);
                    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_SERVER_ERROR, MSG_SERVICE_UNAVAILABLE);
                }

                logger.info("✅ Token renovado com sucesso: ip={}", clientIp);
                return createSuccessResponse(tokenResponse);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Falha na renovação: ip={}, erro={}", clientIp, errorMsg);

                return handleRefreshError(throwable);
            });
    }
    
    /**
     * 🔍 Endpoint de introspecção de token (RFC 7662).
     * 
     * Permite validar e obter informações sobre um token.
     */
    @PostMapping(value = INTROSPECT_ENDPOINT,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Introspecção de token",
        description = "Valida token e retorna informações sobre sua validade e claims (RFC 7662)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Introspecção realizada com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaIntrospeccaoDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token malformado"
        )
    })
    public Mono<ResponseEntity<RespostaIntrospeccaoDTO>> introspect(
            @Valid @RequestBody RequisicaoIntrospeccaoDTO introspectRequest,
            ServerWebExchange exchange) {

        // Programação defensiva: validação de parâmetros
        if (introspectRequest == null || introspectRequest.token() == null || introspectRequest.token().trim().isEmpty()) {
            logger.warn("❌ Requisição de introspecção inválida recebida");
            // RFC 7662: Sempre retorna token inativo para requisições inválidas
            return Mono.just(ResponseEntity.ok()
                .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
                .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
                .body(RespostaIntrospeccaoDTO.inativo()));
        }

        if (authService == null) {
            logger.error("❌ AuthService não está disponível para introspecção");
            return Mono.just(ResponseEntity.ok()
                .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
                .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
                .body(RespostaIntrospeccaoDTO.inativo()));
        }

        String clientIp = getClientIp(exchange);

        logger.debug("🔍 Introspecção de token solicitada: ip={}", clientIp);

        return authService.introspect(introspectRequest.token())
            .map(introspectionResponse -> {
                if (introspectionResponse == null) {
                    logger.warn("⚠️ AuthService retornou resposta nula para introspecção: ip={}", clientIp);
                    return ResponseEntity.ok()
                        .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
                        .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
                        .body(RespostaIntrospeccaoDTO.inativo());
                }

                logger.debug("✅ Introspecção concluída: ativo={}, ip={}",
                           introspectionResponse.ativo(), clientIp);

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
                    .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
                    .body(introspectionResponse);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Erro na introspecção: ip={}, erro={}", clientIp, errorMsg);

                // Sempre retorna token inativo em caso de erro (RFC 7662)
                return Mono.just(ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
                    .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
                    .body(RespostaIntrospeccaoDTO.inativo()));
            });
    }
    
    /**
     * 🚪 Endpoint de revogação de token (logout).
     * 
     * Revoga refresh token e invalida sessão.
     */
    @PostMapping(value = REVOKE_ENDPOINT,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Revogação de token",
        description = "Revoga refresh token e invalida sessão do usuário"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token revogado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token inválido"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> revoke(
            @Valid @RequestBody RequisicaoRefreshDTO revokeRequest,
            ServerWebExchange exchange) {

        // Programação defensiva: validação de parâmetros
        if (revokeRequest == null || revokeRequest.tokenRenovacao() == null || revokeRequest.tokenRenovacao().trim().isEmpty()) {
            logger.warn("❌ Requisição de revogação inválida recebida");
            // RFC 7009: Sempre retorna sucesso para revogação, mesmo com token inválido
            Map<String, Object> response = new HashMap<>();
            response.put(JSON_KEY_REVOKED, true);
            response.put(JSON_KEY_MESSAGE, MSG_TOKEN_REVOKED_SIMPLE);
            return Mono.just(ResponseEntity.ok(response));
        }

        if (authService == null) {
            logger.error("❌ AuthService não está disponível para revogação");
            Map<String, Object> response = new HashMap<>();
            response.put(JSON_KEY_REVOKED, true);
            response.put(JSON_KEY_MESSAGE, MSG_TOKEN_REVOKED_SIMPLE);
            return Mono.just(ResponseEntity.ok(response));
        }

        String clientIp = getClientIp(exchange);

        logger.info("🚪 Tentativa de revogação: ip={}", clientIp);

        return authService.revoke(revokeRequest.tokenRenovacao())
            .then(Mono.fromCallable(() -> {
                logger.info("✅ Token revogado com sucesso: ip={}", clientIp);

                Map<String, Object> response = new HashMap<>();
                response.put(JSON_KEY_REVOKED, true);
                response.put(JSON_KEY_MESSAGE, MSG_TOKEN_REVOKED);
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("⚠️ Erro na revogação: ip={}, erro={}", clientIp, errorMsg);

                // RFC 7009: Sempre retorna sucesso para revogação
                Map<String, Object> response = new HashMap<>();
                response.put(JSON_KEY_REVOKED, true);
                response.put(JSON_KEY_MESSAGE, MSG_TOKEN_REVOKED_SIMPLE);
                return Mono.just(ResponseEntity.ok(response));
            });
    }
    
    /**
     * ✅ Health check do serviço de autenticação.
     */
    @GetMapping(value = HEALTH_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço",
        description = "Verifica se o serviço de autenticação está funcionando"
    )
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        logger.debug("✅ Health check solicitado");

        // Programação defensiva: validação do serviço
        if (authService == null) {
            logger.error("❌ AuthService não está disponível para health check");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(JSON_KEY_STATUS, STATUS_DOWN);
            errorResponse.put(JSON_KEY_SERVICE, SERVICE_NAME);
            errorResponse.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());
            return Mono.just(ResponseEntity.status(503).body(errorResponse));
        }

        return authService.healthCheck()
            .map(isHealthy -> {
                Map<String, Object> response = new HashMap<>();
                response.put(JSON_KEY_STATUS, isHealthy ? STATUS_UP : STATUS_DOWN);
                response.put(JSON_KEY_SERVICE, SERVICE_NAME);
                response.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("⚠️ Health check falhou: {}", errorMsg);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put(JSON_KEY_STATUS, STATUS_DOWN);
                errorResponse.put(JSON_KEY_SERVICE, SERVICE_NAME);
                errorResponse.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());
                return Mono.just(ResponseEntity.status(503).body(errorResponse));
            });
    }
    
    // ============================================================================
    // 🔐 ENDPOINTS DE AUTENTICAÇÃO DOIS FATORES (2FA)
    // ============================================================================
    
    /**
     * 📱 Gerar código 2FA para usuário.
     */
    @PostMapping(value = "/2fa/generate",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Gerar código 2FA",
        description = "Gera código de dois fatores para usuário"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Código gerado com sucesso",
            content = @Content(schema = @Schema(implementation = Resposta2FADTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    public Mono<ResponseEntity<Resposta2FADTO>> gerarCodigo2FA(
            @Valid @RequestBody Requisicao2FADTO request,
            ServerWebExchange exchange) {
        
        if (request == null || twoFactorService == null) {
            return Mono.just(ResponseEntity.badRequest().body(Resposta2FADTO.falha()));
        }
        
        String clientIp = getClientIp(exchange);
        logger.info("🔐 Gerando código 2FA: usuario={}, canal={}, ip={}", 
                   request.usuarioId(), request.canal(), clientIp);
        
        return twoFactorService.gerarCodigo(request.usuarioId(), request.canal())
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                logger.error("❌ Erro ao gerar código 2FA: {}", throwable.getMessage());
                return Mono.just(ResponseEntity.status(500).body(Resposta2FADTO.falha()));
            });
    }
    
    /**
     * ✅ Verificar código 2FA do usuário.
     */
    @PostMapping(value = "/2fa/verify",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Verificar código 2FA",
        description = "Verifica código de dois fatores do usuário"
    )
    public Mono<ResponseEntity<Map<String, Boolean>>> verificarCodigo2FA(
            @Valid @RequestBody Verificacao2FADTO request,
            ServerWebExchange exchange) {
        
        if (request == null || twoFactorService == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("ok", false)));
        }
        
        String clientIp = getClientIp(exchange);
        logger.info("🔍 Verificando código 2FA: usuario={}, ip={}", request.usuarioId(), clientIp);
        
        return twoFactorService.verificarCodigo(request.usuarioId(), request.codigo())
            .map(valido -> ResponseEntity.ok(Map.of("ok", valido)))
            .onErrorResume(throwable -> {
                logger.error("❌ Erro ao verificar código 2FA: {}", throwable.getMessage());
                return Mono.just(ResponseEntity.status(500).body(Map.of("ok", false)));
            });
    }
    
    /**
     * 🚫 Desabilitar 2FA para usuário (admin only).
     */
    @PostMapping(value = "/2fa/disable/{usuarioId}",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Desabilitar 2FA",
        description = "Desabilita autenticação de dois fatores para usuário (admin only)"
    )
    public Mono<ResponseEntity<Map<String, String>>> desabilitar2FA(
            @PathVariable String usuarioId,
            ServerWebExchange exchange) {
        
        if (usuarioId == null || usuarioId.trim().isEmpty() || twoFactorService == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("message", "usuarioId é obrigatório")));
        }
        
        String clientIp = getClientIp(exchange);
        logger.info("🚫 Desabilitando 2FA: usuario={}, ip={}", usuarioId, clientIp);
        
        return twoFactorService.desabilitar2FA(usuarioId)
            .then(Mono.fromCallable(() -> 
                ResponseEntity.ok(Map.of("message", "2FA desabilitado com sucesso"))))
            .onErrorResume(throwable -> {
                logger.error("❌ Erro ao desabilitar 2FA: {}", throwable.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(Map.of("message", "Erro interno")));
            });
    }
    
    // ============================================================================
    // 🩺 ENDPOINTS DE DIAGNÓSTICO
    // ============================================================================
    
    /**
     * 👤 Diagnóstico detalhado do usuário (admin only).
     */
    @GetMapping(value = "/diagnostics/usuario/{username}",
               produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Diagnóstico de usuário",
        description = "Diagnóstico detalhado do usuário (admin only)"
    )
    public Mono<ResponseEntity<DiagnosticoUsuarioDTO>> diagnosticoUsuario(
            @PathVariable String username,
            ServerWebExchange exchange) {
        
        if (username == null || username.trim().isEmpty() || diagnosticoService == null) {
            DiagnosticoUsuarioDTO erro = new DiagnosticoUsuarioDTO(
                false, false, 0, null, null, List.of("Username inválido"), "ERRO"
            );
            return Mono.just(ResponseEntity.badRequest().body(erro));
        }
        
        String clientIp = getClientIp(exchange);
        logger.info("🩺 Diagnóstico solicitado: usuario={}, ip={}", username, clientIp);
        
        return diagnosticoService.diagnosticoUsuario(username)
            .map(ResponseEntity::ok)
            .onErrorResume(throwable -> {
                logger.error("❌ Erro no diagnóstico: {}", throwable.getMessage());
                DiagnosticoUsuarioDTO erro = new DiagnosticoUsuarioDTO(
                    false, false, 0, null, null, List.of("Erro interno"), "ERRO"
                );
                return Mono.just(ResponseEntity.status(500).body(erro));
            });
    }
    
    /**
     * 🏥 Diagnóstico completo de health do sistema (admin only).
     */
    @GetMapping(value = "/diagnostics/health",
               produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Diagnóstico de health",
        description = "Diagnóstico completo de health do sistema (admin only)"
    )
    public Mono<ResponseEntity<DiagnosticoHealthDTO>> diagnosticoHealth(
            ServerWebExchange exchange) {
        
        if (diagnosticoService == null) {
            DiagnosticoHealthDTO erro = new DiagnosticoHealthDTO(
                "DOWN", LocalDateTime.now(), Map.of(), Map.of(), Map.of(), 0, "1.0.0"
            );
            return Mono.just(ResponseEntity.status(503).body(erro));
        }
        
        String clientIp = getClientIp(exchange);
        logger.info("🏥 Health check completo solicitado: ip={}", clientIp);
        
        return diagnosticoService.diagnosticoHealth()
            .map(health -> {
                HttpStatus status = "UP".equals(health.status()) ? 
                    HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
                return ResponseEntity.status(status).body(health);
            })
            .onErrorResume(throwable -> {
                logger.error("❌ Erro no health check: {}", throwable.getMessage());
                DiagnosticoHealthDTO erro = new DiagnosticoHealthDTO(
                    "DOWN", LocalDateTime.now(), 
                    Map.of("error", throwable.getMessage()),
                    Map.of("error", throwable.getMessage()),
                    Map.of("error", throwable.getMessage()),
                    0, "1.0.0"
                );
                return Mono.just(ResponseEntity.status(503).body(erro));
            });
    }
    
    // Métodos auxiliares privados
    
    private String getClientIp(ServerWebExchange exchange) {
        // Programação defensiva: validação do exchange
        if (exchange == null || exchange.getRequest() == null) {
            logger.warn("⚠️ Exchange ou request nulo ao obter IP do cliente");
            return CLIENT_IP_UNKNOWN;
        }

        // Verificar X-Forwarded-For (proxy/load balancer)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            String[] ips = xForwardedFor.split(COMMA_SEPARATOR);
            if (ips.length > 0 && !ips[0].trim().isEmpty()) {
                return ips[0].trim();
            }
        }

        // Verificar X-Real-IP (nginx)
        String xRealIp = exchange.getRequest().getHeaders().getFirst(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return xRealIp.trim();
        }

        // Fallback para endereço remoto direto
        try {
            if (exchange.getRequest().getRemoteAddress() != null &&
                exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao obter endereço remoto: {}", e.getMessage());
        }

        return CLIENT_IP_UNKNOWN;
    }
    
    private String getUserAgent(ServerWebExchange exchange) {
        // Programação defensiva: validação do exchange
        if (exchange == null || exchange.getRequest() == null || exchange.getRequest().getHeaders() == null) {
            logger.warn("⚠️ Exchange, request ou headers nulo ao obter User-Agent");
            return null;
        }

        try {
            return exchange.getRequest().getHeaders().getFirst(HEADER_USER_AGENT);
        } catch (Exception e) {
            logger.warn("⚠️ Erro ao obter User-Agent: {}", e.getMessage());
            return null;
        }
    }
    
    private Mono<ResponseEntity<Object>> handleAuthError(Throwable throwable) {
        // Programação defensiva: análise do tipo de erro
        String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;

        // Implementação simplificada - seria expandida com diferentes tipos de erro
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(JSON_KEY_ERROR, ERROR_INVALID_GRANT);
        errorResponse.put(JSON_KEY_ERROR_DESCRIPTION, MSG_INVALID_CREDENTIALS);

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
            .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
            .body(errorResponse));
    }
    
    private Mono<ResponseEntity<Object>> handleRefreshError(Throwable throwable) {
        // Programação defensiva: análise do tipo de erro
        String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(JSON_KEY_ERROR, ERROR_INVALID_GRANT);
        errorResponse.put(JSON_KEY_ERROR_DESCRIPTION, MSG_INVALID_REFRESH_TOKEN);

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HEADER_CACHE_CONTROL, CACHE_CONTROL_NO_STORE)
            .header(HEADER_PRAGMA, PRAGMA_NO_CACHE)
            .body(errorResponse));
    }
}