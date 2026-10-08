# Resumão para a entrevista — Java EE / Jakarta EE (Gestão de Obras)

> **Como usar este documento**
> - **Parte 1 (15 minutos):** o que é obrigatório saber de cor, para revisar antes de entrar.
> - **Parte 2:** como apresentar o projeto (*pitch*) e contar as histórias reais do desenvolvimento.
> - **Parte 3:** banco de perguntas e respostas, por tema. Cada pergunta tem:
>   - **Resposta curta** (para falar em ~30 segundos);
>   - **🛠️ Destrinchando na prática** (o exemplo concreto do projeto, o código, o resultado real
>     dos testes e os *trade-offs* — os prós e contras de cada escolha — para você aprofundar);
>   - **↪️ Se perguntarem mais** (as perguntas de acompanhamento mais comuns);
>   - **📖 Onde estudar** (link para a seção exata dos relatórios).
> - Perguntas marcadas com **🌐** não estão nos relatórios do projeto: são temas que aparecem com
>   frequência em entrevistas da área (pesquisados em listas de perguntas e relatos de candidatos),
>   respondidos com conhecimento geral.
> - Siglas são explicadas na primeira vez que aparecem; os glossários completos estão no fim de cada
>   relatório.

### Siglas que aparecem aqui (leia primeiro)

| Sigla | Significado | Em uma frase |
|---|---|---|
| **AJAX** | *Asynchronous JavaScript and XML* | Requisição em segundo plano que atualiza só um pedaço da página |
| **API** | *Application Programming Interface* | Operações que um sistema oferece para outros programas |
| **CDI** | *Contexts and Dependency Injection* | Injeção de dependências e escopos do Jakarta EE |
| **CMT** | *Container-Managed Transactions* | O contêiner abre e fecha a transação de cada método do EJB |
| **CRUD** | *Create, Read, Update, Delete* | As quatro operações básicas sobre dados |
| **DAO** | *Data Access Object* | Classe que só lê e grava dados no banco |
| **DI / IoC / DIP** | *Dependency Injection / Inversion of Control / Dependency Inversion Principle* | Mecanismo de injeção / contêiner no controle / princípio de depender de abstrações |
| **DTO** | *Data Transfer Object* | Objeto só para transportar dados (ex.: o JSON da API) |
| **EJB** | *Enterprise JavaBeans* | Componentes de negócio com transação, pool e segurança dados pelo servidor |
| **HTTP** | *HyperText Transfer Protocol* | O protocolo da web (GET, POST…) |
| **JAX-RS** | *Jakarta RESTful Web Services* | Especificação para criar APIs REST |
| **JDBC** | *Java Database Connectivity* | API de baixo nível para executar SQL |
| **JNDI** | *Java Naming and Directory Interface* | "Lista telefônica" de recursos do servidor (ex.: DataSource) |
| **JPA / JPQL** | *Jakarta Persistence (API) / Jakarta Persistence Query Language* | Especificação de ORM / linguagem de consulta sobre entidades |
| **JSF** | *JavaServer Faces* (hoje *Jakarta Faces*) | Framework de telas web baseado em componentes |
| **JSON** | *JavaScript Object Notation* | Formato de texto para dados (`{"id": 1}`) |
| **JTA** | *Jakarta Transactions* | Transações coordenadas pelo servidor |
| **JWT** | *JSON Web Token* | Credencial assinada enviada a cada requisição |
| **MDB** | *Message-Driven Bean* | EJB que consome mensagens de uma fila (JMS — *Jakarta Messaging*) |
| **MVC** | *Model-View-Controller* | Separação entre dados/regras, tela e controle |
| **ORM** | *Object-Relational Mapping* | Mapear objetos Java para tabelas |
| **OSIV** | *Open Session In View* | Sessão do banco aberta até a tela renderizar (padrão do Spring Boot) |
| **POJO** | *Plain Old Java Object* | Classe Java comum, sem herdar de nada especial |
| **REST** | *REpresentational State Transfer* | Estilo de arquitetura de APIs sobre HTTP |
| **SOLID** | Cinco princípios de design (SRP, OCP, LSP, ISP, DIP) | Detalhados na pergunta S1 |
| **URI / URL** | *Uniform Resource Identifier / Locator* | O endereço de um recurso (`/api/obras/1`) |
| **WAR / JAR** | *Web Archive / Java Archive* | Pacote de aplicação web para servidor / pacote Java (no Spring Boot, executável) |
| **XHTML** | *eXtensible HTML* | HTML com as regras rígidas do XML, usado nas páginas JSF |

---

## Parte 1 — Revisão de 15 minutos

### A arquitetura em uma imagem

```
 Navegador (pessoa)                 Sistema externo (app de campo, ERP)
        │  HTTP + AJAX                         │  HTTP + JSON
        ▼                                      ▼
 obras.xhtml + ObraBean (JSF, @ViewScoped)   ObraResource (JAX-RS, @RequestScoped)
        │  @Inject ObraService                 │  @Inject ObraConsulta (só leitura)
        ▼                                      ▼
 ObraServiceBean / RelatorioSegurancaServiceBean  (EJB @Stateless, transação REQUIRED)
        │  @Inject
        ▼
 ObraDao / RelatorioSegurancaDao  (CDI @ApplicationScoped, @Transactional(MANDATORY))
        │  @PersistenceContext EntityManager (proxy)
        ▼
 JPA/Hibernate → DataSource JNDI (java:comp/DefaultDataSource) → H2 (ou PostgreSQL)
```

### 20 fatos para saber de cor

