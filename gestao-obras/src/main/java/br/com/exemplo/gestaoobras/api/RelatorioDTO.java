package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

import java.time.LocalDate;

/**
 * Representação JSON de um relatório de segurança.
 * <p>
 * A obra aparece só pelo ID: {@code getObra().getId()} não dispara consulta nem
 * LazyInitializationException, porque o proxy LAZY do Hibernate já conhece o ID
 * (é o valor da FK obra_id). Serializar a entidade inteira tentaria percorrer a
 * obra fora da transação.
 */
@JsonbPropertyOrder({"id", "obraId", "dataInspecao", "descricao"})
public record RelatorioDTO(Long id, Long obraId, LocalDate dataInspecao, String descricao) {

    public static RelatorioDTO de(RelatorioSeguranca relatorio) {
        return new RelatorioDTO(relatorio.getId(), relatorio.getObra().getId(),
                relatorio.getDataInspecao(), relatorio.getDescricao());
    }
}
