package br.com.exemplo.gestaoobras.service;

/**
 * O registro pedido não existe. Tipo próprio para que a API REST (Passo 5)
 * possa traduzi-lo para HTTP 404, e as demais regras para 422/400.
 */
public class EntidadeNaoEncontradaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
