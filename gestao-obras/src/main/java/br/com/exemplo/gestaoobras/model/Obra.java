package br.com.exemplo.gestaoobras.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Raiz do domínio: uma obra de construção civil.
 */
@Entity
@Table(name = "obra")
public class Obra implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da obra é obrigatório")
    @Size(max = 150, message = "O nome não pode exceder 150 caracteres")
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "A localização é obrigatória")
    @Size(max = 255, message = "A localização não pode exceder 255 caracteres")
    @Column(name = "localizacao", nullable = false, length = 255)
    private String localizacao;

    @NotNull(message = "O status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusObra status = StatusObra.PLANEJADA;

    // Controle de concorrência otimista: impede que dois usuários
    // sobrescrevam silenciosamente as alterações um do outro.
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    /** Exigido pela especificação JPA e pelo binding de formulários JSF. */
    public Obra() {
    }

    public Obra(String nome, String localizacao, StatusObra status) {
        this.nome = nome;
        this.localizacao = localizacao;
        this.status = status;
    }

    // ---- Callbacks do ciclo de vida da entidade ----

    @PrePersist
    protected void antesDeInserir() {
        this.dataCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void antesDeAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    // ---- Comportamento de domínio ----

    public boolean isConcluida() {
        return status == StatusObra.CONCLUIDA;
    }

    // ---- Getters / Setters ----
    // Sem setId(), setVersao() nem setters de auditoria: esses valores
    // pertencem ao JPA e não podem ser alterados pelo código cliente.

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public StatusObra getStatus() {
        return status;
    }

    public void setStatus(StatusObra status) {
        this.status = status;
    }

    public Long getVersao() {
        return versao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    // ---- Identidade ----
    // Baseada no ID gerado pelo banco. Usa instanceof + getId() (e não getClass()
    // nem acesso direto ao campo) para funcionar com proxies LAZY do Hibernate.
    // hashCode constante: se mantém estável antes e depois do persist().

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Obra)) {
            return false;
        }
        Obra outra = (Obra) o;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return Obra.class.hashCode();
    }

    @Override
    public String toString() {
        return "Obra{id=" + id + ", nome='" + nome + "', status=" + status + '}';
    }
}
