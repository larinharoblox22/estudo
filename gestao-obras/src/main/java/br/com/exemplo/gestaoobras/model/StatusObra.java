package br.com.exemplo.gestaoobras.model;

/**
 * Estados possíveis de uma obra ao longo do seu ciclo de vida.
 * Persistido como texto (EnumType.STRING) para que reordenar ou acrescentar
 * constantes nunca corrompa os dados já gravados.
 * <p>
 * Os textos exibidos na tela ficam em {@code messages.properties}
 * (chaves {@code status.*}), e não aqui: o enum só muda por motivo de negócio.
 */
public enum StatusObra {

    PLANEJADA,
    EM_ANDAMENTO,
    SUSPENSA,
    CONCLUIDA;

    /**
     * Regra de transição de status: uma obra concluída é um estado final
     * e não pode voltar a nenhum outro status.
     */
    public boolean podeMudarPara(StatusObra novoStatus) {
        return this != CONCLUIDA || novoStatus == CONCLUIDA;
    }
}
