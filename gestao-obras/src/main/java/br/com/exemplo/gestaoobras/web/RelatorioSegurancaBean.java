package br.com.exemplo.gestaoobras.web;

import br.com.exemplo.gestaoobras.model.Obra;
import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import br.com.exemplo.gestaoobras.service.RegraNegocioException;
import br.com.exemplo.gestaoobras.service.RelatorioSegurancaService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller do diálogo de relatórios de segurança de uma obra.
 * Separado do {@link ObraBean} para que cada bean tenha uma única responsabilidade.
 */
@Named
@ViewScoped
public class RelatorioSegurancaBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private RelatorioSegurancaService relatorioService;

    // Mesmo escopo (@ViewScoped) e mesma tela: o CDI injeta a MESMA instância
    // que a página está usando, e não uma nova.
    @Inject
    private ObraBean obraBean;

    private Obra obra;
    private List<RelatorioSeguranca> relatorios;
    private RelatorioSeguranca novoRelatorio;
    private boolean interditar;

    @PostConstruct
    public void inicializar() {
        prepararNovoRelatorio();
    }

    // ---- Ações disparadas pela página ----

    public void abrir(Obra obra) {
        this.obra = obra;
        prepararNovoRelatorio();
        carregarRelatorios();
    }

    public void registrar() {
        try {
            relatorioService.registrar(obra.getId(), novoRelatorio, interditar);
            Mensagens.info(interditar ? "relatorio.registrado.interdicao" : "relatorio.registrado");
            if (interditar) {
                obraBean.recarregar(); // a obra mudou de status: a tabela principal precisa refletir
            }
            prepararNovoRelatorio();
            carregarRelatorios();
        } catch (RegraNegocioException e) {
            Mensagens.erro(e.getMessage());
        }
    }

    public void excluir(RelatorioSeguranca relatorio) {
        try {
            relatorioService.excluir(relatorio.getId());
            Mensagens.info("relatorio.excluido");
        } catch (RegraNegocioException e) {
            Mensagens.erro(e.getMessage());
        }
        carregarRelatorios();
    }

    // ---- Dados lidos pela página ----

    public Obra getObra() {
        return obra;
    }

    public List<RelatorioSeguranca> getRelatorios() {
        return relatorios;
    }

    public RelatorioSeguranca getNovoRelatorio() {
        return novoRelatorio;
    }

    public boolean isInterditar() {
        return interditar;
    }

    public void setInterditar(boolean interditar) {
        this.interditar = interditar;
    }

    /** Data máxima do calendário: não existe inspeção no futuro. */
    public LocalDate getHoje() {
        return LocalDate.now();
    }

    // ---- Auxiliares ----

    private void prepararNovoRelatorio() {
        novoRelatorio = new RelatorioSeguranca();
        novoRelatorio.setDataInspecao(LocalDate.now());
        interditar = false;
    }

    private void carregarRelatorios() {
        relatorios = relatorioService.listarPorObra(obra.getId());
    }
}
