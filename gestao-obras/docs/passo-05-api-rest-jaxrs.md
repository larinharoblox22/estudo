# Passo 5 — A Integração (API REST com JAX-RS)

> Neste passo a aplicação passa a conversar com **outros sistemas**. Sempre que uma sigla ou
> termo em inglês aparecer pela primeira vez, ele é explicado ali mesmo; no final há um
> **Glossário** para revisão rápida.

## Arquivos deste passo

```
gestao-obras/
├── pom.xml                                        ← + compilação com -parameters
├── postman/
│   └── gestao-obras-api.postman_collection.json   ← coleção para importar no Postman
└── src/main/java/br/com/exemplo/gestaoobras/
    ├── api/                                       ← NOVO: a camada REST
    │   ├── ApiConfig.java                         ← liga o JAX-RS em /api
    │   ├── ObraResource.java                      ← /api/obras (somente leitura)
    │   ├── RelatorioResource.java                 ← /api/relatorios
    │   ├── ObraDTO.java / RelatorioDTO.java       ← o JSON de saída
    │   ├── NovoRelatorioDTO.java                  ← o JSON de entrada do POST
    │   ├── ErroDTO.java                           ← o JSON de erro (formato único)
    │   ├── RegraNegocioExceptionMapper.java       ← exceções de negócio → 404 / 422
    │   └── ValidacaoExceptionMapper.java          ← validação → 400
    ├── service/
    │   ├── ObraConsulta.java                      ← NOVO: contrato só de leitura (ISP)
    │   ├── ObraService.java                       ← agora estende ObraConsulta
    │   ├── ObraServiceBean.java                   ← + pesquisar(nome, status)
    │   └── RelatorioSeguranca*.java               ← + buscarPorId
    └── dao/ObraDao.java                           ← + pesquisar com Criteria API
```

---

## 0. Conceitos de base

- **API** (*Application Programming Interface*, "interface de programação de aplicações"): um
  conjunto de operações que um sistema oferece para **outros programas** usarem. A tela JSF é
  para pessoas; a API é para sistemas (um *app* de inspeção no tablet do técnico de segurança, um
  painel de indicadores, o **ERP** — *Enterprise Resource Planning*, o sistema de gestão da empresa).
- **REST** (*REpresentational State Transfer*, "transferência de estado representacional"): um
  **estilo de arquitetura** para APIs sobre HTTP, descrito por Roy Fielding em 2000. As ideias centrais:
  - tudo é um **recurso** (uma obra, a lista de relatórios de uma obra), identificado por uma
    **URI** (*Uniform Resource Identifier*, o "endereço" do recurso — ex.: `/api/obras/1`);
  - o cliente recebe uma **representação** do recurso (aqui, em JSON), não o objeto em si;
  - as operações usam os **métodos (verbos) do HTTP** com o significado padrão (seção 3);
  - a comunicação é **stateless** (sem estado de sessão; seção 2).
- **JSON** (*JavaScript Object Notation*): formato de texto para dados estruturados, como
  `{"id": 1, "nome": "Edifício Aurora"}`. É o formato mais usado em APIs REST.
- **JAX-RS** (hoje **Jakarta RESTful Web Services**; antes *Java API for RESTful Web Services*):
  a especificação Jakarta EE para criar APIs REST com anotações. O WildFly traz a implementação
  **RESTEasy** (no GlassFish/Payara é o **Jersey**) — mesma lógica de JPA/Hibernate e JSF/Mojarra.
- **JSON-B** (*Jakarta JSON Binding*): a especificação que converte objetos Java ↔ JSON. A
  implementação no WildFly é o **Yasson**. Converter objeto → texto é **serializar**; o caminho
  inverso é **desserializar**.

> 🟢 **Paralelo Spring Boot:** o JAX-RS corresponde ao **Spring Web MVC** com `@RestController`, e
> o JSON-B corresponde ao **Jackson**. A tabela completa está na seção 10.

---

## 1. Como o JAX-RS funciona

### As anotações principais

