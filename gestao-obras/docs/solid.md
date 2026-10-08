# SOLID no projeto Gestão de Obras

Explicação detalhada de onde cada princípio SOLID aparece no código, por que foi
aplicado (ou não) e quais são os trade-offs. Os exemplos citam arquivos e métodos
reais do projeto.

> 📘 **Como ler este documento:** sempre que uma sigla ou um termo em inglês
> aparece pela primeira vez, ele vem acompanhado de uma explicação curta, entre
> parênteses ou em um bloco como este. Quando o termo é aprofundado mais adiante,
> o texto avisa onde. No final há um [Glossário](#glossário) em ordem alfabética
> para consulta rápida.
>
> ***Trade-off*** (palavra que aparece logo acima e muitas vezes neste texto) é
> uma **troca consciente**: você ganha uma coisa e, em troca, abre mão de outra.
> Por exemplo, "menos classes" em troca de "um pouco menos de separação de
> responsabilidades".

> 📘 **O que é SOLID?**
> SOLID é um acrônimo (uma palavra formada pelas iniciais de outras) que reúne
> cinco princípios de *design* de código orientado a objetos. *Design*, aqui, é a
> forma como você divide o código em classes e decide quem conhece quem. Os
> princípios foram organizados e popularizados por **Robert C. Martin**; o
> acrônimo em si foi sugerido por Michael Feathers, no começo dos anos 2000.
> O objetivo dos cinco é o mesmo: deixar o código **fácil de mudar** sem quebrar
> o que já funciona. Cada letra é a inicial do nome de um princípio em inglês:
>
> | Letra | Sigla | Nome em inglês | Tradução |
> |---|---|---|---|
> | **S** | SRP | *Single Responsibility Principle* | Princípio da Responsabilidade Única |
> | **O** | OCP | *Open/Closed Principle* | Princípio Aberto/Fechado |
> | **L** | LSP | *Liskov Substitution Principle* | Princípio da Substituição de Liskov |
> | **I** | ISP | *Interface Segregation Principle* | Princípio da Segregação de Interfaces |
> | **D** | DIP | *Dependency Inversion Principle* | Princípio da Inversão de Dependência |
>
> "*Principle*" é "princípio": uma diretriz de design, e não uma regra que o
> compilador verifica. Aplicar SOLID é uma questão de julgamento, e é por isso
> que este documento fala tanto de trade-offs.

> 📘 **Quem é Robert C. Martin ("Uncle Bob")?**
> Engenheiro de software americano, um dos 17 signatários do Manifesto Ágil
> (2001) e autor de livros muito citados em entrevistas, como *Clean Code*
> ("Código Limpo", 2008) e *Clean Architecture* ("Arquitetura Limpa", 2017).
> "*Uncle Bob*" significa "Tio Bob", o apelido pelo qual ele é conhecido na
> comunidade.

## Situação atual (após o Passo 5)

Os "Passos" são as etapas em que este projeto vem sendo construído, cada uma com
o seu relatório na pasta `docs/`. Em linhas gerais: Passo 1, o modelo de dados;
Passo 2, a camada de acesso a dados; Passo 3, a camada de negócio; Passo 4, a
tela web; Passo 5, um serviço web para outros sistemas.

| Princípio | Situação |
|---|---|
| **S** — Responsabilidade Única | ✅ Aplicado. O ponto do enum (texto de tela) foi resolvido no Passo 4; resta só um trade-off consciente: as anotações na entidade `Obra` |
| **O** — Aberto/Fechado | ✅ Aplicado |
| **L** — Substituição de Liskov | ✅ Aplicado (o caso dos proxies do Hibernate é o mais interessante) |
| **I** — Segregação de Interfaces | ✅ Aplicado no Passo 5: a API REST depende do contrato só de leitura `ObraConsulta`; os services já eram separados por agregado. Resta, por escolha consciente, o `GenericDao` com CRUD completo |
| **D** — Inversão de Dependência | ✅ Entre apresentação e negócio (interfaces `@Local`, Passo 3); 🟡 entre negócio e DAO, por escolha pragmática |

> 📘 **Vocabulário rápido desta tabela** (cada item é aprofundado mais adiante):
> - **enum** (de *enumeration*, "enumeração"): um tipo Java com um conjunto
>   fixo de valores possíveis, como o `StatusObra`. Detalhes na seção S.
> - **entidade** (*entity*): uma classe Java cujos objetos são gravados como
>   linhas de uma tabela do banco de dados, como a `Obra`. Detalhes na seção S.
> - **proxy** ("procurador", "representante"): um objeto que se passa por outro
>   e fica entre quem chama e o objeto real, como um procurador que assina em
>   seu nome. Detalhes nas seções O e L.
> - **Hibernate**: a biblioteca que, neste projeto, faz a ponte entre os objetos
>   Java e as tabelas do banco. Detalhes nas seções L e D.
> - **service** ("serviço"): a classe da camada de negócio que executa os casos
>   de uso, como "cadastrar obra" ou "excluir obra".
> - **agregado** (*aggregate*): termo do **DDD** (*Domain-Driven Design*,
>   "Projeto Orientado ao Domínio", livro de Eric Evans, de 2003). É um grupo de
>   objetos de negócio tratado como uma unidade, com uma "raiz" por onde o grupo
>   é acessado. Aqui, na prática: obras de um lado, relatórios de segurança do
>   outro, cada um com o seu service.
> - **contrato**: o que um método ou uma interface promete: o que recebe, o que
>   devolve e quais erros pode lançar. Detalhes na seção L.
> - **interface `@Local`**: uma interface Java marcada com a anotação `@Local`,
>   que vira a "porta de entrada" oficial de um componente de negócio. Detalhes
>   na seção D.
> - **DAO** (*Data Access Object*, "Objeto de Acesso a Dados"): a classe
>   responsável por ler e gravar um tipo de dado no banco. Detalhes na seção S.
> - **apresentação** (camada de apresentação): o código que conversa com quem
>   está "do lado de fora": a tela web e, no futuro, os outros sistemas.
> - **pragmática**: escolhida pelo custo-benefício prático, e não por pureza
>   teórica.

---

## S — Single Responsibility Principle (Responsabilidade Única)

Sigla: **SRP**. *Single* = único; *Responsibility* = responsabilidade;
*Principle* = princípio.

**Definição (Robert C. Martin):** *"uma classe deve ter um, e somente um, motivo
para mudar"*. O "motivo para mudar" é um ator ou interesse do negócio, não
"fazer uma coisa só".

> 📘 **O que é um "ator", na prática?**
> Em *Clean Architecture*, o próprio Martin reformulou o princípio assim: *"um
> módulo deve ser responsável por um, e somente um, ator"*. **Ator** é a pessoa
> ou o grupo que pede mudanças: o setor de segurança do trabalho, o
> administrador do banco de dados, quem cuida das telas.
>
> **Analogia com o nosso domínio:** em uma construtora, o engenheiro calculista
> e o arquiteto podem pedir mudanças no mesmo prédio, mas por motivos
> diferentes: o calculista mexe na estrutura e o arquiteto mexe na fachada. Se o
> cálculo estrutural e o desenho da fachada estivessem no mesmo documento, cada
> pedido de um arriscaria estragar o trabalho do outro. O SRP diz: um documento
> (uma classe) para cada um.

### Onde está aplicado

**1. Cada arquivo tem um único motivo para mudar:**

| Arquivo | Responsabilidade | Só muda quando… |
|---|---|---|
| `Obra.java` | Estado e regras básicas de uma obra | o conceito de "obra" mudar |
| `StatusObra.java` | Os estados possíveis e a regra de transição entre eles | surgir um novo estado ou uma nova regra de transição |
| `GenericDao.java` | A mecânica de CRUD comum | a forma de fazer CRUD com JPA mudar |
| `ObraDao.java` | Como obras são consultadas (JPQL) | uma consulta de obras mudar |
| `ObraServiceBean.java` | Regras de negócio e fronteira transacional das obras | uma regra de negócio mudar |
| `persistence.xml` | Onde está o banco e como conectar | a infraestrutura mudar |
| `dados-iniciais.sql` | Dados de demonstração | os dados de exemplo mudarem |
| `messages.properties` | Os textos exibidos na tela (Passo 4) | um texto de tela mudar ou a aplicação ganhar um idioma novo |

> 📘 **Siglas e termos desta tabela**
> - **enum**: em Java, `enum` cria um tipo com um conjunto **fechado** de
>   constantes. Um `StatusObra` só pode valer `PLANEJADA`, `EM_ANDAMENTO`,
>   `SUSPENSA` ou `CONCLUIDA`. Diferente de uma `String`, o compilador impede
>   valores inválidos, e o enum pode ter métodos (como o `podeMudarPara`).
> - **CRUD** (*Create, Read, Update, Delete*, "Criar, Ler, Atualizar,
>   Excluir"): as quatro operações básicas sobre dados. No `GenericDao` elas se
>   chamam `inserir`, `buscarPorId`/`listarTodos`, `atualizar` e `removerPorId`.
> - **Jakarta EE** (*Enterprise Edition*, "edição corporativa"): o conjunto de
>   **especificações** oficiais do Java para aplicações corporativas
>   (persistência, transações, telas web, serviços web...). Já se chamou Java EE
>   e, antes, J2EE; a história está no final da seção D. O Spring Boot, aliás,
>   usa várias dessas especificações, como a de persistência.
> - **JPA**: a especificação de persistência do Jakarta EE. O nome oficial hoje
>   é *Jakarta Persistence*; a sigla vem do nome antigo, *Java Persistence API*,
>   e continua sendo usada por todos. Ela define como fazer **ORM** (*Object-
>   Relational Mapping*, "mapeamento objeto-relacional"): traduzir objetos Java
>   em linhas de tabelas e vice-versa, sem você escrever o SQL de cada operação.
>   É exatamente o que você usa no Spring Boot com o `spring-boot-starter-data-jpa`.
> - **API** (*Application Programming Interface*, "Interface de Programação de
>   Aplicações"): o conjunto de classes, métodos ou endereços que um software
>   expõe para outro software usar. A JPA é uma API Java; a "API REST" do
>   Passo 5 será uma API acessada pela rede.
> - **JPQL** (*Jakarta Persistence Query Language*; antes, *Java Persistence
>   Query Language*): a linguagem de consulta do JPA. Parece SQL, mas fala de
>   **classes e atributos Java**, e não de tabelas e colunas: em
>   `SELECT o FROM Obra o ORDER BY o.nome`, `Obra` é a classe e `nome` é o
>   atributo. É a mesma linguagem do `@Query` do Spring Data JPA.
> - **Sufixo `Bean`** em `ObraServiceBean`: é a convenção de nome para a classe
>   que **implementa** um componente de negócio (aqui, a interface
>   `ObraService`). *Bean* é explicado na seção O.
> - **Fronteira transacional**: o ponto do código em que uma transação começa e
>   termina. "Transação" é explicada no item 2, logo abaixo.
> - **XML** (*eXtensible Markup Language*, "Linguagem de Marcação Extensível"):
>   formato de texto organizado em *tags* (marcações) como `<persistence-unit>`.
>   O `persistence.xml` é o arquivo de configuração do JPA: equivale às
>   propriedades `spring.datasource.*` e `spring.jpa.*` do `application.properties`.
> - **SQL** (*Structured Query Language*, "Linguagem de Consulta
>   Estruturada"): a linguagem padrão dos bancos relacionais. O
>   `dados-iniciais.sql` equivale ao `data.sql` do Spring Boot.
> - **`messages.properties`**: um arquivo `.properties` (formato `chave=valor`,
>   o mesmo do `application.properties`) com os textos da tela. Detalhes no
>   item b), mais abaixo.

Exemplo concreto: se o DBA pedir para otimizar a busca por nome, você mexe **só**
no `ObraDao`. A `Obra`, o `GenericDao`, o Service, a tela JSF e a API REST não
são tocados.

> 📘 **DBA, JSF e REST**
> - **DBA** (*Database Administrator*): o administrador do banco de dados, quem
>   cuida de desempenho, índices, *backup* (cópia de segurança) etc.
> - **JSF** (*JavaServer Faces*; hoje *Jakarta Faces*): o *framework* de telas
>   web do Jakarta EE. Um ***framework*** ("arcabouço") é um código pronto que
>   define a estrutura da aplicação e chama o seu código nos momentos certos.
>   No JSF você monta páginas com **componentes** (tabelas, campos, botões) que
>   se ligam a uma classe Java no servidor. O mais próximo no mundo Spring seria
>   Spring MVC com Thymeleaf (a biblioteca de modelos de página mais usada com
>   o Spring), mas o JSF é orientado a componentes e guarda o estado da tela no
>   servidor.
> - **REST** (*Representational State Transfer*, "Transferência de Estado
>   Representacional"): estilo de arquitetura descrito por Roy Fielding em 2000.
>   O sistema expõe **recursos** (por exemplo, `/obras`) em endereços **URL**
>   (*Uniform Resource Locator*, o "endereço web"), acessados pelos métodos do
>   **HTTP** (*HyperText Transfer Protocol*, o protocolo da web): `GET` para
>   ler, `POST` para criar, `PUT` para atualizar, `DELETE` para excluir.
>   Normalmente os dados viajam em **JSON** (*JavaScript Object Notation*, um
>   formato de texto para dados). Uma **API REST** é uma API nesse estilo, o que
>   no Spring você faz com `@RestController`.

**2. O DAO não decide transação** (`GenericDao`):

```java
@Transactional(Transactional.TxType.MANDATORY)
public abstract class GenericDao<T, ID> {
```

> 📘 **O que é uma transação?**
> Uma transação é um grupo de operações no banco que precisa acontecer **por
> inteiro ou não acontecer**. O exemplo clássico é uma transferência bancária:
> debitar de uma conta e creditar em outra. Se o crédito falhar depois do
> débito, o dinheiro "some". Por isso as duas operações formam uma única
> transação.
> - ***Commit*** ("confirmar"): o fim com sucesso. Todas as alterações são
>   gravadas de vez.
> - ***Rollback*** ("reverter"): o fim com falha. Todas as alterações feitas
>   desde o início da transação são desfeitas, como se nada tivesse acontecido.
>
> As garantias de uma transação são resumidas pela sigla **ACID**:
> **A**tomicidade (tudo ou nada), **C**onsistência (o banco sai de um estado
> válido e chega a outro estado válido), **I**solamento (transações simultâneas
> não interferem umas nas outras; o grau exato depende do nível de isolamento
> configurado) e **D**urabilidade (depois do commit, o dado não se perde, nem se
> o servidor cair).

> 📘 **O que significam `@Transactional` e `MANDATORY`?**
> - Uma **anotação** (*annotation*: tudo o que começa com `@`) é um rótulo
>   colocado no código que outros programas (o compilador, o servidor, um
>   framework) leem para decidir o que fazer. Sozinha, ela não executa nada.
> - O `@Transactional` daqui vem da especificação **JTA** (hoje *Jakarta
>   Transactions*; a sigla vem do nome antigo, *Java Transaction API*). Ele diz
>   ao **contêiner** (o ambiente do servidor que cria e gerencia os objetos da
>   aplicação; detalhes na seção O) como tratar transações quando um método da
>   classe é chamado. É o "primo" do `@Transactional` do Spring.
> - `TxType` (*transaction type*, "tipo de transação") define a **propagação**:
>   o que fazer se já existe, ou não, uma transação em andamento quando o
>   método é chamado. Os valores possíveis:
>   - `REQUIRED` ("necessária", o padrão): usa a transação existente ou abre uma nova;
>   - `REQUIRES_NEW` ("exige nova"): sempre abre uma nova, suspendendo a atual;
>   - `MANDATORY` ("obrigatória"): **exige** que já exista uma; se não existir, lança exceção;
>   - `SUPPORTS` ("suporta"): usa a existente, se houver; senão, roda sem transação;
>   - `NOT_SUPPORTED` ("não suportada"): suspende a existente e roda sem transação;
>   - `NEVER` ("nunca"): se existir uma transação, lança exceção.
> - No Spring, o equivalente é `@Transactional(propagation = Propagation.MANDATORY)`.

Esse é o ponto mais sutil. Decidir **onde uma transação começa e termina**
depende do **caso de uso**. Por exemplo, "excluir obra + excluir os relatórios
dela" tem de ser uma única transação.

Se o DAO abrisse a própria transação, ele teria **dois** motivos para mudar:
- a forma de acessar os dados;
- as regras de atomicidade do negócio.

Com `MANDATORY`, o DAO só acessa dados, e a decisão transacional fica no Service
(Passo 3). Testamos isso no WildFly: uma chamada sem transação é bloqueada com
`TransactionalException`.

> 📘 **WildFly e `TransactionalException`**
> - **WildFly** é o **servidor de aplicação** Jakarta EE usado no projeto,
>   desenvolvido pela Red Hat (até 2013 se chamava JBoss Application Server).
>   Um servidor de aplicação é um programa que executa a sua aplicação e já
>   fornece prontas as implementações das especificações: transações,
>   persistência, injeção de dependências, telas web. No Spring Boot, a sua
>   aplicação carrega um servidor web embutido e as bibliotecas que escolheu; no
>   Jakarta EE tradicional, é o servidor que fornece tudo isso, e você faz o
>   ***deploy*** ("implantação": instalar a aplicação no servidor) nele.
> - **`TransactionalException`** é a exceção que o `@Transactional` do JTA
>   lança quando a regra de propagação é violada. Aqui: um método `MANDATORY`
>   foi chamado sem transação ativa.

### Onde o SRP está "esticado" (sendo honesto)

Até o Passo 3 havia dois pontos de tensão. O segundo (b) foi resolvido no
Passo 4; o primeiro (a) continua, por escolha consciente.

**a) A `Obra` mistura três interesses através de anotações:**

```java
@NotBlank(...)                                         // validação
@Column(name = "nome", nullable = false, length = 150) // mapeamento do banco
private String nome;                                    // domínio
```

> 📘 **Os três interesses, um por linha**
> - `@NotBlank` vem da **Bean Validation** (hoje *Jakarta Validation*), a
>   especificação de validação por anotações. *Blank* é "em branco":
>   `@NotBlank` significa "não pode ser nulo, vazio nem só espaços". É a mesma
>   especificação que você usa no Spring Boot com o
>   `spring-boot-starter-validation`; a implementação mais usada é o
>   Hibernate Validator.
> - `@Column` vem do JPA. Diz em qual coluna o atributo é gravado (`nome`), se
>   ela aceita nulo e qual o tamanho máximo. Isso é **mapeamento**: a tradução
>   entre o atributo Java e a coluna do banco.
> - `private String nome;` é o **domínio**: o conceito de negócio em si ("uma
>   obra tem um nome"), sem nenhum detalhe técnico.

Um purista de Clean Architecture ou de arquitetura hexagonal separaria isso em
três peças: uma `Obra` de domínio pura, uma `ObraEntity` JPA e um *mapper* entre
elas. Optamos pelo pragmatismo, por dois motivos:
- anotações são **metadados** (dados *sobre* o código: descrevem a classe para
  quem a lê, como o JPA e o validador, mas não executam nada sozinhas), não
  comportamento;
- separar dobraria o número de classes sem ganho real em um CRUD.

> 📘 **Clean Architecture, arquitetura hexagonal, *entity* e *mapper***
> - **Clean Architecture** ("Arquitetura Limpa"): proposta de Robert C. Martin
>   (artigo de 2012, livro de 2017). Organiza o sistema em camadas
>   concêntricas, com as regras de negócio no centro. A regra principal é que
>   as dependências apontam **para dentro**: o negócio não conhece banco,
>   framework nem tela.
> - **Arquitetura hexagonal** (também chamada *Ports and Adapters*, "Portas e
>   Adaptadores"): proposta por Alistair Cockburn em 2005. O núcleo de negócio
>   define **portas** (interfaces), e o mundo externo (banco, tela, outros
>   sistemas) se conecta por **adaptadores** que implementam essas portas. O
>   hexágono é só um desenho para mostrar que o núcleo tem vários "lados" de
>   conexão, sem um "em cima" e um "embaixo".
> - ***Entity*** ("entidade"): no JPA, uma classe anotada com `@Entity`, cujos
>   objetos correspondem a linhas de uma tabela. Uma `ObraEntity` seria uma
>   classe só para isso, sem regras de negócio.
> - ***Mapper*** ("mapeador"): uma classe que copia os dados de uma
>   representação para outra (`Obra` ⇄ `ObraEntity`). No mundo Spring, é comum
>   gerar mappers com a biblioteca MapStruct.
>
> As duas arquiteturas compartilham a ideia central: o modelo de domínio não
> depende de detalhes técnicos. É isso que leva à separação em três peças.

Na entrevista, a resposta madura é: *"sei que existe a alternativa e sei quando
vale a pena: quando o domínio é rico ou o modelo do banco diverge muito do modelo
de negócio"*.

**b) O `StatusObra` guardava texto de tela — ✅ Resolvido no Passo 4**

**Antes (até o Passo 3)**, cada constante do enum carregava o texto que o
usuário vê na tela:

```java
EM_ANDAMENTO("Em andamento"),
```

Por que isso era um problema: era uma violação real, ainda que pequena. Se
amanhã a aplicação precisasse de inglês, o enum mudaria por um motivo de
**apresentação**, e esse é um segundo motivo para mudar (o primeiro é o
negócio: um estado novo, uma regra de transição nova). Repare na diferença entre
o **valor** que o sistema usa (`EM_ANDAMENTO`, gravado no banco) e o **rótulo**
que a pessoa lê ("Em andamento"): são interesses de atores diferentes. O lugar
certo desse texto é um arquivo de mensagens (`messages.properties`) do JSF.
Isso estava planejado para o Passo 4, quando a tela passasse a existir, e foi o
que aconteceu.

**Depois (Passo 4)**, a responsabilidade ficou dividida em três peças.

1. O enum ficou só com o que é de negócio: os estados e a regra de transição.

```java
public enum StatusObra {

    PLANEJADA,
    EM_ANDAMENTO,
    SUSPENSA,
    CONCLUIDA;

    public boolean podeMudarPara(StatusObra novoStatus) {
        return this != CONCLUIDA || novoStatus == CONCLUIDA;
    }
}
```

2. Os textos de tela foram para `src/main/resources/messages.properties`:

```properties
status.PLANEJADA=Planejada
status.EM_ANDAMENTO=Em andamento
status.SUSPENSA=Suspensa
status.CONCLUIDA=Concluída
```

3. A página JSF monta a chave e busca o texto:

```
#{msg['status.' += obra.status]}
```

> 📘 **Como essa expressão funciona, passo a passo**
> O `#{...}` é **EL** (*Expression Language*, "Linguagem de Expressão"; hoje
> *Jakarta Expression Language*): uma minilinguagem usada dentro das páginas JSF
> para ler e escrever dados dos objetos Java. Para uma obra em andamento, a
> expressão é avaliada assim:
> 1. `obra.status` chama `obra.getStatus()` e obtém `StatusObra.EM_ANDAMENTO`.
> 2. `'status.' += obra.status` **concatena** (junta) textos. O `+=` é o
>    operador de concatenação de *strings* (textos) da EL, disponível desde a
>    EL 3.0; na EL, o `+` sozinho é só soma numérica. Na concatenação, o enum é
>    convertido para texto pelo seu nome, então o resultado é
>    `"status.EM_ANDAMENTO"`.
> 3. `msg[...]` procura essa chave no arquivo de mensagens. `msg` é o nome com
>    que o arquivo foi registrado para as páginas (no JSF, isso é feito na
>    configuração da aplicação, o `faces-config.xml`, ou na própria página, com
>    `<f:loadBundle>`). Resultado final: `Em andamento`.

> 📘 **O ganho: SRP e internacionalização (i18n)**
> - Agora o `StatusObra` só muda por motivo de **negócio**. Um texto de tela
>   muda só no `messages.properties`, sem tocar em código Java.
> - **Internacionalização** (*internationalization*), abreviada **i18n**
>   porque entre o "i" e o "n" da palavra em inglês há 18 letras, é preparar a
>   aplicação para funcionar em vários idiomas **sem mudar o código**.
> - Para oferecer a aplicação em inglês, basta criar um `messages_en.properties`
>   com as mesmas chaves (`status.EM_ANDAMENTO=In progress`, por exemplo) e
>   declarar o inglês entre os idiomas suportados na configuração do JSF
>   (`<supported-locale>`, no `faces-config.xml`). **O enum não é tocado.**
> - Quem escolhe o arquivo é o mecanismo de ***resource bundle*** ("pacote de
>   recursos") do próprio Java, usado pelo JSF: ele olha o ***locale*** do
>   usuário (a combinação de idioma e região, como `pt_BR` ou `en_US`) e carrega
>   `messages_en.properties` para inglês. Se uma chave faltar nesse arquivo, ele
>   usa a do `messages.properties` como reserva (*fallback*, o "plano B").
> - **Analogia:** o enum é o código interno de cada estado, como o código de
>   barras de um produto; o arquivo de mensagens é a etiqueta que o cliente lê.
>   Trocar a etiqueta por uma em inglês não exige mexer no código de barras.
> - No Spring Boot, o mecanismo é o mesmo: um `messages.properties` na raiz do
>   *classpath* (o conjunto de pastas e bibliotecas onde o Java procura classes
>   e recursos) é carregado automaticamente pelo `MessageSource`.

---

## O — Open/Closed Principle (Aberto/Fechado)

Sigla: **OCP**. *Open* = aberto; *Closed* = fechado.

**Definição:** *"entidades de software devem estar abertas para extensão, mas
fechadas para modificação"*. Você acrescenta comportamento novo **escrevendo
código novo**, não editando código que já funciona e já foi testado.

> 📘 **De onde vem, e o que "entidade" quer dizer aqui**
> - O princípio foi formulado por **Bertrand Meyer**, cientista da computação
>   francês, criador da linguagem Eiffel e da ideia de *Design by Contract*
>   ("Projeto por Contrato", veja a seção L), no livro *Object-Oriented Software
>   Construction* (1988). Na versão original de Meyer, a extensão era feita por
>   herança; a frase acima é a versão popularizada por Robert C. Martin, que
>   reinterpretou o princípio com abstrações (interfaces e classes abstratas).
> - Atenção: "entidades de software" aqui significa **classes, módulos e
>   funções** em geral, e **não** as entidades JPA (`@Entity`).
> - **Analogia:** um videogame é "fechado" (você não abre o console para soldar
>   peças) e "aberto" (aceita jogos novos pelo encaixe padrão). O encaixe padrão
>   é a **abstração**; cada jogo novo é uma **extensão**.

### Onde está aplicado

**1. O `GenericDao` é fechado; os DAOs concretos estendem**

Imagine que amanhã surge a entidade `Equipamento` (gruas, betoneiras). Basta isto:

```java
@ApplicationScoped
public class EquipamentoDao extends GenericDao<Equipamento, Long> {
    public EquipamentoDao() {
        super(Equipamento.class);
    }
}
```

> 📘 **`@ApplicationScoped`, CDI, *bean* e *singleton***
> - **CDI** (*Contexts and Dependency Injection*, "Contextos e Injeção de
>   Dependência"; hoje *Jakarta CDI*) é a especificação de injeção de
>   dependências do Jakarta EE. Faz o papel que o contêiner de *beans* faz no
>   Spring.
> - ***Bean*** (literalmente "grão"): no Java corporativo, um objeto cujo ciclo
>   de vida (criação, uso, destruição) é gerenciado pelo contêiner, e não pelo
>   seu código. É o mesmo sentido do *bean* do Spring.
> - **Escopo** (*scope*): por quanto tempo uma instância vive e quem a
>   compartilha. `@ApplicationScoped` ("com escopo de aplicação") significa:
>   **uma única instância para a aplicação inteira**, criada no primeiro uso e
>   compartilhada por todos.
> - Na prática, isso funciona como um ***singleton*** (do inglês "único"): uma
>   classe da qual existe uma única instância compartilhada por toda a
>   aplicação. É o escopo padrão dos beans do Spring (`@Component`, `@Service`,
>   `@Repository`). Consequência importante: um DAO `@ApplicationScoped` não
>   pode guardar em seus campos dados de um usuário, porque todos compartilham o
>   mesmo objeto. (O `EntityManager` injetado é seguro porque, como explica o
>   comentário no `GenericDao`, ele é um proxy que entrega a cada transação o
>   seu próprio contexto de persistência.)
> - Não confunda com as anotações chamadas `@Singleton` (existe uma no EJB e
>   outra em `jakarta.inject`): elas também resultam em instância única, mas
>   seguem outras regras.

O `GenericDao` **não é alterado em nenhuma linha**, e o `Equipamento` já ganha
`inserir`, `atualizar`, `buscarPorId` e os outros. O que torna isso possível:
- os **generics** `<T, ID>`, que deixam o tipo em aberto;
- o `Class<T> classeEntidade` recebido no construtor, que é usado no
  `em.find(classeEntidade, id)`.

> 📘 **O que são *generics*, e por que o `Class<T>`?**
> - ***Generics*** ("genéricos") são os parâmetros de tipo entre `< >`. Em
>   `GenericDao<T, ID>`, `T` e `ID` são "lacunas" de tipo, preenchidas por quem
>   estende: em `GenericDao<Equipamento, Long>`, `T` vira `Equipamento` e `ID`
>   vira `Long`. É o mesmo mecanismo de `List<String>` ou do
>   `JpaRepository<Obra, Long>` do Spring Data.
> - Em Java, os generics só existem na compilação. Isso se chama ***type
>   erasure*** ("apagamento de tipo"): quando o programa roda, o `GenericDao`
>   não tem acesso direto (sem recorrer a reflexão) à informação de que `T` é
>   `Equipamento`. Por isso a subclasse passa o objeto `Class` explicitamente
>   (`Equipamento.class`), e o `em.find(classeEntidade, id)` o usa para saber
>   qual entidade buscar.
> - O `em` é o `EntityManager`, o objeto do JPA que executa as operações no
>   banco. Detalhes na seção D.

Veja o **contraexemplo** que evitamos:

```java
// ❌ Violação do OCP: cada entidade nova obriga a EDITAR esta classe
public Object buscarPorId(String tipo, Long id) {
    if (tipo.equals("obra")) return em.find(Obra.class, id);
    else if (tipo.equals("relatorio")) return em.find(RelatorioSeguranca.class, id);
    // + um "else if" para cada entidade nova...
}
```

**2. Estender comportamento sem mexer na classe-base**

O `ObraDao` precisava de uma listagem ordenada. Ele **sobrescreve** o
`listarTodos()` em vez de alterar o `GenericDao`. O `RelatorioSegurancaDao`
continua usando a versão genérica, sem ser afetado.

> 📘 **Sobrescrever (*override*)**: é quando a subclasse redefine um método
> herdado, com a mesma assinatura (mesmo nome e mesmos parâmetros). A anotação
> `@Override` em cima do método pede ao compilador que confira se ele realmente
> existe na classe-mãe; se você errar o nome, o código não compila. Não confunda
> com **sobrecarga** (*overload*), que é ter vários métodos com o mesmo nome e
> parâmetros diferentes.

**3. Comportamentos transversais adicionados por anotação**

`@Transactional`, `@PersistenceContext`, `@Stateless` e as anotações de Bean
Validation acrescentam comportamento (transação, injeção, pool, validação)
**sem modificar o código** das classes. O contêiner "embrulha" a classe com um
interceptador. Em Spring é o mesmo mecanismo, a AOP. Para criar uma regra de
validação nova, você adiciona uma anotação; o motor de validação não muda.

> 📘 **Contêiner, *interceptor*, *proxy* e AOP: como o "embrulho" funciona**
> - **Contêiner** (*container*): a parte do servidor de aplicação que cria,
>   configura, injeta e destrói os seus objetos, e acrescenta serviços a eles
>   (transação, segurança...). No Spring, quem faz esse papel é o
>   `ApplicationContext`. Não confunda com um contêiner Docker (um pacote
>   isolado com uma aplicação e tudo de que ela precisa para rodar, criado
>   pela ferramenta Docker), que é outra coisa.
> - **Comportamentos transversais** (*cross-cutting concerns*, "interesses que
>   atravessam"): necessidades que aparecem em muitas classes ao mesmo tempo,
>   como transação, segurança, log e validação. Se cada classe implementasse
>   isso na mão, o mesmo código se repetiria em todo lugar.
> - Para acrescentar esses comportamentos, o contêiner costuma entregar um
>   **proxy** no lugar do objeto real: quem chama o método fala com o proxy, que
>   faz algo **antes** (abrir a transação, validar), repassa a chamada ao objeto
>   real e faz algo **depois** (commit ou rollback).
> - ***Interceptor*** ("interceptador"): uma classe com código que roda "em
>   volta" das chamadas a métodos (anotada com `@AroundInvoke`, na especificação
>   *Jakarta Interceptors*). O `@Transactional` do JTA, por exemplo, é
>   implementado por um interceptor.
> - **AOP** (*Aspect-Oriented Programming*, "Programação Orientada a
>   Aspectos"): o nome geral da técnica de separar os comportamentos
>   transversais (os "aspectos") do código de negócio e aplicá-los de fora. No
>   Spring, o `@Transactional` funciona exatamente assim, por meio de proxies
>   AOP.
> - `@PersistenceContext` pede ao contêiner que **injete** um `EntityManager`
>   gerenciado por ele (injeção é explicada na seção D).

> 📘 **O que é EJB, e o `@Stateless`?**
> - **EJB** (*Enterprise JavaBeans*; hoje *Jakarta Enterprise Beans*) é a
>   especificação de componentes de negócio do Jakarta EE. Um EJB é uma classe
>   gerenciada pelo contêiner que ganha "de graça" transações, segurança,
>   controle de concorrência e um pool de instâncias.
> - `@Stateless` ("sem estado") cria um ***session bean*** ("bean de sessão", o
>   tipo de EJB que executa a lógica de negócio chamada pelos clientes) **sem
>   estado de conversa**. O contêiner mantém um ***pool*** ("reservatório") de
>   instâncias prontas e entrega uma qualquer a cada chamada, garantindo que
>   cada instância atenda uma chamada por vez. Por isso a classe não pode
>   guardar, em seus campos, dados de um cliente específico.
> - Por padrão, todo método público de um EJB roda em uma transação
>   (`REQUIRED`). No Spring, o mais parecido é um `@Service` com
>   `@Transactional`.

**4. (Passo 3) Regras e erros extensíveis**

- A regra de transição de status mora em `StatusObra.podeMudarPara()`. Uma regra
  nova (ex.: "obra planejada não pode ir direto para concluída") entra no enum,
  e o `ObraServiceBean` não muda.
- Novos tipos de erro estendem `RegraNegocioException`. Quem já trata a
  classe-mãe continua funcionando sem alteração.

> ⚠️ **Cuidado com uma leitura errada do OCP:** ele **não** diz "nunca edite uma
> classe". O `ObraDao` vai ganhar métodos novos de consulta, e isso é normal. O
> OCP vale para as **abstrações estáveis** (o `GenericDao`), que são usadas por
> muita gente e não devem mudar a cada requisito.

---

## L — Liskov Substitution Principle (Substituição de Liskov)

Sigla: **LSP**. *Substitution* = substituição.

**Definição (Barbara Liskov):** *se S é subtipo de T, objetos do tipo T podem ser
substituídos por objetos do tipo S sem quebrar o programa*.

> 📘 **Quem é Barbara Liskov, e o que é "subtipo"?**
> - Barbara Liskov é uma cientista da computação americana, professora do
>   **MIT** (*Massachusetts Institute of Technology*, "Instituto de Tecnologia
>   de Massachusetts") e vencedora do Prêmio Turing de 2008, considerado o
>   "Nobel da computação". Ela apresentou a ideia em 1987, na palestra *Data
>   Abstraction and Hierarchy*, e a formalizou com Jeannette Wing em 1994.
> - **Subtipo**: uma subclasse (`ObraDao` em relação a `GenericDao`) ou uma
>   classe que implementa uma interface (`ObraServiceBean` em relação a
>   `ObraService`). **T** é o tipo "pai"; **S** é o tipo "filho".
> - Em uma frase: **se o código funciona com o pai, tem de continuar
>   funcionando com qualquer filho.**

Na prática, a subclasse precisa honrar o **contrato** da classe-mãe:
- não pode exigir mais (pré-condições mais fortes);
- não pode entregar menos (pós-condições mais fracas);
- não pode lançar exceções inesperadas.

> 📘 **Contrato, pré-condição e pós-condição**
> Esse vocabulário vem do ***Design by Contract*** ("Projeto por Contrato"), de
> Bertrand Meyer, o mesmo do OCP.
> - **Contrato**: o acordo entre quem chama um método e quem o implementa. Diz o
>   que precisa ser verdade antes, o que será verdade depois e quais erros podem
>   acontecer.
> - **Pré-condição**: o que precisa ser verdade **antes** da chamada, ou seja,
>   o que o método **exige** de quem chama. Ex.: "o id não pode ser nulo".
> - **Pós-condição**: o que o método **garante** depois, ou seja, o que ele
>   **entrega**. Ex.: "retorna todas as entidades".
> - **"Mais forte" e "mais fraca"**: uma condição mais forte é mais exigente
>   (vale em menos situações). O subtipo pode deixar a pré-condição igual ou
>   mais fraca (exigir menos) e a pós-condição igual ou mais forte (entregar
>   mais), nunca o contrário.
>
> **Analogia:** uma transportadora promete "levar seu pacote de qualquer
> endereço de São Paulo até o destino em 2 dias". Um motorista substituto pode
> aceitar pacotes de qualquer endereço do estado (exige menos) e entregar em 1
> dia (entrega mais). Mas, se ele só aceitar pacotes do centro (exige mais) ou
> entregar em 5 dias (entrega menos), o cliente que confiava na promessa é
> prejudicado. O cliente nem sabe qual motorista veio: confia no contrato.

### Onde está aplicado

**1. O `ObraDao.listarTodos()` respeita o contrato**

O contrato do `GenericDao.listarTodos()` é *"retorna todas as entidades"*. O
`ObraDao` continua retornando **todas**, só que ordenadas por nome. Isso
**fortalece** a pós-condição, o que é permitido. Qualquer código escrito contra a
classe-mãe continua correto:

```java
GenericDao<Obra, Long> dao = obraDao;   // tratado pela classe-mãe
List<Obra> todas = dao.listarTodos();    // continua recebendo TODAS as obras ✅
```

Algumas sobrescritas que **quebrariam** o LSP:

```java
// ❌ Retorna menos do que o contrato promete (pós-condição enfraquecida)
@Override public List<Obra> listarTodos() {
    return listarPorStatus(StatusObra.EM_ANDAMENTO);
}

// ❌ Lança uma exceção que a classe-mãe não lança
@Override public List<Obra> listarTodos() {
    throw new UnsupportedOperationException();
}
```

Os dois compilam, mas quebram em produção quem confiava no contrato. Essa é a
essência do LSP: **o compilador não pega; quem garante é o design**.

> 📘 `UnsupportedOperationException` ("exceção de operação não suportada") é
> uma exceção da biblioteca padrão do Java que significa "este método existe,
> mas eu me recuso a executá-lo". **Produção** é o ambiente real, usado pelos
> usuários de verdade (em oposição aos ambientes de desenvolvimento e de teste).

**2. Os proxies do Hibernate (o caso mais interessante do projeto)**

Com o `FetchType.LAZY` do Passo 1, quando você chama `relatorio.getObra()`,
**não recebe uma `Obra`**. Recebe uma **subclasse gerada em tempo de execução**,
algo como `Obra$HibernateProxy$xyz`. O sistema inteiro trata esse objeto como uma
`Obra` normal. **Isso é LSP acontecendo de verdade**, e o código foi escrito para
não quebrá-lo:

> 📘 **`FetchType.LAZY`, Hibernate e *proxy*, com calma**
> - ***Fetch*** é "buscar". O `FetchType` diz ao JPA **quando** carregar um
>   relacionamento (aqui, a obra de um relatório). `EAGER` ("ansioso") carrega
>   junto, na mesma hora; é o padrão do `@ManyToOne`. `LAZY` ("preguiçoso") só
>   carrega quando alguém realmente usar os dados. O projeto escolheu `LAZY`
>   para que listar relatórios não dispare uma consulta extra na tabela `obra`.
> - **Hibernate** é a implementação de JPA mais usada: é a que vem no WildFly e
>   a padrão do Spring Boot. É ele quem gera os proxies.
> - Para "adiar" a busca, o Hibernate precisa entregar **alguma coisa** no
>   lugar da obra. Essa coisa é um **proxy**: um objeto que se passa pela obra.
>   Como um procurador que assina por você, ele atende pelo mesmo "nome" (é do
>   tipo `Obra`) e só vai ao banco buscar os dados de verdade quando você chama
>   um método que precisa deles, como `getNome()`. No Hibernate, `getId()` não
>   dispara essa busca, porque o proxy já nasce sabendo o ID (ele veio da coluna
>   `obra_id` do relatório).
> - Para se passar por `Obra`, o proxy é uma **subclasse** de `Obra` gerada **em
>   tempo de execução** (*runtime*: enquanto o programa roda, e não na
>   compilação). Por isso o LSP é tão importante aqui: o sistema inteiro recebe
>   uma subclasse sem saber.

| Decisão | Onde | O que quebraria sem ela |
|---|---|---|
| `equals` usa `instanceof` | `Obra.equals` | Com `getClass()`, `obra.equals(proxy)` daria `false`, porque as classes são diferentes. O proxy não seria um substituto válido. |
| `equals` usa `outra.getId()` | `Obra.equals` | Com `outra.id`, o resultado seria `null`: os **campos** do proxy ficam vazios e só os **métodos** são delegados. |
| A classe **não** é `final` | `Obra` | O Hibernate nem conseguiria criar a subclasse. |
| `toString` não inclui a obra | `RelatorioSeguranca.toString` | Um simples log lançaria `LazyInitializationException` fora da transação. |

> 📘 **Entendendo cada linha da tabela**
> - `equals` ("é igual a"): o método que define quando dois objetos são
>   **iguais em valor**. É diferente de `==`, que só diz se é **a mesma
>   instância** na memória. Na `Obra`, duas obras são iguais se têm o mesmo ID.
> - `instanceof` ("é instância de"): operador que pergunta "este objeto é deste
>   tipo **ou de um subtipo dele**?". Um proxy de `Obra` é `instanceof Obra`,
>   então o resultado é `true`.
> - `getClass()` ("obter a classe"): retorna a classe **exata** do objeto em
>   tempo de execução. Para o proxy, retorna algo como `Obra$HibernateProxy$xyz`,
>   que é diferente de `Obra`. Comparar com `getClass()` daria `false`.
> - **Campos × métodos:** o proxy é um "casco". Os campos dele (herdados de
>   `Obra`) não recebem os dados do banco; o `id` dele, por exemplo, fica
>   `null`. Já cada chamada de **método** é repassada (**delegada**) para a obra
>   real. Por isso `outra.id` lê o campo vazio do proxy, enquanto
>   `outra.getId()` passa pela lógica do proxy e devolve o valor certo.
> - `final` ("final", "definitivo"): em uma classe, a palavra-chave `final`
>   proíbe que existam subclasses dela (em um método, proíbe sobrescrevê-lo).
>   Como o proxy **é** uma subclasse, uma `Obra` `final` impediria o Hibernate
>   de criá-lo.
> - `toString`: o método que gera a representação em texto do objeto, chamado
>   automaticamente quando você o concatena com uma `String` ou o escreve no
>   **log** (o registro de mensagens que a aplicação grava enquanto roda).
> - `LazyInitializationException` ("exceção de inicialização preguiçosa"): é
>   uma exceção do Hibernate lançada quando você usa um proxy `LAZY` que ainda
>   não foi carregado depois que o **contexto de persistência** (*Persistence
>   Context*) foi fechado. O contexto de persistência é o "espaço de trabalho"
>   do `EntityManager`, que guarda as entidades carregadas e mantém o acesso ao
>   banco; neste projeto, ele vive enquanto durar a transação. Sem ele, o proxy
>   não tem como buscar os dados. Quando um caso de uso precisa da obra fora da
>   transação, a solução é carregá-la junto na própria consulta, com
>   `JOIN FETCH` (uma instrução JPQL que traz o relacionamento na mesma
>   consulta), como faz o `RelatorioSegurancaDao.listarRecentesComObra`.

Isso foi validado no teste do Passo 1: `assertEquals(obra, relatorio.getObra())`
passou com o proxy **ainda não inicializado**.

> 📘 `assertEquals(esperado, atual)` é uma verificação do **JUnit**, o
> framework de testes mais usado em Java: o teste falha se os dois valores não
> forem iguais segundo o `equals`. "Proxy ainda não inicializado" quer dizer
> que ele ainda não tinha ido ao banco buscar os dados da obra.

> ⚠️ **Limite a conhecer:** um `equals` com `instanceof` pode quebrar a simetria se
> um dia existir uma subclasse "de verdade" com o próprio `equals` (por exemplo,
> herança JPA com `ObraPublica extends Obra`). Hoje não existe; se surgir, esse
> ponto precisa ser reavaliado.

> 📘 **Simetria e herança JPA**
> - **Simetria** é uma das regras do contrato do `equals`, definido na própria
>   classe `Object` do Java: se `a.equals(b)` é `true`, então `b.equals(a)`
>   também precisa ser. As outras regras são reflexividade (`a.equals(a)`),
>   transitividade (se `a` = `b` e `b` = `c`, então `a` = `c`), consistência
>   (chamar várias vezes dá o mesmo resultado) e `a.equals(null)` ser `false`.
> - **Herança JPA** é mapear uma hierarquia de classes de entidade para o banco
>   (com a anotação `@Inheritance`), por exemplo uma `ObraPublica` que estende
>   `Obra` e tem campos próprios.

**3. (Passo 3) Hierarquia de exceções e contratos documentados**

- `EntidadeNaoEncontradaException` é uma `RegraNegocioException`: qualquer
  `catch (RegraNegocioException e)` continua funcionando, e ela herda o
  `@ApplicationException(rollback = true)`.
- As interfaces `ObraService` e `RelatorioSegurancaService` documentam o contrato
  com `@throws`. Qualquer implementação (inclusive um mock) deve honrá-lo.

> 📘 **`catch`, `@ApplicationException`, `@throws` e *mock***
> - `catch (RegraNegocioException e)` é o bloco que **captura** e trata a
>   exceção. Como `EntidadeNaoEncontradaException` é subtipo, ela também é
>   capturada ali: LSP de novo.
> - No EJB existem dois tipos de exceção:
>   - **Exceção de sistema** (*system exception*): um erro inesperado, como um
>     `NullPointerException`. O contêiner faz rollback, descarta a instância do
>     bean e entrega o erro ao cliente "embrulhado" em uma `EJBException`.
>   - **Exceção de aplicação** (*application exception*): um erro "esperado" do
>     negócio, como "já existe uma obra com esse nome". Chega ao cliente
>     exatamente como foi lançada.
>
>   As exceções *checked* (as que o compilador obriga a tratar ou declarar) já
>   são de aplicação por padrão; as *unchecked* (filhas de `RuntimeException`),
>   como a `RegraNegocioException`, só se forem marcadas com
>   `@ApplicationException`. O `rollback = true` pede que a transação seja
>   desfeita mesmo assim, porque o padrão, para exceções de aplicação, é **não**
>   desfazer. As subclasses herdam essa configuração (o atributo `inherited` da
>   anotação vale `true` por padrão).
> - `@throws` é uma marcação do **Javadoc** (o sistema de documentação do Java,
>   escrito nos comentários `/** ... */`) que lista as exceções que um método
>   pode lançar e em que situação. Faz parte do contrato.
> - ***Mock*** ("imitação"): um objeto falso, usado em testes, que finge ser
>   uma dependência real e responde o que o teste mandar. Até um mock tem de
>   honrar o contrato; senão o teste "passa" verificando um comportamento que a
>   implementação real nunca teria.

---

## I — Interface Segregation Principle (Segregação de Interfaces)

Sigla: **ISP**. *Segregation* = separação.

**Definição:** *"clientes não devem ser forçados a depender de métodos que não
usam"*. É melhor ter várias interfaces pequenas e específicas do que uma única
interface "gorda".

> 📘 **"Cliente" e interface "gorda"**
> - Aqui, **cliente** não é o usuário final: é **o código que usa** uma
>   interface (uma classe que chama outra).
> - Interface "gorda" (*fat interface*) é a que acumula métodos para todo tipo
>   de cliente, de modo que cada um usa só uma parte dela.
> - **Analogia:** entregar um controle remoto universal com 60 botões para quem
>   só precisa ligar a TV e trocar de canal. Para essa pessoa, os outros 58
>   botões só atrapalham e podem ser apertados por engano. Em código, o "aperto
>   por engano" é chamar um método que não deveria estar disponível, e o
>   "atrapalhar" é ter de mudar (ou recompilar e testar de novo) por causa de
>   métodos que você nem usa.

### Situação

**Onde já aparece (Passo 3):** em vez de um único `GestaoObrasService` com tudo,
há dois contratos separados por agregado: `ObraService` e
`RelatorioSegurancaService`. Quem só lida com relatórios não depende das
operações de obra, e vice-versa.

**✅ Onde foi aplicado de forma explícita (Passo 5): o contrato `ObraConsulta`.**
A API REST para sistemas externos só **lê** obras. Em vez de ela depender do
`ObraService` inteiro (que tem `salvar` e `excluir`), as operações de leitura
foram extraídas para uma interface própria, e o `ObraService` passou a
estendê-la:

```java
public interface ObraConsulta {                 // só leitura
    List<Obra> listarTodas();
    List<Obra> listarPorStatus(StatusObra status);
    List<Obra> pesquisarPorNome(String termo);
    List<Obra> pesquisar(String nome, StatusObra status);
    Obra buscarPorId(Long id);
}

@Local
public interface ObraService extends ObraConsulta {   // leitura + escrita
    Obra salvar(Obra obra);
    void excluir(Long id);
}
```

```java
// API REST (Passo 5): só precisa ler
@Inject
private ObraConsulta obraConsulta;

// Tela JSF (Passo 4): lê e escreve
@Inject
private ObraService obraService;
```

> 📘 **Como uma interface sem `@Local` é injetada?** O mesmo EJB
> (`ObraServiceBean`) atende as duas: o CDI aceita injetar um EJB por qualquer
> **superinterface** da sua interface `@Local`. Continua existindo uma única
> implementação.

O ganho é o **menor privilégio** (*least privilege*: cada parte do sistema só
recebe o acesso de que precisa). O `ObraResource` **não consegue** chamar
`salvar` ou `excluir`, nem por engano, porque o compilador não deixa. E qualquer
tentativa de escrita em `/api/obras/{id}` recebe **405 Method Not Allowed** do
próprio JAX-RS (comprovado pela coleção do Postman).

**Onde ainda não está aplicado:** o `GenericDao` entrega o CRUD **completo** a
todos os DAOs. Veja onde isso pode doer no nosso domínio.

Em obras, um relatório de inspeção de segurança costuma ter valor **legal e de
auditoria**: depois de registrado, não deveria ser editado. Se essa for uma regra
do negócio, o `RelatorioSegurancaDao` **herda um `atualizar()` que nunca deveria
existir para ele**. Esse é o *refused bequest* (a "herança recusada"), sintoma
clássico de violação do ISP.

> 📘 **O que é *refused bequest*?**
> *Bequest* é "legado", aquilo que alguém deixa em testamento; *refused
> bequest* é, portanto, "herança recusada". É um dos ***code smells***
> ("cheiros de código": sinais de que o design pode ter um problema, mesmo que
> o código funcione) catalogados por Martin Fowler e Kent Beck no livro
> *Refactoring* (1999). **Martin Fowler** é um autor britânico de livros
> clássicos de engenharia de software. O sintoma aparece quando uma subclasse
> herda métodos que não quer ou não deveria ter, e acaba ignorando-os ou
> sobrescrevendo-os para lançar exceção (o que, como você viu na seção L, também
> quebra o LSP).

**Por que o `ObraConsulta` só foi criado no Passo 5, e não antes:** o ISP é sobre
**clientes**. Separar interfaces sem saber quem vai usá-las é especulação, e
acabaríamos com interfaces que ninguém consome (viola o YAGNI, *"you aren't gonna
need it"*). A separação foi feita quando o cliente só de leitura (a API) passou a
existir. Pelo mesmo motivo, o `GenericDao` continua com o CRUD completo: hoje,
nenhum cliente sofre com isso.

> 📘 **YAGNI** (*"you aren't gonna need it"*, "você não vai precisar disso"):
> princípio do **XP** (*Extreme Programming*, "Programação Extrema", uma
> metodologia ágil) que diz para não construir algo antes de existir uma
> necessidade real. Código feito "para o caso de um dia precisar" custa para
> escrever, testar e manter, e muitas vezes nunca é usado, ou é usado de um jeito
> diferente do imaginado.

**Se um dia for preciso aplicar o mesmo nos DAOs** (por exemplo, se relatórios virarem registros imutáveis):

```java
public interface LeituraDao<T, ID> {
    Optional<T> buscarPorId(ID id);
    List<T> listarTodos();
}

public interface EscritaDao<T, ID> {
    void inserir(T entidade);
    T atualizar(T entidade);
    boolean removerPorId(ID id);
}
```

Cada cliente depende só do que usa. A API REST de leitura recebe um
`LeituraDao<Obra, Long>` e nem "enxerga" o `removerPorId`.

> 📘 `Optional<T>` ("opcional") é uma classe do Java (desde o Java 8) que
> representa "um valor que pode estar ausente". Retornar `Optional` em vez de
> `null` obriga quem chama a tratar explicitamente o caso "não encontrado".

> 🟢 **Paralelo com Spring:** o Spring Data é um ótimo exemplo de ISP feito pelo
> framework. A hierarquia vai de `Repository` (zero métodos) para `CrudRepository`,
> depois `ListCrudRepository`, `PagingAndSortingRepository` e por fim
> `JpaRepository`. Você pode estender só o `Repository<Obra, Long>` e declarar
> apenas o `findAll()` de que precisa.

> 📘 **Spring Data e seus *repositories***
> - **Spring Data** é o projeto do Spring que **gera a implementação** de
>   interfaces de acesso a dados: você declara a interface e ele cria, em tempo
>   de execução, a classe que a implementa.
> - `Repository` ("repositório"): interface **marcadora**, sem métodos; só diz
>   "isto é um repositório".
> - `CrudRepository`: acrescenta as operações CRUD (`save`, `findById`,
>   `findAll`, `deleteById`...), devolvendo as listas como `Iterable`.
> - `ListCrudRepository`: o mesmo, mas devolvendo `List` (existe desde o Spring
>   Data 3).
> - `PagingAndSortingRepository`: acrescenta paginação e ordenação
>   (`findAll(Pageable)`, `findAll(Sort)`).
> - `JpaRepository`: acrescenta recursos específicos do JPA, como `flush`
>   (enviar ao banco, na hora, as alterações pendentes) e operações em lote.
> - **Detalhe de precisão:** desde o Spring Data 3 (o do Spring Boot 3), a
>   hierarquia não é mais uma linha reta. A `PagingAndSortingRepository` deixou
>   de estender `CrudRepository` e estende só `Repository`, e a `JpaRepository`
>   combina `ListCrudRepository` com `ListPagingAndSortingRepository`. Ou seja,
>   o próprio Spring separou ainda mais as interfaces: ISP em ação.
> - Dá para estender só `Repository` e declarar `findAll()` porque o Spring
>   Data reconhece métodos com a mesma assinatura dos de `CrudRepository` e os
>   encaminha para a implementação padrão.

---

## D — Dependency Inversion Principle (Inversão de Dependência)

Sigla: **DIP**. *Dependency* = dependência; *Inversion* = inversão.

**Definição, em duas partes:**
- (a) módulos de alto nível (negócio) não devem depender de módulos de baixo nível
  (detalhes técnicos); ambos devem depender de **abstrações**;
- (b) abstrações não devem depender de detalhes; os detalhes é que dependem das
  abstrações.

> 📘 **Alto nível, baixo nível e "abstração"**
> - **Módulo de alto nível**: o que expressa **o que** o sistema faz, ou seja,
>   as regras de negócio ("uma obra concluída não pode mudar de status").
> - **Módulo de baixo nível**: o **como** técnico (JPA, SQL, banco de dados,
>   HTTP).
> - **Abstração**: um tipo que descreve **o que** algo faz, sem dizer **como**.
>   Em Java, normalmente uma interface.
> - **Analogia clássica:** uma luminária não é soldada direto na fiação da
>   parede; ela tem um plugue que encaixa na **tomada**. A tomada (o padrão) é a
>   abstração: a luminária depende dela, e a instalação elétrica também se
>   adapta a ela. Dá para trocar a fiação sem trocar a luminária, e vice-versa.

### ⭐ A distinção que mais cai em entrevista

Três conceitos parecidos que **não são a mesma coisa**:

| Conceito | O que é | No nosso código |
|---|---|---|
| **IoC** (Inversão de Controle) | Quem cria e gerencia os objetos é o **contêiner**, não o seu código | O WildFly cria o `ObraDao` e o `ObraServiceBean` |
| **DI** (Injeção de Dependência) | O **mecanismo** pelo qual a dependência chega pronta | `@PersistenceContext`, `@Inject`, `@EJB` |
| **DIP** (Inversão de Dependência) | Um **princípio de design** sobre a **direção** das dependências | Depender de abstrações, não de implementações |

Usar `@Inject` **não garante** DIP. Dá para injetar uma classe concreta, e aí você
tem DI sem DIP. Saber explicar isso impressiona o entrevistador.

> 📘 **As três siglas, com calma**
> - **IoC** (*Inversion of Control*, "Inversão de Controle"): no código
>   "tradicional", o seu `main` cria os objetos e decide quando chamar o quê.
>   Com IoC, o controle passa para o **framework/contêiner**: ele cria os
>   objetos e chama o seu código nos momentos certos. É o chamado "princípio de
>   Hollywood": *"don't call us, we'll call you"* ("não ligue para nós, nós
>   ligamos para você"). É também o que diferencia um framework (ele chama o
>   seu código) de uma biblioteca (o seu código chama ela).
> - **DI** (*Dependency Injection*, "Injeção de Dependência"): a forma mais
>   comum de aplicar IoC às dependências. Em vez de a classe fazer `new` daquilo
>   de que precisa, a dependência é "injetada" de fora: pelo construtor, por um
>   *setter* (método `setAlgo(...)`) ou direto no campo, como neste projeto. O
>   termo foi popularizado por Martin Fowler, em um artigo de 2004.
> - `@Inject` é a anotação de injeção do CDI, equivalente ao `@Autowired` do
>   Spring (o Spring, aliás, também aceita `@Inject`). `@EJB` injeta
>   especificamente um EJB. `@PersistenceContext` injeta um `EntityManager`.
> - **DIP** (*Dependency Inversion Principle*): o princípio desta seção. DI é a
>   **ferramenta**; DIP é a **decisão de design** sobre de quem você depende.

### Onde está aplicado

**1. Os DAOs dependem da especificação JPA, não do Hibernate**

```java
protected EntityManager em;   // jakarta.persistence.EntityManager → INTERFACE da especificação
```

Não existe **nenhum** `import org.hibernate` no código da aplicação. Quem fornece a
implementação é o servidor: Hibernate no WildFly, EclipseLink no GlassFish. É
exatamente o diagrama do DIP: o detalhe (Hibernate) implementa a abstração
(`EntityManager`), e o nosso código só conhece a abstração.

> 📘 **Especificação × implementação; `EntityManager`, EclipseLink e GlassFish**
> - O Jakarta EE é feito de **especificações**: documentos e interfaces Java
>   que dizem **o que** deve existir e como deve se comportar. O **como** é
>   escrito pelas **implementações**, de fornecedores diferentes. Lembra da
>   tomada? A especificação é o padrão da tomada; cada fabricante produz a sua
>   (as implementações), e qualquer aparelho com o plugue padrão funciona em
>   qualquer uma.
> - `EntityManager` ("gerenciador de entidades"): a interface central do JPA.
>   É por ela que você insere (`persist`), atualiza (`merge`), remove
>   (`remove`), busca (`find`) e consulta (`createQuery`). É o que o
>   `JpaRepository` do Spring usa por baixo dos panos.
> - **EclipseLink**: outra implementação de JPA, mantida pela **Eclipse
>   Foundation** (a fundação sem fins lucrativos que hoje cuida do Jakarta EE).
> - **GlassFish**: outro servidor de aplicação Jakarta EE, hoje mantido pela
>   Eclipse Foundation (originalmente criado pela Sun Microsystems). Usa o
>   EclipseLink como implementação de JPA.
> - `org.hibernate` é o pacote das classes próprias do Hibernate. Importá-lo
>   prenderia o código ao Hibernate; usando só `jakarta.persistence`, o mesmo
>   código roda em qualquer servidor.

```
ObraDao  ──depende de──►  EntityManager (interface)  ◄──implementa──  Hibernate
```

**2. O código não sabe qual é o banco** (`persistence.xml`)

O código depende de um `DataSource` obtido por **nome JNDI**, não de um driver
concreto. Trocar H2 por PostgreSQL é configuração do servidor; nenhuma classe Java
muda.

> 📘 **DataSource, JNDI, driver, H2 e PostgreSQL**
> - **`DataSource`** ("fonte de dados"): o objeto que fornece conexões com o
>   banco, normalmente a partir de um ***pool* de conexões** (um conjunto de
>   conexões abertas que são reaproveitadas, porque abrir uma conexão nova é
>   caro). No Spring Boot, é o que as propriedades `spring.datasource.*`
>   configuram.
> - **JNDI** (*Java Naming and Directory Interface*, "Interface Java de Nomes e
>   Diretórios"): um "catálogo de nomes" do servidor de aplicação. O servidor
>   registra recursos (como o `DataSource`) sob um nome, e a aplicação os pede
>   pelo nome. **Analogia:** uma lista telefônica: você procura pelo nome, sem
>   precisar saber onde a pessoa mora. Aqui o nome é
>   `java:comp/DefaultDataSource`, definido no `persistence.xml`.
> - ***Driver*** (no caso, um driver **JDBC**, de *Java Database
>   Connectivity*, a API básica do Java para falar com bancos relacionais): a
>   biblioteca específica de cada banco que traduz as chamadas JDBC para o
>   "idioma" daquele banco. Há um driver para o H2, outro para o PostgreSQL etc.
> - **H2**: um banco de dados relacional escrito em Java, leve, que pode rodar
>   **em memória** (os dados somem quando a aplicação para). Ótimo para
>   desenvolvimento e testes; no WildFly, o `DataSource` padrão é um H2 em
>   memória.
> - **PostgreSQL**: um banco de dados relacional de código aberto, robusto e
>   muito usado em produção.

**3. O DAO não cria as próprias dependências**

Não existe `new EntityManager()` nem `Persistence.createEntityManagerFactory(...)`
no código. A dependência chega injetada (DI + IoC), e é isso que **permite**
depender só da abstração.

> 📘 `Persistence.createEntityManagerFactory(...)` é a forma "manual" de
> inicializar o JPA, usada fora de um servidor de aplicação: você cria a
> fábrica (*factory*) de `EntityManager`s e passa a gerenciar sozinho a criação,
> o fechamento e as transações. Em um servidor Jakarta EE, o contêiner faz tudo
> isso por você.

**4. (Passo 3) Clientes dependem do contrato de negócio, não da implementação**

```java
@Local
public interface ObraService { ... }                       // abstração

@Stateless
public class ObraServiceBean implements ObraService { ... } // detalhe
```

O `RelatorioSegurancaServiceBean` injeta `ObraService` (a interface), e o mesmo
farão o bean JSF (Passo 4) e o recurso REST (Passo 5). Nenhum deles conhece o
`ObraServiceBean`.

> 📘 **`@Local`, `@Remote`, bean JSF e recurso REST**
> - `@Local` marca a interface como **interface de negócio local** do EJB: ela
>   pode ser chamada por código que roda na mesma **JVM** (*Java Virtual
>   Machine*, a "máquina virtual Java" que executa o programa; na prática,
>   normalmente a mesma aplicação), e os objetos são passados por referência,
>   como em uma chamada Java comum.
> - O oposto é `@Remote`: a interface pode ser chamada de **outra** JVM, pela
>   rede, e os parâmetros são **serializados** (transformados em bytes para
>   viajar) e, portanto, copiados.
> - **Bean JSF**: a classe Java por trás de uma tela JSF (também chamada
>   *backing bean*, "bean de apoio"), que guarda os dados do formulário e
>   responde aos botões. Hoje é um bean CDI com a anotação `@Named`, que dá
>   ao bean um nome pelo qual as páginas o acessam na EL.
> - **Recurso REST** (*resource*): no Jakarta EE, uma classe escrita com a
>   especificação *Jakarta RESTful Web Services* (o antigo **JAX-RS**, de *Java
>   API for RESTful Web Services*) que responde a requisições HTTP em um
>   endereço. É o equivalente ao `@RestController` do Spring.

### Onde continua parcial (entre negócio e DAO)

O `ObraServiceBean` faz:

```java
@Inject
private ObraDao obraDao;   // ← classe CONCRETA
```

No sentido estrito, isso é o negócio (alto nível) dependendo diretamente da
persistência (baixo nível). O DIP "de livro" seria:

```java
// Abstração definida pela camada de NEGÓCIO, segundo o que ELA precisa
public interface ObraRepository {
    List<Obra> listarTodos();
    void inserir(Obra obra);
    // ...
}

// O detalhe (JPA) implementa a abstração
@ApplicationScoped
public class JpaObraRepository implements ObraRepository { ... }
```

A "inversão" do nome está aqui: **a interface pertence a quem a usa (o negócio),
não a quem a implementa (a persistência)**. A seta de dependência passa a apontar
da persistência para o negócio, e não o contrário.

> 📘 **O padrão *Repository*** ("repositório"): padrão de projeto descrito por
> Martin Fowler e por Eric Evans (no DDD) em que a camada de negócio enxerga os
> dados como uma **coleção de objetos** ("me dê as obras", "guarde esta obra"),
> sem saber de SQL nem de JPA. A diferença de ênfase em relação ao DAO: o DAO
> nasce orientado à tecnologia de persistência (uma classe por tabela ou
> entidade, com operações de banco); o Repository nasce orientado ao domínio e,
> no DIP "de livro", é **definido pelo negócio**. Em muitos projetos, os dois
> nomes acabam sendo usados quase como sinônimos.

**Por que muitos projetos (Jakarta EE e Spring) aceitam a classe concreta:**
- quase sempre só existe **uma** implementação;
- o Mockito consegue fazer mock de classes concretas, então os testes não sofrem;
- os proxies do CDI funcionam com classes;
- uma interface para uma implementação única vira cerimônia.

> 📘 **Mockito, proxies do CDI e "cerimônia"**
> - **Mockito**: a biblioteca de mocks mais usada em Java
>   (`mock(ObraDao.class)`, `when(...).thenReturn(...)`). Ela cria mocks tanto
>   de interfaces quanto de classes concretas, então ter uma interface não é
>   pré-requisito para testar.
> - **Proxies do CDI**: para beans com escopo como `@ApplicationScoped`, o CDI
>   injeta um proxy (o *client proxy*) que é uma **subclasse** gerada da classe
>   do bean. Por isso funciona sem interface (e, assim como no Hibernate, exige
>   que a classe não seja `final`).
> - **Cerimônia** (*ceremony*), ou ***boilerplate*** ("código clichê",
>   repetitivo): código que você escreve porque a estrutura exige, e não porque
>   resolve um problema. Uma interface com exatamente os mesmos métodos da única
>   classe que a implementa costuma ser isso.

**Contra-argumento:** em código de negócio crítico ou de longa duração, a interface
protege contra uma troca de tecnologia (por exemplo, ler obras de um serviço
externo em vez do banco).

**Curiosidade histórica do J2EE, que costuma cair em entrevista:** no EJB 2.x, as
interfaces eram **obrigatórias** (Home, Remote e Local para cada bean). A
plataforma forçava o DIP, à custa de muito código repetitivo. O EJB 3.1
(Java EE 6) criou a *no-interface view* e as tornou opcionais.

> 📘 **J2EE, Java EE, Jakarta EE e as interfaces Home/Remote/Local**
> - **Linha do tempo dos nomes:** **J2EE** (*Java 2 Platform, Enterprise
>   Edition*) foi o nome de 1999 a 2006. A partir da versão 5 (2006), passou a
>   se chamar **Java EE** (*Java Platform, Enterprise Edition*). Em 2017, a
>   Oracle transferiu a plataforma para a Eclipse Foundation, e ela foi
>   renomeada **Jakarta EE** (nome adotado em 2018). A partir do Jakarta EE 9
>   (2020), os pacotes mudaram de `javax.*` para `jakarta.*`; por isso este
>   projeto importa `jakarta.persistence`, `jakarta.ejb` etc.
> - **EJB 2.x** (EJB 2.0, de 2001, e 2.1, de 2003, na era J2EE): para cada
>   bean, você escrevia:
>   - a interface **Home** (literalmente "casa"; na prática, a **fábrica** do
>     bean), com métodos como `create()` para obter uma instância e, nos
>     *entity beans* (o modelo de persistência da época, depois substituído
>     pelo JPA), métodos de busca como `findByPrimaryKey(...)`;
>   - a interface **Remote** (ou a **Local**, criada no EJB 2.0 para chamadas
>     dentro da mesma JVM), com os métodos de negócio que o cliente chamava;
>   - a classe do bean, implementando métodos de *callback* obrigatórios
>     (métodos que o contêiner chama em momentos do ciclo de vida, como
>     `ejbCreate`, `ejbActivate`, `ejbPassivate` e `ejbRemove`);
>   - e um descritor em XML (o `ejb-jar.xml`) declarando tudo isso.
>
>   O cliente procurava a Home no JNDI, chamava `create()` e só então usava o
>   bean. Era DIP obrigatório, mas com muito boilerplate.
> - **EJB 3.0** (Java EE 5, 2006) eliminou a Home e a obrigatoriedade do XML,
>   usando anotações. **EJB 3.1** (Java EE 6, 2009) criou a ***no-interface
>   view*** ("visão sem interface"): um `@Stateless` sem interface nenhuma
>   expõe todos os seus métodos públicos aos clientes locais. É por isso que,
>   hoje, a interface `@Local` é uma **escolha** de design (como neste
>   projeto), e não uma obrigação.
> - Curiosidade extra: o próprio padrão DAO ficou famoso pelo catálogo *Core
>   J2EE Patterns*, da Sun, dessa mesma época.

---

## Planejamento das próximas melhorias

| Passo | Princípio | O que será feito |
|---|---|---|
| 3 ✅ | D | Interfaces de negócio `@Local` (`ObraService`, `RelatorioSegurancaService`) |
| 4 ✅ | S | Tirar o texto de tela ("Em andamento") do enum e levar para o `messages.properties` |
| 5 ✅ | I | A API REST passa a depender de um contrato só de leitura (`ObraConsulta`) |

---

## Glossário

| Termo | Significado |
|---|---|
| **ACID** | Atomicidade, Consistência, Isolamento e Durabilidade: as garantias de uma transação. |
| **Agregado** (*aggregate*) | Termo do DDD: grupo de objetos de negócio tratado como uma unidade, com uma "raiz" de acesso. |
| **Anotação** (*annotation*) | Rótulo iniciado por `@` que o compilador, o servidor ou um framework leem para decidir o que fazer; sozinha, não executa nada. |
| **AOP** (*Aspect-Oriented Programming*) | Programação Orientada a Aspectos: aplicar de fora, geralmente por proxies, comportamentos transversais como transação e log. |
| **API** (*Application Programming Interface*) | Interface de Programação de Aplicações: o que um software expõe para outro usar. |
| **`@ApplicationException`** | Anotação do EJB que marca uma exceção como "de aplicação" (esperada do negócio); com `rollback = true`, desfaz a transação. |
| **`@ApplicationScoped`** | Escopo CDI de instância única para a aplicação inteira (na prática, um *singleton*). |
| **Apresentação** (camada de) | O código que conversa com quem está "do lado de fora": a tela web e os outros sistemas. |
| **Arquitetura hexagonal** (*Ports and Adapters*) | Arquitetura de Alistair Cockburn: o núcleo de negócio define portas (interfaces) e o mundo externo se conecta por adaptadores. |
| **`assertEquals`** | Verificação do JUnit: o teste falha se os dois valores não forem iguais segundo o `equals`. |
| **Backup** | Cópia de segurança dos dados. |
| **Barbara Liskov** | Cientista da computação do MIT, Prêmio Turing de 2008; formulou o princípio da substituição (o L do SOLID). |
| **Bean** | Objeto cujo ciclo de vida é gerenciado pelo contêiner, e não pelo seu código. |
| **Bean JSF** (*backing bean*) | Classe Java por trás de uma tela JSF, que guarda os dados do formulário e responde aos botões. |
| **Bean Validation** (*Jakarta Validation*) | Especificação de validação por anotações (`@NotBlank`, `@Size`...). |
| **Bertrand Meyer** | Cientista da computação francês, criador da linguagem Eiffel e do *Design by Contract*; formulou o OCP (1988). |
| **Boilerplate** / **cerimônia** | Código repetitivo, escrito porque a estrutura exige, e não porque resolve um problema. |
| **Callback** | Método que o contêiner (ou framework) chama em momentos específicos do ciclo de vida de um objeto. |
| **`catch`** | Bloco que captura e trata uma exceção. |
| **CDI** (*Contexts and Dependency Injection*) | Especificação de injeção de dependências e escopos do Jakarta EE. |
| **Checked / unchecked** | Exceções *checked* o compilador obriga a tratar ou declarar; *unchecked* (filhas de `RuntimeException`) não. |
| **Classpath** | O conjunto de pastas e bibliotecas onde o Java procura classes e recursos. |
| **Clean Architecture** | "Arquitetura Limpa", de Robert C. Martin: camadas concêntricas com o negócio no centro e dependências apontando para dentro. |
| **Cliente** (no ISP) | O código que usa uma interface, e não o usuário final. |
| **Code smell** | "Cheiro de código": sinal de que o design pode ter um problema, mesmo que o código funcione. |
| **`@Column`** | Anotação do JPA que mapeia um atributo para uma coluna (nome, se aceita nulo, tamanho). |
| **Commit** | Fim bem-sucedido de uma transação: as alterações são gravadas de vez. |
| **Contêiner** (*container*) | Parte do servidor de aplicação que cria, injeta e gerencia os objetos e lhes acrescenta serviços. Não é o contêiner Docker. |
| **Contexto de persistência** (*Persistence Context*) | O "espaço de trabalho" do `EntityManager`, que guarda as entidades carregadas; aqui, vive enquanto durar a transação. |
| **Contrato** | O que um método promete: o que exige antes, o que garante depois e quais erros pode lançar. |
| **Cross-cutting concerns** | Comportamentos transversais: necessidades que atravessam muitas classes (transação, segurança, log, validação). |
| **CRUD** (*Create, Read, Update, Delete*) | As quatro operações básicas sobre dados: criar, ler, atualizar e excluir. |
| **DAO** (*Data Access Object*) | Objeto de Acesso a Dados: a classe que lê e grava um tipo de dado no banco. |
| **DataSource** | Objeto que fornece conexões com o banco, normalmente a partir de um pool de conexões. |
| **DBA** (*Database Administrator*) | Administrador do banco de dados. |
| **DDD** (*Domain-Driven Design*) | Projeto Orientado ao Domínio, abordagem de Eric Evans (2003) centrada no modelo de negócio. |
| **Delegar** | Repassar uma chamada para outro objeto, que faz o trabalho de verdade (é o que o proxy faz). |
| **Deploy** | Implantação: instalar a aplicação no servidor. |
| **Design** | A forma como o código é dividido em classes e como elas se relacionam. |
| **Design by Contract** | "Projeto por Contrato", de Bertrand Meyer: métodos definidos por pré-condições, pós-condições e invariantes. |
| **DI** (*Dependency Injection*) | Injeção de Dependência: a dependência chega pronta de fora, em vez de a classe fazer `new`. |
| **DIP** (*Dependency Inversion Principle*) | Princípio da Inversão de Dependência: depender de abstrações; a interface pertence a quem a usa. |
| **Domínio** | Os conceitos e as regras do negócio, sem detalhes técnicos. |
| **Driver JDBC** | Biblioteca específica de cada banco que traduz as chamadas JDBC para o protocolo daquele banco. |
| **Eclipse Foundation** | Fundação sem fins lucrativos que hoje cuida do Jakarta EE. |
| **EclipseLink** | Implementação de JPA mantida pela Eclipse Foundation; usada pelo GlassFish. |
| **`@EJB`** | Anotação que injeta um EJB. |
| **EJB** (*Enterprise JavaBeans*) | Especificação de componentes de negócio do Jakarta EE (transação, pool, segurança); hoje *Jakarta Enterprise Beans*. |
| **EL** (*Expression Language*) | Minilinguagem usada nas páginas JSF (`#{...}`) para ler e escrever dados dos objetos Java. |
| **Entity** (entidade) | Classe anotada com `@Entity`, cujos objetos correspondem a linhas de uma tabela. |
| **`EntityManager`** | Interface central do JPA, usada para inserir, atualizar, remover, buscar e consultar entidades. |
| **Enum** (*enumeration*) | Tipo Java com um conjunto fixo de constantes (ex.: `StatusObra`). |
| **`equals`** | Método que define quando dois objetos são iguais em valor (diferente de `==`, que compara a instância). |
| **Escopo** (*scope*) | Por quanto tempo uma instância vive e quem a compartilha. |
| **Especificação** | Documento e interfaces que dizem o que deve existir e como deve se comportar; o "como" fica com as implementações. |
| **Exceção de aplicação / de sistema** | No EJB: a de aplicação é um erro esperado do negócio e chega ao cliente como foi lançada; a de sistema é inesperada, causa rollback e chega embrulhada em `EJBException`. |
| **`faces-config.xml`** | Arquivo de configuração do JSF (onde se registram, por exemplo, o arquivo de mensagens e os idiomas suportados). |
| **Factory** | Fábrica: objeto cuja função é criar outros objetos. |
| **Fallback** | "Plano B": o que é usado quando a primeira opção não está disponível. |
| **Fat interface** | Interface "gorda", com métodos para todo tipo de cliente. |
| **`FetchType`** (`LAZY` / `EAGER`) | Define quando o JPA carrega um relacionamento: só quando for usado (`LAZY`, "preguiçoso") ou junto, na hora (`EAGER`, "ansioso"). |
| **`final`** | Palavra-chave que, em uma classe, proíbe subclasses e, em um método, proíbe sobrescrevê-lo. |
| **`flush`** | Enviar ao banco, na hora, as alterações pendentes do contexto de persistência. |
| **Framework** | Código pronto que define a estrutura da aplicação e chama o seu código nos momentos certos. |
| **Generics** | Parâmetros de tipo entre `< >` (ex.: `GenericDao<T, ID>`). |
| **`getClass()`** | Retorna a classe exata de um objeto em tempo de execução. |
| **GlassFish** | Servidor de aplicação Jakarta EE da Eclipse Foundation (originalmente da Sun); usa o EclipseLink. |
| **H2** | Banco de dados relacional escrito em Java, que pode rodar em memória; padrão do WildFly. |
| **Herança JPA** | Mapear uma hierarquia de classes de entidade para o banco (`@Inheritance`). |
| **Hibernate** | A implementação de JPA mais usada; vem no WildFly e é a padrão do Spring Boot. |
| **Home** (interface) | No EJB 2.x, a interface-fábrica obrigatória para obter instâncias de um bean (`create()`). |
| **HTTP** (*HyperText Transfer Protocol*) | O protocolo de comunicação da web (`GET`, `POST`, `PUT`, `DELETE`...). |
| **i18n** (*internationalization*) | Internacionalização: preparar a aplicação para vários idiomas sem mudar o código ("i" + 18 letras + "n"). |
| **`@Inject`** | Anotação de injeção do CDI; equivale ao `@Autowired` do Spring. |
| **`instanceof`** | Operador que pergunta se um objeto é de um tipo ou de um subtipo dele. |
| **Interceptor** | Interceptador: código que roda "em volta" das chamadas a métodos (antes e depois). |
| **IoC** (*Inversion of Control*) | Inversão de Controle: o framework/contêiner cria os objetos e chama o seu código ("não ligue para nós, nós ligamos para você"). |
| **ISP** (*Interface Segregation Principle*) | Princípio da Segregação de Interfaces: várias interfaces pequenas em vez de uma "gorda". |
| **J2EE / Java EE / Jakarta EE** | Nomes sucessivos da plataforma Java corporativa: J2EE (1999–2006), Java EE (2006–2017) e Jakarta EE (desde 2018, na Eclipse Foundation). |
| **Javadoc** | Sistema de documentação do Java, escrito em comentários `/** ... */`. |
| **JAX-RS** (*Java API for RESTful Web Services*) | Antigo nome da especificação de serviços REST do Jakarta EE (hoje *Jakarta RESTful Web Services*). |
| **JDBC** (*Java Database Connectivity*) | API básica do Java para falar com bancos relacionais. |
| **JNDI** (*Java Naming and Directory Interface*) | "Catálogo de nomes" do servidor, onde recursos como o `DataSource` são registrados e procurados pelo nome. |
| **`JOIN FETCH`** | Instrução JPQL que traz um relacionamento na mesma consulta, contornando o `LAZY` naquele caso de uso. |
| **JPA** (*Java Persistence API*, hoje *Jakarta Persistence*) | Especificação de persistência e ORM do Jakarta EE. |
| **JPQL** (*Jakarta/Java Persistence Query Language*) | Linguagem de consulta do JPA, parecida com SQL, mas sobre classes e atributos Java. |
| **JSF** (*JavaServer Faces*, hoje *Jakarta Faces*) | Framework de telas web baseado em componentes do Jakarta EE. |
| **JSON** (*JavaScript Object Notation*) | Formato de texto para troca de dados, comum em APIs REST. |
| **JTA** (*Java Transaction API*, hoje *Jakarta Transactions*) | Especificação de transações do Jakarta EE (de onde vem o `@Transactional` usado no `GenericDao`). |
| **JUnit** | O framework de testes mais usado em Java. |
| **JVM** (*Java Virtual Machine*) | A máquina virtual que executa os programas Java. |
| **`LazyInitializationException`** | Exceção do Hibernate ao usar um proxy `LAZY` não carregado depois que o contexto de persistência foi fechado. |
| **`@Local`** | Marca a interface de negócio local de um EJB: chamada na mesma JVM, com objetos passados por referência. |
| **Locale** | Combinação de idioma e região do usuário (ex.: `pt_BR`, `en_US`). |
| **Log** | Registro de mensagens que a aplicação grava enquanto roda. |
| **LSP** (*Liskov Substitution Principle*) | Princípio da Substituição de Liskov: um subtipo deve poder substituir o tipo pai sem quebrar o programa. |
| **Mapeamento** | A tradução entre um atributo Java e uma coluna do banco (ex.: `@Column`). |
| **Mapper** | Classe que copia dados de uma representação para outra (ex.: `Obra` ⇄ `ObraEntity`). |
| **MapStruct** | Biblioteca Java que gera *mappers* automaticamente. |
| **Martin Fowler** | Autor britânico de livros clássicos de engenharia de software, como *Refactoring*; popularizou o termo "injeção de dependência". |
| **`messages.properties`** | Arquivo `chave=valor` com os textos da tela; variantes como `messages_en.properties` trazem outros idiomas. |
| **Metadados** | Dados sobre o código: descrevem a classe para quem a lê, mas não executam nada. |
| **MIT** (*Massachusetts Institute of Technology*) | Instituto de Tecnologia de Massachusetts, universidade americana onde Barbara Liskov leciona. |
| **Mock** | Objeto falso, usado em testes, que finge ser uma dependência real. |
| **Mockito** | A biblioteca de mocks mais usada em Java; faz mock de interfaces e de classes concretas. |
| **`@Named`** | Anotação do CDI que dá ao bean um nome pelo qual as páginas JSF o acessam na EL. |
| **No-interface view** | Recurso do EJB 3.1: um bean sem interface expõe seus métodos públicos aos clientes locais. |
| **`@NotBlank`** | Anotação da Bean Validation: o texto não pode ser nulo, vazio nem só espaços. |
| **OCP** (*Open/Closed Principle*) | Princípio Aberto/Fechado: aberto para extensão, fechado para modificação. |
| **`Optional`** | Classe do Java 8+ que representa um valor que pode estar ausente, evitando retornar `null`. |
| **ORM** (*Object-Relational Mapping*) | Mapeamento objeto-relacional: traduzir objetos Java em linhas de tabelas e vice-versa. |
| **Overload** (sobrecarga) | Vários métodos com o mesmo nome e parâmetros diferentes (não confundir com *override*). |
| **Override** (sobrescrever) | A subclasse redefine um método herdado, com a mesma assinatura; marcado com `@Override`. |
| **`persistence.xml`** | Arquivo de configuração do JPA: unidade de persistência, `DataSource` e propriedades da implementação. |
| **`@PersistenceContext`** | Anotação que injeta um `EntityManager` gerenciado pelo contêiner. |
| **Pool** | Conjunto de recursos prontos e reaproveitáveis (instâncias de EJB, conexões com o banco). |
| **Pós-condição** | O que um método garante depois da chamada (o que ele entrega). |
| **PostgreSQL** | Banco de dados relacional de código aberto, muito usado em produção. |
| **Pré-condição** | O que precisa ser verdade antes da chamada (o que o método exige). |
| **Produção** | O ambiente real, usado pelos usuários de verdade. |
| **Propagação** (`TxType`) | Regra que diz o que fazer quando um método transacional é chamado com ou sem uma transação em andamento (`REQUIRED`, `MANDATORY`...). |
| **Proxy** | Objeto que se passa por outro e fica entre quem chama e o objeto real (ex.: proxies LAZY do Hibernate, proxies do CDI). |
| **Recurso REST** (*resource*) | Classe que responde a requisições HTTP em um endereço (o `@RestController` do Spring). |
| **Refused bequest** | "Herança recusada": *code smell* em que a subclasse herda métodos que não quer ou não deveria ter. |
| **`@Remote`** | Interface de EJB chamável de outra JVM pela rede, com parâmetros serializados (copiados). |
| **Repository** (padrão) | Abstração que mostra os dados ao negócio como uma coleção de objetos; no DIP "de livro", é definida pela camada de negócio. |
| **Repository, CrudRepository, ListCrudRepository, PagingAndSortingRepository, JpaRepository** | Interfaces do Spring Data, das mais simples (sem métodos) às mais completas (CRUD, paginação, recursos do JPA). |
| **Resource bundle** | Mecanismo do Java que carrega textos por idioma a partir de arquivos como o `messages.properties`. |
| **REST** (*Representational State Transfer*) | Estilo de arquitetura em que recursos são acessados por URLs e métodos HTTP. |
| **Robert C. Martin ("Uncle Bob")** | Autor de *Clean Code* e *Clean Architecture*; organizou e popularizou os princípios SOLID. |
| **Rollback** | Fim com falha de uma transação: todas as alterações são desfeitas. |
| **Runtime** | Tempo de execução: enquanto o programa roda (e não na compilação). |
| **Serializar** | Transformar um objeto em bytes para gravá-lo ou enviá-lo pela rede. |
| **Service** | Classe da camada de negócio que executa os casos de uso. |
| **Session bean** | Tipo de EJB que executa a lógica de negócio chamada pelos clientes. |
| **Setter** | Método que altera o valor de um atributo (`setNome(...)`); uma das formas de injetar dependências. |
| **Simetria** (do `equals`) | Regra do `equals`: se `a.equals(b)`, então `b.equals(a)`. |
| **Singleton** | Classe da qual existe uma única instância compartilhada por toda a aplicação. |
| **SOLID** | Acrônimo dos cinco princípios de design orientado a objetos: SRP, OCP, LSP, ISP e DIP. |
| **Spring Data** | Projeto do Spring que gera a implementação de interfaces de acesso a dados (*repositories*). |
| **SQL** (*Structured Query Language*) | Linguagem padrão dos bancos de dados relacionais. |
| **SRP** (*Single Responsibility Principle*) | Princípio da Responsabilidade Única: um, e somente um, motivo (ator) para mudar. |
| **`@Stateless`** | Cria um *session bean* EJB sem estado de conversa, servido a partir de um pool de instâncias. |
| **String** | Texto, em Java (a classe `String`). |
| **Subtipo** | Subclasse, ou classe que implementa uma interface, em relação ao tipo "pai". |
| **Thymeleaf** | Biblioteca de modelos de página web mais usada com o Spring MVC. |
| **Trade-off** | Troca consciente: ganhar algo abrindo mão de outra coisa. |
| **Transação** | Grupo de operações no banco que acontece por inteiro ou não acontece. |
| **`@Transactional`** | Anotação (aqui, da JTA) que diz ao contêiner como tratar transações nas chamadas de método. |
| **`TransactionalException`** | Exceção lançada pelo `@Transactional` da JTA quando a regra de propagação é violada (ex.: `MANDATORY` sem transação). |
| **Type erasure** | "Apagamento de tipo": em Java, a informação dos generics existe só na compilação. |
| **`UnsupportedOperationException`** | Exceção padrão do Java que significa "este método existe, mas não executa esta operação". |
| **URL** (*Uniform Resource Locator*) | O endereço de um recurso na web. |
| **WildFly** | Servidor de aplicação Jakarta EE da Red Hat (antigo JBoss Application Server); usa o Hibernate. |
| **XML** (*eXtensible Markup Language*) | Formato de texto organizado em *tags*, usado em arquivos de configuração como o `persistence.xml`. |
| **XP** (*Extreme Programming*) | Programação Extrema, metodologia ágil de onde vem o YAGNI. |
| **YAGNI** (*"you aren't gonna need it"*) | "Você não vai precisar disso": não construir algo antes de haver uma necessidade real. |
