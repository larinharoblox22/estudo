package br.com.exemplo.gestaoobras.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Traduz falhas de Bean Validation (ex.: descrição vazia no POST) para
 * 400 Bad Request, com um item em "detalhes" para cada campo inválido.
 */
@Provider
public class ValidacaoExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<String> detalhes = e.getConstraintViolations().stream()
                .map(violacao -> nomeDoCampo(violacao) + ": " + violacao.getMessage())
                .sorted()
                .toList();
        return ErroDTO.resposta(400, "Bad Request", "Dados inválidos na requisição.", detalhes);
    }

    /** O caminho vem como "registrarRelatorio.dados.descricao"; interessa só o último nome. */
    private static String nomeDoCampo(ConstraintViolation<?> violacao) {
        String nome = "";
        for (Path.Node no : violacao.getPropertyPath()) {
            nome = no.getName();
        }
        return nome;
    }
}
