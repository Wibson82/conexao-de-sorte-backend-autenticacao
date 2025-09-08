package br.tec.facilitaservicos.autenticacao.controller;

import br.tec.facilitaservicos.autenticacao.service.KeyVaultMonitoringService;
import br.tec.facilitaservicos.autenticacao.service.KeyVaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para diagnósticos e monitoramento do Azure Key Vault.
 * Fornece endpoints para verificar saúde, métricas e estatísticas.
 */
@RestController
@RequestMapping("/api/v1/diagnostics/keyvault")
@Tag(name = "Key Vault Diagnostics", description = "Endpoints para monitoramento do Azure Key Vault")
public class KeyVaultDiagnosticsController {

    private static final Logger logger = LoggerFactory.getLogger(KeyVaultDiagnosticsController.class);

    private final KeyVaultService keyVaultService;
    private final KeyVaultMonitoringService monitoringService;

    public KeyVaultDiagnosticsController(KeyVaultService keyVaultService, 
                                       KeyVaultMonitoringService monitoringService) {
        this.keyVaultService = keyVaultService;
        this.monitoringService = monitoringService;
    }

    /**
     * Verifica a saúde do Key Vault.
     */
    @GetMapping("/health")
    @Operation(summary = "Verificar saúde do Key Vault", 
               description = "Testa conectividade e acesso ao Azure Key Vault")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Testar acesso com um secret de teste
            boolean isAvailable = keyVaultService.isKeyVaultAvailable().block();
            
            response.put("status", isAvailable ? "UP" : "DOWN");
            response.put("keyVaultAvailable", isAvailable);
            response.put("timestamp", System.currentTimeMillis());
            
            if (isAvailable) {
                response.put("message", "Key Vault está acessível");
                logger.info("✅ Health check do Key Vault: SUCCESS");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Key Vault não está acessível");
                logger.warn("⚠️ Health check do Key Vault: FAILED");
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("keyVaultAvailable", false);
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.error("❌ Health check do Key Vault: ERROR - {}", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Obtém estatísticas gerais do Key Vault.
     */
    @GetMapping("/stats")
    @Operation(summary = "Estatísticas do Key Vault", 
               description = "Retorna métricas e estatísticas de uso do Key Vault")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            KeyVaultMonitoringService.KeyVaultStats stats = monitoringService.getGeneralStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalAccesses", stats.getTotalAccesses());
            response.put("totalErrors", stats.getTotalErrors());
            response.put("errorRate", String.format("%.2f%%", stats.getErrorRate() * 100));
            response.put("cacheHits", stats.getCacheHits());
            response.put("cacheMisses", stats.getCacheMisses());
            response.put("cacheHitRate", String.format("%.2f%%", stats.getCacheHitRate() * 100));
            response.put("uniqueSecrets", stats.getUniqueSecrets());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.debug("📊 Estatísticas do Key Vault solicitadas");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao obter estatísticas do Key Vault: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao obter estatísticas",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Obtém estatísticas de um secret específico.
     */
    @GetMapping("/stats/secret/{secretName}")
    @Operation(summary = "Estatísticas de secret específico", 
               description = "Retorna estatísticas de acesso para um secret específico")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecretStats(@PathVariable String secretName) {
        try {
            KeyVaultMonitoringService.SecretAccessStats stats = monitoringService.getSecretStats(secretName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("secretName", secretName);
            response.put("totalAccesses", stats.getTotalAccesses());
            response.put("successfulAccesses", stats.getSuccessfulAccesses());
            response.put("failedAccesses", stats.getFailedAccesses());
            response.put("successRate", String.format("%.2f%%", stats.getSuccessRate() * 100));
            response.put("averageDuration", stats.getAverageDuration().toMillis() + "ms");
            response.put("maxDuration", stats.getMaxDuration().toMillis() + "ms");
            response.put("minDuration", stats.getMinDuration().toMillis() + "ms");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.debug("📊 Estatísticas do secret '{}' solicitadas", secretName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao obter estatísticas do secret '{}': {}", secretName, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao obter estatísticas do secret",
                "secretName", secretName,
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Limpa o cache de secrets.
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "Limpar cache de secrets", 
               description = "Remove todos os secrets do cache local")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            keyVaultService.clearSecretCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Cache de secrets limpo com sucesso");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("🧹 Cache de secrets limpo via API");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao limpar cache de secrets: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao limpar cache",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Força rotação de chaves JWT.
     */
    @PostMapping("/keys/rotate")
    @Operation(summary = "Forçar rotação de chaves", 
               description = "Força a rotação das chaves JWT no Key Vault")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rotateKeys() {
        try {
            keyVaultService.rotateKeys();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Rotação de chaves iniciada com sucesso");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("🔄 Rotação de chaves JWT iniciada via API");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Erro ao iniciar rotação de chaves: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", "Erro ao rotacionar chaves",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Testa acesso a um secret específico.
     */
    @PostMapping("/test/secret/{secretName}")
    @Operation(summary = "Testar acesso a secret", 
               description = "Testa se um secret específico pode ser acessado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testSecretAccess(@PathVariable String secretName) {
        try {
            String secret = keyVaultService.getSecret(secretName).block();
            boolean hasValue = secret != null && !secret.trim().isEmpty();
            
            Map<String, Object> response = new HashMap<>();
            response.put("secretName", secretName);
            response.put("accessible", hasValue);
            response.put("hasValue", hasValue);
            response.put("valueLength", hasValue ? secret.length() : 0);
            response.put("timestamp", System.currentTimeMillis());
            
            if (hasValue) {
                response.put("status", "SUCCESS");
                response.put("message", "Secret acessível e com valor");
                logger.info("✅ Teste de acesso ao secret '{}': SUCCESS", secretName);
            } else {
                response.put("status", "WARNING");
                response.put("message", "Secret acessível mas sem valor");
                logger.warn("⚠️ Teste de acesso ao secret '{}': EMPTY", secretName);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Teste de acesso ao secret '{}': ERROR - {}", secretName, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "secretName", secretName,
                "accessible", false,
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}