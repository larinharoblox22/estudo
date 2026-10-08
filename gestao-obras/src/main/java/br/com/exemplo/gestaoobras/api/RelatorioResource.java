package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.service.RelatorioSegurancaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Recurso REST "relatorios": /api/relatorios.
 */
@Path("/relatorios")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class RelatorioResource {

    private static final int LIMITE_MAXIMO = 100;

    @Inject
    private RelatorioSegurancaService relatorioService;

    /**
     * GET /api/relatorios/recentes?limite=5
     * Um limite máximo protege o servidor de alguém pedir um milhão de registros.
     */
    @GET
    @Path("recentes")
    public List<RelatorioDTO> listarRecentes(@QueryParam("limite") @DefaultValue("10") int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new BadRequestException(ErroDTO.resposta(400, "Bad Request",
                    "O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".", List.of()));
        }
        return relatorioService.listarRecentes(limite).stream()
                .map(RelatorioDTO::de)
                .toList();
    }

    /** GET /api/relatorios/5 → 200, ou 404 se não existir. */
    @GET
    @Path("{id: \\d+}")
    public RelatorioDTO buscar(@PathParam("id") Long id) {
        return RelatorioDTO.de(relatorioService.buscarPorId(id));
    }

    /**
     * DELETE /api/relatorios/5 → 204 No Content (método void = resposta sem corpo).
     * Repetir a chamada responde 404: o ESTADO do servidor não muda mais
     * (DELETE é idempotente), mas a RESPOSTA pode ser diferente.
     */
    @DELETE
    @Path("{id: \\d+}")
    public void excluir(@PathParam("id") Long id) {
        relatorioService.excluir(id);
    }
}
