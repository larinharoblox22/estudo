package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import jakarta.ejb.Local;

import java.util.List;

/**
 * Contrato de negócio para os relatórios de segurança das obras.
 */
@Local
public interface RelatorioSegurancaService {

    /**
     * Relatórios de uma obra, do mais recente para o mais antigo.
     *
     * @throws EntidadeNaoEncontradaException se a obra não existir
     */
    List<RelatorioSeguranca> listarPorObra(Long obraId);

    /** Últimos relatórios de todas as obras, com a obra já carregada. */
    List<RelatorioSeguranca> listarRecentes(int limite);

    /**
     * Registra um relatório na obra. Se {@code interditarObra} for verdadeiro
     * (não conformidade grave), a obra passa para SUSPENSA na mesma transação.
     *
     * @throws EntidadeNaoEncontradaException se a obra não existir
     * @throws RegraNegocioException          se a obra estiver concluída
     */
    RelatorioSeguranca registrar(Long obraId, RelatorioSeguranca relatorio, boolean interditarObra);

    /** @throws EntidadeNaoEncontradaException se o relatório não existir */
    void excluir(Long id);
}
