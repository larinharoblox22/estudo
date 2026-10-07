package br.com.exemplo.gestaoobras.dao;

import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Acesso a dados da entidade {@link RelatorioSeguranca}.
 */
@ApplicationScoped
public class RelatorioSegurancaDao extends GenericDao<RelatorioSeguranca, Long> {

    // r.obra.id lê diretamente a FK obra_id: não gera JOIN nem carrega a obra.
    private static final String JPQL_LISTAR_POR_OBRA =
            "SELECT r FROM RelatorioSeguranca r WHERE r.obra.id = :obraId ORDER BY r.dataInspecao DESC";

    // JOIN FETCH: traz a obra na MESMA query, contornando o LAZY só neste caso de uso.
    private static final String JPQL_LISTAR_RECENTES_COM_OBRA =
            "SELECT r FROM RelatorioSeguranca r JOIN FETCH r.obra ORDER BY r.dataInspecao DESC, r.id DESC";

    private static final String JPQL_CONTAR_POR_OBRA =
            "SELECT COUNT(r) FROM RelatorioSeguranca r WHERE r.obra.id = :obraId";

    private static final String JPQL_REMOVER_POR_OBRA =
            "DELETE FROM RelatorioSeguranca r WHERE r.obra.id = :obraId";

    public RelatorioSegurancaDao() {
        super(RelatorioSeguranca.class);
    }

    /** Relatórios de uma obra, do mais recente para o mais antigo (a obra fica como proxy LAZY). */
    public List<RelatorioSeguranca> listarPorObra(Long obraId) {
        return em.createQuery(JPQL_LISTAR_POR_OBRA, RelatorioSeguranca.class)
                 .setParameter("obraId", obraId)
                 .getResultList();
    }

    /**
     * Últimos relatórios de todas as obras, já com a obra carregada.
     * Seguro de usar fora da transação (ex.: em uma página JSF exibindo o nome da obra).
     */
    public List<RelatorioSeguranca> listarRecentesComObra(int limite) {
        return em.createQuery(JPQL_LISTAR_RECENTES_COM_OBRA, RelatorioSeguranca.class)
                 .setMaxResults(limite)
                 .getResultList();
    }

    public long contarPorObra(Long obraId) {
        return em.createQuery(JPQL_CONTAR_POR_OBRA, Long.class)
                 .setParameter("obraId", obraId)
                 .getSingleResult();
    }

    /**
     * Bulk delete: um único DELETE no banco, sem carregar as entidades.
     * Atenção: ignora o Persistence Context e os callbacks @PreRemove.
     *
     * @return número de registros removidos
     */
    public int removerPorObra(Long obraId) {
        return em.createQuery(JPQL_REMOVER_POR_OBRA)
                 .setParameter("obraId", obraId)
                 .executeUpdate();
    }
}
