package br.tec.facilitaservicos.feedback.apresentacao.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@Tag(name = "Feedback", description = "Recebimento e consulta de feedbacks")
public class FeedbackController {

    private final ConcurrentLinkedQueue<Map<String, Object>> storage = new ConcurrentLinkedQueue<>();

    @PostMapping("/feedback")
    @Operation(summary = "Enviar feedback")
    public Mono<ResponseEntity<Map<String, Object>>> enviar(
            @RequestBody Map<String, Object> req) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> item = Map.of(
                "id", id,
                "mensagem", req.getOrDefault("mensagem", ""),
                "categoria", req.getOrDefault("categoria", "GERAL"),
                "data", LocalDateTime.now().toString()
        );
        storage.add(item);
        return Mono.just(ResponseEntity.ok(Map.of("id", id)));
    }

    @GetMapping("/feedback")
    @Operation(summary = "Listar feedbacks")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Map<String, Object>> listar() {
        return Flux.fromIterable(storage);
    }
}

