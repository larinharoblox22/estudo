package br.com.exemplo.gestaoobras.api;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * Formato único de erro da API: todo erro tratado responde com este JSON,
 * para o sistema cliente poder tratá-los sempre da mesma forma.
 *
 * @param status   código HTTP (repetido no corpo para facilitar logs)
 * @param erro     nome curto do código HTTP
 * @param mensagem explicação para quem consome a API
 * @param detalhes lista de problemas específicos (ex.: um item por campo inválido)
 */
@JsonbPropertyOrder({"status", "erro", "mensagem", "detalhes"})
public record ErroDTO(int status, String erro, String mensagem, List<String> detalhes) {

    /** 422: o JSON está bem formado, mas viola uma regra de negócio. */
    public static final int UNPROCESSABLE_CONTENT = 422;

    static Response resposta(int status, String erro, String mensagem, List<String> detalhes) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroDTO(status, erro, mensagem, detalhes))
                .build();
    }
}
