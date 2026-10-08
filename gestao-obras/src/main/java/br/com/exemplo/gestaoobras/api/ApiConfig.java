package br.com.exemplo.gestaoobras.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Liga o JAX-RS e define o prefixo de todos os endpoints REST:
 * http://localhost:8080/gestao-obras/api/...
 * <p>
 * Sem sobrescrever nenhum método, o servidor descobre sozinho as classes com
 * {@code @Path} (recursos) e {@code @Provider} (como os mapeadores de exceção).
 */
@ApplicationPath("/api")
public class ApiConfig extends Application {
}
