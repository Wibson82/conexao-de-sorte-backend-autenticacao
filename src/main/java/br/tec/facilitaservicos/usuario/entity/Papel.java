package br.tec.facilitaservicos.usuario.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade Papel para R2DBC (100% reativo).
 * Representa papéis/roles no sistema de autenticação.
 * Entidade genérica reutilizável em qualquer setor.
 */
@Table("papel")
public class Papel {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("nome")
    private String nome; // ADMIN, USER, MODERATOR, MANAGER, etc.
    
    @Column("descricao")
    private String descricao;
    
    @Column("ativo")
    private boolean ativo = true;
    
    @Column("permissoes")
    private String permissoes; // JSON com permissões específicas
    
    @Column("nivel_acesso")
    private Integer nivelAcesso = 1; // 1=básico, 5=admin
    
    @CreatedDate
    @Column("data_criacao")
    private LocalDateTime dataCriacao;
    
    @LastModifiedDate
    @Column("data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    @Column("criado_por")
    private Long criadoPor;
    
    @Column("atualizado_por")
    private Long atualizadoPor;
    
    @Version
    @Column("versao")
    private Long versao;
    
    // Construtores
    public Papel() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public Papel(String nome, String descricao) {
        this();
        this.nome = nome;
        this.descricao = descricao;
    }
    
    public Papel(String nome, String descricao, Integer nivelAcesso) {
        this(nome, descricao);
        this.nivelAcesso = nivelAcesso;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public String getPermissoes() {
        return permissoes;
    }
    
    public void setPermissoes(String permissoes) {
        this.permissoes = permissoes;
    }
    
    public Integer getNivelAcesso() {
        return nivelAcesso;
    }
    
    public void setNivelAcesso(Integer nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
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
    
    public Long getCriadoPor() {
        return criadoPor;
    }
    
    public void setCriadoPor(Long criadoPor) {
        this.criadoPor = criadoPor;
    }
    
    public Long getAtualizadoPor() {
        return atualizadoPor;
    }
    
    public void setAtualizadoPor(Long atualizadoPor) {
        this.atualizadoPor = atualizadoPor;
    }
    
    public Long getVersao() {
        return versao;
    }
    
    public void setVersao(Long versao) {
        this.versao = versao;
    }
    
    // Métodos de negócio
    
    /**
     * Verifica se é um papel administrativo
     */
    public boolean isAdmin() {
        return nivelAcesso != null && nivelAcesso >= 4;
    }
    
    /**
     * Verifica se pode gerenciar outros usuários
     */
    public boolean podeGerenciarUsuarios() {
        return nivelAcesso != null && nivelAcesso >= 3;
    }
    
    /**
     * Ativa o papel
     */
    public void ativar() {
        this.ativo = true;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Desativa o papel
     */
    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Papel papel = (Papel) o;
        return Objects.equals(id, papel.id) &&
               Objects.equals(nome, papel.nome);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, nome);
    }
    
    @Override
    public String toString() {
        return String.format(
            "Papel{id=%d, nome='%s', descricao='%s', ativo=%s, nivelAcesso=%d}",
            id, nome, descricao, ativo, nivelAcesso
        );
    }
}