package br.tec.facilitaservicos.autenticacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * ============================================================================
 * = APLICA��O PRINCIPAL - MICROSERVI�O AUTENTICA��O
 * ============================================================================
 * 
 * Microservi�o de autentica��o 100% reativo usando:
 * - Spring Boot 3.5+
 * - WebFlux (reativo)
 * - R2DBC (reativo)
 * - Spring Security reativo
 * - JWT com JWKS
 * - Azure Key Vault para rota��o de chaves
 * - Observabilidade com Micrometer
 * - Resilience4j para rate limiting
 * 
 * Endpoints:
 * - POST /auth/token - Gera��o de token
 * - POST /auth/refresh - Renova��o de token
 * - POST /auth/introspect - Introspec��o de token
 * - GET /oauth2/jwks - JWK Set p�blico
 * 
 * ============================================================================
 */
@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing
public class AutenticacaoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AutenticacaoApplication.class, args);
    }
}