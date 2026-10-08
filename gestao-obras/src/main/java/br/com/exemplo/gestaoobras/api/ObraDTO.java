package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.StatusObra;
import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * Representação JSON de uma obra para sistemas externos (o "contrato" da API).
 * <p>
 * É um {@code record}: classe imutável cujos campos, construtor, getters,
 * equals/hashCode e toString são gerados pelo compilador.
 * A entidade não é exposta diretamente: campos internos (versão, auditoria)
 * ficam de fora, e a API não muda se a tabela mudar.
 * {@code @JsonbPropertyOrder}: ordem dos campos no JSON (o padrão do JSON-B é alfabética).
 */
@JsonbPropertyOrder({"id", "nome", "localizacao", "status"})
public record ObraDTO(Long id, String nome, String localizacao, StatusObra status) {

    public static ObraDTO de(Obra obra) {
        return new ObraDTO(obra.getId(), obra.getNome(), obra.getLocalizacao(), obra.getStatus());
    }
}
