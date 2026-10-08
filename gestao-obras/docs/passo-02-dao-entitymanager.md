# Passo 2 — O Padrão DAO e o EntityManager

> 📘 **Antes de começar: os dois protagonistas deste passo**
>
> - **DAO** (*Data Access Object*, "objeto de acesso a dados"): uma classe cuja
>   única função é ler e gravar dados no banco, escondendo do resto do sistema
>   *como* isso é feito (qual SQL, qual tabela, qual tecnologia). Quem usa o DAO
>   só chama métodos como `buscarPorId(10L)` e recebe objetos Java prontos.
> - **`EntityManager`** ("gerenciador de entidades"): a interface principal do
>   **JPA** (*Jakarta Persistence API*, antes chamada *Java Persistence API*), a
>   especificação Java para **ORM** (*Object-Relational Mapping*, "mapeamento
>   objeto-relacional", ou seja, transformar objetos Java em linhas de tabelas e
>   vice-versa). É por ele que você insere, busca, atualiza e remove *entidades*
>   (classes anotadas com `@Entity`, como `Obra`). Pense nele como o "balcão de
>   atendimento" entre os seus objetos Java e o banco de dados.

## Arquivos deste passo

```
gestao-obras/src/main/
├── java/br/com/exemplo/gestaoobras/dao/
│   ├── GenericDao.java              ← CRUD genérico (reutilizado por todos os DAOs)
│   ├── ObraDao.java                 ← queries específicas de Obra
│   └── RelatorioSegurancaDao.java   ← queries específicas de RelatorioSeguranca
└── resources/META-INF/
    ├── persistence.xml              ← configuração da Persistence Unit
    └── dados-iniciais.sql           ← dados de demonstração
```

> 📘 **Termos que aparecem na árvore acima**
>
> - **CRUD**: sigla de *Create, Read, Update, Delete* (criar, ler, atualizar,
>   apagar), as quatro operações básicas que praticamente toda tabela precisa. Em
>   **SQL** (*Structured Query Language*, "linguagem de consulta estruturada", a
>   linguagem padrão dos bancos de dados relacionais), elas correspondem a
>   `INSERT`, `SELECT`, `UPDATE` e `DELETE`.
> - ***Query*** ("consulta"): um comando enviado ao banco para buscar (ou
>   alterar) dados.
> - **`persistence.xml`**: o arquivo de configuração do JPA. Ele fica
>   obrigatoriamente em uma pasta `META-INF/` (a pasta padrão de "metadados"
>   dentro de um pacote Java); em um projeto Maven, colocá-lo em
>   `src/main/resources/META-INF/` faz com que ele vá parar no lugar certo durante o
>   build. É explicado em detalhe na seção 2.
> - ***Persistence Unit*** ("unidade de persistência"): um conjunto *nomeado* de
>   configurações de persistência (quais entidades, qual banco, quais
>   propriedades). Também é explicada na seção 2.

## 1. Isolamento de Responsabilidades

```
 JSF ObraBean (Passo 4)     JAX-RS ObraResource (Passo 5)
              \                 /
               ▼               ▼
        ObraService — EJB @Stateless (Passo 3)   ← regras de negócio + transação
                       │
                       ▼
        ObraDao — CDI @ApplicationScoped (Passo 2)  ← como se lê e grava
                       │
                       ▼
        EntityManager → DataSource (JNDI) → H2 / PostgreSQL
```