| # | Fato | 📖 |
|---|---|---|
| 1 | **J2EE → Java EE → Jakarta EE**; o pacote mudou de `javax.*` para `jakarta.*` (Jakarta EE 9+). | [Passo 1 › 0. Contexto: J2EE → Java EE → Jakarta EE](passo-01-modelo-jpa.md#0-contexto-j2ee--java-ee--jakarta-ee) |
| 2 | **JPA é especificação; Hibernate é implementação** (como JSF/Mojarra e JAX-RS/RESTEasy). | [Passo 1 › 0. Contexto: J2EE → Java EE → Jakarta EE](passo-01-modelo-jpa.md#0-contexto-j2ee--java-ee--jakarta-ee) |
| 3 | Estados da entidade: **New → Managed → Detached / Removed**. `persist` torna Managed; `merge` devolve **outra** instância. | [Passo 1 › 3. Ciclo de vida da entidade](passo-01-modelo-jpa.md#3-ciclo-de-vida-da-entidade) |
| 4 | **`@ManyToOne` e `@OneToOne` são EAGER por padrão**; `@OneToMany` e `@ManyToMany`, LAZY. | [Passo 1 › 4. Por que FetchType.LAZY na relação](passo-01-modelo-jpa.md#4-por-que-fetchtypelazy-na-relação) |
| 5 | **N+1** = 1 consulta da lista + 1 por item. Solução: LAZY + `JOIN FETCH` / *Entity Graph* / *batch fetching*. | [Passo 1 › 4. Por que FetchType.LAZY na relação](passo-01-modelo-jpa.md#4-por-que-fetchtypelazy-na-relação) |
| 6 | **`@Version`** = concorrência otimista → `OptimisticLockException`. | [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas) |
| 7 | **`EnumType.STRING`**, nunca `ORDINAL` (padrão). | [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas) |
| 8 | O **`EntityManager` não é thread-safe**; o injetado por `@PersistenceContext` é um *proxy* ligado à transação. | [Passo 2 › O que é injetado com @PersistenceContext](passo-02-dao-entitymanager.md#o-que-é-injetado-com-persistencecontext) |
| 9 | Persistence Context = *identity map* + cache de 1º nível + *dirty checking*. | [Passo 2 › O Persistence Context faz três coisas](passo-02-dao-entitymanager.md#o-persistence-context-faz-três-coisas) |
| 10 | Session beans: **`@Stateless`** (pool), **`@Stateful`** (por cliente), **`@Singleton`** (um por aplicação); **MDB** para mensagens. | [Passo 3 › Os tipos de session bean](passo-03-ejb-negocio.md#os-tipos-de-session-bean) |
| 11 | Todo método público de EJB é transacional **sem anotação** (CMT, `REQUIRED`). | [Passo 3 › 5. Transações ACID com CMT](passo-03-ejb-negocio.md#5-transações-acid-com-cmt) |
| 12 | 6 atributos: `REQUIRED`, `REQUIRES_NEW`, `MANDATORY`, `SUPPORTS`, `NOT_SUPPORTED`, `NEVER`. | [Passo 3 › Atributos de transação](passo-03-ejb-negocio.md#atributos-de-transação) |
| 13 | **Exceção checked NÃO faz rollback** (EJB e Spring). `RuntimeException` sem `@ApplicationException` = exceção de sistema → rollback + `EJBException` + bean descartado. | [Passo 3 › 6. Exceções e rollback (a pergunta mais traiçoeira sobre EJB)](passo-03-ejb-negocio.md#6-exceções-e-rollback-a-pergunta-mais-traiçoeira-sobre-ejb) |
| 14 | **Auto-invocação** (`this.metodo()`) ignora `@TransactionAttribute` / `@Transactional`. | [Passo 3 › Auto-invocação (self-invocation)](passo-03-ejb-negocio.md#auto-invocação-self-invocation) |
| 15 | JSF tem **6 fases**: Restore View, Apply Request Values, Process Validations, Update Model Values, Invoke Application, Render Response. Validação falhou → pula para Render Response. | [Passo 4 › 2. O ciclo de vida do JSF (as 6 fases)](passo-04-jsf-primefaces.md#2-o-ciclo-de-vida-do-jsf-as-6-fases) |
| 16 | Edição em diálogo exige **`@ViewScoped`** (e `Serializable`); `@RequestScoped` "esquece" a obra. | [Passo 4 › 3. Escopos: @RequestScoped vs @ViewScoped (e os outros)](passo-04-jsf-primefaces.md#3-escopos-requestscoped-vs-viewscoped-e-os-outros) |
| 17 | `@ManagedBean` foi **removido no Faces 4.0** → use CDI `@Named`. | [Passo 4 › @ManagedBean vs @Named (história que cai em entrevista)](passo-04-jsf-primefaces.md#managedbean-vs-named-história-que-cai-em-entrevista) |
| 18 | PrimeFaces: **`process`** = o que vai e é processado (fases 2–4); **`update`** = o que é redesenhado (fase 6). | [Passo 4 › process e update: a dupla mais importante do PrimeFaces](passo-04-jsf-primefaces.md#process-e-update-a-dupla-mais-importante-do-primefaces) |
| 19 | REST: **GET/PUT/DELETE idempotentes**; POST não; PATCH não necessariamente. POST de criação → **201 + `Location`**; DELETE → **204**. | [Passo 5 › 3. Os verbos HTTP corretos](passo-05-api-rest-jaxrs.md#3-os-verbos-http-corretos) |
| 20 | **400** = requisição inválida; **422** = regra de negócio; **406** = `Accept`; **415** = `Content-Type`; **405** = verbo não permitido. | [Passo 5 › 4. Códigos de status HTTP](passo-05-api-rest-jaxrs.md#4-códigos-de-status-http) |

### A tabela mestre Jakarta EE ↔ Spring Boot

| Assunto | Jakarta EE (projeto) | Spring Boot |
|---|---|---|
| Execução | WAR implantado num servidor (WildFly) | *Fat JAR* com Tomcat embutido |
| Persistência | `EntityManager` + DAO escrito à mão | Spring Data JPA (`JpaRepository`) |
| Configuração do banco | `persistence.xml` + DataSource JNDI no servidor | `application.properties` + HikariCP |
| Negócio | EJB `@Stateless` | `@Service` |
| Transação | Automática em todo método público (CMT) | `@Transactional` explícito |
| Injeção | `@Inject` / `@EJB` (campo) | `@Autowired` (construtor) |
| Tela | JSF + PrimeFaces (`@Named` + escopo) | Spring MVC + Thymeleaf / SPA |
| API | JAX-RS (`@Path`, `@GET`) | `@RestController` (`@GetMapping`) |
| Erros da API | `ExceptionMapper` | `@RestControllerAdvice` |
| JSON | JSON-B (Yasson) | Jackson |
| LAZY fora da transação | `LazyInitializationException` (sem OSIV) | Escondido pelo OSIV (*Open Session In View*) |

---

## Parte 2 — O projeto como argumento

### Pitch de 90 segundos ("me fale de um projeto seu")

> "Para me preparar para esta vaga, construí do zero uma aplicação **Jakarta EE 10** de gestão de
> obras: cadastro de obras com status e relatórios de inspeção de segurança.
>
> - **Persistência:** entidades JPA com relação `@ManyToOne` LAZY, controle de concorrência com
>   `@Version` e DAOs sobre o `EntityManager`.
> - **Negócio:** EJBs `@Stateless` atrás de interfaces `@Local`, com regras reais: nome único, obra
>   concluída é estado final e uma interdição suspende a obra **na mesma transação** do relatório.
> - **Tela:** JSF com PrimeFaces, com bean `@ViewScoped`, diálogos AJAX e textos em
>   `messages.properties`.
> - **Integração:** API REST com JAX-RS para sistemas externos, com DTOs, códigos HTTP corretos e
>   tratamento de erros padronizado.
>
> Validei tudo num WildFly real:
> - forcei um rollback para comprovar a atomicidade;
> - reproduzi o bug que um `@RequestScoped` causa na edição;
> - testei a tela com Playwright (navegador automatizado);
> - testei a API com uma coleção do Postman de 24 requisições.
>
> Como venho do Spring Boot, documentei cada decisão com o equivalente no Spring."

### Histórias reais para contar (formato STAR)

**STAR** = *Situação, Tarefa, Ação, Resultado*: a estrutura recomendada para responder "conte uma
situação em que…". Use estas histórias quando perguntarem sobre desafios, bugs ou aprendizados.

| História | S / T | A (o que você fez) | R (resultado) | 📖 |
|---|---|---|---|---|
| **O bug do `@RequestScoped`** | Editar uma obra num diálogo AJAX | Troquei o escopo para `@RequestScoped` de propósito e editei pelo navegador | O "Salvar" tentou **inserir** e a regra de nome único barrou: o bean esqueceu o `id` entre as requisições. Com `@ViewScoped`, funciona. | [Passo 4 › O experimento: o que acontece com @RequestScoped?](passo-04-jsf-primefaces.md#o-experimento-o-que-acontece-com-requestscoped) |
| **Rollback comprovado** | Garantir que excluir obra + relatórios é atômico | Um EJB de teste chamou `excluir()` e depois lançou exceção | Os dois `DELETE` foram desfeitos: a obra e os relatórios continuaram lá. | [Passo 3 › A — Atomicidade: tudo ou nada](passo-03-ejb-negocio.md#a--atomicidade-tudo-ou-nada) |
| **Acentos corrompidos** | Dados iniciais com "Edifício" | Descobri que o Hibernate lê o script SQL com o *charset* da JVM (Cp1252 no Windows/Java 17) | Fixei `hibernate.hbm2ddl.charset_name=UTF-8`. | [Passo 2 › Detalhe: hibernate.hbm2ddl.charset_name](passo-02-dao-entitymanager.md#detalhe-hibernatehbm2ddlcharset_name) |
| **A pegadinha do 404** | `?limite=abc` deveria dar 400 | Testei e veio 404: a especificação JAX-RS manda 404 quando `@QueryParam` não converte | Recebi `status` como `String` e converti à mão para responder 400 com mensagem útil. | [Passo 5 › 4. Códigos de status HTTP](passo-05-api-rest-jaxrs.md#4-códigos-de-status-http) |
| **Parâmetro nulo no PostgreSQL** | Consulta "existe outra obra com este nome?" | Evitei `(:id IS NULL OR o.id <> :id)`, que pode falhar no PostgreSQL ("could not determine data type") | Duas consultas escolhidas em Java. | [Passo 2 › 4. JPQL, Criteria API e parâmetros](passo-02-dao-entitymanager.md#4-jpql-criteria-api-e-parâmetros) |
| **Java 8 x WildFly 35** | Subir o WildFly no Windows | `UnsupportedClassVersionError: class file version 55, ... up to 52` | O `JAVA_HOME` apontava para o Java 8; WildFly 35 exige 17+. Configurei o JDK 21 só para o WildFly (`standalone.conf.bat`). | 🌐 (vivido na prática) |

### O que eu melhoraria (mostra senioridade)

- **Índice único** em `LOWER(nome)`: a validação de nome único no service é *check-then-act* e tem
  condição de corrida. [Passo 3 › I — Isolamento: transações concorrentes não se atropelam](passo-03-ejb-negocio.md#i--isolamento-transações-concorrentes-não-se-atropelam)
- **Flyway/Liquibase** em vez de `drop-and-create`, e **PostgreSQL** via DataSource JNDI. [Passo 2 › DataSource por JNDI: a configuração sai do código](passo-02-dao-entitymanager.md#datasource-por-jndi-a-configuração-sai-do-código)
- **Autenticação** na API (JWT) e no JSF (perfis com `@RolesAllowed`). [Passo 5 › Por que isso importa](passo-05-api-rest-jaxrs.md#por-que-isso-importa)
- **`ETag`/`If-Match`** na API para concorrência otimista via HTTP. [Passo 5 › E se a API também alterasse obras? (pergunta comum de entrevista)](passo-05-api-rest-jaxrs.md#e-se-a-api-também-alterasse-obras-pergunta-comum-de-entrevista)
- **`LazyDataModel`** para paginar no banco quando houver milhares de obras. [Passo 4 › Dois tipos de filtro na mesma tela (bom ponto de entrevista)](passo-04-jsf-primefaces.md#dois-tipos-de-filtro-na-mesma-tela-bom-ponto-de-entrevista)
- **Auditoria com `REQUIRES_NEW`** (registrar tentativas mesmo com rollback). [Passo 3 › Atributos de transação](passo-03-ejb-negocio.md#atributos-de-transação)
- **Testes automatizados no build** (unitários com Mockito; integração com Arquillian) e CI. [Passo 3 › 7. Testabilidade: EJBs são POJOs](passo-03-ejb-negocio.md#7-testabilidade-ejbs-são-pojos)

### Vocabulário do domínio (para soar natural)

- **Status como máquina de estados:** `PLANEJADA → EM_ANDAMENTO → SUSPENSA → CONCLUIDA`; concluída é
  final (`StatusObra.podeMudarPara`).
- **Relatório de inspeção de segurança:** registro com valor de **auditoria** (quem, quando, o quê).
  Discussão boa: deveria ser imutável depois de registrado? (ver ISP / *refused bequest* em [SOLID › Situação](solid.md#situação)).
- **Interdição:** não conformidade grave → obra suspensa **na mesma transação** do relatório.
- **Concorrência real:** engenheiro e técnico de segurança editando a mesma obra → `@Version`.
- **Campo × escritório:** técnicos em tablet com rede ruim enviando relatórios pela API → cuidado
  com **reenvio de POST** (não idempotente) — ver pergunta R9.

---

## Parte 3 — Perguntas e respostas

### A. Visão geral e arquitetura

**A1. O que é Java EE / Jakarta EE e qual a diferença para o J2EE?**
- **Resposta curta:** é o conjunto de **especificações** Java para aplicações corporativas
  (persistência, transações, web, mensageria…). J2EE foi o nome até 2006, depois Java EE (5 a 8) e,
  desde que a Oracle doou a plataforma à Eclipse Foundation, Jakarta EE. A partir do Jakarta EE 9
  o pacote mudou de `javax.*` para `jakarta.*`.
- **🛠️ Destrinchando na prática:**
  - O projeto usa Jakarta EE 10 (`jakarta.persistence`, `jakarta.ejb`, `jakarta.faces`…); o código
    legado que você pode encontrar na empresa provavelmente usa `javax.*` — os conceitos são os mesmos.
  - **Especificação × implementação:** o código só importa APIs `jakarta.*`; o WildFly fornece as
    implementações (Hibernate, Mojarra, RESTEasy, Weld). Por isso a dependência é `provided` no `pom.xml`.
  - O Spring Boot 3 também migrou para `jakarta.*`: as anotações JPA são literalmente as mesmas.
- **↪️ Se perguntarem mais:** "Já fez migração `javax` → `jakarta`?" → é troca de imports e de
  dependências, mas exige atualizar servidor e bibliotecas (ex.: PrimeFaces com classificador `jakarta`).
- 📖 [Passo 1 › 0. Contexto: J2EE → Java EE → Jakarta EE](passo-01-modelo-jpa.md#0-contexto-j2ee--java-ee--jakarta-ee)

**A2. Descreva a arquitetura em camadas do seu projeto e por que separar assim.**
- **Resposta curta:** apresentação (JSF e JAX-RS) → negócio (EJB) → acesso a dados (DAO) →
  `EntityManager` → banco. Cada camada só conhece a de baixo e tem um único motivo para mudar.
- **🛠️ Destrinchando na prática:**
  - Desenhe o diagrama da Parte 1 e mostre que a **tela e a API reutilizam o mesmo service**.
  - Exemplo de "um único lugar para mudar": otimizar uma consulta mexe só no DAO; uma regra de
    negócio, só no service; trocar H2 por PostgreSQL, só o DataSource no servidor.
  - **Fronteira transacional no service:** os DAOs têm `@Transactional(MANDATORY)` e falham sem
    transação (testado no WildFly: `TransactionalException`).
- **↪️ Se perguntarem mais:** "Isso não é burocracia demais para um CRUD?" → para um CRUD pequeno
  sim; o valor aparece quando a mesma regra precisa servir tela, API e processos em lote.
- 📖 [Passo 2 › 1. Isolamento de Responsabilidades](passo-02-dao-entitymanager.md#1-isolamento-de-responsabilidades) · [Passo 3 › 1. O padrão Session Facade](passo-03-ejb-negocio.md#1-o-padrão-session-facade)

**A3. Qual a diferença entre um servidor de aplicação e o Spring Boot? WAR × JAR?**
- **Resposta curta:** no Jakarta EE, o **servidor é instalado à parte** e recebe um **WAR** (só o
  código da aplicação); no Spring Boot, o servidor (Tomcat) vai **dentro** de um *fat JAR*
  executável com `java -jar`.
- **🛠️ Destrinchando na prática:**
  - No projeto: `mvnw package` gera `target/gestao-obras.war`; o deploy é copiar para
    `standalone/deployments/` (aparece `gestao-obras.war.deployed`) ou usar o `jboss-cli`.
  - Infraestrutura (DataSource, pool de conexões) fica **no servidor** → o mesmo WAR vai de DEV a
    PROD sem recompilar, e as senhas não ficam no Git.
  - Um servidor pode rodar vários WARs; no Spring Boot é normal um processo por aplicação.
- 📖 [README › Executar](../README.md#executar) · [Passo 2 › DataSource por JNDI: a configuração sai do código](passo-02-dao-entitymanager.md#datasource-por-jndi-a-configuração-sai-do-código)

**A4. 🌐 Qual a diferença entre EJB e CDI?** *(aparece em relatos de entrevista)*
- **Resposta curta:** **CDI** é o mecanismo geral de injeção de dependências e escopos do Jakarta EE;
  **EJB** é um tipo de componente com serviços "de contêiner" embutidos: transações automáticas
  (CMT), pool de instâncias, segurança declarativa, assincronia, *timers* e acesso remoto.
- **🛠️ Destrinchando na prática:**
  - No projeto os **DAOs são CDI** (`@ApplicationScoped`) e os **services são EJB** (`@Stateless`),
    para ter transação automática e pool.
  - Desde o Java EE 7, o CDI ganhou `@Transactional` (JTA): hoje muitos projetos usam só CDI e
    deixam EJB para casos específicos (MDB, `@Asynchronous`, `@Schedule`, remoto). Os nossos DAOs
    usam justamente `@Transactional(MANDATORY)`.
  - O CDI injeta EJBs também (`@Inject ObraService` funciona).
- 📖 [Passo 3 › 4. Injeção de dependências: @Inject vs @EJB](passo-03-ejb-negocio.md#4-injeção-de-dependências-inject-vs-ejb) · [Passo 2 › @Transactional(MANDATORY): quem decide a transação é o negócio](passo-02-dao-entitymanager.md#transactionalmandatory-quem-decide-a-transação-é-o-negócio)

**A5. Quais *design patterns* você usou?**
- **Resposta curta:** DAO, Session Facade, Front Controller, MVC, DTO, Proxy e Template.
- **🛠️ Destrinchando na prática:**

| Padrão | Onde | Por quê |
|---|---|---|
| **DAO** | `ObraDao`, `GenericDao` | Isola o acesso a dados |
| **Session Facade** (Core J2EE Patterns) | `ObraServiceBean` | Uma operação de negócio = um caso de uso = uma transação |
| **Front Controller** | `FacesServlet` | Um ponto de entrada para todas as requisições JSF |
| **MVC** | XHTML / `ObraBean` / service | Separa tela, controle e modelo |
| **DTO** | `ObraDTO`, `NovoRelatorioDTO` | Contrato da API separado da entidade |
| **Proxy** | `EntityManager` injetado, EJBs, entidades LAZY | Interceptar chamadas (transação, carregamento sob demanda) |
| **Template** | `layout.xhtml` (Facelets) | Reutilizar o layout |
| **Service Locator** (histórico) | JNDI `java:global/...` | Substituído pela injeção |

- 📖 [Passo 3 › 1. O padrão Session Facade](passo-03-ejb-negocio.md#1-o-padrão-session-facade) · [Passo 4 › O Front Controller: FacesServlet](passo-04-jsf-primefaces.md#o-front-controller-facesservlet) · [Passo 5 › 5. DTOs: por que não devolver a entidade?](passo-05-api-rest-jaxrs.md#5-dtos-por-que-não-devolver-a-entidade)

---

### J. JPA / Hibernate

**J1. Qual a diferença entre JDBC, JPA e Hibernate?**
- **Resposta curta:** **JDBC** (*Java Database Connectivity*) é a API de baixo nível: você escreve o
  SQL e lê o `ResultSet`. **JPA** é a especificação de **ORM** (*Object-Relational Mapping*): você
  trabalha com objetos e ela gera o SQL. **Hibernate** é uma implementação da JPA (a do WildFly).
- **🛠️ Destrinchando na prática:**
  - No projeto não há nenhum `import org.hibernate` no código: dependemos só da especificação
    (DIP). Trocar por EclipseLink seria trocar de servidor/provider.
  - O JDBC continua por baixo: o Hibernate usa o DataSource e `PreparedStatement`.
- 📖 [Passo 1 › 0. Contexto: J2EE → Java EE → Jakarta EE](passo-01-modelo-jpa.md#0-contexto-j2ee--java-ee--jakarta-ee) · [SOLID › Onde está aplicado](solid.md#onde-está-aplicado-3)

**J2. O que uma classe precisa para ser uma entidade JPA?**
- **Resposta curta:** `@Entity`, um `@Id`, construtor sem argumentos (`public` ou `protected`) e não
  ser `final` (o Hibernate cria subclasses *proxy*).
- **🛠️ Destrinchando na prática:**
  - Mostre `Obra`: `@Table(name = "obra")`, `@Id @GeneratedValue(strategy = IDENTITY)`, `@Column(nullable = false, length = 150)`.
  - Encapsulamento: **não existe `setId()` nem `setVersao()`** — esses valores pertencem ao JPA.
  - A obrigatoriedade da obra no relatório aparece 3 vezes, de propósito: `@NotNull` (mensagem amigável),
    `optional = false` (otimização) e `nullable = false` (constraint no banco).
- 📖 [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas) · [Passo 1 › 1. Encapsulamento](passo-01-modelo-jpa.md#1-encapsulamento)

**J3. Explique o ciclo de vida de uma entidade. Qual a diferença entre `persist` e `merge`?**
- **Resposta curta:** New (objeto novo) → `persist` → **Managed** (monitorado) → fim da transação →
  **Detached** → `merge` → Managed de novo; `remove` → **Removed**. O `merge` **copia** o estado para
  uma instância gerenciada e **devolve essa outra instância**.
- **🛠️ Destrinchando na prática:**
  - No teste, `assertNotSame(obraOriginal, obraRetornadaPeloMerge)` passou: continuar alterando o
    objeto original depois do `merge` não grava nada.
  - Na tela JSF, a obra em edição vive no bean `@ViewScoped` entre requisições → está **Detached** →
    o service usa `merge`.
  - Spring Data esconde isso: `save()` faz `persist` se o ID é nulo, senão `merge`.
- **↪️ Se perguntarem mais:** `clear()` (desanexa tudo), `detach(e)` (desanexa uma),
  `refresh(e)` (recarrega do banco, descartando alterações).
- 📖 [Passo 1 › 3. Ciclo de vida da entidade](passo-01-modelo-jpa.md#3-ciclo-de-vida-da-entidade) · [Passo 2 › Operações principais](passo-02-dao-entitymanager.md#operações-principais)

**J4. O que é o Persistence Context? E o cache de 1º e 2º nível?**
- **Resposta curta:** é o conjunto de entidades gerenciadas por um `EntityManager`. Funciona como
  *identity map* (mesmo ID = mesma instância), **cache de 1º nível** (o 2º `find` não vai ao banco)
  e *unit of work* com **dirty checking** (gera os `UPDATE` no *flush*). O **cache de 2º nível** é
  compartilhado entre `EntityManager`s (opcional; no WildFly, Infinispan).
- **🛠️ Destrinchando na prática:**
  - Log real do teste: um `persist` seguido de `setLocalizacao()` na mesma transação gerou
    `insert` **e** `update ... where id=? and versao=?` sem nenhum `save()`.
  - No `registrar` com interdição, a obra vem de outro EJB na **mesma transação** → mesmo Persistence
    Context → `obra.setStatus(SUSPENSA)` vira `UPDATE` sozinho.
- **↪️ Se perguntarem mais (🌐):** cache de 2º nível com `@Cacheable` + `shared-cache-mode` no
  `persistence.xml`; bom para tabelas lidas muito e alteradas pouco.
- 📖 [Passo 2 › O Persistence Context faz três coisas](passo-02-dao-entitymanager.md#o-persistence-context-faz-três-coisas) · [Passo 3 › Persistence Context compartilhado e dirty checking](passo-03-ejb-negocio.md#persistence-context-compartilhado-e-dirty-checking)

**J5. LAZY × EAGER: quais os padrões e qual você usa?**
- **Resposta curta:** `@ManyToOne`/`@OneToOne` são **EAGER** por padrão; `@OneToMany`/`@ManyToMany`,
  LAZY. Uso **LAZY em tudo** e busco explicitamente o que cada caso de uso precisa (`JOIN FETCH`).
- **🛠️ Destrinchando na prática:**
  - `@ManyToOne(fetch = LAZY, optional = false)` em `RelatorioSeguranca.obra`.
  - Prova no log: `em.find(RelatorioSeguranca.class, id)` gerou só
    `select ... from relatorio_seguranca`, sem *join* com `obra`; a obra virou um *proxy* com só o ID.
  - Analogia: LAZY é o garçom que só traz a sobremesa quando você pede.
- 📖 [Passo 1 › 4. Por que FetchType.LAZY na relação](passo-01-modelo-jpa.md#4-por-que-fetchtypelazy-na-relação)

**J6. O que é o problema N+1 e como resolver?** *(uma das mais cobradas)*
- **Resposta curta:** uma consulta traz N registros e, para cada um, outra consulta busca a
  associação: 1 + N consultas. Resolve-se com **LAZY + `JOIN FETCH`** no caso de uso que precisa,
  **Entity Graph** ou ***batch fetching*** (buscar associações em lotes).
- **🛠️ Destrinchando na prática:**
  - Exemplo: 500 relatórios de 50 obras com EAGER → até 51 consultas.
  - `RelatorioSegurancaDao.listarRecentesComObra`:
    `SELECT r FROM RelatorioSeguranca r JOIN FETCH r.obra ORDER BY ...` → **um** `select ... join obra`.
  - `r.obra.id` na JPQL lê a FK e **não** gera *join* (`where rs1_0.obra_id=?`).
- **↪️ Se perguntarem mais (🌐):** `JOIN FETCH` de **duas coleções** `List` → `MultipleBagFetchException`
  ou produto cartesiano; `JOIN FETCH` de coleção + `setMaxResults` → paginação em memória
  (aviso `HHH90003004`). Alternativas: *batch fetching* (`@BatchSize`) ou projeção em DTO.
- 📖 [Passo 1 › 4. Por que FetchType.LAZY na relação](passo-01-modelo-jpa.md#4-por-que-fetchtypelazy-na-relação) · [Passo 2 › 4. JPQL, Criteria API e parâmetros](passo-02-dao-entitymanager.md#4-jpql-criteria-api-e-parâmetros)

**J7. O que é `LazyInitializationException` e o OSIV?**
- **Resposta curta:** acessar uma associação LAZY **depois que o Persistence Context fechou**. O Spring
  Boot liga por padrão o **OSIV** (*Open Session In View*), que mantém a sessão aberta até a tela
  renderizar e esconde o problema; o Jakarta EE não tem OSIV.
- **🛠️ Destrinchando na prática:**
  - `RelatorioSeguranca.toString()` **não** inclui a obra: um simples log fora da transação quebraria.
  - Na API, `RelatorioDTO` leva só `obraId`: `getObra().getId()` não inicializa o proxy (o ID é a FK).
  - Soluções: `JOIN FETCH` na camada de negócio, DTO montado dentro da transação.
- 📖 [Passo 1 › 4. Por que FetchType.LAZY na relação](passo-01-modelo-jpa.md#4-por-que-fetchtypelazy-na-relação) · [Passo 5 › 5. DTOs: por que não devolver a entidade?](passo-05-api-rest-jaxrs.md#5-dtos-por-que-não-devolver-a-entidade)

**J8. Concorrência otimista × pessimista.**
- **Resposta curta:** **otimista** (`@Version`): ninguém bloqueia; no `UPDATE ... WHERE versao = ?` a
  versão é conferida e, se mudou, `OptimisticLockException`. **Pessimista**: bloqueia a linha
  (`SELECT ... FOR UPDATE`) até o fim da transação.
- **🛠️ Destrinchando na prática:**
  - Teste real: duas cópias da mesma obra; a 1ª grava (v0 → v1), a 2ª falha:
    `EJBException <- OptimisticLockException <- StaleObjectStateException`.
  - Na tela, essa exceção vira a mensagem "Esta obra foi alterada por outro usuário…".
  - Por que otimista aqui: conflitos raros e telas abertas por muito tempo (bloquear seria inviável).
- **↪️ Se perguntarem mais (🌐):** pessimista em JPA: `em.find(Obra.class, id, LockModeType.PESSIMISTIC_WRITE)`;
  útil em disputas curtas e frequentes (ex.: saldo, estoque).
- 📖 [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas) · [Passo 3 › I — Isolamento: transações concorrentes não se atropelam](passo-03-ejb-negocio.md#i--isolamento-transações-concorrentes-não-se-atropelam) · [Passo 4 › 6. Boas práticas aplicadas](passo-04-jsf-primefaces.md#6-boas-práticas-aplicadas)

**J9. `GenerationType.IDENTITY` × `SEQUENCE`.**
- **Resposta curta:** `IDENTITY` usa a coluna autoincremento do banco e obriga o `INSERT` imediato
  (o Hibernate precisa do ID), o que **desativa o *batching* JDBC**. `SEQUENCE` busca IDs de uma
  sequência, com `allocationSize` reservando vários de uma vez — mais performático no PostgreSQL.
- **🛠️ Destrinchando na prática:** escolhi `IDENTITY` pela portabilidade H2/PostgreSQL; em produção
  com volume, trocaria por `SEQUENCE`.
- 📖 [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas)

**J10. Por que `EnumType.STRING`?**
- **Resposta curta:** o padrão `ORDINAL` grava a posição (0, 1, 2…); inserir uma constante no meio do
  enum muda o significado dos dados já gravados.
- **🛠️ Destrinchando na prática:** o Hibernate até gerou um `CHECK (status in ('PLANEJADA','EM_ANDAMENTO',...))`.
  E os rótulos de tela saíram do enum para o `messages.properties` (SRP).
- 📖 [Passo 1 › 2. Anotações JPA usadas](passo-01-modelo-jpa.md#2-anotações-jpa-usadas) · [SOLID › Onde o SRP está "esticado" (sendo honesto)](solid.md#onde-o-srp-está-esticado-sendo-honesto)

**J11. Como implementar `equals`/`hashCode` numa entidade?**
- **Resposta curta:** pelo ID, com `instanceof` e `getId()` (não `getClass()` nem acesso ao campo), e
  `hashCode` constante.
- **🛠️ Destrinchando na prática:**
  - O proxy LAZY é **subclasse** da entidade com campos vazios: `getClass()` daria falso e `outra.id` daria `null`.
  - `hashCode` constante porque o ID é `null` antes do `persist`: um objeto num `HashSet` "sumiria" depois de gravado.
  - Teste: `assertEquals(obra, relatorio.getObra())` passou com o proxy **não inicializado**.
  - É também um exemplo do princípio de Liskov (o proxy substitui a `Obra`).
- 📖 [Passo 1 › 6. equals/hashCode e Serializable](passo-01-modelo-jpa.md#6-equalshashcode-e-serializable) · [SOLID › Onde está aplicado](solid.md#onde-está-aplicado-2)

**J12. JPQL × SQL nativo × Criteria API. Como evitar SQL Injection?**
- **Resposta curta:** JPQL consulta **entidades e atributos**, e o provider traduz para o dialeto do
  banco; SQL nativo é preso ao banco; a Criteria API monta consultas com objetos — ideal para
  **filtros opcionais**. SQL Injection: **sempre parâmetros** (`setParameter`), nunca concatenação.
- **🛠️ Destrinchando na prática:**
  - `ObraDao.pesquisar(nome, status)` com Criteria: cada filtro só entra no `WHERE` se tiver valor —
    usado por `GET /api/obras?nome=&status=`.
  - `"... WHERE nome = '" + nome + "'"` → nunca.
- 📖 [Passo 2 › 4. JPQL, Criteria API e parâmetros](passo-02-dao-entitymanager.md#4-jpql-criteria-api-e-parâmetros) · [Passo 5 › A busca com filtros opcionais (Criteria API)](passo-05-api-rest-jaxrs.md#a-busca-com-filtros-opcionais-criteria-api)

**J13. Relacionamento bidirecional, `mappedBy`, *owning side* e cascade.**
- **Resposta curta:** o *owning side* é o lado que tem a FK (`@ManyToOne` / `@JoinColumn`); o
  `mappedBy` marca o lado inverso. Cascade (`PERSIST`, `MERGE`, `REMOVE`, `ALL`…) propaga operações
  do pai para os filhos; `orphanRemoval` apaga o filho removido da coleção.
- **🛠️ Destrinchando na prática:**
  - Escolhi relação **unidirecional**: uma obra pode ter milhares de relatórios; uma coleção mapeada
    convida a carregar todos. "Relatórios da obra" é uma consulta paginável no DAO.
  - Sem cascade: a exclusão de obra faz **bulk delete** dos relatórios e depois remove a obra, na mesma transação.
- 📖 [Passo 1 › 5. Relação unidirecional (sem @OneToMany em Obra)](passo-01-modelo-jpa.md#5-relação-unidirecional-sem-onetomany-em-obra) · [Passo 3 › A — Atomicidade: tudo ou nada](passo-03-ejb-negocio.md#a--atomicidade-tudo-ou-nada)

**J14. Armadilhas da API do `EntityManager`.**
- **Resposta curta:** `getSingleResult()` lança `NoResultException`/`NonUniqueResultException`;
  `merge()` devolve outra instância; *bulk update/delete* (`executeUpdate`) ignora o Persistence
  Context, os *callbacks* e o `@Version`.
- **🛠️ Destrinchando na prática:** `buscarPorId` usa `find()` + `Optional`; `removerPorObra` usa bulk
  delete conscientemente (1 `DELETE` em vez de N).
- **↪️ Se perguntarem mais:** `find` × `getReference` (no Hibernate clássico, `get` × `load`): `find` vai
  ao banco e devolve `null` se não existir; `getReference` devolve um proxy e só falha ao usá-lo.
- 📖 [Passo 2 › Armadilhas clássicas (perguntas de entrevista)](passo-02-dao-entitymanager.md#armadilhas-clássicas-perguntas-de-entrevista) · [Passo 2 › Operações principais](passo-02-dao-entitymanager.md#operações-principais)

---

### D. DAO, `EntityManager` e configuração

**D1. DAO × Repository: qual a diferença?**
- **Resposta curta:** DAO (Core J2EE Patterns) abstrai a **persistência** (operações CRUD); Repository
  (DDD — *Domain-Driven Design*) abstrai uma **coleção de objetos do domínio**. Na prática, viram sinônimos.
- **🛠️ Destrinchando na prática:** `GenericDao<T, ID>` concentra o CRUD; `ObraDao` acrescenta só as
  consultas do domínio e sobrescreve `listarTodos()` para ordenar.
- 📖 [Passo 2 › DAO vs Repository](passo-02-dao-entitymanager.md#dao-vs-repository) · [Passo 2 › GenericDao<T, ID>: reutilização com generics](passo-02-dao-entitymanager.md#genericdaot-id-reutilização-com-generics)

**D2. O `EntityManager` é thread-safe? Como um DAO singleton pode usá-lo?**
- **Resposta curta:** não é. O que o `@PersistenceContext` injeta é um **proxy** que, a cada chamada,
  usa o `EntityManager` da transação JTA corrente. Por isso o DAO `@ApplicationScoped` é seguro.
- **🛠️ Destrinchando na prática:** igual no Spring (`SharedEntityManagerCreator`). Três peças:
  `EntityManagerFactory` (1 por unidade, pesada, thread-safe), `EntityManager` (1 por transação), Persistence Context.
- 📖 [Passo 2 › Três peças](passo-02-dao-entitymanager.md#três-peças) · [Passo 2 › O que é injetado com @PersistenceContext](passo-02-dao-entitymanager.md#o-que-é-injetado-com-persistencecontext)

**D3. O que é o `persistence.xml`? JTA × RESOURCE_LOCAL?**
- **Resposta curta:** configura a *Persistence Unit* (entidades, DataSource, propriedades). **JTA**:
  o servidor coordena as transações (pode envolver vários recursos, com *two-phase commit*);
  **RESOURCE_LOCAL**: a aplicação controla a transação JDBC (`em.getTransaction().begin()`), como no Spring Boot.
- **🛠️ Destrinchando na prática:** `transaction-type="JTA"` + `<jta-data-source>java:comp/DefaultDataSource</jta-data-source>`;
  `drop-and-create` ≈ `ddl-auto=create-drop`; `sql-load-script-source` ≈ `data.sql`.
- 📖 [Passo 2 › 2. persistence.xml: a Persistence Unit](passo-02-dao-entitymanager.md#2-persistencexml-a-persistence-unit) · [Passo 2 › JTA vs RESOURCE_LOCAL](passo-02-dao-entitymanager.md#jta-vs-resource_local)

**D4. Como você trocaria o H2 por PostgreSQL?**
- **Resposta curta:** sem mexer no código: cria-se o DataSource no servidor (driver + URL + credenciais)
  e troca-se o nome JNDI no `persistence.xml`; o Hibernate detecta o dialeto.
- **🛠️ Destrinchando na prática:** `jboss-cli`: `deploy postgresql.jar` + `data-source add ... --jndi-name=java:jboss/datasources/GestaoObrasDS`.
  Em produção: `schema-generation` = `none` e Flyway/Liquibase para versionar o esquema.
- 📖 [Passo 2 › DataSource por JNDI: a configuração sai do código](passo-02-dao-entitymanager.md#datasource-por-jndi-a-configuração-sai-do-código)

**D5. `EntityManager` × Spring Data JPA.**
- **Resposta curta:** o Spring Data **gera DAOs** em cima do mesmo `EntityManager`. O `save()` do
  `SimpleJpaRepository` é literalmente `isNew ? persist : merge`.
- **🛠️ Destrinchando na prática:** compare `ObraDao` (à mão) com `interface ObraRepository extends JpaRepository<Obra, Long>`
  e consultas derivadas (`findByStatusOrderByNome`). Trade-off: menos código × menos controle e conceitos escondidos.
  No Jakarta EE 11 existe o **Jakarta Data**, parecido com o Spring Data.
- 📖 [Passo 2 › 5. EntityManager vs Spring Data JPA](passo-02-dao-entitymanager.md#5-entitymanager-vs-spring-data-jpa)

---

### E. EJB e transações

**E1. Quais os tipos de session bean e quando usar cada um?**
- **Resposta curta:** `@Stateless` (sem estado de conversa, **pool**: a maioria dos serviços);
  `@Stateful` (uma instância por cliente: carrinho, assistente em etapas); `@Singleton` (uma por
  aplicação: cache, configuração, com `@Lock(READ/WRITE)`). **MDB** (`@MessageDriven`) consome
  mensagens JMS e, tecnicamente, não é *session bean*.
- **🛠️ Destrinchando na prática:**
  - Nossos services são `@Stateless`: cada chamada pega uma instância exclusiva do pool →
    thread-safe sem `synchronized`; os campos só guardam dependências injetadas.
  - Spring: `@Service` (singleton), `@SessionScope`, `@JmsListener`.
- **↪️ Se perguntarem mais (🌐):** `@Stateful` pode ser **passivado** (gravado em disco quando ocioso):
  `@PrePassivate`/`@PostActivate`; `@Remove` encerra a instância.
- 📖 [Passo 3 › Os tipos de session bean](passo-03-ejb-negocio.md#os-tipos-de-session-bean) · [Passo 3 › Ciclo de vida](passo-03-ejb-negocio.md#ciclo-de-vida)

**E2. O que o contêiner EJB oferece "de graça"?**
- **Resposta curta:** transações (CMT), pool, segurança (`@RolesAllowed`), assincronia (`@Asynchronous`),
  agendamento (`@Schedule`), interceptadores e acesso remoto.
- **🛠️ Destrinchando na prática:** compare com o Spring, onde cada um é um recurso à parte
  (`@Transactional`, `@PreAuthorize`, `@Async`, `@Scheduled`, AOP).
- 📖 [Passo 3 › O que o contêiner oferece "de graça" a um EJB](passo-03-ejb-negocio.md#o-que-o-contêiner-oferece-de-graça-a-um-ejb)

**E3. CMT × BMT.**
- **Resposta curta:** **CMT** (*Container-Managed Transactions*): o contêiner abre e fecha a transação
  em cada método (padrão). **BMT** (*Bean-Managed*): `@TransactionManagement(BEAN)` e o código
  controla com `UserTransaction.begin()/commit()`.
- **🛠️ Destrinchando na prática:** o projeto usa CMT; BMT só se justifica quando é preciso controlar
  várias transações dentro de um método (ex.: processar um lote com commits parciais).
- 📖 [Passo 3 › 5. Transações ACID com CMT](passo-03-ejb-negocio.md#5-transações-acid-com-cmt)

**E4. Explique os atributos de transação. `REQUIRED` × `REQUIRES_NEW`?**
- **Resposta curta:** `REQUIRED` entra na transação existente ou cria uma; `REQUIRES_NEW` **sempre**
  cria uma nova, suspendendo a atual (se a externa sofrer rollback, a nova continua gravada).
- **🛠️ Destrinchando na prática:**
  - `RelatorioSegurancaServiceBean.registrar` chama `obraService.buscarPorId` (`REQUIRED`) → **mesma
    transação**, mesmo Persistence Context.
  - DAOs com `MANDATORY`: chamados sem transação → erro (testado).
  - Exemplo clássico de `REQUIRES_NEW`: **log de auditoria** "tentativa de excluir obra X" que precisa
    ficar gravado mesmo se a exclusão falhar.
  - Spring tem `NESTED` (*savepoints*); o EJB não.
- 📖 [Passo 3 › Atributos de transação](passo-03-ejb-negocio.md#atributos-de-transação)

**E5. O que é ACID? Dê exemplos do seu projeto.**
- **Resposta curta:** **A**tomicidade (tudo ou nada), **C**onsistência (de um estado válido a outro),
  **I**solamento (transações concorrentes não se veem pela metade), **D**urabilidade (commit sobrevive a falhas).
- **🛠️ Destrinchando na prática:**
  - **A:** `excluir` = `DELETE` relatórios + `DELETE` obra; experimento com falha forçada desfez os dois.
  - **C:** FK, `NOT NULL`, `CHECK`, Bean Validation e regras do service (nome único, concluída é final).
  - **I:** `READ COMMITTED` + `@Version`; ressalva do *check-then-act* no nome único → índice único.
  - **D:** WAL (*Write-Ahead Log*) do PostgreSQL; o H2 em memória **não** é durável.
- 📖 [Passo 3 › A — Atomicidade: tudo ou nada](passo-03-ejb-negocio.md#a--atomicidade-tudo-ou-nada) · [Passo 3 › C — Consistência: o banco só passa de um estado válido para outro](passo-03-ejb-negocio.md#c--consistência-o-banco-só-passa-de-um-estado-válido-para-outro) · [Passo 3 › I — Isolamento: transações concorrentes não se atropelam](passo-03-ejb-negocio.md#i--isolamento-transações-concorrentes-não-se-atropelam) · [Passo 3 › D — Durabilidade: depois do commit, o dado sobrevive](passo-03-ejb-negocio.md#d--durabilidade-depois-do-commit-o-dado-sobrevive)

**E6. 🌐 Quais são os níveis de isolamento?**
- **Resposta curta:**

| Nível | Leitura suja | Leitura não repetível | Leitura fantasma |
|---|---|---|---|
| `READ UNCOMMITTED` | possível | possível | possível |
| `READ COMMITTED` (padrão PostgreSQL) | evitada | possível | possível |
| `REPEATABLE READ` (padrão MySQL/InnoDB) | evitada | evitada | possível (pelo padrão SQL) |
| `SERIALIZABLE` | evitada | evitada | evitada |

- **🛠️ Destrinchando na prática:** *leitura suja* = ler dado não commitado; *não repetível* = ler a mesma
  linha duas vezes e ela mudou; *fantasma* = repetir a consulta e surgirem linhas novas. Mais
  isolamento = menos concorrência. No projeto, o `@Version` cobre o *lost update* sem subir o nível.
- 📖 [Passo 3 › I — Isolamento: transações concorrentes não se atropelam](passo-03-ejb-negocio.md#i--isolamento-transações-concorrentes-não-se-atropelam)

**E7. Exceção de sistema × exceção de aplicação. Uma exceção checked faz rollback?** *(a mais traiçoeira)*
- **Resposta curta:** **sistema** = `RuntimeException` sem `@ApplicationException` → rollback sempre,
  cliente recebe `EJBException`, bean descartado. **Aplicação** = checked, ou runtime com
  `@ApplicationException` → chega como está; **rollback só com `rollback = true`** ou `setRollbackOnly()`.
  Portanto, **checked não faz rollback por padrão** (no Spring também não).
- **🛠️ Destrinchando na prática:**
  - `RegraNegocioException` tem `@ApplicationException(rollback = true)`: chega limpa à tela e à API e desfaz tudo.
  - Resultados reais: nome duplicado → `RegraNegocioException`; nome vazio → `EJBException <- ConstraintViolationException`.
  - A subclasse `EntidadeNaoEncontradaException` herda o comportamento → a API a traduz em 404.
- 📖 [Passo 3 › 6. Exceções e rollback (a pergunta mais traiçoeira sobre EJB)](passo-03-ejb-negocio.md#6-exceções-e-rollback-a-pergunta-mais-traiçoeira-sobre-ejb)

**E8. O que é o problema da auto-invocação?**
- **Resposta curta:** `this.outroMetodo()` não passa pelo proxy do contêiner → o
  `@TransactionAttribute` (ou o `@Transactional` do Spring) do método chamado é ignorado.
- **🛠️ Destrinchando na prática:** por isso o `RelatorioSegurancaServiceBean` chama `obraService`
  injetado (proxy). Saídas: mover para outro bean ou `sessionContext.getBusinessObject(...)`.
- 📖 [Passo 3 › Auto-invocação (self-invocation)](passo-03-ejb-negocio.md#auto-invocação-self-invocation)

**E9. `@Local`, `@Remote` e *no-interface view*.**
- **Resposta curta:** `@Local` = interface de negócio para a mesma JVM (passagem **por referência**);
  `@Remote` = outras JVMs (passagem **por valor**, serialização); *no-interface view* (EJB 3.1) = sem interface.
- **🛠️ Destrinchando na prática:**
  - `ObraService` é `@Local`: tela e API dependem da interface (DIP).
  - Passagem por referência na prática: no `inserir`, o `persist` preenche o `id` **no mesmo objeto**
    que o bean JSF enviou.
  - História: no EJB 2.x as interfaces Home/Remote/Local eram obrigatórias.
  - Nomes JNDI reais: `java:global/gestao-obras/ObraServiceBean!...ObraService`.
- 📖 [Passo 3 › 3. Interface @Local e Inversão de Dependência](passo-03-ejb-negocio.md#3-interface-local-e-inversão-de-dependência) · [Passo 3 › Nomes JNDI portáveis](passo-03-ejb-negocio.md#nomes-jndi-portáveis)

**E10. `@Inject` × `@EJB`.**
- **Resposta curta:** `@Inject` (CDI) injeta qualquer bean, inclusive EJB, por tipo + qualificadores;
  `@EJB` só EJBs, aceita `beanName`/`lookup` e EJB remoto. Uso `@Inject` por padrão.
- **🛠️ Destrinchando na prática:** no `RelatorioSegurancaServiceBean` usei os dois de propósito
  (`@EJB ObraService`, `@Inject RelatorioSegurancaDao`). Em EJB, injeção por campo (a especificação
  exige construtor sem argumentos); no Spring, recomenda-se construtor.
- 📖 [Passo 3 › 4. Injeção de dependências: @Inject vs @EJB](passo-03-ejb-negocio.md#4-injeção-de-dependências-inject-vs-ejb)

**E11. Como testar um EJB?**
- **Resposta curta:** desde o EJB 3 ele é um **POJO** (*Plain Old Java Object*) anotado: regra de
  negócio se testa com JUnit + Mockito, sem servidor; comportamento de contêiner (transação, JNDI) se
  testa em integração (Arquillian ou servidor real).
- **🛠️ Destrinchando na prática:** `@InjectMocks ObraServiceBean` + `@Mock ObraDao`, `when(existeComNome).thenReturn(true)`,
  `assertThrows(RegraNegocioException...)`, `verify(obraDao, never()).inserir(any())`. As regras de contêiner
  foram validadas no WildFly real.
- 📖 [Passo 3 › 7. Testabilidade: EJBs são POJOs](passo-03-ejb-negocio.md#7-testabilidade-ejbs-são-pojos)

**E12. 🌐 Quando usar MDB / mensageria?**
- **Resposta curta:** para processamento **assíncrono e desacoplado**: quem envia não espera quem
  processa. `@MessageDriven` implementa `MessageListener.onMessage()` lendo de uma fila/tópico JMS.
- **🛠️ Destrinchando na prática (cenário do domínio):** ao interditar uma obra, publicar uma mensagem
  "obra interditada" e um MDB notificar o responsável por e-mail — sem atrasar nem arriscar a
  transação do relatório. No WildFly, mensageria exige a configuração `standalone-full.xml`.
- 📖 [Passo 3 › Os tipos de session bean](passo-03-ejb-negocio.md#os-tipos-de-session-bean)

---

### F. JSF e PrimeFaces

**F1. Quais as fases do ciclo de vida do JSF? O que acontece se a validação falhar?**
- **Resposta curta:** Restore View → Apply Request Values → Process Validations (conversão + validação)
  → Update Model Values → Invoke Application → Render Response. Se a validação falhar, **pula para
  Render Response**: o bean não é atualizado e a ação não roda. No primeiro GET, vai de Restore View
  direto para Render Response.
- **🛠️ Destrinchando na prática:**
  - Acompanhe o clique em "Salvar": fase 2 pega `"Viaduto Norte"` como texto; fase 3 converte
    `"PLANEJADA"` em `StatusObra` e valida `@NotBlank`; fase 4 chama `setNome`; fase 5 chama `obraBean.salvar()`.
  - Teste real: salvar vazio mostrou "O nome da obra é obrigatório" — a mensagem vem da **entidade**
    (validação escrita uma vez, usada na tela, no JPA e na API).
  - Diferença com o Spring MVC: lá o *binding* acontece **antes** do `@Valid`.
- 📖 [Passo 4 › 2. O ciclo de vida do JSF (as 6 fases)](passo-04-jsf-primefaces.md#2-o-ciclo-de-vida-do-jsf-as-6-fases) · [Passo 4 › O que acontece quando a validação falha?](passo-04-jsf-primefaces.md#o-que-acontece-quando-a-validação-falha)

**F2. Quais os escopos e qual usar? Por que `@ViewScoped` precisa de `Serializable`?**
- **Resposta curta:** Request (uma requisição), **View** (a tela atual, várias requisições AJAX),
  Session (a sessão toda), Application (todos). Telas com diálogos/edição → `@ViewScoped`. Precisa
  de `Serializable` porque o estado fica na sessão HTTP, que pode ser serializada (cluster, passivação).
- **🛠️ Destrinchando na prática:**
  - **Conte o experimento:** com `@RequestScoped`, editar uma obra gerou "Já existe uma obra com o nome…"
    — o bean da requisição "Salvar" era novo, a obra estava sem `id`, e o service tentou **inserir**.
  - `@SessionScoped` para tudo: memória presa e **várias abas** compartilhando o mesmo bean.
  - Por isso `Obra` implementa `Serializable` desde o Passo 1.
- **↪️ Se perguntarem mais:** `ViewExpiredException` = o estado da tela sumiu (sessão expirada).
- 📖 [Passo 4 › 3. Escopos: @RequestScoped vs @ViewScoped (e os outros)](passo-04-jsf-primefaces.md#3-escopos-requestscoped-vs-viewscoped-e-os-outros) · [Passo 4 › O experimento: o que acontece com @RequestScoped?](passo-04-jsf-primefaces.md#o-experimento-o-que-acontece-com-requestscoped) · [Passo 4 › Por que o @ViewScoped exige implements Serializable?](passo-04-jsf-primefaces.md#por-que-o-viewscoped-exige-implements-serializable)

**F3. `@ManagedBean` ainda existe?**
- **Resposta curta:** depreciado no JSF 2.3 e **removido no Faces 4.0**; hoje: `@Named` + escopo CDI.
- **🛠️ Destrinchando na prática:** pegadinha em código legado — duas anotações `@ViewScoped`
  (`javax.faces.bean` e `javax.faces.view`); misturar `@Named` com a antiga faz o escopo ser ignorado
  e o bean se comportar como request.
- 📖 [Passo 4 › @ManagedBean vs @Named (história que cai em entrevista)](passo-04-jsf-primefaces.md#managedbean-vs-named-história-que-cai-em-entrevista)

**F4. JSF × Spring MVC: qual a diferença fundamental?**
- **Resposta curta:** JSF é ***component-based*** e ***stateful***: o servidor guarda a árvore de
  componentes da tela (identificada pelo `ViewState`) e trata eventos por componente. Spring MVC é
  ***action-based*** e ***stateless***: cada requisição chama um método e nada fica guardado.
- **🛠️ Destrinchando na prática:** o preço é memória por tela aberta; o ganho é programar telas ricas
  quase sem JavaScript. O `FacesServlet` faz o papel do `DispatcherServlet` (*Front Controller*).
- 📖 [Passo 4 › A grande diferença: JSF é component-based e stateful](passo-04-jsf-primefaces.md#a-grande-diferença-jsf-é-component-based-e-stateful) · [Passo 4 › O Front Controller: FacesServlet](passo-04-jsf-primefaces.md#o-front-controller-facesservlet)

**F5. Como o PrimeFaces faz AJAX? O que são `process` e `update`?**
- **Resposta curta:** o componente envia um POST em segundo plano com `partial.execute` (= `process`:
  o que processar, fases 2–4) e `partial.render` (= `update`: o que redesenhar, fase 6); a resposta
  é um `partial-response` XML com só os pedaços HTML, que o JavaScript troca no DOM.
- **🛠️ Destrinchando na prática:**
  - Números reais: resposta do filtro ≈ **11 mil caracteres**; página inteira ≈ **40 mil**.
  - Botão "Editar": `process="@this"` (não envia nem valida campos) + `update=":dlgObra"`.
  - Botão "Salvar": `process="@form"` + `update="@form :formObras:tabelaObras"` +
    `oncomplete="if (!args.validationFailed) PF('dlgObra').hide()"`.
  - `Mensagens.erro()` chama `validationFailed()` para um erro de **regra de negócio** também manter o diálogo aberto.
- 📖 [Passo 4 › 4. PrimeFaces e AJAX](passo-04-jsf-primefaces.md#4-primefaces-e-ajax) · [Passo 4 › O que realmente trafega (capturado no teste)](passo-04-jsf-primefaces.md#o-que-realmente-trafega-capturado-no-teste) · [Passo 4 › process e update: a dupla mais importante do PrimeFaces](passo-04-jsf-primefaces.md#process-e-update-a-dupla-mais-importante-do-primefaces)

**F6. Boas práticas em *backing beans*.**
- **Resposta curta:** nunca acessar o banco em *getter* (o JSF o chama várias vezes por requisição);
  carregar em `@PostConstruct` e nas ações; editar uma **cópia nova** vinda do banco; regras de negócio
  no service, não no bean.
- **🛠️ Destrinchando na prática:**
  - `editar(obra)` faz `obraService.buscarPorId(...)`: editar a linha da tabela mudaria a tela mesmo
    com "Cancelar" e poderia usar uma `versao` velha.
  - O formulário some para obra concluída, mas a **regra real** está no service.
- 📖 [Passo 4 › 6. Boas práticas aplicadas](passo-04-jsf-primefaces.md#6-boas-práticas-aplicadas)

**F7. 🌐 Para que serve `immediate="true"`?**
- **Resposta curta:** antecipa conversão/validação (em *inputs*) ou a ação (em botões) para a fase
  Apply Request Values, pulando a validação dos demais campos — clássico para botão "Cancelar".
- **🛠️ Destrinchando na prática:** no projeto o "Cancelar" é `type="button"` (nem vai ao servidor) e
  os botões de ação usam `process="@this"`, que no PrimeFaces resolve o mesmo problema de forma mais explícita.
- 📖 [Passo 4 › 2. O ciclo de vida do JSF (as 6 fases)](passo-04-jsf-primefaces.md#2-o-ciclo-de-vida-do-jsf-as-6-fases)

**F8. Conversores, validadores e i18n.**
- **Resposta curta:** conversores transformam texto ↔ objeto (fase 3); validadores verificam regras;
  i18n (*internationalization*) com `resource-bundle` → `messages.properties`.
- **🛠️ Destrinchando na prática:** enum sem conversor (o JSF usa `EnumConverter` sozinho);
  `<f:convertDateTime type="localDate" pattern="dd/MM/yyyy"/>`; `#{msg['status.' += obra.status]}`;
  para inglês, basta `messages_en.properties`. 🌐 Conversor próprio: `@FacesConverter(forClass = ..., managed = true)`.
- 📖 [Passo 4 › 5. Componentes usados](passo-04-jsf-primefaces.md#5-componentes-usados) · [Passo 4 › 6. Boas práticas aplicadas](passo-04-jsf-primefaces.md#6-boas-práticas-aplicadas) · [SOLID › Onde o SRP está "esticado" (sendo honesto)](solid.md#onde-o-srp-está-esticado-sendo-honesto)

**F9. Como lidar com uma tabela de milhares de registros?**
- **Resposta curta:** `LazyDataModel` do PrimeFaces: a tabela pede só a página atual e o banco faz
  paginação/filtro/ordenação (`setFirstResult`/`setMaxResults`).
- **🛠️ Destrinchando na prática:** a tela tem dois filtros — status (vai ao banco via service) e nome
  (`filterBy`, filtra a lista em memória). Para volume, tudo iria ao banco.
- 📖 [Passo 4 › Dois tipos de filtro na mesma tela (bom ponto de entrevista)](passo-04-jsf-primefaces.md#dois-tipos-de-filtro-na-mesma-tela-bom-ponto-de-entrevista)

**F10. Que segurança o JSF oferece?**
- **Resposta curta:** escape de HTML por padrão (contra **XSS** — *Cross-Site Scripting*), `ViewState`
  exigido nos POSTs (dificulta **CSRF** — *Cross-Site Request Forgery*) e `WEB-INF` inacessível.
- **🛠️ Destrinchando na prática:** `<script>` digitado na descrição aparece como texto; o
  `layout.xhtml` fica em `WEB-INF`.
- 📖 [Passo 4 › Segurança que o JSF oferece de graça](passo-04-jsf-primefaces.md#segurança-que-o-jsf-oferece-de-graça)

---

### R. REST e JAX-RS

**R1. O que é REST?**
- **Resposta curta:** estilo de arquitetura (Roy Fielding, 2000): recursos identificados por URIs,
  representações (JSON), interface uniforme com os verbos HTTP, comunicação *stateless*, respostas cacheáveis.
- **🛠️ Destrinchando na prática:** `GET /api/obras/1` é o recurso "obra 1"; `/api/obras/1/relatorios`
  é um sub-recurso. URL com substantivos no plural; filtros na *query string*.
- **↪️ Se perguntarem mais:** Modelo de Maturidade de Richardson — a API está no **nível 2** (recursos +
  verbos + status); o nível 3 é **HATEOAS** (links para as próximas ações).
- 📖 [Passo 5 › 0. Conceitos de base](passo-05-api-rest-jaxrs.md#0-conceitos-de-base) · [Passo 5 › Boas práticas de URL aplicadas](passo-05-api-rest-jaxrs.md#boas-práticas-de-url-aplicadas) · [Passo 5 › 11. Perguntas prováveis na entrevista](passo-05-api-rest-jaxrs.md#11-perguntas-prováveis-na-entrevista)

**R2. O que significa *stateless* e por que importa?**
- **Resposta curta:** cada requisição traz tudo o que é necessário; o servidor não guarda sessão do
  cliente. Isso permite **escalar horizontalmente** (qualquer instância atende qualquer requisição,
  sem *sticky sessions*).
- **🛠️ Destrinchando na prática:**
  - Prova: a API **não envia `Set-Cookie`**; a tela JSF envia `JSESSIONID`. Há um teste no Postman para isso.
  - Recursos `@RequestScoped` (uma instância por requisição) × bean JSF `@ViewScoped`.
  - Autenticação sem sessão: *token* (ex.: JWT no header `Authorization: Bearer`).
  - Stateless ≠ sem dados: as obras continuam no banco.
- 📖 [Passo 5 › 2. Statelessness ("ausência de estado")](passo-05-api-rest-jaxrs.md#2-statelessness-ausência-de-estado) · [Passo 5 › A prova no nosso projeto: API vs tela JSF](passo-05-api-rest-jaxrs.md#a-prova-no-nosso-projeto-api-vs-tela-jsf)

**R3. Quais verbos HTTP você usaria para cada operação? O que é idempotência?**
- **Resposta curta:** GET (ler), POST (criar), PUT (substituir inteiro), PATCH (alterar parte), DELETE
  (remover). **Seguro** = não altera o servidor (GET, HEAD, OPTIONS). **Idempotente** = repetir tem o
  mesmo efeito que fazer uma vez (GET, PUT, DELETE; POST não; PATCH não necessariamente).
- **🛠️ Destrinchando na prática:**
  - `DELETE /api/relatorios/5` → 204; repetir → 404. **Mesmo efeito, resposta diferente** — continua idempotente.
  - `POST /api/obras/1/relatorios` duas vezes = dois relatórios.
  - Escrita em `/api/obras/{id}` → **405** com `Allow: HEAD, GET, OPTIONS` (obras são só leitura).
- 📖 [Passo 5 › 3. Os verbos HTTP corretos](passo-05-api-rest-jaxrs.md#3-os-verbos-http-corretos) · [Passo 5 › DELETE é idempotente, mas a resposta muda (resultado real)](passo-05-api-rest-jaxrs.md#delete-é-idempotente-mas-a-resposta-muda-resultado-real)

**R4. PUT × PATCH × POST.**
- **Resposta curta:** PUT substitui o recurso **inteiro** (campo omitido = apagado/padrão) e é
  idempotente; PATCH altera **parte** e não precisa ser idempotente; POST cria (ou executa uma ação) e não é idempotente.
- **🛠️ Destrinchando na prática:** se a API alterasse obras: `PUT /api/obras/{id}` com a obra completa;
  `PATCH /api/obras/{id}` com `{"status":"SUSPENSA"}`.
- 📖 [Passo 5 › E se a API também alterasse obras? (pergunta comum de entrevista)](passo-05-api-rest-jaxrs.md#e-se-a-api-também-alterasse-obras-pergunta-comum-de-entrevista)

**R5. Que status devolver em cada situação?**
- **Resposta curta:**

| Situação | Status |
|---|---|
| Leitura ok | 200 |
| Criação | **201 + `Location`** |
| Exclusão / sem corpo | **204** |
| Requisição malformada / dados inválidos | 400 |
| Não autenticado 🌐 | **401** |
| Autenticado sem permissão 🌐 | **403** |
| Não existe | 404 |
| Verbo não suportado | 405 |
| Formato pedido (`Accept`) não suportado | 406 |
| Conflito de estado (ex.: duplicado, versão) 🌐 | 409 |
| `If-Match` não confere | 412 |
| Formato enviado (`Content-Type`) não suportado | 415 |
| Regra de negócio violada | 422 |
| Erro inesperado | 500 |

- **🛠️ Destrinchando na prática:** todos os da API foram testados no Postman (24 requisições, 47 verificações).
  Exemplo real de 201: `Location: http://localhost:8080/gestao-obras/api/relatorios/5`.
- 📖 [Passo 5 › 4. Códigos de status HTTP](passo-05-api-rest-jaxrs.md#4-códigos-de-status-http) · [Passo 5 › POST: 201 Created + Location (resultado real)](passo-05-api-rest-jaxrs.md#post-201-created--location-resultado-real)

**R6. Por que usar DTO em vez de devolver a entidade?**
- **Resposta curta:** (1) LAZY fora da transação; (2) contrato estável com outros sistemas; (3)
  segurança: não vazar campos internos e evitar *mass assignment*; (4) validação específica da entrada.
- **🛠️ Destrinchando na prática:** `ObraDTO` sem `versao`/auditoria; `NovoRelatorioDTO` sem `id` e sem
  `obraId` (a obra vem da URL); *records* do Java 16+; `@JsonbPropertyOrder` para a ordem dos campos.
- 📖 [Passo 5 › 5. DTOs: por que não devolver a entidade?](passo-05-api-rest-jaxrs.md#5-dtos-por-que-não-devolver-a-entidade)

**R7. Como você trata erros numa API JAX-RS?**
- **Resposta curta:** `ExceptionMapper` + `@Provider`: os recursos não têm `try/catch`; o JAX-RS
  escolhe o mapeador da superclasse **mais próxima** da exceção. Um formato único de erro.
- **🛠️ Destrinchando na prática:**
  - `RegraNegocioExceptionMapper` → 404 (`EntidadeNaoEncontradaException`) ou 422.
  - `ValidacaoExceptionMapper` → 400 com um item por campo:
    `["dataInspecao: A data da inspeção não pode estar no futuro", "descricao: A descrição é obrigatória"]`.
  - `-parameters` no `pom.xml` para aparecer `dados` em vez de `arg1`.
  - No Spring: `@RestControllerAdvice`; formato padrão: `ProblemDetail` (RFC 9457).
- 📖 [Passo 5 › 6. Tratamento de erros com ExceptionMapper](passo-05-api-rest-jaxrs.md#6-tratamento-de-erros-com-exceptionmapper)

**R8. Uma pegadinha da especificação JAX-RS?**
- **Resposta curta:** se um `@QueryParam`/`@PathParam` não converte para o tipo Java (`limite=abc` num
  `int`), a especificação manda **404**, não 400 (no Spring é 400). Solução: receber `String` e
  validar, ou um `ParamConverter`.
- **🛠️ Destrinchando na prática:** no projeto há os dois casos lado a lado: `status` (convertido à mão →
  400 com os valores aceitos) e `limite` (int → 404). Está na pasta de erros do Postman.
- 📖 [Passo 5 › 4. Códigos de status HTTP](passo-05-api-rest-jaxrs.md#4-códigos-de-status-http)

**R9. 🌐 Um app de campo reenvia o POST quando a rede cai. Como evitar relatórios duplicados?**
- **Resposta curta:** POST não é idempotente. Usar uma **chave de idempotência**: o cliente gera um
  UUID por relatório e o envia (ex.: header `Idempotency-Key` ou campo no corpo); o servidor guarda a
  chave com índice único e, se ela já existe, devolve o recurso já criado em vez de criar outro.
  Alternativa: o cliente escolhe o ID e usa **PUT** (idempotente).
- **🛠️ Destrinchando na prática:** é exatamente o cenário de técnicos de segurança em canteiros com
  sinal ruim — ótimo para mostrar visão de domínio.
- 📖 [Passo 5 › 3. Os verbos HTTP corretos](passo-05-api-rest-jaxrs.md#3-os-verbos-http-corretos)

**R10. 🌐 REST × SOAP (JAX-RS × JAX-WS).**
- **Resposta curta:** SOAP é um protocolo com envelope XML, contrato formal **WSDL**
  (*Web Services Description Language*) e padrões WS-* (segurança, transação); REST é um estilo sobre
  HTTP, geralmente JSON, mais leve. Em Jakarta EE: JAX-WS (*XML Web Services*) para SOAP, JAX-RS para REST.
- **🛠️ Destrinchando na prática:** SOAP ainda aparece em integrações com sistemas legados, bancos e
  governo; REST é o padrão para apps e integrações novas.

**R11. 🌐 Versionamento, paginação, documentação e segurança de API.**
- **Resposta curta:** versão na URL (`/api/v1/...`) ou em header; paginação com `?page=&size=` (ou
  *cursor*) e limite máximo; documentação com **OpenAPI/Swagger**; segurança com HTTPS, autenticação
  por token e **CORS** (*Cross-Origin Resource Sharing*: headers que autorizam um front-end de outro
  domínio a chamar a API).
- **🛠️ Destrinchando na prática:** o projeto já limita `?limite=` entre 1 e 100 (400 fora disso) e tem
  a coleção do Postman como documentação executável (rodada com Newman).
- 📖 [Passo 5 › 8. A coleção do Postman](passo-05-api-rest-jaxrs.md#8-a-coleção-do-postman) · [README › Postman](../README.md#postman)

---

### S. SOLID e orientação a objetos

**S1. Explique SOLID com exemplos do seu código.** *(aparece em relatos de entrevista)*
- **Resposta curta e prática:**

| Princípio | Exemplo no projeto |
|---|---|
| **S** — Responsabilidade Única | DAO não decide transação (`MANDATORY`); texto de tela saiu do enum para o `messages.properties` |
| **O** — Aberto/Fechado | `EquipamentoDao extends GenericDao<Equipamento, Long>` sem tocar no `GenericDao` |
| **L** — Liskov | Proxy LAZY substitui `Obra` (`equals` com `instanceof` + `getId()`); `ObraDao.listarTodos()` só fortalece o contrato |
| **I** — Segregação de Interfaces | API depende de `ObraConsulta` (só leitura), não do `ObraService` completo |
| **D** — Inversão de Dependência | Tela/API dependem de interfaces `@Local`; DAOs dependem de `EntityManager` (especificação), não do Hibernate |

- **🛠️ Destrinchando na prática:** seja honesto sobre os *trade-offs*: entidade com anotações de JPA e
  validação (pragmatismo × arquitetura hexagonal); service depende de `ObraDao` concreto (DIP parcial,
  escolha consciente).
- **↪️ Se perguntarem mais:** "Qual o mais importante?" → defenda o **SRP** (base dos outros) ou o
  **DIP** (testabilidade), com exemplo.
- 📖 [SOLID › Situação atual (após o Passo 5)](solid.md#situação-atual-após-o-passo-5)

**S2. IoC × DI × DIP.**
- **Resposta curta:** **IoC** (*Inversion of Control*) = o contêiner cria e gerencia os objetos; **DI**
  (*Dependency Injection*) = o mecanismo que entrega as dependências prontas (`@Inject`); **DIP** =
  princípio de design: depender de abstrações. Usar `@Inject` numa classe concreta é DI sem DIP.
- **🛠️ Destrinchando na prática:** `@Inject ObraConsulta` (DI + DIP) × `@Inject ObraDao` (DI sem DIP estrito).
- 📖 [SOLID › D — Dependency Inversion Principle (Inversão de Dependência)](solid.md#d--dependency-inversion-principle-inversão-de-dependência)

**S3. 🌐 Classe abstrata × interface.** *(aparece em relatos de entrevista)*
- **Resposta curta:** classe abstrata tem **estado e implementação compartilhados** e herança única;
  interface define um **contrato** (pode ter métodos `default`), e uma classe implementa várias.
- **🛠️ Destrinchando na prática:** o projeto tem os dois: `GenericDao<T, ID>` é **abstrata** (guarda o
  `EntityManager` e a `Class<T>`, implementa o CRUD); `ObraConsulta`/`ObraService` são **interfaces**
  (contratos que tela e API usam).
- 📖 [Passo 2 › GenericDao<T, ID>: reutilização com generics](passo-02-dao-entitymanager.md#genericdaot-id-reutilização-com-generics) · [Passo 5 › 7. ISP aplicado: o contrato ObraConsulta](passo-05-api-rest-jaxrs.md#7-isp-aplicado-o-contrato-obraconsulta)

**S4. 🌐 Polimorfismo e encapsulamento com exemplos.**
- **Resposta curta:** polimorfismo = o mesmo chamado com comportamentos diferentes conforme o tipo
  real; encapsulamento = esconder o estado e controlar o acesso.
- **🛠️ Destrinchando na prática:** `ObraDao` sobrescreve `listarTodos()`; o JAX-RS escolhe o
  `ExceptionMapper` pelo tipo da exceção; `Obra` não tem `setId()`; `setObra()` do relatório recusa `null`.
- 📖 [Passo 1 › 1. Encapsulamento](passo-01-modelo-jpa.md#1-encapsulamento) · [SOLID › Onde está aplicado](solid.md#onde-está-aplicado-1)

---

### C. 🌐 Java "de base" (rápidas)

**C1. Contrato `equals`/`hashCode`.** Objetos iguais por `equals` **devem** ter o mesmo `hashCode`;
quem sobrescreve um sobrescreve o outro. Na prática: a entidade usa o ID (J11). 📖 [Passo 1 › 6. equals/hashCode e Serializable](passo-01-modelo-jpa.md#6-equalshashcode-e-serializable)

**C2. Exceções checked × unchecked.** Checked (`Exception`) obrigam tratamento/declaração; unchecked
(`RuntimeException`) não. Na prática: `RegraNegocioException` é unchecked + `@ApplicationException` (E7). 📖 [Passo 3 › 6. Exceções e rollback (a pergunta mais traiçoeira sobre EJB)](passo-03-ejb-negocio.md#6-exceções-e-rollback-a-pergunta-mais-traiçoeira-sobre-ejb)

**C3. `==` × `equals`; `String` imutável.** `==` compara referências; `equals`, conteúdo. `String` é
imutável (e reaproveitada no *string pool*) → segura para compartilhar entre threads.

**C4. Como funciona o `HashMap`.** Um array de "baldes"; o `hashCode` define o balde; colisões viram
lista e, a partir do Java 8, árvore (quando um balde passa de 8 itens); dobra de tamanho ao passar de
75% de ocupação (*load factor* 0,75). Por isso `hashCode` mal feito degrada a performance.

**C5. `Optional`, *Streams* e *records* no projeto.** `buscarPorId` devolve `Optional` (força tratar o
"não encontrado"); recursos REST usam `.stream().map(ObraDTO::de).toList()`; DTOs são *records* imutáveis.
📖 [Passo 2 › Operações principais](passo-02-dao-entitymanager.md#operações-principais) · [Passo 5 › 5. DTOs: por que não devolver a entidade?](passo-05-api-rest-jaxrs.md#5-dtos-por-que-não-devolver-a-entidade)

**C6. Versões do Java e `UnsupportedClassVersionError`.** Cada `.class` registra a versão de compilação
(52 = Java 8, 55 = 11, 61 = 17, 65 = 21). Rodar código compilado em versão mais nova numa JVM mais
velha gera esse erro — o caso real do WildFly 35 com Java 8.

---

### M. Ferramentas e ambiente

**M1. Maven: ciclo de vida e escopos.**
- **Resposta curta:** fases `validate → compile → test → package → verify → install → deploy`; escopos
  `compile` (padrão), `provided` (o servidor fornece), `runtime`, `test`.
- **🛠️ Destrinchando na prática:** a API Jakarta EE é `provided` (não vai no WAR); o PrimeFaces é
  `compile` (vai no WAR); o **Maven Wrapper** (`mvnw`/`mvnw.cmd`) fixa a versão do Maven; o profile
  `wildfly` (`-Pwildfly`) baixa e sobe o servidor.
- 📖 [README › Pré-requisito: só o Java](../README.md#pré-requisito-só-o-java) · [README › Executar](../README.md#executar)

**M2. Como você faz deploy no WildFly?**
- **Resposta curta:** copiar o WAR para `standalone/deployments` (arquivos `.deployed`/`.failed`),
  `jboss-cli` (`deploy app.war --force`) ou console web (porta 9990, com usuário criado no `add-user`).
- **🛠️ Destrinchando na prática:** `.failed` contém o erro; `-Djboss.socket.binding.port-offset=100`
  se a 8080 estiver ocupada; `JAVA_HOME` precisa ser 17+ para o WildFly 35.
- 📖 [README › Executar](../README.md#executar)

---

### Perguntas para fazer ao entrevistador (mostram interesse)

1. A aplicação usa Java EE com `javax` ou já Jakarta EE? Há plano de migração?
2. Qual servidor de aplicação (WildFly, JBoss EAP, WebLogic…) e qual versão do JSF/PrimeFaces?
3. Como são feitos os testes automatizados e o deploy (CI/CD)?
4. Quais módulos do sistema de gestão de obras existem hoje e com que sistemas ele se integra (ERP, apps de campo)?
5. Como é a divisão do time e o processo de *code review*?

---

### Fontes da pesquisa sobre perguntas frequentes

- [Guru99 — Perguntas de entrevista J2EE](https://www.guru99.com/pt/j2ee-interview-questions.html)
- [DigitalOcean — JSF interview questions](https://digitalocean.com/community/tutorials/jsf-interview-questions-and-answers)
- [Javarevisited — N+1 em Hibernate](https://javarevisited.blogspot.com/2024/03/what-is-n1-select-problem-in-hibernate.html)
- [Hyring — Hibernate interview questions](https://hyring.com/jobseeker-toolkit/interview-questions/technical/hibernate)
- [InterviewPrep — EJB interview questions](https://interviewprep.org/enterprise-javabeans-ejb-interview-questions/)
- [Johns Hopkins — EJB container-managed transactions](https://jcs.ep.jhu.edu/ejava-javaee/coursedocs/content/html/ejbtx-container.html)
- [Educative — REST API interview questions](https://educative.io/blog/rest-api-interview-questions)
- [Merge — REST API interview questions](https://merge.dev/blog/rest-api-interview-questions)
- [TabNews — Bancos de dados e JPA em entrevistas técnicas](https://www.tabnews.com.br/seujorge/bancos-de-dados-e-jpa-perguntas-em-entrevistas-tecnicas-parte-ii)
- [Glassdoor — relatos de entrevista (ex.: Foton Informática: EJB × CDI, injeção de dependência)](https://static.glassdoor.com.br/Interview/Foton-Informatica-Interview-E462505-RVW95379945.htm)
- [IONOS — perguntas comuns sobre Java](https://www.ionos.com/pt-br/digitalguide/sites-de-internet/desenvolvimento-web/perguntas-da-entrevista-sobre-java.md)