| Anotação | Onde | O que faz | Spring Boot |
|---|---|---|---|
| `@ApplicationPath("/api")` | `ApiConfig` | Liga o JAX-RS e define o prefixo de todas as URLs | `spring.mvc.servlet.path` / `server.servlet.context-path` |
| `@Path("/obras")` | Classe | Caminho do recurso | `@RequestMapping("/obras")` |
| `@GET`, `@POST`, `@DELETE`... | Método | Qual verbo HTTP o método atende | `@GetMapping`, `@PostMapping`... |
| `@Path("{id: \\d+}")` | Método | Sub-caminho com variável; `\\d+` (expressão regular: "um ou mais dígitos") só aceita números | `@GetMapping("/{id}")` |
| `@PathParam("id")` | Parâmetro | Lê a variável da URL | `@PathVariable` |
| `@QueryParam("status")` + `@DefaultValue` | Parâmetro | Lê da *query string* (`?status=...`) | `@RequestParam(defaultValue=...)` |
| `@Produces(APPLICATION_JSON)` | Classe/método | Formato que o método **devolve** | `produces = ...` |
| `@Consumes(APPLICATION_JSON)` | Método | Formato que o método **aceita** no corpo | `consumes = ...` |
| `@Context UriInfo` | Campo | Dados da URL atual (usado para montar o `Location`) | `ServletUriComponentsBuilder` |
| `Response` | Retorno | Resposta completa: status, headers e corpo | `ResponseEntity` |
| `@Provider` + `ExceptionMapper` | Classe | Traduz exceções em respostas HTTP | `@RestControllerAdvice` + `@ExceptionHandler` |
| `@Valid` | Parâmetro | Valida o corpo com Bean Validation antes de chamar o método | `@Valid @RequestBody` |

### O caminho de uma requisição

```
Cliente (Postman, outro sistema)
   │  POST /gestao-obras/api/obras/1/relatorios   (corpo JSON)
   ▼
Undertow (o servidor web dentro do WildFly)
   ▼
RESTEasy (JAX-RS)
   1. escolhe a classe e o método pelo caminho + verbo  → ObraResource.registrarRelatorio
   2. converte os parâmetros: "1" → Long id; JSON → NovoRelatorioDTO (JSON-B)
   3. valida (@NotNull, @Valid)                         → falhou? ValidacaoExceptionMapper → 400
   4. chama o método → que chama o EJB (transação, Passo 3)
                                                        → RegraNegocioException? mapper → 404/422
   5. serializa o retorno em JSON (JSON-B)
   ▼
201 Created + Location + JSON
```

---

## 2. Statelessness ("ausência de estado")

**Definição:** em uma API *stateless*, **cada requisição traz tudo o que o servidor precisa para
atendê-la**. O servidor não guarda, entre uma requisição e outra, nenhuma informação de
"conversa" com o cliente (sem sessão HTTP, sem "o usuário estava na página 2").

⚠️ Stateless **não** significa "sem dados": as obras continuam no banco. O que não existe é
**estado de sessão** do cliente no servidor.

### A prova no nosso projeto: API vs tela JSF

Headers reais capturados no WildFly:

```
GET /gestao-obras/api/obras        →  (nenhum Set-Cookie)                       ← API REST
GET /gestao-obras/obras.xhtml      →  Set-Cookie: JSESSIONID=36CmXV7K…; path=/gestao-obras  ← tela JSF
```

A tela JSF cria uma sessão (o `@ViewScoped` do Passo 4 guarda o estado da tela nela); a API não.
A coleção do Postman tem um teste que verifica exatamente isso.

| | Tela JSF (Passo 4) | API REST (Passo 5) |
|---|---|---|
| Estado entre requisições | Sim: árvore de componentes + bean `@ViewScoped` na sessão | Não |
| Ciclo de vida do controller | Uma instância por **tela aberta** | `@RequestScoped`: uma instância **por requisição** |
| Identificação do cliente | Cookie `JSESSIONID` | Nenhuma (em produção: um *token* em cada requisição) |

### Por que isso importa

- **Escalabilidade horizontal:** com várias instâncias do servidor atrás de um **balanceador de
  carga** (*load balancer*, o componente que distribui as requisições entre servidores), **qualquer**
  instância atende **qualquer** requisição. Com sessão, é preciso *sticky sessions* (prender o
  usuário a um servidor) ou replicar sessões.
- **Resiliência:** se um servidor reinicia, nenhum cliente perde "a conversa".
- **Cache:** respostas de GET podem ser guardadas por *proxies* e navegadores, porque não dependem
  de uma sessão.
- **Autenticação sem sessão:** em uma API real, cada requisição levaria um *token* — por exemplo,
  um **JWT** (*JSON Web Token*, um texto assinado com a identidade do usuário) no header
  `Authorization: Bearer <token>`. (Não implementado neste projeto de estudo.)

