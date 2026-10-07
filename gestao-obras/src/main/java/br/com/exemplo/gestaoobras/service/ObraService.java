package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.StatusObra;
import jakarta.ejb.Local;

import java.util.List;

/**
 * Contrato de negócio para a gestão de obras (Session Facade).
 * <p>
 * {@code @Local}: interface de negócio do EJB. Os clientes (bean JSF no Passo 4,
 * recurso REST no Passo 5) dependem desta abstração, nunca da implementação.
 * Todo método roda em uma transação; as violações de regra são sinalizadas com
 * {@link RegraNegocioException}, que desfaz a transação.
 */
@Local
public interface ObraService {

    /** Todas as obras, ordenadas por nome. */
    List<Obra> listarTodas();

    /** Obras com o status informado; com {@code null}, retorna todas. */
    List<Obra> listarPorStatus(StatusObra status);

    /** Pesquisa parcial por nome; com termo vazio, retorna todas. */
    List<Obra> pesquisarPorNome(String termo);

    /** @throws EntidadeNaoEncontradaException se a obra não existir */
    Obra buscarPorId(Long id);

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
