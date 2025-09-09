package br.tec.facilitaservicos.usuario.repository;

import br.tec.facilitaservicos.usuario.entity.UsuarioPapel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository reativo para UsuarioPapel usando R2DBC.
 * 100% reativo e não-bloqueante.
 */
@Repository
public interface UsuarioPapelRepository extends ReactiveCrudRepository<UsuarioPapel, Long> {
    
    /**
     * Busca todos os papéis de um usuário
     */
    Flux<UsuarioPapel> findByUsuarioId(Long usuarioId);
    
    /**
     * Busca papéis ativos de um usuário
     */
    Flux<UsuarioPapel> findByUsuarioIdAndAtivoTrue(Long usuarioId);
    
    /**
     * Busca usuários com um papel específico
     */
    Flux<UsuarioPapel> findByPapelId(Long papelId);
    
    /**
     * Busca usuários ativos com um papel específico
     */
    Flux<UsuarioPapel> findByPapelIdAndAtivoTrue(Long papelId);
    
    /**
     * Busca relação específica usuário-papel
     */
    Mono<UsuarioPapel> findByUsuarioIdAndPapelId(Long usuarioId, Long papelId);
    
    /**
     * Verifica se usuário tem papel específico ativo
     */
    @Query("SELECT COUNT(*) > 0 FROM usuario_papel WHERE usuario_id = :usuarioId AND papel_id = :papelId AND ativo = true AND (data_expiracao IS NULL OR data_expiracao > NOW())")
    Mono<Boolean> usuarioTemPapelAtivo(Long usuarioId, Long papelId);
    
    /**
     * Busca papéis válidos (ativos e não expirados) de um usuário
     */
    @Query("SELECT * FROM usuario_papel WHERE usuario_id = :usuarioId AND ativo = true AND (data_expiracao IS NULL OR data_expiracao > NOW())")
    Flux<UsuarioPapel> findPapeisValidosDoUsuario(Long usuarioId);
    
    /**
     * Busca papéis expirados
     */
    @Query("SELECT * FROM usuario_papel WHERE data_expiracao IS NOT NULL AND data_expiracao < NOW() AND ativo = true")
    Flux<UsuarioPapel> findPapeisExpirados();
    
    /**
     * Conta papéis ativos de um usuário
     */
    Mono<Long> countByUsuarioIdAndAtivoTrue(Long usuarioId);
    
    /**
     * Remove todos os papéis de um usuário
     */
    @Query("UPDATE usuario_papel SET ativo = false, data_atualizacao = NOW() WHERE usuario_id = :usuarioId")
    Mono<Integer> removerTodosPapeisDoUsuario(Long usuarioId);
    
    /**
     * Busca usuários com papéis administrativos
     */
    @Query("SELECT up.* FROM usuario_papel up INNER JOIN papel p ON up.papel_id = p.id WHERE p.nivel_acesso >= 4 AND up.ativo = true AND (up.data_expiracao IS NULL OR up.data_expiracao > NOW())")
    Flux<UsuarioPapel> findUsuariosComPapeisAdministrativos();
}