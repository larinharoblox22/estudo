package pt.exemplo.gestaoobras.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Operações CRUD comuns a todas as entidades.
 * <p>
 * O DAO não abre transações: {@code MANDATORY} exige que quem o chama
 * (a camada de negócio, Passo 3) já tenha uma transação ativa. Assim, a
 * fronteira transacional é decidida num único sítio.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo da chave primária
 */
@Transactional(Transactional.TxType.MANDATORY)
public abstract class GenericDao<T, ID> {

    // Proxy gerido pelo contentor: cada transação JTA recebe o seu próprio
    // Persistence Context, por isso é seguro partilhar este campo entre threads.
    @PersistenceContext(unitName = "gestaoObrasPU")
    protected EntityManager em;

    private final Class<T> classeEntidade;

    protected GenericDao(Class<T> classeEntidade) {
        this.classeEntidade = classeEntidade;
    }

    /** NEW → MANAGED. O INSERT é executado no flush/commit (com IDENTITY, de imediato). */
    public void inserir(T entidade) {
        em.persist(entidade);
    }

    /**
     * DETACHED → MANAGED. Devolve a instância gerida; a que foi passada
     * continua detached e não deve voltar a ser usada.
     */
    public T atualizar(T entidade) {
        return em.merge(entidade);
    }

    /** MANAGED → REMOVED. Devolve {@code false} se o registo já não existir. */
    public boolean removerPorId(ID id) {
        T entidade = em.find(classeEntidade, id);
        if (entidade == null) {
            return false;
        }
        em.remove(entidade);
        return true;
    }

    /** Consulta primeiro o Persistence Context (cache de 1.º nível) e só depois a BD. */
    public Optional<T> buscarPorId(ID id) {
        return Optional.ofNullable(em.find(classeEntidade, id));
    }

    public List<T> listarTodos() {
        CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(classeEntidade);
        query.select(query.from(classeEntidade));
        return em.createQuery(query).getResultList();
    }
}