> 🟢 **Paralelo Spring Boot:** o `@RestController` é *singleton* e também não deve guardar estado
> de cliente; o Spring Security, configurado como `STATELESS`, não cria sessão.

---

## 3. Os verbos HTTP corretos

Dois conceitos para entender a tabela:
- **Seguro** (*safe*): o método **não altera** o estado do servidor (só lê).
- **Idempotente**: repetir a mesma requisição N vezes tem o **mesmo efeito no servidor** que fazer
  uma vez. (A **resposta** pode mudar — veja o DELETE abaixo.)

| Verbo | Significado | Seguro | Idempotente | Sucesso típico | Usado aqui |
|---|---|---|---|---|---|
| **GET** | Ler um recurso ou uma coleção | ✅ | ✅ | 200 OK | Obras, relatórios |
| **POST** | Criar um recurso novo (ou executar uma ação) | ❌ | ❌ | 201 Created + `Location` | Registrar relatório |
| **PUT** | Substituir o recurso **inteiro** | ❌ | ✅ | 200 OK / 204 No Content | — |
| **PATCH** | Alterar **parte** do recurso | ❌ | ❌ (em geral) | 200 OK / 204 No Content | — |
| **DELETE** | Remover o recurso | ❌ | ✅ | 204 No Content | Excluir relatório |
| **HEAD** | Igual ao GET, mas só os headers | ✅ | ✅ | 200 OK | Automático (JAX-RS) |
| **OPTIONS** | Quais métodos a URL aceita | ✅ | ✅ | 200 OK + `Allow` | Automático (JAX-RS) |

### Os endpoints da nossa API

**Endpoint** é a combinação "verbo + URL" que o cliente chama.

| Método e URL | O que faz | Sucesso | Erros possíveis |
|---|---|---|---|
| `GET /api/obras?nome=&status=` | Lista obras (filtros opcionais e combináveis) | 200 | 400 (status inválido) |
| `GET /api/obras/{id}` | Uma obra | 200 | 404 |
| `GET /api/obras/{id}/relatorios` | Relatórios da obra (sub-recurso) | 200 | 404 |
| `POST /api/obras/{id}/relatorios` | Registra relatório (com interdição opcional) | 201 + `Location` | 400, 404, 415, 422 |
| `GET /api/relatorios/recentes?limite=` | Últimos relatórios (limite de 1 a 100) | 200 | 400, 404 (ver pegadinha) |
| `GET /api/relatorios/{id}` | Um relatório | 200 | 404 |
| `DELETE /api/relatorios/{id}` | Exclui relatório | 204 | 404 |
| Qualquer escrita em `/api/obras/{id}` | Não existe: obras são somente leitura | — | 405 + `Allow` |
| Qualquer GET com `Accept: application/xml` | A API só produz JSON | — | 406 |

### Boas práticas de URL aplicadas

- **Substantivos, não verbos:** `/obras`, e não `/listarObras` — o verbo já está no método HTTP.
- **Plural para coleções:** `/obras` (a coleção) e `/obras/1` (um item dela).
- **Hierarquia para sub-recursos:** `/obras/1/relatorios` = "os relatórios **da** obra 1". No POST,
  a obra vem da URL e não do corpo: o cliente não consegue mandar um relatório "para a obra errada".
- **Query string para filtros, ordenação e paginação:** `?status=SUSPENSA` refina a coleção; não
  identifica um recurso novo.

### POST: 201 Created + `Location` (resultado real)

```
POST /api/obras/1/relatorios
{"dataInspecao":"2026-10-07","descricao":"Rede de proteção instalada no 3º pavimento.","interditarObra":false}

HTTP/1.1 201 Created
Location: http://localhost:8080/gestao-obras/api/relatorios/5
{"id":5,"obraId":1,"dataInspecao":"2026-10-07","descricao":"Rede de proteção instalada no 3º pavimento."}
```

O header **`Location`** diz ao cliente **onde** está o recurso recém-criado. POST **não é
idempotente**: enviar duas vezes cria dois relatórios.

### DELETE é idempotente, mas a resposta muda (resultado real)

```
DELETE /api/relatorios/5   →  204 No Content
DELETE /api/relatorios/5   →  404 Not Found  {"status":404,"erro":"Not Found","mensagem":"Relatório 5 não encontrado.","detalhes":[]}
```

