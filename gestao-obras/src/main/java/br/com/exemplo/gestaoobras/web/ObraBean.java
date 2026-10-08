package br.com.exemplo.gestaoobras.web;

import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.StatusObra;
import br.com.exemplo.gestaoobras.service.ObraService;
import br.com.exemplo.gestaoobras.service.RegraNegocioException;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.OptimisticLockException;

import java.io.Serializable;
import java.util.List;

/**
 * Controller (o "C" do MVC) da tela de obras.
 * <p>
 * {@code @Named}: torna o bean acessível nas páginas como {@code #{obraBean}}.
 * {@code @ViewScoped}: o bean vive enquanto o usuário estiver nesta tela,
 * atravessando várias requisições AJAX (abrir diálogo, salvar, filtrar...).
 * Por isso precisa ser {@link Serializable}: o estado da view pode ser
 * guardado na sessão HTTP e serializado.
 */
@Named
@ViewScoped
public class ObraBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Depende do contrato de negócio (@Local), nunca do DAO nem do ObraServiceBean.
    @Inject
    private ObraService obraService;

    private List<Obra> obras;
    private StatusObra filtroStatus;
    private Obra obraEmEdicao;

    /** Executado uma única vez, logo após a criação do bean e das injeções. */
    @PostConstruct
    public void inicializar() {
        obraEmEdicao = new Obra();
        carregarObras();
    }

    // ---- Ações disparadas pela página ----

    public void filtrar() {
        carregarObras();
    }

    public void novaObra() {
        obraEmEdicao = new Obra();
    }

    public void editar(Obra obra) {
        // Cópia nova, vinda do banco: editar a instância da tabela mudaria a linha
        // na tela mesmo se o usuário cancelasse, e a versão (@Version) poderia estar velha.
        obraEmEdicao = obraService.buscarPorId(obra.getId());
    }

    public void salvar() {
        try {
            Obra salva = obraService.salvar(obraEmEdicao);
            Mensagens.info("obra.salva", salva.getNome());
            carregarObras();
        } catch (RegraNegocioException e) {
            Mensagens.erro(e.getMessage());
        } catch (EJBException e) {
            if (!causadaPorConcorrencia(e)) {
                throw e;
            }
            Mensagens.erro(Mensagens.texto("obra.alterada.por.outro"));
        }
    }

    public void excluir(Obra obra) {
        try {
            obraService.excluir(obra.getId());
            Mensagens.info("obra.excluida", obra.getNome());
        } catch (RegraNegocioException e) {
            Mensagens.erro(e.getMessage());
        }
        carregarObras();
    }

    /** Chamado pelo bean de relatórios quando uma interdição muda o status de uma obra. */
    public void recarregar() {
        carregarObras();
    }

    // ---- Dados lidos pela página ----
    // Getters só retornam campos: o JSF chama um getter várias vezes por requisição,
    // então NUNCA se consulta o banco dentro de um getter.

    public List<Obra> getObras() {
        return obras;
    }

    public StatusObra[] getStatusDisponiveis() {
        return StatusObra.values();
    }

    /** Cor da etiqueta de status na tabela (lógica de apresentação, por isso fica no bean). */
    public String severidade(StatusObra status) {
        return switch (status) {
            case PLANEJADA -> "warning";
            case EM_ANDAMENTO -> "success";
            case SUSPENSA -> "danger";
            case CONCLUIDA -> "info";
        };
    }

    public boolean isEdicao() {
        return obraEmEdicao != null && obraEmEdicao.getId() != null;
    }

    public StatusObra getFiltroStatus() {
        return filtroStatus;
    }

    public void setFiltroStatus(StatusObra filtroStatus) {
        this.filtroStatus = filtroStatus;
    }

    public Obra getObraEmEdicao() {
        return obraEmEdicao;
    }

    // ---- Auxiliares ----

    private void carregarObras() {
        obras = obraService.listarPorStatus(filtroStatus);
    }

    /** Percorre a cadeia de causas: o contêiner EJB embrulha a OptimisticLockException. */
    private static boolean causadaPorConcorrencia(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof OptimisticLockException) {
                return true;
            }
        }
        return false;
    }
}
