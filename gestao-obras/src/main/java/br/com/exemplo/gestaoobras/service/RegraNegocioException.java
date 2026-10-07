package br.com.exemplo.gestaoobras.service;

import jakarta.ejb.ApplicationException;

/**
 * Violação de uma regra de negócio (ex.: nome de obra duplicado).
 * <p>
 * {@code @ApplicationException}: para o contêiner EJB, esta é uma exceção
 * "esperada" do negócio. Ela chega ao cliente como está (sem ser embrulhada em
 * EJBException) e, com {@code rollback = true}, desfaz a transação inteira.
 * Subclasses herdam esse comportamento.
 */
@ApplicationException(rollback = true)
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