O **efeito** no servidor é o mesmo (o relatório não existe), por isso o DELETE é idempotente. A
**resposta** é diferente, e isso é permitido.

### E se a API também alterasse obras? (pergunta comum de entrevista)

| Operação | Verbo e URL | Resposta |
|---|---|---|
| Criar obra | `POST /api/obras` | 201 + `Location: /api/obras/{novoId}` |
| Substituir obra inteira | `PUT /api/obras/{id}` | 200 com a obra, ou 204 |
| Mudar só o status | `PATCH /api/obras/{id}` com `{"status":"SUSPENSA"}` | 200 / 204 |
| Concorrência (o `@Version` do Passo 1) | O GET devolve um header **`ETag`** (uma "impressão digital" da versão); o PUT envia **`If-Match`** com esse valor | **412 Precondition Failed** se outra pessoa alterou antes |

---

## 4. Códigos de status HTTP

Os códigos são agrupados pelo primeiro dígito: **2xx** = sucesso, **4xx** = erro de **quem
chamou** (o cliente pode corrigir), **5xx** = erro **do servidor**.

| Código | Nome | Quando a nossa API usa |
|---|---|---|
| 200 | OK | GET com sucesso |
| 201 | Created | POST criou o relatório |
| 204 | No Content | DELETE com sucesso (método `void`) |
| 400 | Bad Request | Dados inválidos, status inválido, limite fora da faixa, corpo ausente |
| 404 | Not Found | Obra ou relatório inexistente (e a pegadinha abaixo) |
| 405 | Method Not Allowed | Escrita em `/api/obras/{id}` (somente leitura) |
| 406 | Not Acceptable | O cliente pediu um formato que a API não produz (`Accept`) |
| 415 | Unsupported Media Type | O cliente enviou um formato que a API não aceita (`Content-Type`) |
| 422 | Unprocessable Content | JSON válido, mas que viola uma **regra de negócio** (obra concluída) |
| 500 | Internal Server Error | Erro inesperado (exceção não tratada) |

**400 vs 422:** o 400 diz "sua requisição está malformada ou com dados inválidos"; o 422 diz "sua
requisição está correta, mas o negócio não permite". (O nome oficial atual, desde a RFC 9110 de
2022, é *Unprocessable Content*; servidores mais antigos ainda escrevem *Unprocessable Entity*, como
o WildFly na linha de status.) **RFC** (*Request for Comments*) é o nome dos documentos que
padronizam os protocolos da internet.

**`Accept` vs `Content-Type`:** o `Accept` diz o formato que o cliente quer **receber** (errado →
406); o `Content-Type` diz o formato do que o cliente **envia** (errado → 415). Isso é a
**negociação de conteúdo** (*content negotiation*).

### ⚠️ A pegadinha da especificação JAX-RS (comprovada no teste)

```
GET /api/relatorios/recentes?limite=abc   →  404 Not Found   (e não 400!)
```

A especificação JAX-RS manda responder **404** quando um `@QueryParam` ou `@PathParam` **não pode
ser convertido** para o tipo Java do parâmetro (`"abc"` → `int`). A justificativa é que, para a
especificação, essa URL "não corresponde" a nenhum recurso. (Para `@HeaderParam` e
`@CookieParam`, a regra é 400.)

Por isso o parâmetro `status` de `/api/obras` é recebido como `String` e convertido à mão:

```java
private static StatusObra converterStatus(String valor) {
    ...
    try {
        return StatusObra.valueOf(valor.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new BadRequestException(ErroDTO.resposta(400, "Bad Request",
                "Status inválido: " + valor,
                List.of("Valores aceitos: " + Arrays.toString(StatusObra.values()))));
    }
}
```

Resultado: `GET /api/obras?status=XYZ` → **400** com a lista de valores aceitos. O `limite` foi
deixado como `int` de propósito, para você ver as duas situações lado a lado no Postman.

> 🟢 **Paralelo Spring Boot:** no Spring, um `@RequestParam int` inválido gera **400**
> (`MethodArgumentTypeMismatchException`). É uma diferença real entre as duas tecnologias.

---

## 5. DTOs: por que não devolver a entidade?

**DTO** (*Data Transfer Object*, "objeto de transferência de dados") é uma classe feita só para
**carregar dados** entre camadas ou sistemas, sem regras de negócio. Usamos *records* do Java 16+:
classes imutáveis em que o compilador gera construtor, *getters*, `equals`, `hashCode` e `toString`.

