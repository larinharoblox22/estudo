package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.StatusObra;

import java.util.List;

/**
 * Contrato SOMENTE DE LEITURA sobre obras (ISP — Segregação de Interfaces).
 * <p>
 * Quem só precisa consultar (como a API REST para sistemas externos) depende
 * desta interface e nem "enxerga" as operações de escrita. Ela é implementada
 * pelo mesmo EJB {@code ObraServiceBean}, porque {@link ObraService} a estende:
 * o CDI aceita injetar um EJB por qualquer superinterface da sua interface
 * {@code @Local}.
 */
public interface ObraConsulta {

    /** Todas as obras, ordenadas por nome. */
    List<Obra> listarTodas();

    /** Obras com o status informado; com {@code null}, retorna todas. */
    List<Obra> listarPorStatus(StatusObra status);

    /** Pesquisa parcial por nome; com termo vazio, retorna todas. */
    List<Obra> pesquisarPorNome(String termo);

    /**
     * Pesquisa com filtros opcionais combináveis: cada filtro nulo ou vazio é ignorado.
     *
     * @param nome   trecho do nome (sem diferenciar maiúsculas), ou {@code null}
     * @param status status exato, ou {@code null}
     */
    List<Obra> pesquisar(String nome, StatusObra status);

    /** @throws EntidadeNaoEncontradaException se a obra não existir */
    Obra buscarPorId(Long id);
}
