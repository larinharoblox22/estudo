package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.dao.RelatorioSegurancaDao;
import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import br.com.exemplo.gestaoobras.model.StatusObra;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Implementação do {@link RelatorioSegurancaService}.
 */
@Stateless
public class RelatorioSegurancaServiceBean implements RelatorioSegurancaService {

    // @EJB: injeção específica de EJB (aqui, outro session bean, via sua interface @Local).
    // A chamada passa pelo contêiner e, por ser REQUIRED, ENTRA na transação já aberta:
    // as duas classes compartilham a mesma transação e o mesmo Persistence Context.
    @EJB
    private ObraService obraService;

    // @Inject: injeção CDI, usada para tudo o que não é EJB (como os DAOs).
    @Inject
    private RelatorioSegurancaDao relatorioDao;

    @Override
    public List<RelatorioSeguranca> listarPorObra(Long obraId) {
        obraService.buscarPorId(obraId); // garante EntidadeNaoEncontradaException se a obra não existir
        return relatorioDao.listarPorObra(obraId);
    }

    @Override
    public RelatorioSeguranca buscarPorId(Long id) {
        return relatorioDao.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Relatório " + id + " não encontrado."));
    }

    @Override
    public List<RelatorioSeguranca> listarRecentes(int limite) {
        return relatorioDao.listarRecentesComObra(limite);
    }

    @Override
    public RelatorioSeguranca registrar(Long obraId, RelatorioSeguranca relatorio, boolean interditarObra) {
        Obra obra = obraService.buscarPorId(obraId); // MANAGED nesta transação

        if (obra.isConcluida()) {
            throw new RegraNegocioException("Não é possível registrar relatórios em uma obra concluída.");
        }

        relatorio.setObra(obra);
        relatorioDao.inserir(relatorio);

        if (interditarObra) {
            // Nenhum "update" explícito: a obra está MANAGED, e o dirty checking
            // gera o UPDATE no commit, na mesma transação do INSERT acima.
            obra.setStatus(StatusObra.SUSPENSA);
        }
        return relatorio;
    }

    @Override
    public void excluir(Long id) {
        if (!relatorioDao.removerPorId(id)) {
            throw new EntidadeNaoEncontradaException("Relatório " + id + " não encontrado.");
        }
    }
}
