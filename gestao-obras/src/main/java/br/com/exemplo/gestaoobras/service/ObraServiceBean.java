package br.com.exemplo.gestaoobras.service;

import br.com.exemplo.gestaoobras.dao.ObraDao;
import br.com.exemplo.gestaoobras.dao.RelatorioSegurancaDao;
import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.StatusObra;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Implementação do {@link ObraService}.
 * <p>
 * {@code @Stateless}: o contêiner mantém um pool de instâncias e entrega uma
 * diferente a cada chamada; por isso a classe não guarda estado de cliente.
 * Sem nenhuma anotação extra, todo método público já é transacional
 * (REQUIRED): o contêiner abre a transação na entrada e faz commit na saída,
 * ou rollback se for lançada uma exceção de sistema ou de aplicação com rollback.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED) // é o padrão; explícito só para fins didáticos
public class ObraServiceBean implements ObraService {

    @Inject
    private ObraDao obraDao;

    @Inject
    private RelatorioSegurancaDao relatorioDao;

    @Override
    public List<Obra> listarTodas() {
        return obraDao.listarTodos();
    }

    @Override
    public List<Obra> listarPorStatus(StatusObra status) {
        if (status == null) {
            return listarTodas();
        }
        return obraDao.listarPorStatus(status);
    }

    @Override
    public List<Obra> pesquisarPorNome(String termo) {
        if (termo == null || termo.isBlank()) {
            return listarTodas();
        }
        return obraDao.pesquisarPorNome(termo.trim());
    }

    @Override
    public Obra buscarPorId(Long id) {
        return obraDao.buscarPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Obra " + id + " não encontrada."));
    }

    @Override
    public Obra salvar(Obra obra) {
        validarNomeUnico(obra);

        if (obra.getId() == null) {
            obraDao.inserir(obra);
            return obra;
        }

        Obra atual = buscarPorId(obra.getId());
        if (!atual.getStatus().podeMudarPara(obra.getStatus())) {
            throw new RegraNegocioException("Uma obra concluída não pode ter o status alterado.");
        }
        // Se outro usuário gravou a obra depois que ela foi lida, as versões diferem
        // e o merge lança OptimisticLockException (o @Version do Passo 1).
        return obraDao.atualizar(obra);
    }

    @Override
    public void excluir(Long id) {
        Obra obra = buscarPorId(id);

        // Duas escritas, uma única transação (atomicidade): se a segunda falhar,
        // o contêiner desfaz também a primeira e nenhum relatório fica órfão.
        relatorioDao.removerPorObra(obra.getId());
        obraDao.removerPorId(obra.getId());
    }

    private void validarNomeUnico(Obra obra) {
        if (obraDao.existeComNome(obra.getNome(), obra.getId())) {
            throw new RegraNegocioException("Já existe uma obra com o nome \"" + obra.getNome() + "\".");
        }
    }
}
