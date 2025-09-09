package br.tec.facilitaservicos.usuario.repository;

import br.tec.facilitaservicos.usuario.entity.Endereco;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository reativo para Endereco usando R2DBC.
 * 100% reativo e não-bloqueante.
 */
@Repository
public interface EnderecoRepository extends ReactiveCrudRepository<Endereco, Long> {
    
    /**
     * Busca todos os endereços de um usuário
     */
    Flux<Endereco> findByUsuarioId(Long usuarioId);
    
    /**
     * Busca endereços ativos de um usuário
     */
    Flux<Endereco> findByUsuarioIdAndAtivoTrue(Long usuarioId);
    
    /**
     * Busca endereço principal de um usuário
     */
    Mono<Endereco> findByUsuarioIdAndPrincipalTrueAndAtivoTrue(Long usuarioId);
    
    /**
     * Busca endereços por tipo
     */
    Flux<Endereco> findByUsuarioIdAndTipo(Long usuarioId, String tipo);
    
    /**
     * Busca endereços ativos por tipo
     */
    Flux<Endereco> findByUsuarioIdAndTipoAndAtivoTrue(Long usuarioId, String tipo);
    
    /**
     * Busca endereços por CEP
     */
    Flux<Endereco> findByCep(String cep);
    
    /**
     * Busca endereços por cidade e estado
     */
    Flux<Endereco> findByCidadeAndEstado(String cidade, String estado);
    
    /**
     * Verifica se usuário tem endereço principal
     */
    Mono<Boolean> existsByUsuarioIdAndPrincipalTrueAndAtivoTrue(Long usuarioId);
    
    /**
     * Remove endereço principal atual do usuário
     */
    @Query("UPDATE endereco SET principal = false, data_atualizacao = NOW() WHERE usuario_id = :usuarioId AND principal = true")
    Mono<Integer> removerEnderecoPrincipalAtual(Long usuarioId);
    
    /**
     * Conta endereços ativos de um usuário
     */
    Mono<Long> countByUsuarioIdAndAtivoTrue(Long usuarioId);
    
    /**
     * Busca endereços com coordenadas GPS
     */
    @Query("SELECT * FROM endereco WHERE usuario_id = :usuarioId AND latitude IS NOT NULL AND longitude IS NOT NULL AND ativo = true")
    Flux<Endereco> findEnderecosComCoordenadas(Long usuarioId);
    
    /**
     * Busca endereços próximos a uma coordenada (raio em km)
     */
    @Query("SELECT * FROM endereco WHERE ativo = true AND latitude IS NOT NULL AND longitude IS NOT NULL AND " +
           "(6371 * ACOS(COS(RADIANS(:latitude)) * COS(RADIANS(latitude)) * COS(RADIANS(longitude) - RADIANS(:longitude)) + " +
           "SIN(RADIANS(:latitude)) * SIN(RADIANS(latitude)))) <= :raioKm")
    Flux<Endereco> findEnderecosProximos(Double latitude, Double longitude, Double raioKm);
    
    /**
     * Desativa todos os endereços de um usuário
     */
    @Query("UPDATE endereco SET ativo = false, data_atualizacao = NOW() WHERE usuario_id = :usuarioId")
    Mono<Integer> desativarTodosEnderecosDoUsuario(Long usuarioId);
}