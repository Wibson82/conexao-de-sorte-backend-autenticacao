package br.tec.facilitaservicos.feedback.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controlador de Feedback - Sistema de coleta de feedback dos usuarios.
 */
@RestController
@RequestMapping("/v1/feedback")
public class FeedbackController {

    @PostMapping("/feedback")
    @PreAuthorize("hasAuthority('SCOPE_feedback.write')")
    public Mono<Map<String, Object>> enviarFeedback(@RequestBody Map<String, Object> feedback) {
        return Mono.just(Map.of(
            "status", "feedback recebido",
            "id", "fb_" + System.currentTimeMillis(),
            "message", "Obrigado pelo seu feedback!",
            "feedback", feedback
        ));
    }

    @GetMapping("/listar")
    @PreAuthorize("hasAuthority('SCOPE_feedback.read')")
    public Mono<Map<String, Object>> listarFeedbacks() {
        return Mono.just(Map.of(
            "feedbacks", "lista_de_feedbacks_stub",
            "total", 0
        ));
    }

    @GetMapping("/estatisticas")
    @PreAuthorize("hasAuthority('SCOPE_feedback.read')")
    public Mono<Map<String, Object>> estatisticasFeedback() {
        return Mono.just(Map.of(
            "total_feedbacks", 0,
            "avaliacao_media", 0.0,
            "status", "estatisticas stub"
        ));
    }
}