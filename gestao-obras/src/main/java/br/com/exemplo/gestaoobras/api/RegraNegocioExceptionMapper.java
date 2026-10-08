package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.service.EntidadeNaoEncontradaException;
import br.com.exemplo.gestaoobras.service.RegraNegocioException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Traduz as exceções de negócio (Passo 3) para respostas HTTP.
 * <p>
 * {@code @Provider}: o JAX-RS registra a classe sozinho. Quando um recurso lança
 * uma RegraNegocioException (ou subclasse), o JAX-RS chama {@link #toResponse}
 * em vez de devolver um erro 500. Os recursos ficam livres de try/catch.
 */
@Provider
public class RegraNegocioExceptionMapper implements ExceptionMapper<RegraNegocioException> {

    @Override
    public Response toResponse(RegraNegocioException e) {
        if (e instanceof EntidadeNaoEncontradaException) {
            return ErroDTO.resposta(404, "Not Found", e.getMessage(), List.of());
        }
        // 422: a requisição está bem formada, mas viola uma regra de negócio
        // (ex.: registrar relatório em obra concluída).
        return ErroDTO.resposta(ErroDTO.UNPROCESSABLE_CONTENT, "Unprocessable Content", e.getMessage(), List.of());
    }
}
