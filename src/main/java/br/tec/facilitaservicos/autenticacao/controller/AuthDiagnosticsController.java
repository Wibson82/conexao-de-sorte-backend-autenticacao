package br.tec.facilitaservicos.autenticacao.controller;

import br.tec.facilitaservicos.autenticacao.client.UserServiceClient;
import br.tec.facilitaservicos.autenticacao.service.KeyVaultService;
import br.tec.facilitaservicos.autenticacao.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/diagnostics")
@Tag(name = "Auth Diagnostics", description = "Diagnósticos de autenticação para suporte/administradores")
public class AuthDiagnosticsController {

    private final UserServiceClient userServiceClient;
    private final KeyVaultService keyVaultService;
    private final TwoFactorService twoFactorService;

    public AuthDiagnosticsController(UserServiceClient userServiceClient, KeyVaultService keyVaultService, TwoFactorService twoFactorService) {
        this.userServiceClient = userServiceClient;
        this.keyVaultService = keyVaultService;
        this.twoFactorService = twoFactorService;
    }

    @GetMapping(value = "/usuario/existe/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Verificar existência de usuário")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> usuarioExiste(@PathVariable String username) {
        return userServiceClient.findByEmailOrNomeUsuario(username)
                .map(u -> Map.<String, Object>of("existe", true, "id", u.getId()))
                .switchIfEmpty(Mono.just(Map.<String, Object>of("existe", false)))
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health detalhado de autenticação")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Mono<Boolean> db = userServiceClient.countUsuariosAtivos().map(cnt -> cnt >= 0).onErrorReturn(false);
        Mono<Boolean> kvPub = keyVaultService.getPublicKey().map(k -> k != null).onErrorReturn(false);
        Mono<Boolean> kvPriv = keyVaultService.getPrivateKey().map(k -> k != null).onErrorReturn(false);
        Mono<Boolean> cache = twoFactorService.cachePing().map(resp -> "pong".equals(resp)).onErrorReturn(false);

        return Mono.zip(db, kvPub, kvPriv, cache)
                .map(tuple -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("database", tuple.getT1());
                    m.put("keyvaultPublic", tuple.getT2());
                    m.put("keyvaultPrivate", tuple.getT3());
                    m.put("cache", tuple.getT4());
                    m.put("status", (tuple.getT1() && tuple.getT2() && tuple.getT3() && tuple.getT4()) ? "UP" : "DEGRADED");
                    return ResponseEntity.ok(m);
                });
    }
}

