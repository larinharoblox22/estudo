package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corpo (JSON) aceito no POST de um novo relatório.
 * <p>
 * DTO de entrada separado do de saída: o cliente não pode mandar {@code id}
 * nem {@code obraId} (a obra vem da URL), o que evita "mass assignment"
 * (o cliente alterar campos que não deveria). As anotações de Bean Validation
 * são verificadas pelo JAX-RS antes de o método do recurso ser chamado.
 */
public record NovoRelatorioDTO(

        @NotNull(message = "A data da inspeção é obrigatória")
        @PastOrPresent(message = "A data da inspeção não pode estar no futuro")
        LocalDate dataInspecao,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 2000, message = "A descrição não pode exceder 2000 caracteres")
        String descricao,

        boolean interditarObra) {

    public RelatorioSeguranca paraEntidade() {
        RelatorioSeguranca relatorio = new RelatorioSeguranca();
        relatorio.setDataInspecao(dataInspecao);
        relatorio.setDescricao(descricao);
        return relatorio;
    }
}