```java
@JsonbPropertyOrder({"id", "nome", "localizacao", "status"})
public record ObraDTO(Long id, String nome, String localizacao, StatusObra status) {
    public static ObraDTO de(Obra obra) { ... }
}
```

Por que não serializar a entidade `Obra` diretamente:

1. **LAZY e transação:** o JSON-B percorre todos os *getters*. Em um `RelatorioSeguranca`, isso
   tocaria a obra LAZY **fora da transação** (o EJB já terminou) e lançaria
   `LazyInitializationException` (Passo 1). O `RelatorioDTO` leva só o `obraId`:
   `getObra().getId()` não dispara consulta, porque o *proxy* LAZY já conhece o ID (é o valor da FK).
2. **Contrato estável:** a API é um **contrato** com outros sistemas. Se a tabela ganhar uma coluna,
   o JSON não muda sem querermos.
3. **Segurança:** campos internos (`versao`, `dataCriacao`) não vazam. Na entrada, o
   `NovoRelatorioDTO` não tem `id` nem `obraId`, o que evita *mass assignment* (o cliente mandar
   campos que não deveria alterar).
4. **Validação específica:** as regras de entrada da API ficam no DTO de entrada.

`@JsonbPropertyOrder` define a ordem dos campos no JSON; sem ela, o JSON-B usa ordem alfabética
(vimos `{"id":1,"localizacao":...,"nome":...}` antes de anotar).

> 🟢 **Paralelo Spring Boot:** mesma prática (*records* + Jackson). No Jackson, a ordem é
> `@JsonPropertyOrder`.

---

## 6. Tratamento de erros com `ExceptionMapper`

Todo erro tratado responde no **mesmo formato** (`ErroDTO`), para o sistema cliente tratar os
erros sempre do mesmo jeito:

```json
{"status":400,"erro":"Bad Request","mensagem":"Dados inválidos na requisição.",
 "detalhes":["dataInspecao: A data da inspeção não pode estar no futuro","descricao: A descrição é obrigatória"]}
```

| Situação | Quem trata | Resposta |
|---|---|---|
| `EntidadeNaoEncontradaException` (Passo 3) | `RegraNegocioExceptionMapper` | 404 |
| Outra `RegraNegocioException` | `RegraNegocioExceptionMapper` | 422 |
| Falha de Bean Validation no corpo | `ValidacaoExceptionMapper` | 400 com um detalhe por campo |
| Parâmetro inválido (`status`, `limite`) | `BadRequestException` com resposta pronta | 400 |
| Verbo não suportado / formato não aceito | O próprio JAX-RS | 405 / 406 / 415 |

Pontos que valem uma explicação:

- **Os recursos não têm `try/catch`.** Eles só chamam o service; quem traduz a exceção para HTTP é
  o mapeador. Separação de responsabilidades.
- **As exceções de negócio chegam "limpas" do EJB** porque são `@ApplicationException` (Passo 3).
  Uma exceção de **sistema** chegaria embrulhada em `EJBException` e viraria **500**.
- **Qual mapeador vence?** O JAX-RS escolhe o mapeador cujo tipo é a superclasse **mais próxima**
  da exceção lançada. O RESTEasy tem um mapeador próprio para falhas de validação, mas o nosso, para
  `ConstraintViolationException`, foi o usado — comprovado pelo formato `ErroDTO` nas respostas 400.
- **Nomes dos campos:** o `pom.xml` agora compila com `-parameters` (guarda no `.class` o nome real
  dos parâmetros). Antes disso, a mensagem do corpo ausente dizia `arg1: ...`; agora diz
  `dados: O corpo da requisição é obrigatório`.

> 🟢 **Paralelo Spring Boot:** `@RestControllerAdvice` + `@ExceptionHandler`. O Spring 6 também
> oferece o `ProblemDetail`, que segue a **RFC 9457** (antiga RFC 7807; *Problem Details for HTTP APIs*, formato
> padronizado de erro com o tipo `application/problem+json`). É uma alternativa padronizada ao nosso
> `ErroDTO` — vale citar na entrevista.

---

## 7. ISP aplicado: o contrato `ObraConsulta`

O último ponto pendente do `solid.md`. Antes, quem quisesse ler obras precisava depender do
`ObraService` inteiro, inclusive de `salvar` e `excluir`. Agora:

