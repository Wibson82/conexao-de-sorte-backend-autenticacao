package br.tec.facilitaservicos.usuario.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade Endereco para R2DBC (100% reativo).
 * Representa endereços dos usuários no sistema.
 * Entidade genérica reutilizável em qualquer setor.
 */
@Table("endereco")
public class Endereco {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("usuario_id")
    private Long usuarioId;
    
    @Column("tipo")
    private String tipo = "RESIDENCIAL"; // RESIDENCIAL, COMERCIAL, ENTREGA, COBRANCA
    
    @Column("logradouro")
    private String logradouro;
    
    @Column("numero")
    private String numero;
    
    @Column("complemento")
    private String complemento;
    
    @Column("bairro")
    private String bairro;
    
    @Column("cidade")
    private String cidade;
    
    @Column("estado")
    private String estado; // Sigla: SP, RJ, MG
    
    @Column("cep")
    private String cep;
    
    @Column("pais")
    private String pais = "BR";
    
    @Column("principal")
    private boolean principal = false;
    
    @Column("ativo")
    private boolean ativo = true;
    
    @Column("latitude")
    private Double latitude;
    
    @Column("longitude")
    private Double longitude;
    
    @Column("referencia")
    private String referencia;
    
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
    public Endereco() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public Endereco(Long usuarioId, String logradouro, String numero, String cep, String cidade, String estado) {
        this();
        this.usuarioId = usuarioId;
        this.logradouro = logradouro;
        this.numero = numero;
        this.cep = cep;
        this.cidade = cidade;
        this.estado = estado;
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
    
    public String getTipo() {
        return tipo;
    }
    
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
    public String getLogradouro() {
        return logradouro;
    }
    
    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }
    
    public String getNumero() {
        return numero;
    }
    
    public void setNumero(String numero) {
        this.numero = numero;
    }
    
    public String getComplemento() {
        return complemento;
    }
    
    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }
    
    public String getBairro() {
        return bairro;
    }
    
    public void setBairro(String bairro) {
        this.bairro = bairro;
    }
    
    public String getCidade() {
        return cidade;
    }
    
    public void setCidade(String cidade) {
        this.cidade = cidade;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public String getCep() {
        return cep;
    }
    
    public void setCep(String cep) {
        this.cep = cep;
    }
    
    public String getPais() {
        return pais;
    }
    
    public void setPais(String pais) {
        this.pais = pais;
    }
    
    public boolean isPrincipal() {
        return principal;
    }
    
    public void setPrincipal(boolean principal) {
        this.principal = principal;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getReferencia() {
        return referencia;
    }
    
    public void setReferencia(String referencia) {
        this.referencia = referencia;
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
     * Retorna endereço formatado
     */
    public String getEnderecoCompleto() {
        StringBuilder sb = new StringBuilder();
        
        if (logradouro != null) sb.append(logradouro);
        if (numero != null) sb.append(", ").append(numero);
        if (complemento != null) sb.append(" - ").append(complemento);
        if (bairro != null) sb.append(", ").append(bairro);
        if (cidade != null) sb.append(", ").append(cidade);
        if (estado != null) sb.append(" - ").append(estado);
        if (cep != null) sb.append(", CEP: ").append(cep);
        
        return sb.toString();
    }
    
    /**
     * Formata CEP brasileiro (apenas números para formato com hífen)
     */
    public String getCepFormatado() {
        if (cep == null || cep.length() < 8) return cep;
        
        String cepNumeros = cep.replaceAll("\\D", "");
        if (cepNumeros.length() == 8) {
            return cepNumeros.substring(0, 5) + "-" + cepNumeros.substring(5);
        }
        return cep;
    }
    
    /**
     * Verifica se tem coordenadas GPS
     */
    public boolean temCoordenadas() {
        return latitude != null && longitude != null;
    }
    
    /**
     * Define como endereço principal
     */
    public void definirComoPrincipal() {
        this.principal = true;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Remove como endereço principal
     */
    public void removerComoPrincipal() {
        this.principal = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Ativa endereço
     */
    public void ativar() {
        this.ativo = true;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Desativa endereço
     */
    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Valida se o endereço tem dados mínimos
     */
    public boolean isValido() {
        return logradouro != null && !logradouro.trim().isEmpty() &&
               cidade != null && !cidade.trim().isEmpty() &&
               estado != null && !estado.trim().isEmpty() &&
               cep != null && !cep.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endereco endereco = (Endereco) o;
        return Objects.equals(id, endereco.id) &&
               Objects.equals(usuarioId, endereco.usuarioId) &&
               Objects.equals(cep, endereco.cep) &&
               Objects.equals(numero, endereco.numero);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, usuarioId, cep, numero);
    }
    
    @Override
    public String toString() {
        return String.format(
            "Endereco{id=%d, usuarioId=%d, tipo='%s', endereco='%s', principal=%s, ativo=%s}",
            id, usuarioId, tipo, getEnderecoCompleto(), principal, ativo
        );
    }
}