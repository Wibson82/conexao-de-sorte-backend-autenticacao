package br.tec.facilitaservicos.usuario.repository;

import br.tec.facilitaservicos.usuario.entity.Papel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository reativo para Papel usando R2DBC.
 * 100% reativo e não-bloqueante.
 */
@Repository
public interface PapelRepository extends ReactiveCrudRepository<Papel, Long> {
    
    /**
     * Busca papel por nome
     */
    Mono<Papel> findByNome(String nome);
    
    /**
     * Busca papéis ativos
     */
    Flux<Papel> findByAtivoTrue();
    
    /**
     * Busca papéis por nível de acesso
     */
    Flux<Papel> findByNivelAcessoGreaterThanEqual(Integer nivelMinimo);
    
    /**
     * Busca papéis administrativos (nível 4+)
     */
    @Query("SELECT * FROM papel WHERE nivel_acesso >= 4 AND ativo = true")
    Flux<Papel> findPapeisAdministrativos();
    
    /**
     * Busca papéis que podem gerenciar usuários (nível 3+)
     */
    @Query("SELECT * FROM papel WHERE nivel_acesso >= 3 AND ativo = true")
    Flux<Papel> findPapeisGerenciais();
    
    /**
     * Verifica se papel existe por nome
     */
    Mono<Boolean> existsByNome(String nome);
    
    /**
     * Conta papéis ativos
     */
    Mono<Long> countByAtivoTrue();
}