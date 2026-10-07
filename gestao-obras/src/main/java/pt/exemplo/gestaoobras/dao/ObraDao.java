package pt.exemplo.gestaoobras.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import pt.exemplo.gestaoobras.model.Obra;
import pt.exemplo.gestaoobras.model.StatusObra;

import java.util.List;

/**
 * Acesso a dados da entidade {@link Obra}.
 * Todo o JPQL sobre obras vive aqui e em mais lado nenhum.
 */
@ApplicationScoped
public class ObraDao extends GenericDao<Obra, Long> {

    // JPQL: consulta sobre entidades e atributos Java, não sobre tabelas e colunas.
    private static final String JPQL_LISTAR_TODAS =
            "SELECT o FROM Obra o ORDER BY o.nome";

    private static final String JPQL_LISTAR_POR_STATUS =
            "SELECT o FROM Obra o WHERE o.status = :status ORDER BY o.nome";

    private static final String JPQL_PESQUISAR_POR_NOME =
            "SELECT o FROM Obra o WHERE LOWER(o.nome) LIKE :termo ORDER BY o.nome";

    private static final String JPQL_CONTAR_COM_NOME =
            "SELECT COUNT(o) FROM Obra o WHERE LOWER(o.nome) = LOWER(:nome)";

    private static final String JPQL_CONTAR_COM_NOME_EXCETO_ID =
            "SELECT COUNT(o) FROM Obra o WHERE LOWER(o.nome) = LOWER(:nome) AND o.id <> :id";

    public ObraDao() {
        super(Obra.class);
    }

    /** Sobrepõe a versão genérica para garantir uma ordenação estável. */
    @Override
    public List<Obra> listarTodos() {
        return em.createQuery(JPQL_LISTAR_TODAS, Obra.class)
                 .getResultList();
    }

    public List<Obra> listarPorStatus(StatusObra status) {
        return em.createQuery(JPQL_LISTAR_POR_STATUS, Obra.class)
                 .setParameter("status", status)
                 .getResultList();
    }

    /**
     * Pesquisa parcial e sem distinção de maiúsculas.
     * O termo é passado como parâmetro (nunca concatenado na String),
     * o que impede SQL Injection.
     */
    public List<Obra> pesquisarPorNome(String termo) {
        return em.createQuery(JPQL_PESQUISAR_POR_NOME, Obra.class)
                 .setParameter("termo", "%" + termo.toLowerCase() + "%")
                 .getResultList();
    }

    /**
     * Verifica se já existe outra obra com o mesmo nome.
     *
     * @param idIgnorar ID da obra em edição (para não colidir consigo própria);
     *                  {@code null} numa inserção
     */
    public boolean existeComNome(String nome, Long idIgnorar) {
        TypedQuery<Long> query = (idIgnorar == null)
                ? em.createQuery(JPQL_CONTAR_COM_NOME, Long.class)
                : em.createQuery(JPQL_CONTAR_COM_NOME_EXCETO_ID, Long.class)
                    .setParameter("id", idIgnorar);
        return query.setParameter("nome", nome).getSingleResult() > 0;
    }
}
