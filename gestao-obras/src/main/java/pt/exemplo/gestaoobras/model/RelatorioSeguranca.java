package pt.exemplo.gestaoobras.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Relatório de uma inspeção de segurança realizada numa obra.
 * Lado "dono" (owning side) da relação: é esta tabela que guarda a FK obra_id.
 */
@Entity
@Table(name = "relatorio_seguranca",
       indexes = @Index(name = "idx_relatorio_obra", columnList = "obra_id"))
public class RelatorioSeguranca implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "A data da inspeção é obrigatória")
    @PastOrPresent(message = "A data da inspeção não pode estar no futuro")
    @Column(name = "data_inspecao", nullable = false)
    private LocalDate dataInspecao;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 2000, message = "A descrição não pode exceder 2000 caracteres")
    @Column(name = "descricao", nullable = false, length = 2000)
    private String descricao;

    // LAZY: carregar um relatório NÃO dispara um SELECT à tabela obra.
    // optional = false: todo o relatório pertence obrigatoriamente a uma obra
    // (permite ao Hibernate gerar INNER JOIN em vez de LEFT JOIN).
    @NotNull(message = "O relatório tem de estar associado a uma obra")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_relatorio_obra"))
    private Obra obra;

    /** Exigido pela especificação JPA e pelo binding de formulários JSF. */
    public RelatorioSeguranca() {
    }

    public RelatorioSeguranca(Obra obra, LocalDate dataInspecao, String descricao) {
        setObra(obra);
        this.dataInspecao = dataInspecao;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDataInspecao() {
        return dataInspecao;
    }

    public void setDataInspecao(LocalDate dataInspecao) {
        this.dataInspecao = dataInspecao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = Objects.requireNonNull(obra, "obra não pode ser nula");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RelatorioSeguranca)) {
            return false;
        }
        RelatorioSeguranca outro = (RelatorioSeguranca) o;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return RelatorioSeguranca.class.hashCode();
    }

    // Não inclui 'obra': tocar no proxy LAZY fora de uma transação
    // lançaria LazyInitializationException só por fazer log do objeto.
    @Override
    public String toString() {
        return "RelatorioSeguranca{id=" + id + ", dataInspecao=" + dataInspecao + '}';
    }
}
