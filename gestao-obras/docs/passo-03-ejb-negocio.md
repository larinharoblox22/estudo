# Passo 3 — A Camada de Negócio (EJB)

> 📘 **Antes de começar: o que é EJB?**
>
> **EJB** (*Enterprise JavaBeans*, algo como "componentes Java corporativos") é a
> especificação que define componentes Java **gerenciados por um servidor de
> aplicação**. Em vez de você criar o objeto com `new`, o servidor cria, guarda e
> destrói esses objetos e "embrulha" cada chamada com serviços prontos: transações,
> segurança, pool de instâncias, execução assíncrona, agendamento etc. Você escreve
> só a regra de negócio; o resto vem "de graça".
>
> - **Bean** ("grão", em inglês — o nome brinca com *Java*, que também é um tipo de
>   café): é como o mundo Java chama um componente/objeto reutilizável. Um
>   **JavaBean** é uma classe simples com construtor sem argumentos e getters/setters;
>   um **EJB** é a versão "corporativa", cuja vida é controlada pelo servidor.
> - **Servidor de aplicação** (*application server*): o programa que executa a sua
>   aplicação e fornece esses serviços. Neste projeto é o **WildFly** (servidor
>   Jakarta EE de código aberto mantido pela Red Hat; é a base do produto comercial
>   JBoss EAP).
> - **Contêiner** (*container*): a parte do servidor que cria, guarda e destrói os
>   seus componentes e intercepta as chamadas a eles. Pense nele como um "gerente"
>   que controla a vida dos seus objetos. É o mesmo papel que o `ApplicationContext`
>   cumpre no Spring.
> - **Jakarta EE** (*Jakarta Enterprise Edition*): o conjunto de especificações Java
>   para aplicações corporativas, do qual o EJB faz parte. A plataforma já teve
>   outros nomes: **J2EE** (*Java 2 Platform, Enterprise Edition*, até 2006),
>   depois **Java EE** (*Java Platform, Enterprise Edition*) e, desde que a Oracle
>   transferiu a plataforma para a Eclipse Foundation (anunciado em 2017), **Jakarta
>   EE**. Por isso os pacotes mudaram de `javax.*` para `jakarta.*` (a partir do
>   Jakarta EE 9). Na prática, "Java EE" e "Jakarta EE" são a mesma plataforma em
>   épocas diferentes.

## Arquivos deste passo

```
gestao-obras/src/main/java/br/com/exemplo/gestaoobras/
├── model/StatusObra.java                       ← + regra de transição podeMudarPara()
└── service/
    ├── ObraService.java                        ← contrato de negócio (@Local)
    ├── ObraServiceBean.java                    ← implementação (@Stateless)
    ├── RelatorioSegurancaService.java          ← contrato de negócio (@Local)
    ├── RelatorioSegurancaServiceBean.java      ← implementação (@Stateless)
    ├── RegraNegocioException.java             ← @ApplicationException(rollback = true)
    └── EntidadeNaoEncontradaException.java     ← subtipo para "não encontrado" (404 no Passo 5)
```

> 📘 **Lendo a árvore acima:** as anotações `@Local`, `@Stateless` e
> `@ApplicationException` são explicadas nas seções 3, 2 e 6, respectivamente. O
> "404" é um **código de status HTTP** (*HyperText Transfer Protocol*, o protocolo
> de comunicação da web): toda resposta HTTP traz um número de três dígitos que
> resume o resultado do pedido. **404** significa *Not Found* ("não encontrado").
> Como antecipa o Javadoc da própria `EntidadeNaoEncontradaException`, no Passo 5 a
> API vai traduzir essa exceção em 404 e as demais regras de negócio em **422**
> (*Unprocessable Entity*, hoje chamado *Unprocessable Content*: o pedido está bem
> formado, mas viola uma regra) ou **400** (*Bad Request*: o pedido em si é
> inválido/malformado).

## 1. O padrão Session Facade

O Service é uma **fachada**: oferece aos clientes (tela JSF no Passo 4, API REST
no Passo 5) **operações de negócio completas** — "excluir obra", "registrar
relatório com interdição" — e esconde os DAOs, as queries e as transações.

