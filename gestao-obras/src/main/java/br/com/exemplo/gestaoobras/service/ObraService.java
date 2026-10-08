package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.model.Obra;
import jakarta.ejb.Local;

/**
 * Contrato de negócio completo para a gestão de obras (Session Facade):
 * as consultas herdadas de {@link ObraConsulta} mais as operações de escrita.
 * <p>
 * {@code @Local}: interface de negócio do EJB. Os clientes (bean JSF no Passo 4,
 * recurso REST no Passo 5) dependem de abstrações, nunca da implementação.
 * Todo método roda em uma transação; as violações de regra são sinalizadas com
 * {@link RegraNegocioException}, que desfaz a transação.
 */
@Local
public interface ObraService extends ObraConsulta {

    /**
     * Insere (ID nulo) ou atualiza (ID preenchido) uma obra.
     *
     * @return a obra gerenciada, já com ID e versão atualizados
     * @throws RegraNegocioException se o nome já existir ou se a obra estiver
     *                               concluída e o novo status for outro
     * @throws EntidadeNaoEncontradaException se for uma atualização de obra inexistente
     */
    Obra salvar(Obra obra);

    /**
     * Exclui a obra e todos os seus relatórios de segurança, em uma única transação.
     *
     * @throws EntidadeNaoEncontradaException se a obra não existir
     */
    void excluir(Long id);
}