> 📘 **Lendo o diagrama, de cima para baixo** (várias siglas aparecem aqui pela
> primeira vez):
>
> - **JSF** (*Jakarta Faces*, antigo *JavaServer Faces*): o framework de telas web
>   do Jakarta EE, baseado em componentes. O `ObraBean` é um ***bean***: no mundo
>   Java EE, "bean" é um objeto cuja criação e ciclo de vida são gerenciados pelo
>   contêiner (e não por um `new` escrito por você). Este, em particular, faz a
>   "ponte" entre a página e o resto do sistema.
> - **JAX-RS** (*Jakarta RESTful Web Services*, antigo *Java API for RESTful Web
>   Services*): a API padrão para criar APIs **REST** (*Representational State
>   Transfer*, o estilo de API HTTP com URLs como `/obras/1` e verbos
>   `GET`/`POST`/`PUT`/`DELETE`). O `ObraResource` é o equivalente a um
>   `@RestController` do Spring.
> - **Contêiner**: o ambiente de execução, dentro do servidor de aplicação, que
>   cria, injeta, gerencia e destrói os seus objetos e oferece serviços prontos
>   (transações, segurança etc.).
> - **EJB** (*Enterprise JavaBeans*): componentes de negócio gerenciados por um
>   "contêiner EJB", que oferece automaticamente transações, segurança e controle
>   de concorrência. `@Stateless` ("sem estado") marca um EJB que não guarda dados
>   de nenhum cliente entre uma chamada e outra; por isso, qualquer instância pode
>   atender qualquer requisição. É o assunto do Passo 3.
> - **Transação**: um grupo de operações no banco tratado como "tudo ou nada":
>   ou todas são confirmadas (*commit*), ou todas são desfeitas (*rollback*).
> - **CDI** (*Contexts and Dependency Injection*, "contextos e injeção de
>   dependência"): o mecanismo padrão do Jakarta EE para criar objetos e
>   injetá-los onde são necessários. É o equivalente ao contêiner de **IoC** do
>   Spring (*Inversion of Control*, "inversão de controle": é o framework, e não o
>   seu código, que cria e conecta os objetos). `@ApplicationScoped` é explicado
>   mais abaixo, no paralelo com o Spring.
> - **`EntityManager` → DataSource (JNDI)**: o `EntityManager` obtém conexões de
>   um **DataSource**, que o servidor localiza por um nome **JNDI**. Os dois termos
>   são explicados na seção 2.
> - **H2 / PostgreSQL**: dois bancos de dados relacionais. O H2 é escrito em Java
>   e pode rodar inteiramente em memória (ótimo para desenvolvimento e testes); o
>   PostgreSQL é um banco "de verdade", muito usado em produção.

Cada camada só conhece a camada imediatamente abaixo e tem **um único motivo
para mudar** (Single Responsibility Principle — **SRP**, o "Princípio da
Responsabilidade Única"; veja o quadro logo após a tabela):

| Se mudar… | Só é preciso alterar… |
|---|---|
| uma query (otimizar, acrescentar `JOIN FETCH`) | o DAO |
| uma regra de negócio ("obra concluída não recebe relatórios") | o Service |
| a tela ou o formato JSON | o Bean JSF / o Resource REST |
| o banco de dados (H2 → PostgreSQL) | o DataSource no servidor (nem o DAO muda) |

> 📘 **O que é o SRP?** É o "S" do **SOLID**, um conjunto de cinco princípios de
> design orientado a objetos popularizado por Robert C. Martin ("Uncle Bob"). A
> formulação clássica é: *"uma classe deve ter um, e apenas um, motivo para
> mudar"*. O título desta seção, "Isolamento de Responsabilidades", é a mesma
> ideia aplicada em escala maior, entre camadas; em inglês ela costuma ser
> chamada de ***separation of concerns*** ("separação de responsabilidades" ou
> "de interesses"): cada parte do sistema cuida de um assunto só. (Veja também
> [`solid.md`](solid.md).)
>
> **Outros termos da tabela:**
> - **`JOIN FETCH`**: um recurso do JPQL para trazer uma entidade relacionada na
>   mesma consulta (detalhado na seção 4).
> - **Service** ("serviço"): a camada onde ficam as regras de negócio, como o
>   `@Service` do Spring.
> - **JSON** (*JavaScript Object Notation*): o formato de texto, como
>   `{"id": 1, "nome": "..."}`, que as APIs REST usam para trocar dados.
> - **Resource REST**: a classe JAX-RS que expõe URLs HTTP (o `ObraResource`).

Na prática, isso resulta em:
- **Reutilização:** o mesmo `ObraDao` atende a tela JSF e a API REST, via Service.
- **Testabilidade:** o Service pode ser testado com DAOs *mock*, sem banco de dados.
  (Um *mock*, "imitação", é um objeto falso que se passa pelo DAO real durante o
  teste: você programa as respostas, por exemplo "quando chamarem
  `buscarPorId(1L)`, devolva esta obra", e depois verifica se o Service o chamou
  corretamente. Bibliotecas como o Mockito criam mocks automaticamente.)
- **Um só lugar para otimizar:** todo o JPQL sobre obras está no `ObraDao`. Um
  problema de performance é procurado em um arquivo, não em 30.
  (**JPQL**, *Jakarta Persistence Query Language*, é a linguagem de consultas do
  JPA: parecida com SQL, mas escrita sobre classes e atributos Java; é detalhada
  na seção 4. *Performance* = desempenho.)

### `GenericDao<T, ID>`: reutilização com generics

O CRUD (`inserir`, `atualizar`, `removerPorId`, `buscarPorId`, `listarTodos`) é
igual para qualquer entidade. Ele é escrito uma vez em uma classe abstrata
parametrizada; cada DAO concreto passa `Obra.class` no construtor e acrescenta
apenas as queries do seu domínio. `ObraDao` sobrescreve `listarTodos()` para impor
ordenação — polimorfismo clássico.

> 📘 **O que são *generics*?** *Generics* ("genéricos") são os parâmetros de tipo
> do Java, aquele `<...>` que você já usa em `List<String>`. Em
> `GenericDao<T, ID>`, `T` e `ID` são "variáveis de tipo": quando `ObraDao`
> declara `extends GenericDao<Obra, Long>`, o compilador passa a tratar `T` como
> `Obra` e `ID` como `Long`, e o método `buscarPorId(ID id)` passa a valer, na
> prática, como `buscarPorId(Long id)` retornando `Optional<Obra>`. O código é
> escrito uma vez e serve, com checagem de tipos pelo compilador, para qualquer
> entidade.
>
> **Por que passar `Obra.class` no construtor, se o tipo já está em
> `<Obra, Long>`?** Por causa do *type erasure* ("apagamento de tipos"): os
> generics do Java existem só em tempo de compilação. Em tempo de execução,
> dentro de `GenericDao`, não há como perguntar "quem é `T`?". Só que o
> `EntityManager` precisa saber a classe concreta (`em.find(Obra.class, id)`),
> então ela é recebida no construtor e guardada no campo `classeEntidade`.
>
> **Classe abstrata**: uma classe que não pode ser instanciada diretamente
> (`new GenericDao<>(Obra.class)` não compila); ela existe para ser estendida.
>
> **Domínio**: o assunto de negócio da aplicação (aqui, obras e relatórios de
> segurança). "Queries do seu domínio" são as consultas que só fazem sentido para
> aquela entidade.
>
> **O que são *override* e polimorfismo?** *Override* ("sobrescrever") é
> redefinir, na subclasse, um método herdado; repare na anotação `@Override` em
> `ObraDao.listarTodos()`. **Polimorfismo** ("muitas formas") é a consequência
> disso: quando alguém chama `listarTodos()`, o Java executa a versão do objeto
> *real*, e não a do tipo da variável. Mesmo que a variável seja declarada como
> `GenericDao<Obra, Long>`, se o objeto for um `ObraDao`, roda a versão com
> `ORDER BY o.nome`.

### `@Transactional(MANDATORY)`: quem decide a transação é o negócio

Os DAOs **não abrem transações**. `MANDATORY` faz com que uma chamada sem
transação ativa falhe com `TransactionalException`. Validamos isso em um WildFly
real: chamado a partir de um EJB, funciona; chamado diretamente de um servlet, é
bloqueado.

> 📘 **Como o `@Transactional` funciona por baixo?** Esta anotação é a do
> **Jakarta Transactions** (`jakarta.transaction.Transactional`), não a do Spring.
> Ela é implementada por um ***interceptor*** CDI: um código que o contêiner
> executa automaticamente "em volta" de cada chamada de método (antes e depois),
> sem que você precise escrevê-lo. É o mesmo conceito dos *aspects* do Spring
> (**AOP**, *Aspect-Oriented Programming*, "programação orientada a aspectos").
> O atributo `TxType` diz ao interceptor o que fazer com a transação; `MANDATORY`
> ("obrigatório") significa: "eu não abro transação nenhuma, mas exijo que já
> exista uma; se não existir, lanço `TransactionalException`" (que traz uma
> `TransactionRequiredException` como causa). Como a anotação está na classe
> `GenericDao`, ela vale para os métodos dela e, por herança, para os das
> subclasses.
>
> - **WildFly**: servidor de aplicação Jakarta EE de código aberto mantido pela
>   Red Hat (o antigo *JBoss Application Server*). É onde este projeto roda.
> - **Servlet**: a classe Java mais básica para tratar requisições HTTP; JSF e
>   JAX-RS são construídos sobre servlets. Um servlet comum não abre transação,
>   por isso a chamada direta ao DAO foi bloqueada.

Por quê? Porque "registrar relatório + atualizar status da obra" precisa ser
**uma** transação. Se cada DAO abrisse a sua, teríamos duas transações
independentes e um estado inconsistente se a segunda falhasse. A fronteira
transacional pertence ao caso de uso, ou seja, ao Service (Passo 3).
(*Fronteira transacional* é o ponto do código onde a transação começa e termina;
*caso de uso* é uma ação completa do ponto de vista do usuário, como "registrar
um relatório de segurança".)

### DAO vs Repository

Termos muitas vezes usados como sinônimos. Formalmente:
- **DAO** (Core J2EE Patterns): abstração da *persistência*, orientada a
  tabelas/operações CRUD.
  (*Core J2EE Patterns* é um catálogo de padrões de projeto publicado pela Sun no
  início dos anos 2000 para o **J2EE**, *Java 2 Platform, Enterprise Edition*, o
  nome antigo do Java EE/Jakarta EE. *Persistência* = gravar dados de forma
  durável, normalmente em um banco, para que sobrevivam ao fim do programa.)
- **Repository** (DDD, Eric Evans): abstração de uma *coleção de agregados* do
  domínio ("me dê as obras em andamento"), sem expor detalhes de persistência.

> 📘 **DDD e agregado.** **DDD** (*Domain-Driven Design*, "projeto orientado ao
> domínio") é uma abordagem, apresentada por Eric Evans no livro de 2003 com esse
> nome, que coloca o *domínio* (as regras e a linguagem do negócio) no centro do
> design. Um **agregado** (*aggregate*) é um grupo de objetos tratado como uma
> unidade para fins de alteração, com uma entidade "raiz" (*aggregate root*) como
> única porta de entrada. Exemplo clássico: um `Pedido` com seus itens; ninguém
> de fora altera um item diretamente, sempre passa pelo `Pedido`, que garante as
> regras (como "o total não pode ser negativo"). Em DDD existe um **Repository**
> ("repositório") por raiz de agregado, e ele se comporta como uma coleção em
> memória ("adicione esta obra", "me dê as obras em andamento"), sem mencionar
> tabelas ou SQL.

O nosso `ObraDao` é um híbrido pragmático, como quase todos os projetos reais.
Saber a distinção é uma boa resposta de entrevista.

> **Paralelo Spring Boot:** `@ApplicationScoped` ≈ `@Repository` (ambos singletons).
> A diferença é que o `@Repository` do Spring traduz as exceções JPA para a
> hierarquia `DataAccessException`. No Jakarta EE você recebe as `PersistenceException`
> originais, e o contêiner EJB as encapsula em `EJBException` (Passo 3).
> Não é preciso `beans.xml`: desde o CDI 1.1 (Java EE 7) as classes com anotação de
> escopo são descobertas automaticamente, como no *component scan* do Spring.

> 📘 **Destrinchando o paralelo acima:**
>
> - **Escopo** (*scope*): define *quanto tempo vive* uma instância de bean e
>   *quem a compartilha*. No CDI há, por exemplo, `@RequestScoped` (uma instância
>   por requisição HTTP), `@SessionScoped` (uma por sessão de usuário) e
>   `@ApplicationScoped` (uma só para a aplicação inteira, enquanto ela estiver no
>   ar). As "anotações de escopo" são essas.
> - ***Singleton***: uma classe da qual existe uma única instância compartilhada
>   por toda a aplicação. No Spring, todo bean é singleton por padrão; no CDI, o
>   efeito equivalente é obtido com `@ApplicationScoped`.
> - **`DataAccessException`**: a raiz da hierarquia de exceções de acesso a dados
>   do Spring. O Spring "traduz" exceções específicas de cada tecnologia (JPA,
>   JDBC, Hibernate) para subclasses dela, como `DataIntegrityViolationException`,
>   para que o seu código não dependa da tecnologia de persistência. Ele faz isso
>   envolvendo as classes `@Repository` em um proxy.
> - **`PersistenceException`**: a exceção base do JPA
>   (`jakarta.persistence.PersistenceException`); `NoResultException`,
>   `OptimisticLockException` etc. são subclasses dela.
> - **`EJBException`**: quando um método de EJB lança uma exceção não verificada
>   (*unchecked*, filha de `RuntimeException`) que não foi marcada como exceção de
>   aplicação, o contêiner EJB marca a transação para *rollback* e entrega a
>   exceção a quem chamou "embrulhada" em uma `EJBException`; a original fica
>   acessível via `getCause()`. Detalhes no Passo 3.
> - **`beans.xml`**: arquivo de configuração do CDI (em `WEB-INF/` ou
>   `META-INF/`). No Java EE 6 (CDI 1.0) ele era obrigatório para "ligar" o CDI
>   em um módulo; hoje serve para ajustar o *modo de descoberta* de beans e ativar
>   recursos opcionais, como interceptors. A descoberta sem `beans.xml` (o
>   *implicit bean archive*) existe desde o CDI 1.1 (Java EE 7); o que o CDI 4
>   (Jakarta EE 10) mudou foi o significado de um
>   `beans.xml` *vazio*, que antes fazia todas as classes virarem beans e agora
>   descobre só as classes anotadas (por exemplo, com uma anotação de escopo).
> - ***Component scan*** ("varredura de componentes"): a varredura de pacotes
>   que o Spring faz na inicialização, procurando classes anotadas com
>   `@Component`, `@Service`, `@Repository` etc. para registrá-las como beans. A
>   "descoberta de beans" do CDI é o equivalente.
> - **Jakarta EE**: o nome atual do Java EE, desde que a plataforma foi
>   transferida da Oracle para a Eclipse Foundation (2017–2018). A partir do
>   Jakarta EE 9, os pacotes mudaram de `javax.*` para `jakarta.*`.

## 2. `persistence.xml`: a Persistence Unit

É o equivalente ao bloco `spring.datasource.*` + `spring.jpa.*` do
`application.properties`.

| `persistence.xml` | Spring Boot |
|---|---|
| `<persistence-unit name="gestaoObrasPU">` | Há só uma, criada pela autoconfiguração |
| `transaction-type="JTA"` | `JpaTransactionManager` (transações locais, *resource-local*) |
| `<jta-data-source>java:comp/DefaultDataSource` | `spring.datasource.url / username / password` |
| `schema-generation.database.action=drop-and-create` | `spring.jpa.hibernate.ddl-auto=create-drop` |
| `sql-load-script-source` | `data.sql` |
| `hibernate.show_sql` | `spring.jpa.show-sql` |

> 📘 **O que é uma *Persistence Unit*?** É um conjunto nomeado (aqui,
> `gestaoObrasPU`) que responde a três perguntas: *quais entidades* o JPA deve
> gerenciar (as tags `<class>`), *qual banco* usar (o DataSource) e *com quais
> propriedades* (geração de tabelas, log de SQL...). Para cada Persistence Unit o
> servidor cria uma `EntityManagerFactory` (explicada na seção 3). Uma aplicação
> pode ter várias, por exemplo uma por banco de dados, e o
> `@PersistenceContext(unitName = "gestaoObrasPU")` do `GenericDao` diz qual usar.
>
> **Outros termos da tabela:**
> - **JTA** e ***resource-local***: os dois tipos de transação do JPA, explicados
>   logo abaixo. `JpaTransactionManager` é a classe do Spring que controla
>   transações "locais" de uma única `EntityManagerFactory`.
> - **`java:comp/DefaultDataSource`**: um nome JNDI (explicado mais abaixo) que
>   todo servidor Java EE 7+ / Jakarta EE é obrigado a oferecer.
> - **DDL e `ddl-auto`**: **DDL** (*Data Definition Language*, "linguagem de
>   definição de dados") é a parte do SQL que define a *estrutura* do banco
>   (`CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`), em oposição à **DML** (*Data
>   Manipulation Language*: `INSERT`, `UPDATE`, `DELETE`). A propriedade
>   `spring.jpa.hibernate.ddl-auto` diz ao Hibernate o que fazer com a estrutura
>   ao subir a aplicação: `none` (nada), `validate` (só confere), `update` (tenta
>   ajustar), `create` (recria) ou `create-drop` (cria ao subir e apaga ao
>   desligar). O `drop-and-create` do JPA apaga e recria as tabelas toda vez que a
>   aplicação sobe.
> - **Hibernate**: a implementação de JPA (o *provider*, "provedor") mais usada;
>   é a que vem no WildFly e no Spring Boot. O JPA é a especificação (interfaces e
>   regras); o Hibernate é o código que de fato gera e executa o SQL. Por isso as
>   propriedades `hibernate.*` são específicas dele.
> - **`sql-load-script-source` / `data.sql`**: um script SQL executado na
>   inicialização para popular o banco com dados de exemplo.
> - **`show_sql`**: imprime no log cada SQL gerado; ótimo para aprender o que o
>   JPA faz por baixo.

### JTA vs RESOURCE_LOCAL

- **JTA:** o servidor gerencia as transações (Jakarta Transactions). Uma transação
  pode abranger vários recursos (dois bancos de dados, uma fila JMS) com *two-phase
  commit*. O nosso código nunca chama `begin()`/`commit()`.
- **RESOURCE_LOCAL:** a aplicação gerencia a transação diretamente sobre a conexão
  JDBC (`em.getTransaction().begin()`). É o que o Spring Boot usa por padrão, e o
  que usamos nos testes fora do servidor.

> 📘 **Entendendo as siglas e termos:**
>
> - **JTA** (*Java Transaction API*, hoje chamada **Jakarta Transactions**): a
>   API padrão de transações do Java EE. Quem coordena a transação é um componente
>   do servidor chamado *transaction manager* ("gerenciador de transações"; no
>   WildFly, o Narayana), e não a sua aplicação.
> - **RESOURCE_LOCAL** ("local ao recurso"): a transação é a da própria conexão
>   com *um* banco (o "recurso"). A aplicação chama `begin()` e
>   `commit()`/`rollback()` explicitamente, ou o Spring faz isso por você quando
>   encontra um `@Transactional`.
> - **JDBC** (*Java Database Connectivity*): a API de baixo nível do Java para
>   falar com bancos relacionais (abrir conexão, executar SQL, ler os
>   resultados). O JPA/Hibernate usa JDBC por baixo.
> - **JMS** (*Java Message Service*, hoje **Jakarta Messaging**): a API padrão
>   para enviar e receber mensagens assíncronas. Uma *fila* (*queue*) JMS é uma
>   fila de mensagens oferecida pelo servidor, na qual um sistema deposita
>   mensagens para outro processar depois.
> - ***Two-phase commit*** (**2PC**, "confirmação em duas fases"): o protocolo
>   que permite que uma transação envolvendo *vários* recursos seja "tudo ou nada"
>   em todos eles. **Fase 1** (*prepare*, "preparar"): o coordenador pergunta a
>   cada recurso "você consegue confirmar?", e cada um responde "sim" (e se
>   compromete a conseguir) ou "não". **Fase 2** (*commit*): só se *todos*
>   responderam "sim", o coordenador manda todos confirmarem; se algum disse
>   "não", manda todos desfazerem. Analogia: um casamento, em que o juiz só declara
>   o casal casado depois que *os dois* disseram "sim". Para isso, os recursos
>   normalmente precisam suportar o padrão **XA** (o padrão do X/Open para
>   transações distribuídas), por isso existem drivers e DataSources "XA".

### DataSource por JNDI: a configuração sai do código

O WAR não sabe a URL, o usuário nem a senha do banco de dados. Ele pede ao
servidor um recurso pelo nome JNDI. `java:comp/DefaultDataSource` é obrigatório
em qualquer servidor Jakarta EE (no WildFly e no Payara aponta para um H2 em
memória), por isso a aplicação sobe sem configuração nenhuma.

> 📘 **O que são WAR, DataSource e JNDI?**
>
> - **WAR** (*Web Application Archive*): o arquivo `.war` (um ZIP com classes,
>   bibliotecas e páginas) que você entrega ao servidor de aplicação para
>   instalar a aplicação. É diferente do JAR executável do Spring Boot, que já
>   traz o servidor embutido; o WAR depende de um servidor externo, como o WildFly.
> - **DataSource** (`javax.sql.DataSource`, "fonte de dados"): um objeto que sabe
>   *entregar conexões* com um banco. Você chama `getConnection()` e recebe uma
>   conexão pronta, sem saber URL, usuário ou senha. Em servidores, ele normalmente
>   é apoiado por um pool de conexões (explicado abaixo).
> - **JNDI** (*Java Naming and Directory Interface*, "interface Java de nomes e
>   diretórios"): o serviço de "lista telefônica" do servidor de aplicação.
>   Recursos (DataSources, filas JMS, EJBs...) são registrados sob um nome, como
>   `java:jboss/datasources/GestaoObrasDS`, e a aplicação os procura por esse
>   nome. Assim, a aplicação conhece só o *nome*; o *conteúdo* (qual banco, qual
>   senha) é configurado pelo administrador no servidor.
> - **Payara**: outro servidor de aplicação Jakarta EE, derivado do GlassFish.
> - **H2 em memória**: o banco existe só na memória RAM e é perdido quando o
>   servidor desliga.

Para migrar para o PostgreSQL, **não se mexe no código**: o administrador cria o
DataSource no servidor e basta trocar o nome JNDI. Exemplo no WildFly
(`jboss-cli.sh`):

```bash
deploy postgresql-42.7.4.jar
data-source add --name=GestaoObrasDS \
    --jndi-name=java:jboss/datasources/GestaoObrasDS \
    --driver-name=postgresql-42.7.4.jar \
    --connection-url=jdbc:postgresql://localhost:5432/gestao_obras \
    --user-name=obras --password=********
```

```xml
<jta-data-source>java:jboss/datasources/GestaoObrasDS</jta-data-source>
```

> 📘 **O que está acontecendo nesses comandos?** O `jboss-cli.sh` é a **CLI**
> (*Command-Line Interface*, "interface de linha de comando") de administração do
> WildFly: um terminal onde você digita comandos para configurar o servidor.
>
> 1. `deploy` faz o ***deploy*** ("implantação", ou seja, instalar e ativar algo
>    no servidor) do JAR do **driver** JDBC do PostgreSQL. O driver é a biblioteca
>    que sabe "conversar" com aquele banco específico.
> 2. `data-source add` cria o DataSource, usando esse driver, e o registra no nome
>    JNDI `java:jboss/datasources/GestaoObrasDS`.
> 3. No `persistence.xml`, basta apontar a `<jta-data-source>` para esse nome.

O *pool* de conexões também é do servidor (no Spring Boot é o HikariCP, embutido
na aplicação). Benefício corporativo: o mesmo WAR é promovido de DEV → QA → PROD
sem ser recompilado; cada ambiente tem o seu próprio DataSource, e as credenciais nunca
passam pelo repositório Git.

> 📘 **O que é um *pool* de conexões?** Abrir uma conexão com o banco é uma
> operação relativamente lenta (rede, autenticação, alocação de recursos no
> banco). Um *pool* ("reservatório") é um conjunto de conexões já abertas que são
> *emprestadas* a quem precisa e *devolvidas* ao terminar, como os carrinhos de
> supermercado: você não fabrica um carrinho ao entrar; pega um da fila e devolve
> na saída. Quando o seu código "fecha" uma conexão obtida de um pool, na verdade
> ela volta para o pool. O tamanho do pool (mínimo e máximo de conexões) é um
> ajuste importante de desempenho.
>
> - **HikariCP**: a biblioteca de pool de conexões padrão do Spring Boot (desde a
>   versão 2), conhecida por ser leve e rápida. No WildFly, o pool é implementado
>   e configurado pelo próprio servidor.
> - **DEV → QA → PROD**: os ambientes por onde uma versão passa: **DEV**
>   (*development*, desenvolvimento), **QA** (*Quality Assurance*, "garantia de
>   qualidade", onde acontecem os testes e a homologação) e **PROD** (produção,
>   onde estão os usuários reais). "Promover" o WAR é levar o mesmo arquivo de um
>   ambiente para o próximo.

> ⚠️ `drop-and-create` e o script de dados iniciais servem só para
> desenvolvimento. Em produção se usa `none` e um gerenciador de migrações
> (Flyway/Liquibase), assim como no Spring Boot.

> 📘 **O que é uma migração de banco?** Uma *migração* é um script versionado que
> leva a estrutura do banco de uma versão para a próxima (por exemplo,
> `V1__cria_tabela_obra.sql`, `V2__adiciona_coluna_localizacao.sql`). **Flyway** e
> **Liquibase** são ferramentas que aplicam essas migrações em ordem e registram,
> em uma tabela de controle no próprio banco, quais já foram executadas. Assim,
> cada ambiente (DEV, QA, PROD) chega ao mesmo esquema de forma previsível e sem
> apagar dados, algo que `drop-and-create` jamais faria.

### Detalhe: `hibernate.hbm2ddl.charset_name`

O Hibernate lê o `dados-iniciais.sql` com o charset padrão da JVM. No
Windows com Java 17 esse charset é `Cp1252`, e "Edifício" ficaria corrompido.
Fixamos `UTF-8` explicitamente. (A partir do Java 18 o default já é UTF-8.)

> 📘 **Charset, UTF-8, Cp1252 e JVM.**
>
> - **Charset** (*character set*, "conjunto de caracteres", também chamado de
>   *encoding* ou codificação): a tabela que diz como transformar caracteres em
>   bytes e vice-versa. Um arquivo de texto é só uma sequência de bytes; para lê-lo
>   corretamente, é preciso usar o mesmo charset com que ele foi gravado.
> - **UTF-8**: a codificação do padrão Unicode mais usada hoje. Representa
>   qualquer caractere de qualquer idioma usando de 1 a 4 bytes (letras sem
>   acento usam 1 byte; o `í` usa 2).
> - **Cp1252** (*Code Page 1252*, também chamado *Windows-1252*): uma codificação
>   antiga do Windows para idiomas da Europa Ocidental, com exatamente 1 byte por
>   caractere.
> - **O problema na prática:** o `í` de "Edifício" foi gravado em UTF-8 como dois
>   bytes (`C3 AD`). Lidos como Cp1252, eles viram *dois* caracteres (um `Ã` e um
>   hífen invisível), e o texto chega ao banco como algo parecido com
>   "EdifÃcio".
> - **JVM** (*Java Virtual Machine*, "máquina virtual Java"): o programa que
>   executa o seu código Java compilado. O "charset padrão da JVM" é o que ela usa
>   quando o código não especifica nenhum. Até o Java 17 ele dependia do sistema
>   operacional; o Java 18 (JEP 400) o fixou em UTF-8. **Default** = padrão.

## 3. Como funciona o `EntityManager`

### Três peças

| Peça | O que é | Quantas | Thread-safe? |
|---|---|---|---|
| `EntityManagerFactory` | Objeto pesado: metadados, pool, caches | 1 por Persistence Unit | Sim |
| `EntityManager` | Sessão de trabalho com o banco | 1 por transação | **Não** |
| Persistence Context | O conjunto de entidades *managed* dentro do `EntityManager` | 1 por `EntityManager` | — |

> 📘 **Termos da tabela:**
>
> - **`EntityManagerFactory`** ("fábrica de EntityManagers"): criada uma única
>   vez quando a aplicação sobe. Ela lê e guarda os **metadados** (as informações
>   de mapeamento: qual classe vai em qual tabela, qual atributo em qual coluna),
>   se liga ao pool de conexões e mantém caches compartilhados. É cara de criar,
>   por isso existe uma só por Persistence Unit. No Hibernate, ela corresponde à
>   `SessionFactory`.
> - **`EntityManager`**: barato, criado e descartado o tempo todo; é a "sessão de
>   trabalho" de uma transação (no Hibernate, corresponde à `Session`).
> - **Persistence Context** ("contexto de persistência"): o conjunto de entidades
>   que o `EntityManager` está acompanhando. É explicado em detalhe logo abaixo.
> - ***Managed*** ("gerenciada"): uma entidade que está dentro de um Persistence
>   Context. O JPA vigia as alterações dela e as grava automaticamente.
> - ***Thread-safe*** ("seguro para threads"): uma *thread* é uma linha de
>   execução independente dentro do programa; um servidor atende várias
>   requisições ao mesmo tempo, normalmente cada uma em sua thread. Um objeto é
>   thread-safe quando pode ser usado por várias threads *simultaneamente* sem
>   corromper o seu estado interno. O `EntityManager` não é: ele guarda o estado
>   (entidades carregadas, alterações pendentes) de *uma* unidade de trabalho, e
>   misturar duas requisições nele daria resultados imprevisíveis.

### O que é injetado com `@PersistenceContext`

O `EntityManager` real **não é thread-safe**. Mas o que o contêiner injeta é um
**proxy**: a cada chamada, o proxy procura o `EntityManager` associado à
transação JTA atual (ou o cria). Por isso o DAO pode ser um singleton
(`@ApplicationScoped`) compartilhado por todas as requisições: cada transação recebe o
seu próprio Persistence Context. Quando a transação termina, o Persistence
Context fecha e as entidades ficam *detached* (aquele estado do Passo 1).

> 📘 **O que é um *proxy*?** Um *proxy* ("procurador", "intermediário") é um
> objeto que se passa por outro: ele implementa a mesma interface
> (`EntityManager`), mas, ao receber uma chamada, não faz o trabalho sozinho;
> decide para quem repassar. Aqui, a cada chamada (`em.find(...)`), ele verifica
> qual é a transação atual daquela thread e repassa a chamada para o
> `EntityManager` *daquela* transação. Analogia: o número de telefone de uma
> central de atendimento. É sempre o mesmo número, mas cada ligação cai em um
> atendente diferente.
>
> - **`@PersistenceContext`**: a anotação que pede ao contêiner para injetar um
>   `EntityManager` (na verdade, esse proxy) no campo.
> - **Requisição**: cada pedido HTTP que chega ao servidor (abrir uma página,
>   chamar uma URL da API).
> - ***Detached*** ("desanexada"): uma entidade que já foi *managed*, mas cujo
>   Persistence Context foi fechado. Ela continua sendo um objeto Java normal, com
>   os dados que tinha, só que ninguém mais vigia as alterações dela; para
>   gravá-las, é preciso fazer `merge` (veja a tabela de operações abaixo).

> **Paralelo Spring Boot:** é exatamente o mesmo mecanismo. O Spring injeta um
> proxy (`SharedEntityManagerCreator`) ligado à transação do
> `@Transactional` atual.

### O Persistence Context faz três coisas

1. **Identity map:** dentro da mesma transação, o mesmo ID sempre retorna a
   mesma instância Java (`em.find(Obra.class, 1L) == em.find(Obra.class, 1L)`).
2. **Cache de 1º nível:** o segundo `find` do mesmo ID não vai ao banco de dados.
3. **Unit of Work + dirty checking:** no fim da transação (*flush*), compara
   cada entidade com o *snapshot* que tirou quando a carregou e gera os
   `UPDATE` necessários.

> 📘 **Destrinchando cada termo:**
>
> - ***Identity map*** ("mapa de identidade"): um padrão de projeto catalogado
>   por Martin Fowler no livro *Patterns of Enterprise Application Architecture*
>   (2002). Internamente, o contexto mantém algo como um mapa
>   "(classe, ID) → objeto". Por isso, dentro da mesma transação, dois `find` do
>   mesmo ID devolvem *o mesmo objeto* (`==` dá `true`), e você nunca tem duas
>   cópias divergentes da mesma linha.
> - **Cache de 1º nível**: consequência direta do identity map. Se a entidade já
>   está no mapa, o `find` a devolve sem ir ao banco. Chama-se "1º nível" porque é
>   o mais próximo e de vida mais curta (pertence a um `EntityManager` e dura uma
>   transação). Existe também um cache de **2º nível**, opcional, compartilhado
>   entre todos os `EntityManager`s da mesma `EntityManagerFactory`.
> - ***Unit of Work*** ("unidade de trabalho"): outro padrão do mesmo livro de
>   Fowler. Em vez de executar um SQL a cada `setNome(...)`, o contexto *anota*
>   tudo o que mudou durante a transação e grava tudo junto no final.
> - ***Snapshot*** ("fotografia"): ao carregar uma entidade, o provider
>   (Hibernate) guarda uma cópia dos valores originais dela.
> - ***Dirty checking*** ("verificação de sujeira"): em inglês, um objeto
>   *dirty* ("sujo") é um objeto alterado e ainda não gravado. No *flush*, o
>   Hibernate compara, campo a campo, cada entidade managed com o seu snapshot; as
>   que mudaram geram um `UPDATE`.
> - ***Flush*** ("descarregar"): o momento em que o contexto *envia* ao banco os
>   SQLs pendentes (`INSERT`, `UPDATE`, `DELETE`). Atenção: **flush não é
>   commit**. Depois do flush, os comandos já foram executados no banco, mas ainda
>   dentro da transação; se ela sofrer *rollback*, tudo é desfeito. O flush
>   acontece automaticamente antes do commit, antes de certas queries (veja
>   `FlushModeType.AUTO` abaixo) ou quando você chama `em.flush()`.

Vimos isso no teste: um `persist` seguido de `setLocalizacao()` na mesma
transação produziu

```sql
insert into obra (...) values (...)
update obra set ..., versao=? where id=? and versao=?
```

sem nenhum `save()` explícito. O `where ... and versao=?` é o `@Version` do
Passo 1 em ação.

> 📘 **Por que um `INSERT` e depois um `UPDATE`?** Porque, com IDs gerados por
> `IDENTITY` (veja a tabela abaixo), o `INSERT` é executado já no `persist`; a
> alteração feita depois, com `setLocalizacao()`, foi detectada pelo dirty
> checking no flush e virou um `UPDATE`.
>
> **`@Version` e *optimistic locking*.** `@Version` marca um campo numérico que o
> JPA incrementa a cada `UPDATE`. É o mecanismo de *optimistic locking* ("bloqueio
> otimista"): em vez de travar a linha enquanto o usuário edita, o JPA só confere,
> na hora de gravar, se ninguém alterou a linha nesse meio-tempo; o
> `where ... and versao=?` faz exatamente isso. Se o `UPDATE` afetar 0 linhas
> (alguém já mudou a versão), o JPA lança `OptimisticLockException` em vez de
> sobrescrever silenciosamente o trabalho do outro usuário.

### Operações principais

| Método | Transição | Nota |
|---|---|---|
| `persist(e)` | NEW → MANAGED | Com `IDENTITY`, o `INSERT` é imediato (precisa do ID). |
| `merge(e)` | DETACHED → MANAGED | **Retorna outra instância**; a que foi passada continua detached. |
| `remove(e)` | MANAGED → REMOVED | Só aceita entidades managed (por isso fazemos `find` primeiro). |
| `find(Classe, id)` | → MANAGED | `null` se não existir. Usa o cache de 1º nível. |
| `getReference(Classe, id)` | → proxy | Não faz `SELECT`; útil para associar FKs sem carregar. |
| `createQuery(jpql, Classe)` | — | `TypedQuery`; antes de executar, faz *flush* automático (`FlushModeType.AUTO`). |
| `flush()` / `clear()` / `detach(e)` | — | Forçar o SQL agora / esvaziar o contexto / soltar uma entidade. |

> 📘 **Os estados de uma entidade e os termos da tabela:**
>
> - **NEW** (também chamado de *transient*, "transitório"): objeto criado com
>   `new`, que o JPA ainda não conhece.
> - **MANAGED**: dentro do Persistence Context, vigiado pelo dirty checking.
> - **DETACHED**: já foi managed, mas o contexto fechou (ou você chamou
>   `detach`/`clear`).
> - **REMOVED**: marcado para exclusão; o `DELETE` sai no próximo flush.
> - **`persist`** ("persistir"): torna um objeto novo managed (vai virar um
>   `INSERT`). **`merge`** ("mesclar"): copia o estado de um objeto detached para
>   uma instância managed (carregando-a do banco, se ela ainda não estiver no
>   contexto) e devolve essa instância. **`remove`** ("remover"): marca para
>   exclusão. **`find`** ("encontrar"): busca pela chave primária.
>   **`getReference`** ("obter referência"): devolve, em geral, um proxy "oco",
>   com apenas o ID preenchido; o banco só é consultado se você acessar outro
>   atributo.
> - **`IDENTITY`** (`GenerationType.IDENTITY`): estratégia em que o ID é gerado
>   pelo próprio banco (coluna de auto-incremento). Como o JPA só descobre o ID
>   depois de inserir, ele precisa executar o `INSERT` imediatamente, em vez de
>   esperar o flush.
> - **FK** (*Foreign Key*, "chave estrangeira"): coluna que aponta para a chave
>   primária de outra tabela, como `relatorio_seguranca.obra_id → obra.id`. Com
>   `getReference`, você pode fazer
>   `relatorio.setObra(em.getReference(Obra.class, 5L))` sem um `SELECT` só para
>   preencher essa coluna.
> - **`TypedQuery`**: uma query que já sabe o tipo do resultado
>   (`TypedQuery<Obra>`), dispensando *casts* (conversões de tipo como
>   `(Obra) resultado`).
> - **`FlushModeType.AUTO`**: o modo de flush padrão. Antes de executar uma
>   query, o provider garante que as alterações pendentes que possam afetar o
>   resultado sejam enviadas ao banco; senão, a query poderia "não ver" uma obra
>   que você acabou de alterar na mesma transação. (No outro modo, `COMMIT`, o
>   flush acontece, em geral, só no commit.)

### Armadilhas clássicas (perguntas de entrevista)

- **`getSingleResult()` lança `NoResultException`** se não houver resultados (e
  `NonUniqueResultException` se houver mais de um). Por isso `buscarPorId` usa
  `find()` + `Optional`. Um `COUNT` sempre retorna uma linha, por isso é seguro.
- **`merge()` retorna uma cópia.** Continuar alterando o objeto original depois
  do `merge` não grava nada.
- **Bulk `DELETE`/`UPDATE` (`executeUpdate`)** vai direto ao banco de dados:
  ignora o Persistence Context, os callbacks `@PreRemove` e o `@Version`.
  Entidades já carregadas ficam desatualizadas. Usamos em `removerPorObra`
  porque é 1 `DELETE` em vez de N, mas com consciência do custo.

> 📘 **Termos desta lista:**
>
> - **`NoResultException` / `NonUniqueResultException`**: exceções do JPA
>   (subclasses de `PersistenceException`) lançadas por `getSingleResult()`
>   quando a query retorna 0 linhas ou mais de 1. Curiosidade de entrevista: pela
>   especificação, essas duas *não* marcam a transação para rollback, ao
>   contrário da maioria das `PersistenceException`.
> - **`Optional`**: a classe do Java 8+ que representa "um valor que pode estar
>   ausente", forçando quem chama a tratar o caso vazio em vez de receber `null`.
> - **`COUNT`**: função de agregação do SQL/JPQL que conta linhas. Mesmo sem
>   registros, ela devolve uma linha, com o valor `0`.
> - ***Bulk*** ("em massa", "em lote") **update/delete**: um único comando
>   `UPDATE`/`DELETE` que afeta muitas linhas de uma vez, executado com
>   **`executeUpdate()`**, que retorna o número de linhas afetadas. Ele é traduzido
>   direto para SQL, sem carregar as entidades; por isso é rápido, mas "passa por
>   fora" do Persistence Context.
> - ***Callbacks*** ("chamadas de retorno"): métodos da entidade anotados com
>   `@PrePersist`, `@PreUpdate`, `@PreRemove`, `@PostLoad` etc., que o JPA chama
>   automaticamente em cada evento do ciclo de vida. Como o bulk delete não
>   carrega as entidades, não há em quem chamar o `@PreRemove`.
> - **1 vs N**: remover N relatórios um a um custaria N `DELETE`s (mais o
>   `SELECT` para carregá-los); o bulk faz 1.

## 4. JPQL, Criteria API e parâmetros

- **JPQL** consulta *entidades e atributos Java* (`Obra`, `o.nome`), não tabelas
  e colunas. O provider traduz para o SQL do dialeto do banco de dados — por isso
  o mesmo DAO funciona no H2 e no PostgreSQL.
  (O *provider* é a implementação do JPA, aqui o Hibernate. *Dialeto* é o
  "sotaque" de SQL de cada banco: funções de data, tipos de coluna e a forma de
  paginar variam; para limitar resultados, por exemplo, o MySQL usa `LIMIT 10`,
  enquanto o padrão SQL usa `FETCH FIRST 10 ROWS ONLY`. O Hibernate tem uma
  classe `Dialect` para cada banco.)
- **`r.obra.id`** navega até o ID da associação e é traduzido diretamente para
  a coluna FK, sem `JOIN`:
  ```sql
  select ... from relatorio_seguranca rs1_0 where rs1_0.obra_id=? order by rs1_0.data_inspecao desc
  ```
  (O `JOIN` do SQL junta as linhas de duas tabelas relacionadas; aqui ele não é
  necessário porque o ID da obra já está na própria tabela de relatórios.)
- **`JOIN FETCH`** contorna o LAZY só no caso de uso que precisa da obra, em uma
  única query:
  ```sql
  select rs1_0.*, o1_0.* from relatorio_seguranca rs1_0
  join obra o1_0 on o1_0.id = rs1_0.obra_id
  order by rs1_0.data_inspecao desc, rs1_0.id desc fetch first ? rows only
  ```
  `setMaxResults` com `JOIN FETCH` é seguro em um `@ManyToOne`. Em um `@OneToMany`, o
  Hibernate paginaria em memória (aviso `HHH90003004`, antigo `HHH000104`).
- **Criteria API** (usada em `GenericDao.listarTodos`) constrói queries com
  objetos Java em vez de Strings. É útil para queries dinâmicas (filtros
  opcionais) e é a base do `Specification` do Spring Data.
- **Parâmetros nomeados (`:termo`)** são enviados como *bind parameters* do
  `PreparedStatement`, nunca concatenados no SQL: **imunes a SQL Injection**.
  Nunca escreva `"... WHERE nome = '" + nome + "'"`.
- Em `existeComNome` escolhemos entre duas queries em Java, em vez de usar
  `(:id IS NULL OR o.id <> :id)`. Um parâmetro `null` nessa forma pode fazer o
  PostgreSQL falhar com *could not determine data type of parameter*.
  ("Não foi possível determinar o tipo de dado do parâmetro": com o valor `null`,
  nada indica se o parâmetro é número, texto ou data, e o PostgreSQL, que é rígido
  com tipos, recusa a query.)

> 📘 **LAZY, `JOIN FETCH`, `setMaxResults` e paginação.**
>
> - **LAZY** ("preguiçoso"): em `RelatorioSeguranca`, a associação com `Obra` está
>   mapeada como `@ManyToOne(fetch = FetchType.LAZY)`. Ao carregar um relatório, o
>   Hibernate coloca no campo `obra` um *proxy* e só busca a obra no banco quando
>   alguém acessa os dados dela, o que precisa acontecer enquanto o Persistence
>   Context está aberto; depois que ele fecha, o Hibernate lança
>   `LazyInitializationException`. (Atenção: o padrão da especificação para
>   `@ManyToOne` é EAGER, "ansioso", que carrega a associação na hora; o LAZY aqui
>   foi uma escolha explícita do Passo 1.)
> - **`JOIN FETCH`**: diz ao JPQL "faça o `JOIN` *e já preencha* a associação com
>   os dados", trazendo relatório e obra em uma única query.
> - **`@ManyToOne` / `@OneToMany`**: associações "muitos para um" (muitos
>   relatórios → uma obra) e "um para muitos" (uma obra → muitos relatórios).
> - **`setMaxResults(n)`**: limita a query a `n` linhas (no SQL vira
>   `fetch first ? rows only`, `LIMIT` etc., conforme o dialeto). Junto com
>   `setFirstResult`, é a base da **paginação**: dividir um resultado grande em
>   "páginas" (linhas 1–20, 21–40...).
> - **Por que é perigoso em `@OneToMany`?** Com `JOIN FETCH` de uma coleção, cada
>   obra aparece repetida em várias linhas do SQL (uma por relatório). Limitar as
>   *linhas* cortaria a coleção de uma obra pela metade. Então o Hibernate desiste
>   de limitar no banco: traz *tudo* e pagina em memória, emitindo o aviso
>   `HHH90003004`, o que pode derrubar a performance com tabelas grandes. Em
>   `@ManyToOne`, cada linha é um relatório com a sua única obra, então limitar no
>   banco é correto.
>
> **Criteria API e `Specification`.** Na *Criteria API* ("API de critérios"), em
> vez de escrever a consulta como texto, você a monta chamando métodos de um
> `CriteriaBuilder` (veja `GenericDao.listarTodos()`). Isso permite acrescentar
> filtros com `if`s, sem concatenar Strings. A **`Specification<T>`** do Spring
> Data JPA é uma interface que encapsula um "pedaço" de filtro construído com a
> Criteria API e permite combinar esses pedaços com `and`/`or`.
>
> **Bind parameters, `PreparedStatement` e SQL Injection.**
> - **`PreparedStatement`**: a interface JDBC para executar um SQL "preparado",
>   com marcadores `?` no lugar dos valores, como `select ... where nome = ?`. O
>   texto do SQL e os valores viajam *separados* até o banco.
> - ***Bind parameter*** ("parâmetro vinculado"): cada valor "amarrado" a um `?`.
>   O `:termo` do JPQL vira um `?` no SQL, e o valor é vinculado pelo
>   `setParameter("termo", ...)`.
> - **SQL Injection** ("injeção de SQL"): o ataque em que um texto digitado pelo
>   usuário passa a fazer parte do comando SQL. Com concatenação, se alguém
>   digitar `' OR '1'='1` no campo nome, o SQL vira
>   `... WHERE nome = '' OR '1'='1'`, que é sempre verdadeiro e devolve todas as
>   linhas (variações piores podem até apagar tabelas). Com bind parameter, esse
>   texto é tratado apenas como *valor* (o banco procura uma obra cujo nome seja
>   literalmente `' OR '1'='1`), nunca como código.
> - **Parâmetros nomeados vs posicionais**: os nomeados usam `:nome`; os
>   posicionais usam `?1`, `?2`. Os dois são bind parameters.

## 5. EntityManager vs Spring Data JPA

No Spring Boot você escreveria apenas:

```java
public interface ObraRepository extends JpaRepository<Obra, Long> {
    List<Obra> findAllByOrderByNome();
    List<Obra> findByStatusOrderByNome(StatusObra status);
    List<Obra> findByNomeContainingIgnoreCaseOrderByNome(String termo);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
```

e o Spring gera a implementação em runtime. **Mas essa implementação usa o
mesmo `EntityManager`.** O `SimpleJpaRepository` do Spring Data faz, por exemplo:

```java
public <S extends T> S save(S entity) {
    if (entityInformation.isNew(entity)) {
        em.persist(entity);
        return entity;
    }
    return em.merge(entity);
}
```

> 📘 **Spring Data JPA, `JpaRepository` e `SimpleJpaRepository`.**
> *Runtime* = tempo de execução (enquanto a aplicação roda), em oposição a tempo
> de compilação. O **Spring Data JPA** é o módulo do Spring que cria repositórios
> automaticamente sobre o JPA. **`JpaRepository<T, ID>`** é a interface que você
> estende (repare: a mesma ideia de generics do nosso `GenericDao<T, ID>`). Na
> inicialização, o Spring cria um *proxy* que implementa a sua interface: os
> métodos herdados (`save`, `findById`...) são delegados ao
> **`SimpleJpaRepository`**, a implementação padrão, e os métodos declarados por
> você têm a query *derivada do nome do método* (`findBy` + `Status` + `OrderBy` +
> `Nome` → `WHERE status = ? ORDER BY nome`). O `entityInformation.isNew(entity)`
> decide se a entidade é nova olhando, em geral, se o ID (ou o campo `@Version`,
> quando existe e não é de tipo primitivo) é `null`.

Ou seja: **o Spring Data JPA é um gerador de DAOs em cima do JPA.** O que aqui
escrevemos à mão é o que ele gera para você.

| Nosso DAO | Spring Data JPA |
|---|---|
| `inserir` / `atualizar` | `save()` (decide `persist` vs `merge` pelo ID) |
| `buscarPorId` | `findById()` → `Optional` |
| `removerPorId` | `deleteById()` |
| `listarPorStatus` | `findByStatusOrderByNome` (query derivada do nome do método) |
| JPQL em uma constante | `@Query("SELECT ...")` |
| `removerPorObra` (bulk) | `@Modifying @Query("DELETE ...")` |
| `GenericDao<T, ID>` | `JpaRepository<T, ID>` |

> 📘 `@Query` permite escrever o JPQL à mão em um método do repositório;
> `@Modifying` avisa ao Spring que essa query altera dados (é um `UPDATE` ou
> `DELETE`), para que ele a execute como bulk (com `executeUpdate()`), e não como
> consulta.

**Trade-offs:** o DAO manual é mais verboso, mas não tem "mágica" — o que você lê é o
que executa, e você tem controle total sobre cada query. O Spring Data elimina o
*boilerplate*, mas esconde conceitos (`persist` vs `merge`, *flush*) que
continuam existindo e causando bugs.
(*Trade-off* = uma troca: ganhar algo abrindo mão de outra coisa. *Boilerplate* =
código repetitivo, "de fôrma", que precisa ser escrito mas não carrega lógica de
negócio, como o CRUD repetido em cada DAO.)

> **Para mencionar na entrevista:** o **Jakarta Data** (Jakarta EE 11) traz
> repositórios declarativos no estilo Spring Data para a plataforma padrão.
> Em projetos Java EE legados você vai encontrar quase sempre o padrão DAO com
> `EntityManager`, como aqui.

> 📘 **Jakarta Data** é uma especificação nova (versão 1.0, incluída no Jakarta
> EE 11) em que você declara uma interface anotada com `@Repository` (do pacote
> `jakarta.data.repository`) e a implementação é gerada para você: a mesma ideia
> do Spring Data, agora como padrão da plataforma. *Declarativo* = você declara
> *o que* quer, não *como* fazer. **Legado** (*legacy*): sistema antigo, ainda em
> produção, com muitos anos de manutenção; em vagas de Java EE, é muito comum
> trabalhar neles.

## 6. Escalabilidade

> 📘 **Escalabilidade** é a capacidade de o sistema atender mais carga (mais
> usuários, mais requisições, mais dados) apenas acrescentando recursos (mais
> threads, mais conexões, mais servidores), sem precisar ser reescrito.

- **DAOs sem estado**: um singleton atende todas as requisições em paralelo; nada
  para sincronizar.
  ("Sem estado", em inglês *stateless*: o DAO não guarda em campos nenhum dado de
  uma requisição específica. O campo `em` é o proxy thread-safe, e
  `classeEntidade` é `final`, definido no construtor. Por isso não há necessidade
  de `synchronized` ou de *locks*, travas que fariam as threads esperarem umas
  pelas outras.)
- **Pool de conexões gerenciado pelo servidor**, dimensionável sem recompilar.
- **Carregar só o necessário**: LAZY por padrão, `JOIN FETCH` por caso de uso,
  `setMaxResults` para limitar, `COUNT` em vez de carregar listas para contar.
- **Bulk operations** para operações em massa (1 SQL em vez de N).
- **Índice em `obra_id`** (Passo 1), que sustenta `listarPorObra` e `contarPorObra`.
  (Um *índice* é uma estrutura auxiliar do banco, como o índice remissivo de um
  livro, que permite achar as linhas de uma obra sem ler a tabela inteira.)

## 7. Resumo Jakarta EE ↔ Spring Boot (Passo 2)

| Conceito | Jakarta EE (aqui) | Spring Boot |
|---|---|---|
| DAO / Repositório | Classe CDI `@ApplicationScoped` escrita à mão | Interface `JpaRepository` gerada |
| Injeção do EntityManager | `@PersistenceContext` (proxy) | `@PersistenceContext` ou construtor (proxy) |
| Configuração | `persistence.xml` + DataSource JNDI no servidor | `application.properties` + HikariCP embutido |
| Transações | JTA, gerenciadas pelo servidor | `JpaTransactionManager` (local) |
| Exigir transação | `@Transactional(TxType.MANDATORY)` | `@Transactional(propagation = MANDATORY)` |
| Queries | JPQL em constantes / Criteria API | Queries derivadas, `@Query`, `Specification` |
| Exceções | `PersistenceException` | `DataAccessException` (traduzida) |
| Descoberta de beans | CDI (sem `beans.xml` desde o CDI 1.1) | Component scan |

> 📘 Na tabela: **injeção** (de dependência) é o contêiner entregar ao objeto
> aquilo de que ele precisa (aqui, o `EntityManager`), em vez de o objeto criá-lo
> com `new`. `TxType.MANDATORY` (Jakarta) e `propagation = Propagation.MANDATORY`
> (Spring) são a mesma regra de *propagação* de transação: "exija uma transação
> existente". Outros valores comuns, presentes nos dois: `REQUIRED` (o padrão: usa
> a transação existente ou cria uma), `REQUIRES_NEW` (sempre cria uma nova,
> suspendendo a atual), `SUPPORTS`, `NOT_SUPPORTED` e `NEVER`.

## 8. Perguntas prováveis na entrevista

1. **O `EntityManager` é thread-safe?** Não. O que é injetado com
   `@PersistenceContext` é um proxy que delega para o `EntityManager` da transação
   atual; esse proxy é seguro.
2. **O que é o Persistence Context?** O conjunto de entidades managed de um
   `EntityManager`: identity map, cache de 1º nível e unit of work com dirty
   checking.
3. **JTA vs RESOURCE_LOCAL?** JTA: transações gerenciadas pelo servidor, podem
   envolver vários recursos. RESOURCE_LOCAL: a aplicação gerencia a transação JDBC.
4. **Diferença entre DAO e Repository?** DAO abstrai persistência (CRUD); Repository
   (DDD) abstrai uma coleção de agregados do domínio.
5. **O que o Spring Data faz por baixo?** Gera DAOs que usam o mesmo
   `EntityManager`; `save()` = `persist` ou `merge` conforme a entidade seja nova.
6. **Como prevenir SQL Injection?** Com parâmetros nomeados/posicionais
   (`setParameter`), nunca com concatenação de Strings.
7. **Cuidados com bulk update/delete?** Ignoram o Persistence Context, callbacks e
   `@Version`; entidades já carregadas ficam desatualizadas.

## Glossário

| Termo | Significado |
|---|---|
| Agregado (*aggregate*) | Em DDD, grupo de objetos alterado como uma unidade, com uma entidade raiz como única porta de entrada. |
| AOP (*Aspect-Oriented Programming*) | Programação orientada a aspectos: código executado "em volta" de métodos sem alterá-los (transações, logs). |
| `@ApplicationScoped` | Escopo CDI com uma única instância para a aplicação inteira; na prática, um singleton. |
| Bean | Objeto cuja criação e ciclo de vida são gerenciados pelo contêiner (CDI, EJB ou Spring). |
| `beans.xml` | Arquivo de configuração do CDI; hoje opcional, ajusta o modo de descoberta de beans e ativa recursos como interceptors. |
| Bind parameter | Valor enviado separado do texto SQL, vinculado a um marcador `?`; impede SQL Injection. |
| Boilerplate | Código repetitivo, "de fôrma", sem lógica de negócio. |
| Bulk update/delete | Um único `UPDATE`/`DELETE` que afeta muitas linhas, executado com `executeUpdate()`, sem passar pelo Persistence Context. |
| Cache de 1º nível | O próprio Persistence Context: evita ir ao banco para buscar de novo uma entidade já carregada na transação. |
| Callback | Método da entidade (`@PrePersist`, `@PreRemove`...) chamado automaticamente pelo JPA em eventos do ciclo de vida. |
| Caso de uso | Uma ação completa do ponto de vista do usuário (ex.: registrar um relatório). |
| CDI (*Contexts and Dependency Injection*) | Mecanismo padrão do Jakarta EE para criar e injetar objetos (beans) com escopos. |
| Charset | Tabela de conversão entre caracteres e bytes (codificação). |
| Classe abstrata | Classe que não pode ser instanciada, só estendida. |
| CLI (*Command-Line Interface*) | Interface de linha de comando, como o `jboss-cli.sh` do WildFly. |
| Commit / Rollback | Confirmar / desfazer todas as operações de uma transação. |
| Component scan | Varredura de pacotes do Spring em busca de classes anotadas para registrá-las como beans. |
| Contêiner | Ambiente de execução do servidor que cria e gerencia objetos e oferece serviços (transações, segurança). |
| Cp1252 (Windows-1252) | Codificação antiga do Windows, 1 byte por caractere; lê errado textos gravados em UTF-8. |
| Criteria API | API do JPA para montar queries com objetos e métodos Java em vez de Strings. |
| CRUD | *Create, Read, Update, Delete*: as quatro operações básicas sobre dados. |
| DAO (*Data Access Object*) | Objeto cuja única função é ler e gravar dados, escondendo como isso é feito. |
| `DataAccessException` | Raiz da hierarquia de exceções de acesso a dados do Spring, independente de tecnologia. |
| DataSource | Objeto que entrega conexões com o banco, normalmente apoiado por um pool. |
| DDD (*Domain-Driven Design*) | Abordagem de design centrada no domínio do negócio (Eric Evans, 2003). |
| DDL (*Data Definition Language*) / `ddl-auto` | Parte do SQL que define a estrutura (`CREATE`, `ALTER`, `DROP`) / propriedade do Spring que controla a geração automática dessa estrutura. |
| Deploy | Implantação: instalar e ativar uma aplicação ou biblioteca no servidor. |
| Detached | Entidade que já foi managed, mas cujo Persistence Context fechou; suas alterações não são mais vigiadas. |
| DEV / QA / PROD | Ambientes de desenvolvimento, de testes (*Quality Assurance*) e de produção. |
| Dialeto | Variação de SQL de cada banco; o Hibernate tem uma classe `Dialect` por banco. |
| Dirty checking | Comparação de cada entidade managed com o seu snapshot para descobrir o que mudou e gerar `UPDATE`s. |
| DML (*Data Manipulation Language*) | Parte do SQL que manipula dados: `INSERT`, `UPDATE`, `DELETE`. |
| Domínio | O assunto de negócio da aplicação (obras, relatórios). |
| Driver (JDBC) | Biblioteca que sabe conversar com um banco específico. |
| EJB (*Enterprise JavaBeans*) | Componentes de negócio gerenciados pelo contêiner EJB, com transações automáticas. |
| `EJBException` | Exceção com que o contêiner EJB embrulha exceções não verificadas lançadas por um EJB. |
| `EntityManager` | Interface principal do JPA para inserir, buscar, atualizar e remover entidades; não é thread-safe. |
| `EntityManagerFactory` | Objeto pesado, um por Persistence Unit, que cria `EntityManager`s; thread-safe. |
| Escalabilidade | Capacidade de atender mais carga acrescentando recursos, sem reescrever o sistema. |
| Escopo (*scope*) | Quanto tempo vive uma instância de bean e quem a compartilha. |
| `executeUpdate()` | Executa um `UPDATE`/`DELETE` em massa e retorna o número de linhas afetadas. |
| FK (*Foreign Key*) | Chave estrangeira: coluna que aponta para a chave primária de outra tabela. |
| Flush | Envio ao banco dos SQLs pendentes do Persistence Context; não é commit. |
| `FlushModeType.AUTO` | Modo padrão: faz flush antes de queries cujo resultado possa ser afetado por alterações pendentes. |
| Flyway / Liquibase | Ferramentas que aplicam migrações de banco versionadas, em ordem. |
| Fronteira transacional | Ponto do código onde a transação começa e termina. |
| Generics | Parâmetros de tipo do Java (`<T, ID>`), que permitem escrever código reutilizável com checagem de tipos. |
| H2 | Banco de dados relacional escrito em Java, que pode rodar inteiro em memória. |
| Hibernate | A implementação (provider) de JPA mais usada; padrão no WildFly e no Spring Boot. |
| HikariCP | Biblioteca de pool de conexões padrão do Spring Boot. |
| `IDENTITY` | Estratégia de ID gerado pelo banco (auto-incremento); obriga o `INSERT` imediato no `persist`. |
| Identity map | Padrão em que o contexto guarda um mapa "(classe, ID) → objeto", garantindo uma única instância por linha. |
| Índice | Estrutura auxiliar do banco que acelera buscas por uma coluna. |
| Injeção de dependência | O contêiner entrega ao objeto aquilo de que ele precisa, em vez de o objeto criar com `new`. |
| Interceptor | Código que o contêiner executa automaticamente antes e depois de chamadas de método (ex.: `@Transactional`). |
| IoC (*Inversion of Control*) | Inversão de controle: o framework, e não o seu código, cria e conecta os objetos. |
| J2EE / Core J2EE Patterns | Nome antigo do Java EE / catálogo de padrões da Sun que formalizou o DAO. |
| Jakarta Data | Especificação do Jakarta EE 11 para repositórios declarativos no estilo Spring Data. |
| Jakarta EE | Nome atual do Java EE, mantido pela Eclipse Foundation (pacotes `jakarta.*`). |
| JAX-RS | *Jakarta RESTful Web Services*: API padrão para criar APIs REST. |
| `jboss-cli.sh` | Ferramenta de linha de comando para administrar o WildFly. |
| JDBC (*Java Database Connectivity*) | API de baixo nível do Java para acessar bancos relacionais. |
| JMS (*Java Message Service*) | Hoje Jakarta Messaging: API para enviar e receber mensagens por filas e tópicos. |
| JNDI (*Java Naming and Directory Interface*) | "Lista telefônica" do servidor: recursos registrados e procurados por nome. |
| `JOIN FETCH` | JPQL que faz o `JOIN` e já preenche a associação, em uma única query. |
| JPA (*Jakarta Persistence API*) | Especificação Java de ORM; define o `EntityManager`, as anotações e o JPQL. |
| `JpaRepository` | Interface do Spring Data JPA que você estende para ganhar um repositório gerado. |
| JPQL (*Jakarta Persistence Query Language*) | Linguagem de consultas do JPA, escrita sobre entidades e atributos Java. |
| JSF (*Jakarta Faces*) | Framework de telas web do Jakarta EE, baseado em componentes. |
| JSON (*JavaScript Object Notation*) | Formato de texto usado por APIs REST para trocar dados. |
| JTA (*Java Transaction API*) | Hoje Jakarta Transactions: transações coordenadas pelo servidor, podendo envolver vários recursos. |
| JVM (*Java Virtual Machine*) | Programa que executa o código Java compilado. |
| LAZY / EAGER | Carregar uma associação só quando acessada / carregá-la junto com a entidade. |
| Legado (*legacy*) | Sistema antigo ainda em produção. |
| Managed | Entidade dentro de um Persistence Context, com alterações vigiadas e gravadas automaticamente. |
| `@ManyToOne` / `@OneToMany` | Associações "muitos para um" e "um para muitos". |
| Metadados | Informações de mapeamento (qual classe em qual tabela, qual atributo em qual coluna). |
| Migração | Script versionado que evolui a estrutura do banco de uma versão para a próxima. |
| Mock | Objeto falso que imita uma dependência real durante um teste. |
| `NoResultException` / `NonUniqueResultException` | Lançadas por `getSingleResult()` com 0 ou mais de 1 resultado; não marcam a transação para rollback. |
| Optimistic locking | Bloqueio otimista: confere, ao gravar, se a linha mudou (via `@Version`), em vez de travá-la. |
| `Optional` | Classe do Java 8+ que representa um valor que pode estar ausente. |
| ORM (*Object-Relational Mapping*) | Mapeamento entre objetos Java e tabelas relacionais. |
| Override | Sobrescrever, na subclasse, um método herdado (`@Override`). |
| Paginação | Dividir um resultado grande em páginas (`setFirstResult` + `setMaxResults`). |
| Parâmetros nomeados / posicionais | Marcadores `:nome` / `?1` no JPQL; ambos viram bind parameters. |
| Payara | Servidor de aplicação Jakarta EE derivado do GlassFish. |
| `persist` / `merge` / `remove` / `find` / `getReference` | Tornar novo managed / copiar detached para managed / marcar para exclusão / buscar por ID / obter um proxy sem `SELECT`. |
| Persistence Context | Conjunto de entidades managed de um `EntityManager`: identity map, cache de 1º nível e unit of work. |
| Persistence Unit | Conjunto nomeado de configurações (entidades, DataSource, propriedades) definido no `persistence.xml`. |
| `persistence.xml` | Arquivo de configuração do JPA, em `META-INF/`. |
| `@PersistenceContext` | Anotação que pede ao contêiner a injeção de um `EntityManager` (um proxy). |
| `PersistenceException` | Exceção base do JPA. |
| Persistência | Gravar dados de forma durável para que sobrevivam ao fim do programa. |
| Polimorfismo | O Java executa a versão do método do objeto real, não a do tipo da variável. |
| Pool de conexões | Conjunto de conexões já abertas, emprestadas e devolvidas (como carrinhos de supermercado). |
| `PreparedStatement` | Interface JDBC para SQL com marcadores `?`, com valores enviados separadamente. |
| Provider | A implementação concreta de uma especificação (o Hibernate é um provider de JPA). |
| Proxy | Objeto intermediário que se passa por outro e repassa as chamadas. |
| Query | Consulta (ou comando) enviada ao banco. |
| Repository | Em DDD, abstração de uma coleção de agregados, sem detalhes de persistência. |
| Requisição | Cada pedido HTTP que chega ao servidor. |
| RESOURCE_LOCAL | Transação local de uma única conexão, controlada pela aplicação. |
| REST (*Representational State Transfer*) | Estilo de API HTTP baseado em URLs e verbos (`GET`, `POST`...). |
| Runtime | Tempo de execução, enquanto a aplicação roda. |
| Separation of concerns | Separação de responsabilidades: cada parte do sistema cuida de um assunto só. |
| Service | Camada onde ficam as regras de negócio. |
| Servlet | Classe Java básica para tratar requisições HTTP. |
| `setMaxResults` | Limita o número de linhas retornadas por uma query. |
| `SimpleJpaRepository` | Implementação padrão, no Spring Data JPA, dos métodos de `JpaRepository`. |
| Singleton | Classe da qual existe uma única instância compartilhada por toda a aplicação. |
| Snapshot | Cópia dos valores originais de uma entidade, usada no dirty checking. |
| Specification | Interface do Spring Data JPA que encapsula filtros da Criteria API e permite combiná-los. |
| Spring Data JPA | Módulo do Spring que gera repositórios (DAOs) sobre o JPA. |
| SQL (*Structured Query Language*) | Linguagem padrão dos bancos relacionais. |
| SQL Injection | Ataque em que texto do usuário vira parte do comando SQL; evitado com bind parameters. |
| SRP (*Single Responsibility Principle*) | "Uma classe deve ter um único motivo para mudar"; o "S" do SOLID. |
| Stateless / `@Stateless` | Sem estado: não guarda dados de uma requisição específica / EJB sem estado. |
| Thread / Thread-safe | Linha de execução / objeto que pode ser usado por várias threads ao mesmo tempo com segurança. |
| Trade-off | Troca: ganhar algo abrindo mão de outra coisa. |
| Transação | Grupo de operações "tudo ou nada". |
| `@Transactional` / `TxType.MANDATORY` | Anotação de controle de transação (interceptor) / regra que exige uma transação já existente. |
| Two-phase commit (2PC) | Protocolo em duas fases (*prepare* e *commit*) para confirmar uma transação em vários recursos ao mesmo tempo. |
| Type erasure | Apagamento dos tipos genéricos após a compilação; por isso o DAO recebe `Obra.class`. |
| `TypedQuery` | Query que já conhece o tipo do resultado. |
| Unit of Work | Padrão que anota todas as alterações da transação e as grava juntas no final. |
| UTF-8 | Codificação Unicode mais usada; de 1 a 4 bytes por caractere. |
| `@Version` | Campo de versão usado no optimistic locking (`where ... and versao=?`). |
| WAR (*Web Application Archive*) | Arquivo `.war` que empacota uma aplicação web para deploy em um servidor. |
| WildFly | Servidor de aplicação Jakarta EE de código aberto da Red Hat (antigo JBoss AS). |
| XA | Padrão do X/Open para transações distribuídas, usado pelo two-phase commit. |