> 📘 **Termos deste trecho**
>
> - **Padrão** (*pattern* ou *design pattern*, "padrão de projeto"): uma solução
>   conhecida, e com nome, para um problema que se repete no design de software. Dar
>   nome às soluções facilita a conversa: "aqui usamos um Session Facade".
> - **Service** ("serviço"): a classe da camada de negócio, que concentra as regras.
> - **Fachada** (*facade*): como a fachada de um prédio, que esconde a estrutura
>   interna e mostra só a porta de entrada. Uma classe-fachada oferece poucas
>   operações simples e, por trás, coordena várias outras classes.
> - **Session Facade** ("fachada de sessão"): uma fachada implementada como *session
>   bean* (um tipo de EJB, explicado na seção 2) — daí o "Session" no nome.
> - **Cliente**: quem chama o bean — pode ser uma tela, um recurso REST ou outro EJB.
>   Não confunda com o cliente pessoa.
> - **JSF** (*Jakarta Faces*, antigo *JavaServer Faces*): o framework de telas web do
>   Jakarta EE, baseado em componentes. O `ObraBean (JSF)` do diagrama abaixo é a
>   classe que dá suporte à tela de obras.
> - **API** (*Application Programming Interface*, "interface de programação de
>   aplicações"): o conjunto de operações que um sistema expõe para **outros
>   programas** usarem.
> - **REST** (*Representational State Transfer*, "transferência de estado
>   representacional"): um estilo de arquitetura para APIs sobre HTTP. Cada recurso
>   tem uma URL (ex.: `/obras/42`) e é manipulado pelos verbos HTTP (`GET`, `POST`,
>   `PUT`, `DELETE`). O `ObraResource (REST)` do diagrama é a classe que vai expor
>   essas URLs no Passo 5.
> - **DAO** (*Data Access Object*, "objeto de acesso a dados"): a classe que sabe
>   falar com o banco (Passo 2). O Service usa os DAOs, mas quem chama o Service nem
>   sabe que eles existem.
> - **Query** ("consulta"): um comando enviado ao banco para buscar dados.
> - **Transação**: um grupo de operações no banco que precisa acontecer "tudo ou
>   nada". Veja a seção 5.
> - **Interdição** (termo do domínio deste sistema): ordem de paralisação de uma obra
>   por risco grave à segurança. No sistema, um relatório com interdição muda a obra
>   para `SUSPENSA`. Relatórios de segurança de obra costumam verificar a
>   conformidade com a **NR-18** (Norma Regulamentadora nº 18 do Ministério do
>   Trabalho, sobre segurança e saúde no trabalho na indústria da construção) — os
>   dados de exemplo do projeto citam essa norma.

```
 ObraBean (JSF)          ObraResource (REST)
        \                     /
         ▼                   ▼
   ObraService  /  RelatorioSegurancaService      ← interfaces @Local (contratos)
         │                   │
   ObraServiceBean   RelatorioSegurancaServiceBean ← @Stateless (regras + transação)
         │                   │
      ObraDao   RelatorioSegurancaDao              ← @Transactional(MANDATORY)
```

> 📘 Na última linha do diagrama, `@Transactional(MANDATORY)` é a anotação de
> transação do próprio Jakarta EE (pacote `jakarta.transaction`, vista no Passo 2),
> usada em beans CDI comuns — CDI é explicado na seção 4. **MANDATORY**
> ("obrigatório") quer dizer que o DAO **exige** que quem o chamou já tenha aberto uma
> transação; ele nunca abre uma sozinho. A tabela completa desses atributos está na
> seção 5.

**Session Facade** é um dos *Core J2EE Patterns*. A ideia original era reduzir o
número de chamadas remotas (cada uma cara); hoje o benefício principal é ter
**uma operação = um caso de uso = uma transação**, em um único lugar.

> 📘 **O que são os *Core J2EE Patterns*?** É um catálogo de padrões de projeto para
> aplicações J2EE, publicado pela Sun Microsystems no livro *Core J2EE Patterns:
> Best Practices and Design Strategies* (Deepak Alur, John Crupi e Dan Malks, 2001).
> Vários padrões dele aparecem neste projeto ou em entrevistas: **Session Facade**,
> **DAO**, **Service Locator** (seção 3) e **Transfer Object** — este último é o que
> hoje se costuma chamar de **DTO** (*Data Transfer Object*, "objeto de
> transferência de dados": um objeto simples, só com dados e sem regras, usado para
> levar informações entre camadas ou pela rede).
>
> **Por que as "chamadas remotas" eram o problema?** Uma **chamada remota** é a
> chamada de um método que roda em outro processo ou outra máquina — em outra **JVM**
> (*Java Virtual Machine*, "máquina virtual Java": o processo que executa o seu código
> Java) —, passando pela rede. Cada uma custa milissegundos, contra nanossegundos de
> uma chamada local. Se a tela chamasse dez métodos pequenos remotamente, pagaria dez
> viagens pela rede; com a fachada, faz **uma** chamada ("excluir obra") e o trabalho
> miúdo acontece do lado do servidor.
>
> **Caso de uso** (*use case*): uma ação completa que tem significado para o usuário,
> como "excluir uma obra" ou "registrar um relatório".

> **Paralelo Spring Boot:** é a sua camada `@Service`. A diferença está no que vem
> "de graça" do contêiner, como você verá a seguir.

## 2. `@Stateless`: o que é e por que usar

Um *session bean* `@Stateless` é um componente gerenciado pelo contêiner EJB que
**não guarda estado de conversa com o cliente** entre chamadas.

> 📘 **Destrinchando a frase**
>
> - **Session bean** ("bean de sessão"): o tipo de EJB que executa lógica de negócio a
>   pedido de um cliente. Existem três sabores: `@Stateless`, `@Stateful` e
>   `@Singleton` (tabela mais abaixo).
> - **`@Stateless`** ("sem estado"): o bean não guarda, nos seus campos, nada que
>   pertença a um cliente específico.
> - **Estado de conversa** (*conversational state*): informações que precisam
>   sobreviver de uma chamada para a próxima do **mesmo** cliente — por exemplo, os
>   itens de um carrinho de compras. Um `@Stateless` não tem isso: cada chamada é
>   independente, como um caixa de banco que atende cada pessoa do zero, sem lembrar
>   da anterior.

### Ciclo de vida

**Ciclo de vida** (*lifecycle*) é a sequência de fases por que um objeto passa, do
"nascimento" à "morte", e os pontos em que o contêiner deixa você executar código seu.

```
 (não existe) ──construtor → injeções → @PostConstruct──► [ pool de instâncias prontas ]
                                                                 │      ▲
                                                   chamada chega │      │ chamada termina
                                                                 ▼      │
                                                          [ executando o método ]
 (não existe) ◄──@PreDestroy── (contêiner decide encolher o pool)
```

> 📘 **Lendo o diagrama**
>
> 1. O contêiner chama o **construtor** da classe (por isso ele precisa existir sem
>    argumentos — seção 4).
> 2. Faz as **injeções**: preenche os campos anotados com `@Inject`, `@EJB` etc.
>    (seção 4).
> 3. Chama o método anotado com **`@PostConstruct`** ("depois de construído"), se
>    houver. É o lugar certo para inicializações que dependem das injeções, porque
>    dentro do construtor os campos injetados ainda estão `null`.
> 4. A instância vai para o **pool** (explicado logo abaixo) e fica esperando chamadas.
> 5. Quando o contêiner decide encolher o pool (ou a aplicação é desligada
>    normalmente), ele chama o método anotado com **`@PreDestroy`** ("antes de
>    destruir"), para você liberar recursos, e descarta a instância.
>
> Métodos como esses, que **você escreve** e o **contêiner chama** em um momento
> definido, são chamados de *callbacks* ("chamadas de volta") de ciclo de vida.
> `@PostConstruct` e `@PreDestroy` são do pacote `jakarta.annotation`, e o Spring
> também os reconhece — provavelmente você já os usou.

O contêiner mantém um **pool** de instâncias. Cada chamada recebe uma instância
**exclusiva** durante a execução do método. Consequências:
- **Thread-safety sem `synchronized`:** duas threads nunca usam a mesma instância ao mesmo tempo.
- **Nenhum estado de cliente nos campos:** a próxima chamada do mesmo cliente
  pode cair em outra instância. Campos só para dependências injetadas.
- **Escalabilidade:** o pool cresce e encolhe com a carga; o tamanho é configuração
  do servidor, não do código.

> 📘 **O que é um pool?** *Pool* ("reservatório") é um conjunto de objetos já criados
> e prontos para uso, que são **emprestados** e **devolvidos**, em vez de criados e
> destruídos a cada vez. Analogia: os carrinhos de um supermercado — você pega um na
> entrada, usa e devolve; ninguém fabrica um carrinho novo para cada cliente. Você já
> conhece a ideia pelo *pool* de conexões com o banco (o HikariCP, padrão do Spring
> Boot).

> 📘 **Thread, thread-safety e `synchronized`**
>
> - **Thread** ("fio" de execução): uma linha de execução independente dentro do
>   programa. Um servidor atende várias requisições ao mesmo tempo, cada uma na sua
>   thread — como vários atendentes trabalhando em paralelo no mesmo balcão.
> - **Thread-safety** ("segurança entre threads"): uma classe é *thread-safe* quando
>   continua funcionando corretamente mesmo usada por várias threads ao mesmo tempo. O
>   perigo é duas threads alterarem o mesmo campo simultaneamente e uma estragar o
>   trabalho da outra.
> - **`synchronized`** ("sincronizado"): palavra-chave do Java que faz com que só uma
>   thread por vez execute aquele método ou bloco; as outras esperam na fila. Funciona,
>   mas pode virar gargalo.
> - Com o pool, o contêiner garante que **cada instância atende uma chamada por vez**,
>   então você não precisa de `synchronized`. Compare com o Spring: um `@Service` é um
>   *singleton* (uma única instância, veja a tabela abaixo) compartilhado por todas as
>   threads, e por isso lá você nunca guarda estado mutável de cliente em campos.
> - **Escalabilidade** (*scalability*): a capacidade de atender mais **carga** (mais
>   usuários e requisições ao mesmo tempo) sem reescrever o código.

### Os tipos de session bean

| EJB | Comportamento | Equivalente aproximado no Spring |
|---|---|---|
| `@Stateless` | Pool, sem estado de conversa | `@Service` (singleton sem estado) |
| `@Stateful` | Uma instância por cliente, mantém estado (ex.: carrinho), pode ser passivada em disco | Bean `@SessionScope` |
| `@Singleton` | Uma instância por aplicação; concorrência controlada por `@Lock(READ/WRITE)` | Singleton padrão (sem locks automáticos) |
| `@MessageDriven` | Consome mensagens JMS | `@JmsListener` / `@KafkaListener` |

> 📘 **Entendendo a tabela**
>
> - **`@Stateful`** ("com estado"): o contêiner dedica uma instância a cada cliente, e
>   ela lembra dos dados entre as chamadas (o exemplo clássico é o carrinho de
>   compras). A conversa termina quando o cliente chama um método anotado com
>   `@Remove` ou quando ela expira por inatividade.
> - **Passivação** (*passivation*): quando um `@Stateful` fica ocioso, o contêiner
>   pode **serializar** o seu estado (transformá-lo em bytes — veja a seção 3),
>   gravá-lo em disco e liberar a memória. Quando o cliente volta, o estado é lido de
>   volta (**ativação**). Os *callbacks* `@PrePassivate` e `@PostActivate` permitem
>   reagir a esses momentos. É como guardar no depósito os móveis de quem viajou e
>   trazê-los de volta quando a pessoa retorna.
> - **`@SessionScope`** (Spring): um bean com uma instância por **sessão HTTP** do
>   usuário. É só uma aproximação: o `@Stateful` é por cliente que tem a referência ao
>   bean, não necessariamente por sessão HTTP.
> - **`@Singleton`**: *singleton* ("único") é o nome do padrão de projeto em que existe
>   **uma única instância** de uma classe. O `@Singleton` do EJB é compartilhado por
>   todas as chamadas — útil, por exemplo, para um cache ou para configurações
>   carregadas na inicialização.
> - **`@Lock(READ/WRITE)`**: como várias threads usam o mesmo `@Singleton` ao mesmo
>   tempo, o contêiner controla a concorrência com *locks* ("travas"). Com
>   `@Lock(WRITE)` (o padrão), só uma thread por vez entra no método; com
>   `@Lock(READ)`, várias threads podem ler ao mesmo tempo. O singleton do Spring não
>   tem nada disso: a responsabilidade é sua.
> - **`@MessageDriven`**: cria um **MDB** (*Message-Driven Bean*, "bean dirigido por
>   mensagens"). Ninguém o chama diretamente: ele "escuta" uma fila e é acionado
>   quando uma mensagem chega. Rigorosamente, o MDB **não é** um *session bean* — é o
>   outro grande tipo de EJB —, mas aparece na tabela para completar o quadro.
> - **JMS** (*Java Message Service*, hoje chamado *Jakarta Messaging*): a API padrão do
>   Java para enviar e receber mensagens por meio de um *message broker* (um servidor
>   intermediário que guarda e entrega as mensagens; o WildFly traz um embutido, o
>   ActiveMQ Artemis, na sua configuração *full*). Mensagens permitem comunicação
>   **assíncrona**: quem envia não fica esperando quem recebe.
> - **Kafka** (Apache Kafka): uma plataforma distribuída de *streaming* de eventos
>   (fluxo contínuo de mensagens), muito usada para mensageria em alto volume. Não
>   segue a API JMS. No Spring, você consome mensagens Kafka com `@KafkaListener`
>   (projeto Spring for Apache Kafka), assim como consome JMS com `@JmsListener`.

### O que o contêiner oferece "de graça" a um EJB

| Serviço | EJB | Spring Boot |
|---|---|---|
| Transações | Automáticas em todo método público (CMT) | `@Transactional` explícito |
| Segurança | `@RolesAllowed` | `@PreAuthorize` / `@Secured` |
| Execução assíncrona | `@Asynchronous` | `@Async` |
| Agendamento | `@Schedule` (Timer Service) | `@Scheduled` |
| Interceptadores | `@Interceptors` / `@AroundInvoke` | AOP (`@Aspect` / `@Around`) |
| Pool de instâncias | Sim | Não (singleton) |
| Acesso remoto | `@Remote` (chamada remota, estilo RMI) | Não tem equivalente direto (usa-se REST/gRPC) |

> 📘 **Entendendo a tabela**
>
> - **CMT** (*Container-Managed Transactions*, "transações gerenciadas pelo
>   contêiner"): detalhado na seção 5.
> - **`@Transactional`**: no Spring, a anotação que liga a transação em um método ou
>   classe. (Existe também uma `@Transactional` do Jakarta, em `jakarta.transaction`,
>   para beans CDI — é a que os DAOs usam.)
> - **`@RolesAllowed`** ("papéis permitidos"): lista os papéis (*roles*, ex.:
>   `"ADMIN"`) que podem chamar o método; o contêiner barra os demais. No Spring
>   Security, `@Secured` faz o mesmo, e `@PreAuthorize` ("autorizar antes") aceita uma
>   expressão, ex.: `@PreAuthorize("hasRole('ADMIN')")`.
> - **`@Asynchronous`** / **`@Async`**: o método roda em **outra thread** e quem chamou
>   segue em frente sem esperar. Se precisar do resultado, o método devolve um
>   `Future` ("futuro": um objeto que representa um resultado que ficará pronto mais
>   tarde).
> - **`@Schedule`** / **`@Scheduled`**: executa um método periodicamente, em horários
>   definidos ("todo dia às 2h"), no estilo do *cron* (o agendador de tarefas do
>   Unix/Linux). No EJB, quem faz isso é o **Timer Service** ("serviço de
>   temporizadores") do contêiner; por padrão, os timers criados com `@Schedule` são
>   **persistentes**, isto é, sobrevivem a um reinício do servidor.
> - **Interceptadores** (*interceptors*): código que "intercepta" a chamada de um
>   método para fazer algo antes e depois dela (log, medição de tempo, auditoria).
>   `@Interceptors` liga uma classe interceptadora ao bean, e o método anotado com
>   `@AroundInvoke` ("em volta da invocação") envolve a chamada original.
> - **AOP** (*Aspect-Oriented Programming*, "programação orientada a aspectos"): a
>   técnica geral por trás dos interceptadores. Um **aspecto** é um interesse que
>   atravessa muitas classes (transação, segurança, log) e que você escreve em um
>   lugar só. No Spring, uma classe anotada com `@Aspect` e um método `@Around` são o
>   equivalente do `@AroundInvoke`.
> - **Singleton** (na linha "Pool de instâncias"): por padrão, o Spring cria **uma**
>   instância de cada bean e a compartilha entre todas as threads; não há pool.
> - **`@Remote`**: permite que outra JVM chame o EJB pela rede (seção 3).
> - **RMI** (*Remote Method Invocation*, "invocação remota de métodos"): o mecanismo do
>   Java para chamar métodos de um objeto que está em outra JVM como se ele fosse
>   local. Os EJBs remotos seguem esse modelo.
> - **gRPC**: framework de chamadas remotas criado pelo Google — RPC é *Remote
>   Procedure Call*, "chamada de procedimento remoto" —, que usa HTTP/2 e mensagens
>   binárias (*Protocol Buffers*). Junto com REST, é a forma moderna de um serviço
>   chamar outro.

No Spring você monta isso à la carte (starters + anotações). No EJB tudo já faz
parte da plataforma.

> 📘 *À la carte* (expressão francesa: "escolhendo do cardápio", item por item).
> *Starters* são as dependências `spring-boot-starter-*` do Spring Boot, cada uma
> trazendo um recurso já configurado (ex.: `spring-boot-starter-data-jpa`).

## 3. Interface `@Local` e Inversão de Dependência

```java
@Local
public interface ObraService { ... }

@Stateless
public class ObraServiceBean implements ObraService { ... }
```

Os clientes dependem da **interface**, nunca da classe `ObraServiceBean`. O
`RelatorioSegurancaServiceBean`, por exemplo, injeta `ObraService`. Isso aplica o
**DIP** (veja `docs/solid.md`): a tela e a API dependem de um contrato estável, e a
implementação pode mudar (ou ser substituída por um mock nos testes) sem afetá-las.

> 📘 **`@Local`, DIP e mock**
>
> - **`@Local`**: marca a interface como a **interface de negócio local** do EJB, isto
>   é, o "cardápio" de métodos que os clientes da mesma aplicação podem chamar.
> - **DIP** (*Dependency Inversion Principle*, "Princípio da Inversão de Dependência" —
>   o "D" do SOLID, seção 8): módulos de alto nível (regras de negócio, telas) não
>   devem depender de detalhes de baixo nível (uma classe concreta); ambos devem
>   depender de **abstrações** (interfaces). Na prática: quem usa o serviço conhece só
>   o `ObraService`, não o `ObraServiceBean`.
> - **Mock** ("imitação", "simulacro"): um objeto falso que se passa por uma
>   dependência real em um teste, devolvendo respostas pré-programadas. Há um exemplo
>   na seção 7.

As três "visões" de um EJB:

| Visão | Como declarar | Quem pode chamar |
|---|---|---|
| **No-interface view** | Só `@Stateless` na classe (desde o EJB 3.1) | Código na mesma aplicação |
| **`@Local`** | Interface de negócio local | Código na mesma JVM/aplicação (passagem por referência) |
| **`@Remote`** | Interface de negócio remota | Outras JVMs (chamada remota; passagem por valor, com serialização) |

> 📘 **Entendendo a tabela**
>
> - **Visão** (*view*): a "face" que o EJB mostra para quem o chama — quais métodos
>   aparecem e como podem ser chamados.
> - **No-interface view** ("visão sem interface"): o cliente injeta a própria classe do
>   bean (ex.: `ObraServiceBean`), e os métodos públicos dela viram os métodos de
>   negócio. O contêiner continua entregando ao cliente um *proxy* (intermediário,
>   explicado na seção 6), não o objeto real.
> - **Passagem por referência** vs **passagem por valor**: com `@Local`, o bean recebe
>   o **mesmo objeto** que o cliente passou; se o bean alterar um campo desse
>   parâmetro, o cliente enxerga a alteração. Com `@Remote`, o objeto viaja pela rede
>   como **cópia**: alterações feitas do lado do servidor não aparecem para o cliente.
>   (Detalhe técnico: em Java tudo é passado por valor — no caso de objetos, o "valor"
>   copiado é a referência. Na terminologia do EJB, "por referência" quer dizer "os
>   dois lados enxergam o mesmo objeto".)
> - **Serialização** (*serialization*): transformar um objeto em uma sequência de bytes
>   para enviá-lo pela rede ou gravá-lo em disco, e depois reconstruí-lo do outro lado
>   (**desserialização**). Em Java, a classe precisa implementar `Serializable` — é por
>   isso que a `Obra` e as exceções do projeto declaram um `serialVersionUID` (um número
>   de versão usado nesse processo).

**História que cai em entrevista:** no EJB 2.x as interfaces eram obrigatórias
(Home + Remote/Local para cada bean), com muito código repetitivo. O EJB 3.0
(Java EE 5) trouxe as anotações, e o EJB 3.1 (Java EE 6) tornou a interface
opcional com a *no-interface view*.

> 📘 **Home, Remote/Local e "código repetitivo"**: no EJB 2.x, cada bean exigia uma
> interface **Home** (uma espécie de "fábrica" que o cliente usava para criar ou
> localizar instâncias do bean), uma interface de componente **Remote** e/ou **Local**
> com os métodos de negócio, uma classe que implementava interfaces do próprio EJB
> (como `SessionBean`, com vários métodos de ciclo de vida obrigatórios, mesmo vazios)
> e um arquivo XML descritor (`ejb-jar.xml`). Esse código repetitivo, que não
> acrescenta lógica nenhuma, é o que em inglês se chama *boilerplate*. As
> **anotações** (*annotations*, como `@Stateless`) são metadados escritos direto no
> código-fonte, que o contêiner lê para decidir o comportamento — elas substituíram
> quase todo aquele XML.

### Nomes JNDI portáveis

Ao implantar, o WildFly registrou (log real):

```
java:global/gestao-obras/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
java:app/gestao-obras/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
java:module/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
```

> 📘 **JNDI e os prefixos `java:`**
>
> - **JNDI** (*Java Naming and Directory Interface*, "interface Java de nomes e
>   diretórios"): uma espécie de "lista telefônica" do servidor. Recursos (EJBs,
>   conexões com o banco, filas JMS) são registrados sob um **nome**, e qualquer código
>   pode procurá-los por esse nome. **Portável** significa que esse formato de nome é
>   definido pela especificação (desde o EJB 3.1) e vale em qualquer servidor, não só
>   no WildFly.
> - **Implantar** (*deploy*): instalar a aplicação no servidor para ela começar a
>   rodar. **Log** ("registro", "diário de bordo"): as mensagens que o servidor escreve
>   enquanto trabalha.
> - Os três prefixos são **escopos de visibilidade**, do mais amplo para o mais
>   restrito:
>   - `java:global/...` — visível para **todas** as aplicações implantadas no servidor;
>   - `java:app/...` — visível só dentro da **mesma aplicação**;
>   - `java:module/...` — visível só dentro do **mesmo módulo** (o mesmo pacote `.war`
>     ou `.jar`).
> - Formato: `java:global/<módulo>/<nome-do-bean>!<interface com o pacote>`. O `!`
>   separa o nome do bean da interface desejada, porque um bean pode ter mais de uma.
>   Aqui o módulo é `gestao-obras`, o nosso `.war` (*Web Archive*, o pacote de uma
>   aplicação web). Se a aplicação fosse um `.ear` (*Enterprise Archive*, um pacote que
>   agrupa vários módulos), o nome da aplicação viria antes do módulo.

No J2EE clássico, o cliente fazia `new InitialContext().lookup("java:global/...")`
(padrão *Service Locator*). Hoje a injeção (`@EJB` / `@Inject`) faz esse lookup
por você. No Spring, o equivalente seria `applicationContext.getBean(...)`.

> 📘 **Lookup e Service Locator**
>
> - **Lookup** ("consulta", "procura"): buscar um recurso no JNDI pelo nome.
>   `InitialContext` é a classe de entrada da API JNDI — o "balcão de informações" onde
>   você faz a consulta. (Um detalhe histórico: na época do J2EE os nomes eram outros,
>   normalmente `java:comp/env/ejb/...`; os nomes portáveis `java:global` só chegaram
>   no EJB 3.1. A mecânica do lookup, porém, é a mesma.)
> - **Service Locator** ("localizador de serviços"): padrão do catálogo *Core J2EE
>   Patterns* em que uma classe central concentra todos esses lookups (e guarda os
>   resultados em cache), para o resto do código não espalhar `InitialContext` por todo
>   lado. A limitação: cada classe ainda precisa **pedir** o que quer. Com a **injeção
>   de dependências** (seção 4), o contêiner **entrega** a dependência pronta, e a
>   classe não pede nada.

## 4. Injeção de dependências: `@Inject` vs `@EJB`

> 📘 **Injeção de dependências (DI) e Inversão de Controle (IoC)**
>
> - **Dependência**: um objeto de que a sua classe precisa para trabalhar (o
>   `RelatorioSegurancaServiceBean` precisa de um `ObraService` e de um DAO).
> - **DI** (*Dependency Injection*, "injeção de dependências"): em vez de a classe criar
>   as suas dependências com `new`, alguém de fora (o contêiner) as cria e as "injeta"
>   — coloca nos campos ou passa no construtor. Analogia: o cozinheiro de um
>   restaurante não vai ao mercado; os ingredientes chegam à bancada dele.
> - **IoC** (*Inversion of Control*, "inversão de controle"): o princípio mais geral por
>   trás da DI. Normalmente o seu código controla o fluxo e chama as bibliotecas; com
>   IoC, o **framework** controla o fluxo (cria os objetos, decide quando chamá-los) e
>   chama o seu código. DI é uma forma de IoC. O "contêiner IoC" do Spring é o
>   `ApplicationContext`.
> - Não confunda **DIP** (um princípio de design: depender de abstrações — seção 3) com
>   **DI** (uma técnica: alguém de fora fornece as dependências). Os dois combinam bem:
>   a DI entrega uma implementação, e o DIP faz você recebê-la pelo tipo da interface.

Usamos as duas de propósito em `RelatorioSegurancaServiceBean`:

```java
@EJB
private ObraService obraService;          // outro EJB

@Inject
private RelatorioSegurancaDao relatorioDao; // bean CDI
```

> 📘 **CDI e as duas anotações**
>
> - **CDI** (*Contexts and Dependency Injection*, "contextos e injeção de dependências";
>   hoje *Jakarta CDI*): a especificação de DI padrão do Jakarta EE, desde o Java EE 6.
>   Os "contextos" são os **escopos**, que definem quanto tempo um objeto vive
>   (`@RequestScoped`: uma requisição; `@SessionScoped`: a sessão do usuário;
>   `@ApplicationScoped`: a aplicação inteira). Um **bean CDI** é qualquer classe
>   gerenciada pelo CDI — como os nossos DAOs.
> - **`@Inject`**: a anotação de injeção usada pelo CDI. Ela vem da especificação
>   *Jakarta Dependency Injection* (pacote `jakarta.inject`), que o Spring também
>   entende.
> - **`@EJB`**: a anotação de injeção específica para EJBs, mais antiga que o CDI.

| | `@Inject` (CDI) | `@EJB` |
|---|---|---|
| Injeta | Qualquer bean CDI, **inclusive EJBs** | Só EJBs |
| Resolução | Por tipo + qualificadores (`@Named`, anotações próprias) | Por tipo, `beanName` ou `lookup` JNDI |
| EJB remoto | Não | Sim |
| Quando usar | Padrão moderno, para quase tudo | EJB remoto, lookup por nome, código legado |

> 📘 **Termos da tabela**
>
> - **Resolução**: como o contêiner decide **qual** objeto injetar em um campo.
> - **Qualificador** (*qualifier*): uma anotação que desempata quando existem **dois
>   beans do mesmo tipo** (ex.: duas implementações de `ObraService`). Você marca a
>   implementação e o ponto de injeção com o mesmo qualificador. É o papel do
>   `@Qualifier` no Spring. "Anotações próprias" são qualificadores que você mesmo cria.
> - **`@Named`**: qualificador pronto do CDI que dá um **nome** ao bean. É também o que
>   torna um bean acessível pelo nome nas telas JSF (ex.: `#{obraBean}`).
> - **`beanName`**: atributo do `@EJB` que escolhe o EJB pelo nome (por padrão, o nome
>   simples da classe, ex.: `ObraServiceBean`).
> - **`lookup`**: atributo do `@EJB` que recebe diretamente um nome JNDI.
> - **Código legado** (*legacy code*): código antigo que continua em produção e que você
>   mantém, mas não escreveria do mesmo jeito hoje.

`@Inject ObraService obraService` funcionaria igual aqui. O `@EJB` deixa explícito
que estamos chamando **outro EJB**, e que a chamada passa pelo contêiner
(transação, segurança, interceptadores).

> **Paralelo Spring Boot:** `@Inject` ≈ `@Autowired`. No Spring, a recomendação é
> injeção pelo **construtor**. Em EJB, a injeção por **campo** é o padrão, porque a
> especificação exige um construtor público sem argumentos.

> 📘 **Injeção por campo vs por construtor**
>
> - **`@Autowired`** ("ligado automaticamente"): a anotação de injeção própria do Spring.
> - **Injeção por campo** (*field injection*): a anotação fica no próprio campo
>   (`@Inject private ObraDao obraDao;`) e o contêiner preenche o campo depois de criar
>   o objeto, usando *reflection* ("reflexão": o recurso do Java que permite inspecionar
>   e manipular classes em tempo de execução, inclusive campos `private`).
> - **Injeção por construtor** (*constructor injection*): as dependências chegam como
>   parâmetros do construtor. Vantagens: os campos podem ser `final`, o objeto nunca
>   existe "pela metade" e, nos testes, basta fazer `new` passando os mocks.
> - **Construtor público sem argumentos** (*no-arg constructor*): um construtor `public`
>   que não recebe parâmetros. O contêiner EJB precisa dele para criar as instâncias.

## 5. Transações ACID com CMT

> 📘 **O que é uma transação e o que significa ACID?**
>
> Uma **transação** é um conjunto de operações no banco tratado como **uma unidade
> só**: ou tudo é confirmado, ou nada é. Analogia: uma transferência bancária —
> debitar de uma conta e creditar na outra precisam acontecer juntas; nunca só uma
> das duas.
>
> **ACID** é a sigla das quatro garantias que um banco transacional oferece (cada uma
> é detalhada mais abaixo, com experimentos reais):
> - **A**tomicidade (*Atomicity*): tudo ou nada.
> - **C**onsistência (*Consistency*): o banco só passa de um estado válido para outro
>   estado válido.
> - **I**solamento (*Isolation*): transações simultâneas não atrapalham umas às outras.
> - **D**urabilidade (*Durability*): depois de confirmada, a alteração não se perde,
>   mesmo que o servidor caia.

**CMT (Container-Managed Transactions):** todo método público de um EJB é
transacional **sem nenhuma anotação**. O contêiner abre a transação JTA ao
entrar no método e faz commit ao sair (ou rollback, conforme a exceção). O
`@TransactionAttribute(REQUIRED)` no `ObraServiceBean` está lá só por didática —
é o padrão.

> 📘 **Termos do parágrafo**
>
> - **CMT** (*Container-Managed Transactions*, "transações gerenciadas pelo
>   contêiner"): você não escreve "abrir" nem "confirmar"; o contêiner faz isso em volta
>   de cada método. A alternativa é a **BMT** (*Bean-Managed Transactions*, "transações
>   gerenciadas pelo bean"): com `@TransactionManagement(TransactionManagementType.BEAN)`,
>   você controla a transação na mão com um `UserTransaction` (`begin()`, `commit()`,
>   `rollback()`). BMT é rara; só vale quando você precisa de controle fino.
> - **JTA** (*Java Transaction API*, hoje *Jakarta Transactions*): a API padrão de
>   transações gerenciadas pelo servidor de aplicação. Uma transação JTA pode até
>   envolver **vários recursos** ao mesmo tempo (dois bancos, ou um banco e uma fila
>   JMS) — a chamada transação distribuída, coordenada pelo padrão **XA** com um
>   *commit* em duas fases. No Spring, quem faz esse papel é o
>   `PlatformTransactionManager` (com JPA, normalmente o `JpaTransactionManager`, que
>   cuida de um banco só).
> - **Commit** ("confirmar", "efetivar"): gravar definitivamente todas as alterações da
>   transação. Só depois do *commit* as outras transações passam a enxergar os dados.
> - **Rollback** ("reverter", "desfazer"): descartar todas as alterações feitas pela
>   transação, como se ela nunca tivesse acontecido.
> - **`@TransactionAttribute`**: a anotação do EJB que define o **atributo de
>   transação** (a propagação, explicada a seguir) de uma classe ou de um método. Sem
>   ela, vale `REQUIRED`.

> ⚠️ **Diferença importante para quem vem do Spring:** no Spring, um método de
> `@Service` **sem** `@Transactional` não tem transação. No EJB, tem.

### Atributos de transação

**Propagação** (*propagation*) é a regra que decide o que acontece quando um método
transacional é chamado: se **já existe** uma transação em andamento, ele entra nela,
cria outra ou falha? Se **não existe**, ele cria uma ou roda sem? No EJB isso se
chama *atributo de transação*; no Spring, `propagation`.

| EJB (`@TransactionAttribute`) | Comportamento | Spring (`propagation`) |
|---|---|---|
| `REQUIRED` (padrão) | Entra na transação existente ou cria uma nova | `REQUIRED` (padrão) |
| `REQUIRES_NEW` | Sempre cria uma nova, suspendendo a atual | `REQUIRES_NEW` |
| `MANDATORY` | Exige uma transação existente, senão falha | `MANDATORY` |
| `SUPPORTS` | Usa a transação se houver; senão roda sem | `SUPPORTS` |
| `NOT_SUPPORTED` | Suspende a transação atual e roda sem | `NOT_SUPPORTED` |
| `NEVER` | Falha se houver transação | `NEVER` |
| — | — | `NESTED` (savepoints; não existe em EJB) |

> 📘 **Traduzindo os nomes** (a tradução literal ajuda a memorizar):
>
> - `REQUIRED` = "exigida": o método precisa de uma transação; usa a que existe ou
>   cria uma.
> - `REQUIRES_NEW` = "exige uma nova": sempre cria a sua, deixando a do chamador
>   **suspensa** (pausada) até terminar. As duas fazem *commit*/*rollback* de forma
>   independente.
> - `MANDATORY` = "obrigatória": a transação **tem** de vir de quem chamou; senão, o
>   contêiner lança uma exceção (no EJB, `EJBTransactionRequiredException`).
> - `SUPPORTS` = "suporta": aceita uma transação, mas não exige.
> - `NOT_SUPPORTED` = "não suportada": roda sempre fora de transação (suspende a que
>   houver).
> - `NEVER` = "nunca": se houver transação, exceção.
> - `NESTED` = "aninhada" (só no Spring): cria uma subtransação dentro da atual usando
>   um **savepoint** ("ponto de salvamento"): uma marca no meio da transação para a
>   qual você pode voltar com um *rollback* parcial, sem desfazer o que veio antes —
>   como um *checkpoint* de videogame. O JTA não oferece transações aninhadas de forma
>   padronizada, por isso o EJB não tem esse atributo.

No projeto:
- **Services:** `REQUIRED` → cada operação de negócio é uma transação.
- **DAOs:** `MANDATORY` (Passo 2) → nunca abrem transação própria.
- **Chamada entre EJBs:** `RelatorioSegurancaServiceBean.registrar` chama
  `obraService.buscarPorId`, que é `REQUIRED` e **entra na mesma transação**.
- **`REQUIRES_NEW`** (não usado aqui) é o caso clássico de log de auditoria
  (registro de quem fez o quê e quando, para rastreabilidade): o
  registro "tentativa de excluir obra X" deve ser gravado mesmo que a operação
  principal sofra rollback.

> 💡 EJB não tem `readOnly` como o `@Transactional(readOnly = true)` do Spring.
> Quando necessário, usa-se uma hint do provider na query (ex.: `org.hibernate.readOnly`).

> 📘 **`readOnly`, *hint* e *provider***
>
> - **`readOnly = true`** ("somente leitura"): no Spring, avisa que a transação só vai
>   ler. Com Hibernate, isso permite otimizações, como não guardar cópias das entidades
>   para comparação (o *dirty checking*, explicado no fim desta seção) e não fazer
>   *flush* (enviar alterações pendentes ao banco).
> - **Hint** ("dica"): uma configuração extra passada a uma query, que o provider pode
>   usar para otimizar — ex.: `query.setHint("org.hibernate.readOnly", true)` carrega as
>   entidades em modo somente leitura.
> - **Provider** ("provedor"): a implementação concreta da especificação **JPA**
>   (*Jakarta Persistence*, antes *Java Persistence API*: a especificação de
>   mapeamento objeto-relacional vista nos Passos 1 e 2). No WildFly, o provider é o
>   **Hibernate**. As hints com prefixo `org.hibernate.` só valem para ele; outros
>   providers as ignoram.

### A — Atomicidade: tudo ou nada

*Atomicidade* vem de "átomo", do grego *átomos*, "indivisível": a transação é uma
unidade que não pode ser dividida.

`ObraServiceBean.excluir` faz duas escritas em uma única transação:

```java
relatorioDao.removerPorObra(obra.getId());   // DELETE relatórios
obraDao.removerPorId(obra.getId());          // DELETE obra
```

> 📘 `DELETE`, `INSERT` e `UPDATE` (que aparecem logo abaixo) são os comandos de
> remoção, inserção e atualização da **SQL** (*Structured Query Language*,
> "linguagem de consulta estruturada"), a linguagem dos bancos relacionais.

**Experimento executado no WildFly:** um EJB de teste chamou `excluir(escola)` e,
logo depois, lançou `RegraNegocioException`. Resultado:

```
09 excluir e falhar (rollback): RegraNegocioException | falha forçada depois do excluir
09 depois: obra existe=Escola Municipal Jardim Botânico relatorios=1
```

Os dois `DELETE` já tinham sido enviados ao banco, mas o rollback desfez ambos.
É a propagação `REQUIRED` em ação: o `excluir` entrou na transação do EJB de
teste, e a exceção dele desfez tudo.

O mesmo vale para `registrar(..., interditarObra = true)`: o `INSERT` do relatório
e o `UPDATE` do status da obra acontecem juntos ou não acontecem.

### C — Consistência: o banco só passa de um estado válido para outro

Garantida em camadas:
- **Banco:** FK `fk_relatorio_obra`, `NOT NULL`, `CHECK` do enum.
- **Bean Validation:** `@NotBlank`, `@Size`… (teste 11: nome vazio rejeitado).
- **Regras de negócio no Service:**

| Regra | Onde | Resultado no WildFly |
|---|---|---|
| Nome de obra único (sem diferenciar maiúsculas) | `ObraServiceBean.validarNomeUnico` | `02 nome duplicado: RegraNegocioException` |
| Obra concluída é estado final | `StatusObra.podeMudarPara` + `ObraServiceBean.salvar` | `05 reabrir concluida: RegraNegocioException` |
| Obra concluída não recebe relatórios | `RelatorioSegurancaServiceBean.registrar` | `06 relatorio em concluida: RegraNegocioException` |
| Interdição suspende a obra | `registrar(..., true)` | `08 depois: status=SUSPENSA` |
| Excluir obra remove os relatórios | `ObraServiceBean.excluir` | `10 depois: EntidadeNaoEncontradaException` |
| Registro inexistente | `buscarPorId` / `excluir` | `07 ...: EntidadeNaoEncontradaException` |

> 📘 **Restrições do banco e Bean Validation** (a primeira e a segunda camadas acima)
>
> - **FK** (*Foreign Key*, "chave estrangeira"): uma coluna que aponta para a chave
>   primária de outra tabela. A `fk_relatorio_obra` garante que todo relatório aponta
>   para uma obra que existe.
> - **`NOT NULL`** ("não nulo"): a coluna não aceita ficar sem valor.
> - **`CHECK`** ("verificação"): uma condição que todo valor da coluna precisa
>   cumprir. Aqui, o `CHECK` da coluna `status` só aceita os nomes das constantes do
>   **enum** `StatusObra` (*enum*, de *enumeration*, "enumeração": um tipo com um
>   conjunto fixo de valores, como `PLANEJADA`, `EM_ANDAMENTO`, `SUSPENSA` e
>   `CONCLUIDA`).
> - **Bean Validation** (*Jakarta Bean Validation*): a especificação que valida objetos
>   a partir de anotações nos campos. `@NotBlank` ("não em branco"): o texto não pode
>   ser nulo, vazio ou só espaços. `@Size` ("tamanho"): limita o tamanho mínimo e/ou
>   máximo (na `Obra`, o nome tem no máximo 150 caracteres). O JPA executa essa
>   validação automaticamente antes de inserir ou atualizar uma entidade.

### I — Isolamento: transações concorrentes não se atropelam

- O **nível de isolamento** vem do DataSource (o padrão do PostgreSQL e do H2 é
  `READ COMMITTED`: ninguém lê dados não commitados de outra transação).
- O **`@Version`** detecta o *lost update* (dois usuários editando a mesma obra).
  Teste 04: duas cópias da mesma obra; a primeira grava (v0 → v1), a segunda,
  com versão velha, é rejeitada:

```
04a atualizar: OK -> v1
04b atualizar com versao velha: EJBException <- OptimisticLockException <- StaleObjectStateException
```

> 📘 **Nível de isolamento, DataSource, PostgreSQL e H2**
>
> - **Transações concorrentes**: transações que estão rodando **ao mesmo tempo**,
>   disputando os mesmos dados.
> - **Nível de isolamento** (*isolation level*): o quanto uma transação "enxerga" do que
>   as outras estão fazendo ao mesmo tempo. O padrão SQL define quatro, do mais
>   permissivo ao mais rígido: `READ UNCOMMITTED`, `READ COMMITTED`, `REPEATABLE READ`
>   e `SERIALIZABLE`. Quanto mais rígido, menos anomalias — e menos desempenho quando
>   há muita concorrência.
> - **`READ COMMITTED`** ("ler o que foi confirmado"): você só enxerga dados que outras
>   transações já confirmaram (*commitaram* — aportuguesamento de *commit*). Isso evita
>   a **leitura suja** (*dirty read*: ler algo que depois sofre rollback e, portanto,
>   "nunca existiu"). Mas, se você ler a mesma linha duas vezes, pode ver valores
>   diferentes, caso outra transação confirme uma alteração no meio.
> - **DataSource** ("fonte de dados"): o objeto, configurado no servidor, que fornece
>   conexões com o banco (com um pool de conexões). A aplicação o obtém por JNDI
>   (Passo 2).
> - **PostgreSQL**: banco de dados relacional de código aberto, muito usado em produção.
> - **H2**: banco relacional escrito em Java, leve, que pode rodar **em memória** (dentro
>   do próprio processo, sem gravar em disco). É o banco que vem configurado por padrão
>   no WildFly para exemplos e desenvolvimento.

> 📘 **Lost update e `@Version`**
>
> - **Lost update** ("atualização perdida"): Ana e Bruno abrem a mesma obra. Ana muda a
>   localização e salva. Bruno, com a tela ainda antiga, muda o status e salva — e, sem
>   proteção, a gravação dele sobrescreve a localização que a Ana tinha acabado de
>   corrigir. A alteração da Ana "se perdeu" sem ninguém perceber.
> - **`@Version`**: anotação do JPA em um campo numérico (na `Obra`, o campo `versao`)
>   que funciona como "número de versão" da linha. A cada atualização, o Hibernate
>   inclui a versão lida na condição e grava a próxima, algo como
>   `UPDATE obra SET ..., versao = 1 WHERE id = 42 AND versao = 0`. Se outra transação
>   já gravou antes (a versão no banco agora é 1), o `WHERE` não encontra nenhuma linha
>   e o Hibernate lança a exceção. (No caso de `merge` de uma cópia com versão velha,
>   como no teste 04, o Hibernate percebe a diferença já no `merge`, ao comparar com a
>   versão atual no banco.)
> - Isso é **concorrência otimista** (*optimistic locking*, "trava otimista"): parte do
>   princípio de que conflitos são raros, não trava nada antecipadamente e só confere
>   no momento de gravar. A alternativa, a **trava pessimista**, bloqueia a linha no
>   banco enquanto alguém a está editando.
> - **v0 → v1**: os números da versão antes e depois da primeira gravação.

> 📘 **Lendo a cadeia de exceções do teste 04:** a seta `<-` aponta para a **causa**, da
> exceção mais externa para a mais interna. O Hibernate detectou o conflito e lançou
> `StaleObjectStateException` ("exceção de estado de objeto obsoleto", do próprio
> Hibernate); o JPA a converteu na exceção padrão `OptimisticLockException` ("falha de
> trava otimista"); e o contêiner EJB, por ser uma exceção de sistema (seção 6), a
> embrulhou em `EJBException`.

- ⚠️ **Limite honesto:** `validarNomeUnico` é um *check-then-act*. Duas transações
  simultâneas podem passar pela verificação antes de qualquer uma gravar. A
  garantia definitiva é um índice único no banco, criado via migração
  (ex.: `CREATE UNIQUE INDEX ux_obra_nome ON obra (LOWER(nome))` no PostgreSQL).
  A verificação no Service continua útil para dar uma mensagem amigável.

> 📘 **Check-then-act, race condition e índice único**
>
> - **Check-then-act** ("verificar e depois agir"): o código primeiro **verifica** uma
>   condição ("já existe obra com esse nome?") e depois **age** com base nela ("não
>   existe, então posso inserir"). Entre o verificar e o agir existe uma janela de
>   tempo em que o mundo pode mudar.
> - **Race condition** ("condição de corrida"): um defeito em que o resultado depende de
>   quem "chega primeiro". Aqui: as transações T1 e T2 verificam ao mesmo tempo, nenhuma
>   encontra o nome (nenhuma gravou ainda), e as duas inserem → duas obras com o mesmo
>   nome. Isso acontece mesmo com `READ COMMITTED`, porque nenhuma das duas enxerga o
>   `INSERT` ainda não confirmado da outra.
> - **Índice único** (*unique index*): um **índice** é uma estrutura auxiliar do banco
>   que acelera buscas (como o índice remissivo no fim de um livro); um índice
>   **único** tem a regra extra de **não aceitar valores repetidos**. Como é o próprio
>   banco que confere, no momento da gravação e de forma atômica, a corrida acaba: a
>   segunda transação recebe um erro de violação de unicidade. O `LOWER(nome)` faz o
>   índice comparar os nomes em minúsculas, ignorando a diferença entre maiúsculas e
>   minúsculas.
> - **Migração** (*migration*): um script versionado que altera a estrutura do banco
>   (criar tabela, índice etc.), aplicado de forma controlada por ferramentas como
>   **Flyway** ou **Liquibase**.

### D — Durabilidade: depois do commit, o dado sobrevive

No PostgreSQL, o commit só retorna depois de gravado no log (WAL). ⚠️ O nosso H2
**em memória** não é durável: os dados somem quando o servidor reinicia. Serve
para desenvolvimento, não para produção.

> 📘 **WAL** (*Write-Ahead Log*, "log de escrita antecipada"): antes de alterar os
> arquivos de dados, o PostgreSQL registra a alteração em um arquivo de log sequencial
> no disco — e só então responde ao *commit*. Se o servidor cair, ao reiniciar o banco
> relê o WAL e refaz o que faltava. Gravar no log é rápido (é só acrescentar no fim do
> arquivo), e é isso que torna a durabilidade barata. Analogia: o garçom anota o pedido
> no bloquinho antes de ir à cozinha; mesmo que ele se distraia, o pedido está escrito.
> **Produção** é o ambiente real, usado pelos usuários; **desenvolvimento** é o ambiente
> onde você programa e testa.

### Persistence Context compartilhado e dirty checking

```java
Obra obra = obraService.buscarPorId(obraId);   // outro EJB, MESMA transação
...
obra.setStatus(StatusObra.SUSPENSA);           // nenhum update() explícito
```

Como as duas classes estão na mesma transação JTA, compartilham o mesmo
Persistence Context: a obra retornada está **managed**, e o dirty checking gera
o `UPDATE` no commit. Confirmado no teste 08 (`status=SUSPENSA`).

> 📘 **Persistence Context, *managed*, *dirty checking* e *flush***
>
> - **Persistence Context** ("contexto de persistência"): o "espaço de trabalho" do
>   `EntityManager` do JPA — o conjunto de entidades que ele carregou e está
>   acompanhando. Funciona como um cache: dentro dele, cada linha do banco corresponde
>   a **um único** objeto Java. Com JTA, o contexto dura o tempo da transação; por isso,
>   quem está na mesma transação compartilha o mesmo contexto (e recebe os mesmos
>   objetos).
> - **Managed** ("gerenciada"): o estado de uma entidade que está dentro de um
>   Persistence Context ativo. Qualquer alteração em uma entidade *managed* é percebida
>   pelo JPA. (Os outros estados, vistos no Passo 2, são *new*, *detached* —
>   "desanexada" — e *removed*.)
> - **Dirty checking** ("verificação de sujeira"): ao carregar uma entidade, o
>   Hibernate guarda uma cópia (*snapshot*, "fotografia") dos valores. No *flush*,
>   compara cada entidade *managed* com a sua fotografia; o que mudou está "sujo"
>   (*dirty*) e vira um `UPDATE` automático. Por isso o `obra.setStatus(...)` basta, sem
>   chamar nenhum `update()`.
> - **Flush** ("descarga"): o momento em que o JPA envia ao banco os comandos SQL
>   pendentes. Acontece no *commit* (ou antes de uma query que dependa desses dados).
>   *Flush* **não** é *commit*: os comandos enviados ainda podem sofrer rollback — foi
>   exatamente o que aconteceu no experimento de atomicidade.

## 6. Exceções e rollback (a pergunta mais traiçoeira sobre EJB)

> 📘 **Relembrando os tipos de exceção do Java**
>
> - **Exceção *checked*** ("verificada"): subclasse de `Exception` que **não** é
>   subclasse de `RuntimeException`. O compilador **obriga** você a tratá-la com
>   `try/catch` ou declará-la com `throws`. Ex.: `IOException`.
> - **Exceção *unchecked*** ("não verificada"): subclasse de **`RuntimeException`**
>   ("exceção de tempo de execução"). O compilador não obriga a tratar. Ex.:
>   `NullPointerException` (uso de uma referência `null`) e `IllegalArgumentException`.
> - **`Error`** ("erro"): problemas graves da JVM, como `OutOfMemoryError`. Também é
>   *unchecked*, e normalmente não se tenta tratar.

O contêiner EJB divide as exceções em dois tipos:

| | Exceção de **sistema** | Exceção de **aplicação** |
|---|---|---|
| O que é | `RuntimeException` sem `@ApplicationException` (ex.: `OptimisticLockException`, `ConstraintViolationException`, `NullPointerException`) | Exceção *checked*, ou `RuntimeException` anotada com `@ApplicationException` |
| Rollback | **Sempre** | **Só** se `rollback = true` (ou `setRollbackOnly()`) |
| O cliente recebe | `EJBException` embrulhando a original (ou `EJBTransactionRolledbackException` se o cliente já tinha transação) | A própria exceção, sem embrulho |
| Instância do bean | **Descartada** do pool | Continua no pool |
| Log | O contêiner registra como erro | Não registra (é esperada) |

> 📘 **Entendendo a tabela**
>
> - **Exceção de sistema** (*system exception*): para o contêiner, um erro **inesperado**
>   ou técnico — algo deu errado na infraestrutura ou no código. Ele assume o pior:
>   desfaz a transação e joga fora a instância do bean (que pode ter ficado em um estado
>   inconsistente).
> - **Exceção de aplicação** (*application exception*): uma situação **prevista** pelo
>   negócio ("nome duplicado"). Faz parte do contrato do método e deve chegar intacta ao
>   cliente.
> - **`@ApplicationException`**: a anotação que transforma uma `RuntimeException` em
>   exceção de aplicação. O atributo `rollback = true` diz se, além disso, a transação
>   deve ser desfeita. A anotação vale também para as subclasses (atributo `inherited`,
>   que é `true` por padrão).
> - **`setRollbackOnly()`** ("marcar só para rollback"): método do contexto do EJB
>   (`SessionContext`, que herda de `EJBContext`) que marca a transação atual para que
>   ela só possa terminar em rollback, sem precisar lançar exceção.
> - **`EJBException`**: a exceção de sistema genérica do EJB (é uma `RuntimeException`).
>   **Embrulhar** (*wrap*) significa criar uma nova exceção e guardar a original dentro
>   dela como **causa** (`getCause()`). Por isso, para descobrir o erro real, você
>   precisa olhar a causa.
> - **`EJBTransactionRolledbackException`** ("transação EJB desfeita"): subclasse de
>   `EJBException`, usada quando o cliente já estava em uma transação, para avisá-lo de
>   que ela foi marcada para rollback.
> - **`OptimisticLockException`**: o conflito de versão visto na seção de Isolamento.
> - **`ConstraintViolationException`** ("exceção de violação de restrição"): aqui, a do
>   Bean Validation (`jakarta.validation`), lançada quando um objeto viola anotações
>   como `@NotBlank`. (O Hibernate tem uma homônima,
>   `org.hibernate.exception.ConstraintViolationException`, para violações de
>   restrições do **banco**, como FK ou índice único.)
> - **`NullPointerException`**: lançada ao usar uma referência `null` como se fosse um
>   objeto — o exemplo clássico de bug, ou seja, de exceção de sistema.
> - **Log** — "o contêiner registra como erro": ele escreve a exceção, com o *stack
>   trace* (a "pilha de chamadas" que mostra por quais métodos o erro passou), no log do
>   servidor.

Por isso `RegraNegocioException` tem `@ApplicationException(rollback = true)`: é
uma situação **esperada** do negócio, deve chegar limpa à tela e precisa desfazer
o que foi feito. A subclasse `EntidadeNaoEncontradaException` herda esse
comportamento. Resultados reais:

```
02 nome duplicado: RegraNegocioException | Já existe uma obra com o nome "edifício AURORA".
11 obra invalida (excecao de sistema): EJBException <- ConstraintViolationException | ...
```

⚠️ **Pegadinha clássica:** em EJB, uma exceção *checked* **não** faz rollback por
padrão — a transação é **commitada**. No Spring é igual: rollback automático só
para `RuntimeException` e `Error`; para *checked* é preciso
`@Transactional(rollbackFor = ...)`. A diferença do Spring é que ele não embrulha
a exceção nem descarta o bean.

> 📘 `rollbackFor` ("rollback para") é o atributo do `@Transactional` do Spring que
> lista as exceções que também devem causar rollback, ex.:
> `@Transactional(rollbackFor = IOException.class)`.

### Auto-invocação (self-invocation)

Se um método do EJB chama outro método **do mesmo bean** com `this.metodo()`, a
chamada **não passa pelo contêiner**: o `@TransactionAttribute` do segundo método
é ignorado. No Spring acontece exatamente o mesmo com `@Transactional` (o proxy é
contornado). A solução é chamar via outro bean (como fizemos com `@EJB ObraService`)
ou via `sessionContext.getBusinessObject(ObraService.class)`.

> 📘 **Por que `this.metodo()` escapa do contêiner? (o *proxy*)**
>
> *Self-invocation* ("auto-invocação") é um objeto chamar um método **de si mesmo**.
> Para entender o problema, é preciso saber que, quando você injeta um EJB (ou um bean
> `@Transactional` do Spring), **não recebe o objeto real**: recebe um **proxy**
> ("procurador", "intermediário") — um objeto gerado pelo contêiner, com os mesmos
> métodos, que fica **na frente** do bean. A cada chamada, o proxy: (1) pega uma
> instância do pool, (2) confere a segurança, (3) abre ou reaproveita a transação,
> (4) chama o método real e (5) faz *commit* ou *rollback*. É como uma recepcionista
> que, antes de passar a ligação, confere quem é e anota o recado.
>
> ```
> cliente ──► [ proxy do contêiner ] ──► instância real do bean
>              (transação, segurança,        │
>               interceptadores)             └─ this.outroMetodo()  ← chamada direta,
>                                                                     NÃO passa pelo proxy
> ```
>
> Dentro do bean, `this` é a instância **real**, não o proxy. Uma chamada
> `this.outroMetodo()` vai direto, sem recepcionista: as anotações de `outroMetodo()`
> (`@TransactionAttribute`, `@RolesAllowed`, `@Asynchronous`...) não são aplicadas.
>
> - **`SessionContext`** ("contexto da sessão"): objeto que o contêiner fornece ao bean
>   (injetado com `@Resource`) para acessar serviços do contêiner.
>   `getBusinessObject(ObraService.class)` devolve **o proxy** do próprio bean;
>   chamando por ele, a chamada passa pelo contêiner.
> - No Spring vale o mesmo para o modo padrão de AOP, que é baseado em proxies.

## 7. Testabilidade: EJBs são POJOs

Desde o EJB 3, um session bean é uma classe Java comum com anotações. As regras de
negócio podem ser testadas **sem servidor**, com mocks (exemplo com JUnit 5 + Mockito):

> 📘 **POJO, JUnit e Mockito**
>
> - **POJO** (*Plain Old Java Object*, "bom e velho objeto Java"): uma classe Java
>   comum, que não precisa herdar de classes nem implementar interfaces de um
>   framework. O termo foi cunhado por Martin Fowler e colegas para valorizar o código
>   simples. Um EJB 3 é um POJO com anotações: fora do servidor, ninguém processa as
>   anotações e ele é só uma classe — dá para fazer `new` e testar.
> - **Mock**: o objeto falso já mencionado na seção 3. Aqui, os DAOs são *mocks*: não
>   acessam banco nenhum, apenas devolvem o que o teste programou.
> - **JUnit 5**: o framework de testes mais usado em Java (`@Test`, `assertThrows`...).
> - **Mockito**: a biblioteca mais usada em Java para criar *mocks*.

```java
@ExtendWith(MockitoExtension.class)
class ObraServiceBeanTest {

    @Mock ObraDao obraDao;
    @Mock RelatorioSegurancaDao relatorioDao;
    @InjectMocks ObraServiceBean service;

    @Test
    void naoPermiteNomeDuplicado() {
        when(obraDao.existeComNome("Edifício Aurora", null)).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> service.salvar(new Obra("Edifício Aurora", "SP", StatusObra.PLANEJADA)));
        verify(obraDao, never()).inserir(any());
    }
}
```

> 📘 **Lendo o teste linha a linha**
>
> - `@ExtendWith(MockitoExtension.class)`: liga o Mockito ao JUnit 5, para que ele
>   processe as anotações abaixo.
> - `@Mock`: cria um objeto falso do tipo do campo.
> - `@InjectMocks`: cria um `ObraServiceBean` **real** e coloca os mocks nos seus campos
>   — o Mockito faz o papel do contêiner.
> - `when(...).thenReturn(true)`: "quando `existeComNome` for chamado com esses
>   argumentos, devolva `true`" — simula um nome que já existe.
> - `assertThrows(...)`: verifica que a chamada lança `RegraNegocioException`.
> - `verify(obraDao, never()).inserir(any())`: confirma que `inserir` **nunca** foi
>   chamado — a regra barrou a gravação antes.

O comportamento do contêiner (transações, rollback, JNDI) se testa com testes de
integração (Arquillian ou implantação real), como fizemos no WildFly.

> 📘 **Teste unitário vs teste de integração; Arquillian**
>
> - **Teste unitário** (*unit test*): testa **uma** classe isolada, com as dependências
>   substituídas por mocks — como o teste acima. É rápido (milissegundos) e não precisa
>   de servidor nem de banco. Prova que a **regra** está certa.
> - **Teste de integração** (*integration test*): testa várias peças funcionando
>   **juntas e de verdade** — contêiner, transações, banco. É mais lento, mas é o único
>   que prova que o rollback, o JNDI e a propagação funcionam.
> - **Arquillian**: framework de testes de integração para Jakarta EE (do ecossistema
>   JBoss/Red Hat). Ele empacota as classes do teste, implanta esse pacote em um
>   servidor real (ou embutido) e roda os testes **dentro** do servidor, com injeção e
>   transações de verdade. É o parente mais próximo do `@SpringBootTest`.

## 8. SOLID neste passo

**SOLID** é um acrônimo de cinco princípios de design orientado a objetos reunidos por
Robert C. Martin ("Uncle Bob"): **S**ingle Responsibility (Responsabilidade Única),
**O**pen/Closed (Aberto/Fechado), **L**iskov Substitution (Substituição de Liskov),
**I**nterface Segregation (Segregação de Interfaces) e **D**ependency Inversion
(Inversão de Dependência). A explicação completa está em `docs/solid.md`.

| Princípio | Onde |
|---|---|
| **S** | Service = regras + fronteira transacional; DAO = acesso a dados; `StatusObra` = regra de transição; exceções = sinalização. |
| **O** | Nova regra de transição se adiciona no `StatusObra`, sem mexer no Service. Novos tipos de erro estendem `RegraNegocioException` sem mudar quem já trata a classe-mãe. |
| **L** | `EntidadeNaoEncontradaException` substitui `RegraNegocioException` em qualquer `catch` e herda o rollback. Os contratos (`@throws`) estão documentados na interface: qualquer implementação deve honrá-los. |
| **I** | Dois services pequenos (por agregado) em vez de um `GestaoObrasService` gigante. A API REST receberá um contrato só de leitura no Passo 5. |
| **D** | Clientes dependem de `ObraService` / `RelatorioSegurancaService` (`@Local`), nunca dos `*Bean`. |

> 📘 **Termos da tabela**
>
> - **Fronteira transacional** (*transaction boundary*): o ponto do código onde a
>   transação começa e termina — aqui, a entrada e a saída de cada método do Service.
> - **Classe-mãe** (superclasse): a classe da qual outra herda. `RegraNegocioException`
>   é a classe-mãe de `EntidadeNaoEncontradaException`.
> - **`catch`** ("capturar"): o bloco que trata uma exceção. Um
>   `catch (RegraNegocioException e)` também captura a subclasse — é a substituição de
>   Liskov na prática.
> - **`@throws`**: marcação do **Javadoc** (a documentação escrita em comentários
>   `/** ... */` no código) que descreve quais exceções um método pode lançar e em que
>   situação.
> - **Agregado** (*aggregate*): termo do **DDD** (*Domain-Driven Design*, "projeto
>   orientado ao domínio", de Eric Evans) para um grupo de objetos de negócio tratados
>   como uma unidade, com uma "raiz" por onde tudo é acessado. Aqui, obras e relatórios
>   de segurança são tratados como agregados distintos, cada um com o seu Service.

## 9. Resumo Jakarta EE ↔ Spring Boot (Passo 3)

| Conceito | Jakarta EE (aqui) | Spring Boot |
|---|---|---|
| Camada de negócio | `@Stateless` | `@Service` |
| Contrato | Interface `@Local` | Interface do service (opcional) |
| Transação | Automática em todo método público (CMT, `REQUIRED`) | `@Transactional` explícito |
| Propagação | `@TransactionAttribute(...)` | `@Transactional(propagation = ...)` |
| Rollback em exceção de negócio | `@ApplicationException(rollback = true)` | Automático para `RuntimeException` |
| Exceção técnica | Embrulhada em `EJBException`; bean descartado | Propagada como está |
| Injeção | `@EJB` / `@Inject` (campo) | `@Autowired` / `@Inject` (construtor) |
| Instâncias | Pool | Singleton |
| Leitura otimizada | Hint do provider | `@Transactional(readOnly = true)` |
| Localização | JNDI (`java:global/...`) | `ApplicationContext` |

## 10. Perguntas prováveis na entrevista

1. **Diferença entre `@Stateless`, `@Stateful` e `@Singleton`?** Pool sem estado / uma
   instância por cliente com estado / uma instância por aplicação com locks.
2. **Qual o atributo de transação padrão de um EJB?** `REQUIRED`, aplicado mesmo sem anotação.
3. **Uma exceção checked lançada por um EJB faz rollback?** Não, por padrão a transação é
   commitada. Só com `@ApplicationException(rollback = true)` ou `setRollbackOnly()`.
4. **Exceção de sistema vs de aplicação?** Sistema: rollback, `EJBException`, bean
   descartado. Aplicação: chega como está, rollback só se configurado.
5. **O que é o problema da auto-invocação?** `this.metodo()` não passa pelo
   contêiner/proxy; as anotações de transação do método chamado são ignoradas.
6. **`@Inject` ou `@EJB`?** `@Inject` por padrão; `@EJB` para EJB remoto, lookup por
   nome ou para deixar explícita a chamada entre EJBs.
7. **Como evitar que dois usuários sobrescrevam a mesma obra?** `@Version`
   (concorrência otimista), que gera `OptimisticLockException`.
8. **Para que serve a interface `@Local`?** Contrato de negócio (DIP); desde o EJB 3.1 é
   opcional (*no-interface view*).

## Glossário

Revisão rápida de todos os termos e siglas explicados acima, em ordem alfabética (as
anotações estão ordenadas sem o `@`, e os códigos HTTP vêm primeiro).

| Termo | Significado |
|---|---|
| **400** (HTTP) | *Bad Request*: o pedido é inválido ou malformado. |
| **404** (HTTP) | *Not Found*: o recurso pedido não existe. Será o destino de `EntidadeNaoEncontradaException` no Passo 5. |
| **422** (HTTP) | *Unprocessable Entity/Content*: o pedido está bem formado, mas viola uma regra de negócio. |
| **À la carte** | Expressão francesa: "escolhendo do cardápio", item por item. |
| **ACID** | Atomicidade, Consistência, Isolamento e Durabilidade: as quatro garantias de uma transação. |
| **Agregado** (*aggregate*) | Termo do DDD: grupo de objetos de negócio tratados como unidade, acessados por uma raiz. |
| **Anotação** (*annotation*) | Metadado escrito no código (ex.: `@Stateless`) que o contêiner lê para decidir o comportamento. |
| **AOP** | *Aspect-Oriented Programming*: técnica para escrever em um só lugar interesses que atravessam muitas classes (log, transação, segurança). |
| **API** | *Application Programming Interface*: conjunto de operações que um sistema expõe para outros programas. |
| **ApplicationContext** | O contêiner IoC do Spring: cria e gerencia os beans. |
| **`@ApplicationException`** | Transforma uma `RuntimeException` em exceção de aplicação; com `rollback = true`, também desfaz a transação. |
| **`@Around` / `@Aspect`** | Anotações de AOP usadas no Spring: `@Aspect` marca a classe do aspecto; `@Around` envolve a execução de um método. |
| **`@AroundInvoke`** | Marca, em um interceptador EJB, o método que envolve a chamada do método de negócio. |
| **Arquillian** | Framework de testes de integração que implanta o teste dentro de um servidor Jakarta EE real ou embutido. |
| **Assíncrono** | Comunicação ou execução em que quem chama não fica esperando o resultado. |
| **`@Async` / `@Asynchronous`** | Executam o método em outra thread, sem o chamador esperar (Spring / EJB). |
| **Atomicidade** | O "A" do ACID: a transação é indivisível — tudo ou nada. |
| **`@Autowired`** | Anotação de injeção de dependências do Spring. |
| **Bean** | Componente/objeto Java reutilizável; nos frameworks, um objeto cuja vida é gerenciada pelo contêiner. |
| **Bean CDI** | Qualquer classe gerenciada pelo CDI (como os DAOs do projeto). |
| **Bean Validation** | Especificação que valida objetos a partir de anotações como `@NotBlank` e `@Size`. |
| **`beanName`** | Atributo do `@EJB` que escolhe o EJB pelo nome. |
| **BMT** | *Bean-Managed Transactions*: o próprio bean controla a transação com `UserTransaction`. Alternativa rara à CMT. |
| **Boilerplate** | Código repetitivo, obrigatório, que não acrescenta lógica. |
| **Callback** | Método que você escreve e o contêiner chama em um momento definido (ex.: `@PostConstruct`). |
| **Caso de uso** (*use case*) | Uma ação completa que tem significado para o usuário ("excluir obra"). |
| **`catch`** | Bloco que captura e trata uma exceção. |
| **CDI** | *Contexts and Dependency Injection*: a especificação de injeção de dependências padrão do Jakarta EE, com escopos (contextos). |
| **Chamada remota** | Chamada a um método que roda em outra JVM/máquina, passando pela rede; muito mais cara que uma chamada local. |
| **`CHECK`** | Restrição SQL: condição que todo valor de uma coluna precisa cumprir. |
| **Check-then-act** | "Verificar e depois agir": padrão sujeito a *race condition*, pois o mundo pode mudar entre a verificação e a ação. |
| **Classe-mãe** | Superclasse: a classe da qual outra herda. |
| **Cliente** (de um EJB) | Quem chama o bean: uma tela, um recurso REST ou outro EJB. |
| **CMT** | *Container-Managed Transactions*: o contêiner abre e fecha a transação em volta de cada método de negócio. |
| **Commit** | Confirmar a transação, gravando definitivamente as alterações. |
| **Concorrência otimista** (*optimistic locking*) | Não trava nada antecipadamente; confere a versão (`@Version`) na hora de gravar. A pessimista trava a linha no banco. |
| **Consistência** | O "C" do ACID: o banco só passa de um estado válido para outro. |
| **`ConstraintViolationException`** | Violação de restrição. Aqui, a do Bean Validation (`jakarta.validation`); o Hibernate tem uma homônima para restrições do banco. |
| **Contêiner** (*container*) | Parte do servidor que cria, gerencia e destrói os componentes e intercepta as chamadas a eles. |
| **Core J2EE Patterns** | Catálogo de padrões da Sun (livro de 2001): Session Facade, DAO, Service Locator, Transfer Object etc. |
| **Cron** | Agendador de tarefas do Unix/Linux; dá nome ao estilo de agendamento usado por `@Schedule`/`@Scheduled`. |
| **DAO** | *Data Access Object*: classe que encapsula o acesso ao banco. |
| **DataSource** | Objeto configurado no servidor que fornece conexões (com pool) ao banco; obtido via JNDI. |
| **DDD** | *Domain-Driven Design*: abordagem de design centrada no domínio do negócio (Eric Evans). |
| **Dependência** | Objeto de que uma classe precisa para trabalhar. |
| **Deploy** (implantar) | Instalar a aplicação no servidor para ela começar a rodar. |
| **DI** | *Dependency Injection*: alguém de fora (o contêiner) fornece as dependências de uma classe. |
| **DIP** | *Dependency Inversion Principle*: depender de abstrações (interfaces), não de classes concretas. O "D" do SOLID. |
| **Dirty checking** | O Hibernate compara as entidades *managed* com a "fotografia" tirada ao carregá-las e gera `UPDATE` para o que mudou. |
| **Dirty read** (leitura suja) | Ler dados ainda não confirmados que depois sofrem rollback. Evitada pelo `READ COMMITTED`. |
| **DTO** | *Data Transfer Object*: objeto só com dados, usado para transportar informações entre camadas (o *Transfer Object* do catálogo J2EE). |
| **Durabilidade** | O "D" do ACID: depois do commit, o dado sobrevive a quedas do servidor. |
| **EAR** | *Enterprise Archive*: pacote que agrupa vários módulos de uma aplicação Jakarta EE. |
| **EJB** | *Enterprise JavaBeans*: componentes Java gerenciados pelo servidor de aplicação, com transações, segurança, pool etc. "de graça". |
| **`@EJB`** | Anotação de injeção específica para EJBs (permite EJB remoto e lookup por nome). |
| **`EJBException`** | Exceção de sistema genérica do EJB; embrulha a exceção original como causa. |
| **`EJBTransactionRequiredException`** | Lançada quando um método `MANDATORY` é chamado sem transação. |
| **`EJBTransactionRolledbackException`** | Subclasse de `EJBException` usada quando o cliente já tinha transação e ela foi marcada para rollback. |
| **Embrulhar** (*wrap*) | Criar uma exceção nova guardando a original como causa (`getCause()`). |
| **Enum** | *Enumeration*: tipo com um conjunto fixo de valores (ex.: `StatusObra`). |
| **`Error`** | Problema grave da JVM (ex.: `OutOfMemoryError`); *unchecked*. |
| **Escalabilidade** (*scalability*) | Capacidade de atender mais carga sem reescrever o código. |
| **Escopo** (contexto, no CDI) | Quanto tempo um bean vive: `@RequestScoped`, `@SessionScoped`, `@ApplicationScoped`. |
| **Estado de conversa** (*conversational state*) | Dados de um cliente que precisam sobreviver entre chamadas (ex.: carrinho). |
| **Exceção *checked*** | Subclasse de `Exception` (fora de `RuntimeException`); o compilador obriga a tratar ou declarar. |
| **Exceção de aplicação** | Situação prevista pelo negócio; chega ao cliente sem embrulho; rollback só se configurado. |
| **Exceção de sistema** | Erro inesperado/técnico; rollback sempre, embrulhada em `EJBException`, instância descartada. |
| **Exceção *unchecked*** | Subclasse de `RuntimeException` (ou `Error`); o compilador não obriga a tratar. |
| **Fachada** (*facade*) | Classe que oferece operações simples e esconde a complexidade de várias outras. |
| **FK** | *Foreign Key* (chave estrangeira): coluna que aponta para a chave primária de outra tabela. |
| **Flush** | Envio ao banco dos comandos SQL pendentes; não é commit. |
| **Fronteira transacional** | Ponto onde a transação começa e termina (aqui, cada método do Service). |
| **Future** | Objeto que representa um resultado que ficará pronto mais tarde (retorno de métodos assíncronos). |
| **gRPC** | Framework de chamadas remotas (RPC) do Google, sobre HTTP/2 e Protocol Buffers. |
| **H2** | Banco relacional leve, escrito em Java, que pode rodar em memória. Não é durável nesse modo. |
| **Hibernate** | A implementação (provider) de JPA usada pelo WildFly. |
| **Hint** | "Dica" de configuração passada a uma query (ex.: `org.hibernate.readOnly`). |
| **Home** (interface) | No EJB 2.x, a "fábrica" usada pelo cliente para criar ou localizar instâncias do bean. |
| **HTTP** | *HyperText Transfer Protocol*: o protocolo de comunicação da web. |
| **Índice único** (*unique index*) | Índice do banco que não aceita valores repetidos; a garantia definitiva de unicidade. |
| **InitialContext** | Classe de entrada da API JNDI, usada para fazer lookups. |
| **Injeção por campo / por construtor** | *Field injection*: o contêiner preenche o campo. *Constructor injection*: as dependências chegam pelo construtor. |
| **`@Inject`** | Anotação de injeção do CDI (pacote `jakarta.inject`); equivalente ao `@Autowired`. |
| **`@InjectMocks`** | Mockito: cria a classe real e injeta nela os mocks. |
| **Interceptador** (*interceptor*) | Código que roda antes e depois da chamada de um método (`@Interceptors` / `@AroundInvoke`). |
| **Interdição** | Termo do domínio: paralisação de uma obra por risco grave; no sistema, muda a obra para `SUSPENSA`. |
| **IoC** | *Inversion of Control*: o framework controla o fluxo e chama o seu código. DI é uma forma de IoC. |
| **Isolamento** | O "I" do ACID: transações simultâneas não atrapalham umas às outras. |
| **Isolation level** (nível de isolamento) | Quanto uma transação enxerga das outras: `READ UNCOMMITTED`, `READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE`. |
| **J2EE / Java EE / Jakarta EE** | Nomes sucessivos da plataforma Java corporativa (até 2006 / até a ida para a Eclipse Foundation / atual). |
| **`java:global` / `java:app` / `java:module`** | Escopos de nomes JNDI portáveis: todo o servidor / a mesma aplicação / o mesmo módulo. |
| **JavaBean** | Classe Java simples com construtor sem argumentos e getters/setters. |
| **Javadoc** | Documentação escrita em comentários `/** ... */` no código (com marcações como `@throws`). |
| **JMS** | *Java Message Service* (hoje *Jakarta Messaging*): API padrão para enviar e receber mensagens via *message broker*. |
| **`@JmsListener` / `@KafkaListener`** | Anotações do Spring para consumir mensagens JMS / Kafka. |
| **JNDI** | *Java Naming and Directory Interface*: a "lista telefônica" do servidor, onde recursos são registrados e procurados por nome. |
| **JPA** | *Jakarta Persistence* (antes *Java Persistence API*): especificação de mapeamento objeto-relacional. |
| **JSF** | *Jakarta Faces* (antes *JavaServer Faces*): framework de telas web baseado em componentes. |
| **JTA** | *Java Transaction API* (hoje *Jakarta Transactions*): API de transações gerenciadas pelo servidor, inclusive distribuídas. |
| **JUnit** | O framework de testes mais usado em Java. |
| **JVM** | *Java Virtual Machine*: o processo que executa o código Java. |
| **Kafka** | Apache Kafka: plataforma distribuída de streaming de eventos/mensagens; não segue a API JMS. |
| **Legado** (*legacy*) | Código antigo que continua em produção. |
| **Lifecycle** (ciclo de vida) | Fases de um objeto, do nascimento à destruição, e os pontos em que o contêiner chama o seu código. |
| **`@Local`** | Interface de negócio local do EJB: chamada na mesma JVM/aplicação, com passagem por referência. |
| **`@Lock(READ/WRITE)`** | Controle de concorrência de um `@Singleton`: `WRITE` (padrão) = uma thread por vez; `READ` = leituras simultâneas. |
| **Log** | Registro das mensagens que o servidor escreve enquanto trabalha. |
| **Log de auditoria** | Registro de quem fez o quê e quando, para rastreabilidade. |
| **Lookup** | Busca de um recurso no JNDI pelo nome. |
| **Lost update** | "Atualização perdida": a gravação de um usuário sobrescreve silenciosamente a de outro. |
| **Managed** | Estado de uma entidade dentro de um Persistence Context ativo; suas alterações viram `UPDATE` automaticamente. |
| **MANDATORY** | Atributo de transação: exige uma transação existente; senão, exceção. |
| **MDB** | *Message-Driven Bean*: EJB (`@MessageDriven`) acionado pela chegada de mensagens, não por chamadas diretas. |
| **Message broker** | Servidor intermediário que guarda e entrega mensagens (ex.: ActiveMQ Artemis). |
| **Migração** (*migration*) | Script versionado de alteração do banco, aplicado por ferramentas como Flyway ou Liquibase. |
| **Mock** | Objeto falso que imita uma dependência real em um teste. |
| **`@Mock`** | Mockito: cria um mock do tipo do campo. |
| **Mockito** | Biblioteca Java para criar mocks. |
| **`@Named`** | Qualificador do CDI que dá nome ao bean (e o expõe às telas JSF). |
| **NESTED** | Propagação do Spring: subtransação com savepoint. Não existe no EJB. |
| **NEVER** | Atributo de transação: falha se houver transação. |
| **No-arg constructor** | Construtor público sem argumentos, exigido pela especificação EJB. |
| **No-interface view** | Visão do EJB sem interface: o cliente injeta a própria classe (desde o EJB 3.1). |
| **NOT NULL** | Restrição SQL: a coluna não aceita ficar sem valor. |
| **NOT_SUPPORTED** | Atributo de transação: suspende a transação atual e roda sem. |
| **`@NotBlank` / `@Size`** | Bean Validation: texto não pode ser nulo/vazio/só espaços / tamanho mínimo e máximo. |
| **NR-18** | Norma Regulamentadora nº 18: segurança e saúde no trabalho na indústria da construção (citada nos dados de exemplo). |
| **`NullPointerException`** | Exceção ao usar uma referência `null` como objeto; exceção de sistema típica. |
| **`OptimisticLockException`** | Exceção do JPA quando a versão (`@Version`) mudou desde a leitura: conflito de concorrência. |
| **Passagem por referência / por valor** | `@Local`: os dois lados enxergam o mesmo objeto. `@Remote`: o objeto viaja como cópia serializada. |
| **Passivação / ativação** | O contêiner grava em disco o estado de um `@Stateful` ocioso e o recarrega quando o cliente volta. |
| **Pattern** (padrão de projeto) | Solução conhecida e com nome para um problema recorrente de design. |
| **Persistence Context** | "Espaço de trabalho" do `EntityManager`: as entidades carregadas e acompanhadas; com JTA, dura a transação. |
| **POJO** | *Plain Old Java Object*: classe Java comum, sem dependência obrigatória de framework. |
| **Pool** | Conjunto de objetos prontos, emprestados e devolvidos em vez de criados a cada uso. |
| **`@PostConstruct` / `@PreDestroy`** | Callbacks chamados depois da criação e das injeções / antes da destruição da instância. |
| **PostgreSQL** | Banco de dados relacional de código aberto; usa WAL para garantir durabilidade. |
| **`@PreAuthorize` / `@Secured`** | Anotações do Spring Security para restringir o acesso a métodos (com expressão / por papéis). |
| **Produção / desenvolvimento** | Ambiente real dos usuários / ambiente onde você programa e testa. |
| **Propagação** (*propagation*) | Regra do que fazer quando um método transacional é chamado com ou sem transação em andamento. |
| **Provider** (JPA) | A implementação concreta da especificação JPA (no WildFly, o Hibernate). |
| **Proxy** | Objeto intermediário gerado pelo contêiner que fica na frente do bean e aplica transação, segurança e interceptadores. |
| **Qualificador** (*qualifier*) | Anotação que desempata entre dois beans do mesmo tipo. |
| **Query** | Consulta enviada ao banco. |
| **Race condition** | "Condição de corrida": o resultado depende de quem chega primeiro. |
| **`READ COMMITTED`** | Nível de isolamento em que só se leem dados já confirmados; padrão do PostgreSQL e do H2. |
| **`readOnly`** | Atributo do `@Transactional` do Spring que marca a transação como somente leitura, permitindo otimizações. |
| **Reflection** | Recurso do Java para inspecionar e manipular classes em tempo de execução (usado na injeção por campo). |
| **`@Remote`** | Interface de negócio remota: chamada por outras JVMs, com passagem por valor (serialização). |
| **`@Remove`** | Marca o método que encerra a conversa de um `@Stateful`. |
| **REQUIRED** | Atributo de transação padrão: entra na transação existente ou cria uma nova. |
| **REQUIRES_NEW** | Atributo de transação: sempre cria uma nova, suspendendo a atual. |
| **REST** | *Representational State Transfer*: estilo de API sobre HTTP, com recursos identificados por URLs. |
| **RMI** | *Remote Method Invocation*: mecanismo Java para chamar métodos de objetos em outra JVM. |
| **`@RolesAllowed`** | Lista os papéis (*roles*) que podem chamar um método EJB. |
| **Rollback** | Desfazer todas as alterações da transação, como se ela nunca tivesse acontecido. |
| **`rollbackFor`** | Atributo do `@Transactional` do Spring que lista exceções extras que devem causar rollback. |
| **`RuntimeException`** | Base das exceções *unchecked* em Java. |
| **Savepoint** | Marca no meio de uma transação para a qual se pode voltar com rollback parcial. |
| **`@Schedule` / `@Scheduled`** | Executam um método periodicamente, estilo *cron* (EJB / Spring). |
| **Self-invocation** (auto-invocação) | Um objeto chamar um método de si mesmo com `this`; não passa pelo proxy. |
| **Serialização** | Transformar um objeto em bytes (e de volta), para enviar pela rede ou gravar em disco. |
| **Service** | Classe da camada de negócio. |
| **Service Locator** | Padrão J2EE em que uma classe central faz os lookups JNDI; substituído pela injeção de dependências. |
| **Servidor de aplicação** (*application server*) | Programa que executa a aplicação e fornece os serviços da plataforma (aqui, o WildFly). |
| **Session bean** | EJB que executa lógica de negócio a pedido de um cliente: `@Stateless`, `@Stateful` ou `@Singleton`. |
| **Session Facade** | Padrão: uma fachada implementada como session bean, com operações de negócio completas. |
| **`SessionContext`** | Objeto do contêiner (injetado com `@Resource`) com `getBusinessObject`, `setRollbackOnly` etc. |
| **`@SessionScope`** | Spring: uma instância do bean por sessão HTTP. |
| **`setRollbackOnly()`** | Marca a transação atual para terminar em rollback, sem lançar exceção. |
| **Singleton** | Padrão em que existe uma única instância de uma classe; escopo padrão dos beans do Spring. |
| **`@Singleton`** (EJB) | Session bean com uma instância por aplicação, com concorrência controlada por `@Lock`. |
| **Snapshot** | "Fotografia" dos valores de uma entidade, usada pelo dirty checking. |
| **SOLID** | Cinco princípios de design OO: Responsabilidade Única, Aberto/Fechado, Liskov, Segregação de Interfaces, Inversão de Dependência. |
| **SQL** | *Structured Query Language*: a linguagem dos bancos relacionais (`INSERT`, `UPDATE`, `DELETE`...). |
| **Stack trace** | A pilha de chamadas que mostra por quais métodos uma exceção passou. |
| **`StaleObjectStateException`** | Exceção do Hibernate para "objeto com estado obsoleto" (conflito de versão); vira `OptimisticLockException`. |
| **Starters** | Dependências `spring-boot-starter-*` que trazem um recurso já configurado. |
| **`@Stateful`** | Session bean com uma instância por cliente, que guarda estado de conversa e pode ser passivado. |
| **`@Stateless`** | Session bean sem estado de conversa, servido a partir de um pool. |
| **SUPPORTS** | Atributo de transação: usa a transação se houver; senão roda sem. |
| **`synchronized`** | Palavra-chave Java que deixa só uma thread por vez executar um método/bloco. |
| **Teste de integração** | Testa várias peças reais funcionando juntas (contêiner, transação, banco). |
| **Teste unitário** (*unit test*) | Testa uma classe isolada, com dependências substituídas por mocks. |
| **Thread** | Linha de execução independente dentro de um programa. |
| **Thread-safety** | Propriedade de uma classe que funciona corretamente mesmo usada por várias threads ao mesmo tempo. |
| **Timer Service** | Serviço de temporizadores do contêiner EJB, usado por `@Schedule`; timers persistentes por padrão. |
| **Transação** | Conjunto de operações no banco tratado como unidade: tudo ou nada. |
| **`@Transactional`** | Anotação que define a transação: a do Spring (`org.springframework...`) ou a do Jakarta (`jakarta.transaction`, para beans CDI). |
| **`@TransactionAttribute`** | Anotação EJB que define o atributo de transação (propagação); padrão `REQUIRED`. |
| **Transfer Object** | Nome do DTO no catálogo *Core J2EE Patterns*. |
| **`@Version`** | Campo de versão do JPA usado na concorrência otimista. |
| **Visão** (*view*) de um EJB | A "face" que o EJB mostra ao cliente: no-interface, `@Local` ou `@Remote`. |
| **WAL** | *Write-Ahead Log*: o PostgreSQL grava cada alteração em um log sequencial antes de confirmar o commit. |
| **WAR** | *Web Archive*: o pacote de uma aplicação web Java (aqui, `gestao-obras`). |
| **WildFly** | Servidor de aplicação Jakarta EE de código aberto da Red Hat (base do JBoss EAP). |
| **XA** | Padrão de transações distribuídas, com commit em duas fases, usado pelo JTA para coordenar vários recursos. |
