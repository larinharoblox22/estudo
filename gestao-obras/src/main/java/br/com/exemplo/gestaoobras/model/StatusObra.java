package br.com.exemplo.gestaoobras.model;

/**
 * Estados possíveis de uma obra ao longo do seu ciclo de vida.
 * Persistido como texto (EnumType.STRING) para que reordenar ou acrescentar
 * constantes nunca corrompa os dados já gravados.
 */
public enum StatusObra {

    PLANEJADA("Planejada"),
    EM_ANDAMENTO("Em andamento"),
    SUSPENSA("Suspensa"),
    CONCLUIDA("Concluída");

    private final String descricao;

    StatusObra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Regra de transição de status: uma obra concluída é um estado final
     * e não pode voltar a nenhum outro status.
     */
    public boolean podeMudarPara(StatusObra novoStatus) {
        return this != CONCLUIDA || novoStatus == CONCLUIDA;
    }
}
