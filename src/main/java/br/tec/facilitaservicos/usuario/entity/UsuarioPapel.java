package br.tec.facilitaservicos.usuario.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade UsuarioPapel para R2DBC (100% reativo).
 * Representa a relação muitos-para-muitos entre Usuario e Papel.
 * Entidade genérica reutilizável em qualquer setor.
 */
@Table("usuario_papel")
public class UsuarioPapel {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("usuario_id")
    private Long usuarioId;
    
    @Column("papel_id")
    private Long papelId;
    
    @Column("ativo")
    private boolean ativo = true;
    
    @Column("data_atribuicao")
    private LocalDateTime dataAtribuicao;
    
    @Column("data_expiracao")
    private LocalDateTime dataExpiracao;
    
    @Column("atribuido_por")
    private Long atribuidoPor;
    
    @Column("observacoes")
    private String observacoes;
    
    @CreatedDate
    @Column("data_criacao")
    private LocalDateTime dataCriacao;
    
    @LastModifiedDate
    @Column("data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    @Version
    @Column("versao")
    private Long versao;
    
    // Construtores
    public UsuarioPapel() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
        this.dataAtribuicao = LocalDateTime.now();
    }
    
    public UsuarioPapel(Long usuarioId, Long papelId) {
        this();
        this.usuarioId = usuarioId;
        this.papelId = papelId;
    }
    
    public UsuarioPapel(Long usuarioId, Long papelId, Long atribuidoPor) {
        this(usuarioId, papelId);
        this.atribuidoPor = atribuidoPor;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public Long getPapelId() {
        return papelId;
    }
    
    public void setPapelId(Long papelId) {
        this.papelId = papelId;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public LocalDateTime getDataAtribuicao() {
        return dataAtribuicao;
    }
    
    public void setDataAtribuicao(LocalDateTime dataAtribuicao) {
        this.dataAtribuicao = dataAtribuicao;
    }
    
    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
    
    public Long getAtribuidoPor() {
        return atribuidoPor;
    }
    
    public void setAtribuidoPor(Long atribuidoPor) {
        this.atribuidoPor = atribuidoPor;
    }
    
    public String getObservacoes() {
        return observacoes;
    }
    
    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
    
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
    
    public Long getVersao() {
        return versao;
    }
    
    public void setVersao(Long versao) {
        this.versao = versao;
    }
    
    // Métodos de negócio
    
    /**
     * Verifica se a atribuição está expirada
     */
    public boolean isExpirado() {
        return dataExpiracao != null && LocalDateTime.now().isAfter(dataExpiracao);
    }
    
    /**
     * Verifica se a atribuição está válida
     */
    public boolean isValido() {
        return ativo && !isExpirado();
    }
    
    /**
     * Expira a atribuição do papel
     */
    public void expirar() {
        this.ativo = false;
        this.dataExpiracao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Renova a atribuição do papel
     */
    public void renovar(LocalDateTime novaDataExpiracao) {
        this.ativo = true;
        this.dataExpiracao = novaDataExpiracao;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Remove completamente a atribuição
     */
    public void remover() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Define expiração temporária (dias)
     */
    public void definirExpiracaoEm(int dias) {
        this.dataExpiracao = LocalDateTime.now().plusDays(dias);
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioPapel that = (UsuarioPapel) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(papelId, that.papelId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioId, papelId);
    }
    
    @Override
    public String toString() {
        return String.format(
            "UsuarioPapel{id=%d, usuarioId=%d, papelId=%d, ativo=%s, expirado=%s}",
            id, usuarioId, papelId, ativo, isExpirado()
        );
    }
}