```
        ObraConsulta  (só leitura: listar, pesquisar, buscarPorId)
              ▲
              │ extends
        ObraService  @Local  (+ salvar, excluir)
              ▲
              │ implements
        ObraServiceBean  @Stateless
```

```java
// API REST: só precisa ler obras
@Inject
private ObraConsulta obraConsulta;

// Tela JSF (Passo 4): lê e escreve
@Inject
private ObraService obraService;
```

- **O mesmo EJB atende os dois.** O CDI aceita injetar um EJB por qualquer **superinterface** da sua
  interface `@Local`, então o `ObraServiceBean` continua sendo uma única classe.
- **Menor privilégio** (*least privilege*: dar a cada parte só o acesso de que ela precisa): o
  `ObraResource` **não consegue** chamar `salvar` ou `excluir`, nem por engano — o compilador
  impede. E o JAX-RS responde **405** a qualquer tentativa de escrita em `/api/obras/{id}`.

### A busca com filtros opcionais (Criteria API)

`GET /api/obras?nome=aurora&status=EM_ANDAMENTO` precisa aceitar qualquer combinação de filtros.
Com JPQL em `String`, seria preciso concatenar pedaços de consulta (frágil). Com a **Criteria API**
(Passo 2), cada filtro só entra no `WHERE` se tiver valor:

```java
List<Predicate> filtros = new ArrayList<>();
if (nome != null && !nome.isBlank()) {
    filtros.add(cb.like(cb.lower(obra.get("nome")), "%" + nome.trim().toLowerCase() + "%"));
}
if (status != null) {
    filtros.add(cb.equal(obra.get("status"), status));
}
consulta.select(obra).where(filtros.toArray(new Predicate[0])).orderBy(cb.asc(obra.get("nome")));
```

Os valores viram **parâmetros** (*bind parameters*) no SQL final, então continuam protegidos contra
SQL Injection.

---

## 8. A coleção do Postman

**Postman** é uma ferramenta para montar, enviar e testar requisições HTTP. Uma **coleção**
(*collection*) é um arquivo JSON com requisições organizadas em pastas.

### Como importar

1. Abra o Postman → **Import** (ou *File → Import*).
2. Selecione `gestao-obras/postman/gestao-obras-api.postman_collection.json`.
3. A coleção "Gestão de Obras — API REST (JAX-RS)" aparece com 3 pastas e 24 requisições.

### O que tem dentro

| Pasta | Requisições |
|---|---|
| **1. Obras (somente leitura)** | Listar, filtrar por status, pesquisar por nome, buscar por ID, relatórios da obra |
| **2. Relatórios de segurança** | POST (201 + Location), GET pelo ID, recentes, DELETE (204), DELETE de novo (404), POST com interdição, conferir obra suspensa |
| **3. Erros e semântica HTTP** | 404, 400 (status, dados, corpo ausente), 415, 422, 405, 406, OPTIONS, HEAD, a pegadinha do 404 e limite fora da faixa |

- **Variáveis da coleção** (aba *Variables*): `baseUrl` (`http://localhost:8080/gestao-obras/api`),
  `obraId`, `obraConcluidaId`, `relatorioId`. Os IDs são preenchidos pelos **scripts** das próprias
  requisições — por exemplo, o POST guarda o ID criado para o GET e o DELETE seguintes usarem.
