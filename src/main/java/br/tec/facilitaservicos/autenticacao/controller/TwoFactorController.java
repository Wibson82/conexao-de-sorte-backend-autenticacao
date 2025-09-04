package br.tec.facilitaservicos.autenticacao.controller;

import br.tec.facilitaservicos.autenticacao.dto.RequisicaoGerar2FADTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoValidar2FADTO;
import br.tec.facilitaservicos.autenticacao.dto.Resposta2FAGeradoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaOkDTO;
import br.tec.facilitaservicos.autenticacao.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth/2fa")
@Tag(name = "2FA", description = "Geração e validação de códigos de autenticação em dois fatores")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public TwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gerar código 2FA")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<ResponseEntity<Resposta2FAGeradoDTO>> generate(@Valid @RequestBody RequisicaoGerar2FADTO req) {
        return twoFactorService.generateCode(req.usuarioId(), req.canal())
                .map(r -> ResponseEntity.ok(Resposta2FAGeradoDTO.of(r.codigoMasked(), r.ttlSegundos())));
    }

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validar código 2FA")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Mono<ResponseEntity<RespostaOkDTO>> verify(@Valid @RequestBody RequisicaoValidar2FADTO req) {
        return twoFactorService.verifyCode(req.usuarioId(), req.canal(), req.codigo())
                .map(ok -> ResponseEntity.ok(ok ? RespostaOkDTO.sucesso() : RespostaOkDTO.falha()));
    }

    @PostMapping(value = "/disable/{usuarioId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Desabilitar 2FA (limpar códigos pendentes)")
    @PreAuthorize("hasRole('ADMIN') or #usuarioId != null")
    public Mono<ResponseEntity<RespostaOkDTO>> disable(@PathVariable Long usuarioId) {
        return twoFactorService.disable(usuarioId)
                .map(v -> ResponseEntity.ok(RespostaOkDTO.sucesso()));
    }
}

