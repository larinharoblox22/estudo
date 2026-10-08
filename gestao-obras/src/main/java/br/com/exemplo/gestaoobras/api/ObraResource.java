package br.com.exemplo.gestaoobras.api;

import br.com.exemplo.gestaoobras.model.RelatorioSeguranca;
import br.com.exemplo.gestaoobras.model.StatusObra;
import br.com.exemplo.gestaoobras.service.ObraConsulta;
import br.com.exemplo.gestaoobras.service.RelatorioSegurancaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Recurso REST "obras": /api/obras.
 * <p>
 * As obras são SOMENTE LEITURA para sistemas externos: só existem métodos GET.
 * Um PUT ou DELETE em /api/obras/{id} recebe 405 (Method Not Allowed) do próprio JAX-RS.
 * <p>
 * {@code @RequestScoped}: uma instância nova por requisição, sem estado guardado
 * entre chamadas (o oposto do @ViewScoped da tela JSF). Isso é o "stateless" do REST.
 */
@Path("/obras")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ObraResource {

    // ISP: depende só do contrato de LEITURA de obras, não do ObraService completo.
    @Inject
    private ObraConsulta obraConsulta;

    @Inject
    private RelatorioSegurancaService relatorioService;

    // Informações da URL da requisição atual (usada para montar o header Location).
    @Context
    private UriInfo uriInfo;

    /**
     * GET /api/obras?nome=aurora&status=EM_ANDAMENTO
     * Os dois filtros são opcionais e podem ser combinados.
     */
    @GET
    public List<ObraDTO> listar(@QueryParam("nome") String nome,
                                @QueryParam("status") String status) {
        return obraConsulta.pesquisar(nome, converterStatus(status)).stream()
                .map(ObraDTO::de)
                .toList();
    }

    /** GET /api/obras/1 → 200, ou 404 se não existir (via mapeador de exceção). */
    @GET
    @Path("{id: \\d+}")
    public ObraDTO buscar(@PathParam("id") Long id) {
        return ObraDTO.de(obraConsulta.buscarPorId(id));
    }

    /** GET /api/obras/1/relatorios → relatórios da obra (sub-recurso). */
    @GET
    @Path("{id: \\d+}/relatorios")
    public List<RelatorioDTO> listarRelatorios(@PathParam("id") Long id) {
        return relatorioService.listarPorObra(id).stream()
                .map(RelatorioDTO::de)
                .toList();
    }

    /**
     * POST /api/obras/1/relatorios → cria um relatório na obra.
     * Responde 201 Created, com o header Location apontando para o novo recurso
     * e o relatório criado no corpo.
     */
    @POST
    @Path("{id: \\d+}/relatorios")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registrarRelatorio(@PathParam("id") Long id,
                                       @NotNull(message = "O corpo da requisição é obrigatório")
                                       @Valid NovoRelatorioDTO dados) {
        RelatorioSeguranca criado = relatorioService.registrar(id, dados.paraEntidade(), dados.interditarObra());

        URI local = uriInfo.getBaseUriBuilder()
                .path(RelatorioResource.class)
                .path(String.valueOf(criado.getId()))
                .build();
        return Response.created(local).entity(RelatorioDTO.de(criado)).build();
    }

    /**
     * Converte o texto da URL para o enum. Feito à mão de propósito: se o parâmetro
     * fosse declarado como StatusObra, um valor inválido geraria 404 (regra da
     * especificação JAX-RS para @QueryParam), e o correto aqui é 400.
     */
    private static StatusObra converterStatus(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return StatusObra.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErroDTO.resposta(400, "Bad Request",
                    "Status inválido: " + valor,
                    List.of("Valores aceitos: " + Arrays.toString(StatusObra.values()))));
        }
    }
}