- **Testes automáticos** (aba *Tests*): cada requisição verifica o código de status esperado e,
  quando faz sentido, o conteúdo (ex.: "Header Location aponta para o novo relatório", "Stateless:
  nenhuma sessão é criada").
- **Documentação** (aba *Docs*): cada requisição explica o conceito que demonstra.
- **Rodar tudo de uma vez:** clique na coleção → **Run** (*Collection Runner*). A ordem importa,
  porque algumas requisições usam IDs guardados pelas anteriores.

### Verificação executada

A coleção foi executada com o **Newman** (o executor de coleções do Postman em linha de comando)
contra o WildFly com a aplicação implantada:

```
requests    24 executadas, 0 falhas
assertions  47 executadas, 0 falhas
```

Ela foi executada também uma segunda vez sem reimplantar (com os dados já alterados pela primeira
execução), e continuou passando: o script da primeira requisição escolhe automaticamente uma obra
que ainda aceita relatórios. Para rodar você mesmo, com o servidor no ar:

```bash
npx newman run gestao-obras/postman/gestao-obras-api.postman_collection.json
```

---

## 9. SOLID neste passo

| Princípio | Onde |
|---|---|
| **S** | Recursos só traduzem HTTP ↔ chamadas de service; DTOs só carregam dados; mapeadores só traduzem exceções. |
| **O** | Um novo tipo de erro de negócio vira resposta HTTP sem mexer nos recursos (basta o mapeador). |
| **L** | `EntidadeNaoEncontradaException` é tratada como `RegraNegocioException` e ganha seu próprio código (404). |
| **I** | ✅ `ObraResource` depende de `ObraConsulta` (só leitura), não do `ObraService` completo. |
| **D** | Os recursos dependem de interfaces (`ObraConsulta`, `RelatorioSegurancaService`), não dos EJBs. |

---

## 10. Resumo JAX-RS ↔ Spring Boot

| Conceito | JAX-RS (aqui) | Spring Boot |
|---|---|---|
| Ativação / prefixo | `@ApplicationPath("/api")` | Automático; prefixo via configuração |
| Recurso | Classe com `@Path` | `@RestController` + `@RequestMapping` |
| Verbos | `@GET`, `@POST`, `@DELETE` | `@GetMapping`, `@PostMapping`, `@DeleteMapping` |
| Variável da URL | `@PathParam` | `@PathVariable` |
| Query string | `@QueryParam` + `@DefaultValue` | `@RequestParam(defaultValue = ...)` |
| Conversão de parâmetro falhou | **404** (query/path) | **400** |
| Corpo | Parâmetro sem anotação + `@Consumes` | `@RequestBody` |
| Resposta completa | `Response.created(uri).entity(...)` | `ResponseEntity.created(uri).body(...)` |
| Método `void` | **204 No Content** | 200 OK (204 só com `@ResponseStatus`) |
| Erros | `ExceptionMapper` + `@Provider` | `@RestControllerAdvice` + `@ExceptionHandler` |
| JSON | JSON-B (Yasson) | Jackson |
| Ciclo de vida do recurso | Por requisição (`@RequestScoped`) | *Singleton* |

---

## 11. Perguntas prováveis na entrevista

1. **O que é REST?** Um estilo de arquitetura: recursos identificados por URIs, representações
   (JSON), verbos HTTP com significado padrão, comunicação stateless.
2. **O que significa stateless?** Cada requisição traz tudo o que é necessário; o servidor não
   guarda sessão do cliente. Permite escalar horizontalmente sem *sticky sessions*.
3. **Diferença entre PUT, PATCH e POST?** PUT substitui o recurso inteiro e é idempotente; PATCH
   altera parte; POST cria (ou executa uma ação) e não é idempotente.
4. **O que é idempotência? O DELETE é idempotente se a segunda chamada devolve 404?** Sim: o efeito
   no servidor é o mesmo; a resposta pode mudar.
5. **Que status devolver ao criar um recurso?** 201 Created com o header `Location`.
6. **400 ou 422?** 400 para requisição malformada/dados inválidos; 422 para requisição correta que
   viola regra de negócio.
7. **406 vs 415?** 406: o formato pedido no `Accept` não é produzido. 415: o formato enviado no
   `Content-Type` não é aceito.
8. **Por que usar DTOs em vez de expor a entidade?** LAZY fora da transação, contrato estável,
   segurança (campos internos, *mass assignment*), validação específica.
9. **Como tratar exceções no JAX-RS?** `ExceptionMapper` anotado com `@Provider`; o JAX-RS escolhe o
   mapeador da superclasse mais próxima.
10. **O que acontece se um `@QueryParam int` recebe "abc"?** Pela especificação, 404. Solução:
    receber como `String` e validar, ou usar um `ParamConverter`.
11. **Qual o nível de maturidade REST desta API?** No **Modelo de Maturidade de Richardson**
    (níveis 0 a 3), está no **nível 2**: recursos + verbos + códigos de status corretos. O nível 3
    seria **HATEOAS** (*Hypermedia As The Engine Of Application State*: as respostas trazem links
    para as próximas ações possíveis). O header `Location` é um pequeno passo nessa direção.

---

## Glossário

| Termo | Significado |
|---|---|
| **`Accept`** | Header em que o cliente diz o formato que quer **receber** (ex.: `application/json`). |
| **API** | *Application Programming Interface*: operações que um sistema oferece para outros programas. |
| **Balanceador de carga** (*load balancer*) | Componente que distribui as requisições entre várias instâncias do servidor. |
| **Bind parameter** | Valor enviado separado do texto SQL (`?`), o que impede SQL Injection. |
| **Coleção (Postman)** | Arquivo JSON com requisições organizadas em pastas, com variáveis, testes e documentação. |
| **`Content-Type`** | Header que diz o formato do corpo **enviado** (ex.: `application/json`). |
| **Criteria API** | API do JPA para montar consultas com objetos Java, útil para filtros opcionais. |
| **Desserializar** | Converter texto (JSON) em objeto Java. |
| **DTO** | *Data Transfer Object*: classe só para carregar dados entre camadas ou sistemas. |
| **Endpoint** | Combinação de verbo HTTP + URL que o cliente chama (ex.: `GET /api/obras`). |
| **ERP** | *Enterprise Resource Planning*: sistema integrado de gestão da empresa. |
| **ETag / If-Match** | Header com a "versão" de um recurso / header que exige essa versão para alterar (412 se mudou). |
| **`ExceptionMapper`** | Interface do JAX-RS que traduz uma exceção em resposta HTTP. |
| **HATEOAS** | *Hypermedia As The Engine Of Application State*: respostas com links para as próximas ações. |
| **Header** | Metadado de uma requisição ou resposta HTTP (`Location`, `Allow`, `Accept`...). |
| **Idempotente** | Repetir a requisição N vezes tem o mesmo efeito no servidor que fazer uma vez. |
| **JAX-RS** | *Jakarta RESTful Web Services*: a especificação Jakarta EE para APIs REST. |
| **Jersey** | Implementação do JAX-RS usada no GlassFish e no Payara. |
| **JSON** | *JavaScript Object Notation*: formato de texto para dados estruturados. |
| **JSON-B** | *Jakarta JSON Binding*: especificação que converte objetos Java ↔ JSON. |
| **JWT** | *JSON Web Token*: texto assinado que identifica o usuário, enviado a cada requisição. |
| **`Location`** | Header da resposta 201 com a URL do recurso criado. |
| **Mass assignment** | Falha em que o cliente consegue alterar campos que não deveria, mandando-os no corpo. |
| **Menor privilégio** (*least privilege*) | Dar a cada parte do sistema só o acesso de que ela precisa. |
| **Negociação de conteúdo** | Acordo de formato entre cliente (`Accept`/`Content-Type`) e servidor (`@Produces`/`@Consumes`). |
| **Newman** | Executor de coleções do Postman em linha de comando. |
| **`@Provider`** | Anotação que registra uma classe de infraestrutura no JAX-RS (ex.: um `ExceptionMapper`). |
| **Query string** | Parte da URL depois do `?`, com parâmetros `chave=valor`. |
| **Record** | Tipo do Java (16+) para classes imutáveis de dados, com código gerado pelo compilador. |
| **Recurso** | Qualquer "coisa" exposta pela API e identificada por uma URI (uma obra, uma lista de relatórios). |
| **Representação** | O formato em que um recurso é enviado (aqui, JSON). |
| **REST** | *REpresentational State Transfer*: estilo de arquitetura para APIs sobre HTTP. |
| **RESTEasy** | Implementação do JAX-RS que vem no WildFly. |
| **RFC** | *Request for Comments*: documentos que padronizam protocolos da internet (ex.: RFC 9110, HTTP). |
| **Richardson (Modelo de Maturidade)** | Classificação de APIs REST em níveis 0 a 3 (3 = HATEOAS). |
| **Seguro** (*safe*) | Método HTTP que não altera o estado do servidor (GET, HEAD, OPTIONS). |
| **Serializar** | Converter objeto Java em texto (JSON) ou *bytes*. |
| **Stateless** | Sem estado de sessão: cada requisição é independente e completa. |
| **Sticky session** | Prender um usuário sempre ao mesmo servidor, por causa da sessão. |
| **Sub-recurso** | Recurso que pertence a outro, expresso na URL (`/obras/1/relatorios`). |
| **Token** | Credencial enviada em cada requisição para identificar o usuário (ex.: JWT). |
| **Undertow** | O servidor web (HTTP) dentro do WildFly. |
| **URI / URL** | Identificador / endereço de um recurso (`/api/obras/1`). |
| **Verbo HTTP** | O método da requisição: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS. |
| **Yasson** | Implementação do JSON-B que vem no WildFly. |
| **2xx / 4xx / 5xx** | Classes de status: sucesso / erro de quem chamou / erro do servidor. |
