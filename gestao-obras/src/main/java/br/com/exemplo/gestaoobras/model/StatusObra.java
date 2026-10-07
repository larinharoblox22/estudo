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
}
