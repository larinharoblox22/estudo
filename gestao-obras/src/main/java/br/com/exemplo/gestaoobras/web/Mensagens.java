package br.com.exemplo.gestaoobras.web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Atalhos para exibir mensagens na tela (FacesMessage) a partir dos beans.
 * Os textos vêm do messages.properties, o mesmo arquivo usado nas páginas.
 */
final class Mensagens {

    /** Nome da variável do resource bundle declarado no faces-config.xml. */
    private static final String BUNDLE = "msg";

    private Mensagens() {
    }

    /** Texto do messages.properties, com os {0}, {1}... substituídos pelos argumentos. */
    static String texto(String chave, Object... argumentos) {
        FacesContext contexto = FacesContext.getCurrentInstance();
        ResourceBundle bundle = contexto.getApplication().getResourceBundle(contexto, BUNDLE);
        return new MessageFormat(bundle.getString(chave), contexto.getViewRoot().getLocale())
                .format(argumentos);
    }

    static void info(String chave, Object... argumentos) {
        adicionar(FacesMessage.SEVERITY_INFO, texto(chave, argumentos));
    }

    /**
     * Exibe um erro e marca a requisição como "falha de validação".
     * O PrimeFaces repassa isso ao navegador como {@code args.validationFailed},
     * e a página usa essa informação para manter o diálogo aberto.
     */
    static void erro(String mensagem) {
        adicionar(FacesMessage.SEVERITY_ERROR, mensagem);
        FacesContext.getCurrentInstance().validationFailed();
    }

    private static void adicionar(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
