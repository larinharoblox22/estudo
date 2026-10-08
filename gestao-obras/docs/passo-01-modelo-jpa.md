# Passo 1 — O Modelo (JPA & OOP)

> 📘 **As duas siglas do título**
>
> - **JPA** (*Java Persistence API* — "API Java de Persistência"): a **especificação** Java que define como mapear objetos Java para tabelas de um banco de dados relacional e como gravá-los e lê-los. No Jakarta EE o nome oficial passou a ser *Jakarta Persistence*, mas todo mundo continua dizendo "JPA".
>   - **API** (*Application Programming Interface*, "interface de programação de aplicações"): o conjunto de interfaces, classes e anotações que você usa no seu código para conversar com uma biblioteca ou serviço.
>   - **Persistência**: fazer os dados "sobreviverem" ao fim do programa, gravando-os em um lugar durável — aqui, um banco de dados.
> - **OOP** (*Object-Oriented Programming*), em português **POO** (Programação Orientada a Objetos): o paradigma em que o programa é organizado em **objetos** que juntam dados (campos) e comportamento (métodos). Os pilares clássicos são encapsulamento, herança, polimorfismo e abstração; neste passo o destaque é o **encapsulamento** (seção 1).

## Arquivos deste passo

```
gestao-obras/
├── pom.xml
└── src/main/java/br/com/exemplo/gestaoobras/model/
    ├── StatusObra.java
    ├── Obra.java
    └── RelatorioSeguranca.java
```

> 📘 **O que é cada arquivo**
>
> - **`pom.xml`**: o arquivo de configuração do **Maven**, a ferramenta de *build* (compilar, testar e empacotar o projeto) e de gerenciamento de dependências — provavelmente a mesma que você já usa no Spring Boot. **POM** significa *Project Object Model* ("modelo de objeto do projeto"): é ali que ficam a identificação do projeto, as bibliotecas de que ele depende (*dependencies*) e o formato do pacote final. O arquivo é escrito em **XML** (*eXtensible Markup Language*), um formato de texto estruturado com *tags* no estilo `<nome>valor</nome>`.
> - **`StatusObra.java`**: um **enum** (de *enumeration*, "enumeração"): um tipo Java com um conjunto **fixo** de valores possíveis — aqui `PLANEJADA`, `EM_ANDAMENTO`, `SUSPENSA` e `CONCLUIDA`. Cada um desses valores é chamado de **constante** do enum, e o compilador não deixa você usar nenhum valor fora dessa lista.
> - **`Obra.java`** e **`RelatorioSeguranca.java`**: as duas **entidades** (*entities*) do sistema. Uma entidade é uma classe Java cujos objetos são gravados no banco: a classe corresponde a uma **tabela**, e cada objeto, a uma **linha** dessa tabela.

## 0. Contexto: J2EE → Java EE → Jakarta EE

"J2EE" é o nome histórico (1999–2006). Passou a se chamar **Java EE** (5 a 8) e,
desde que a Oracle doou a plataforma à Eclipse Foundation, se chama **Jakarta EE**.
A grande diferença prática é o pacote: `javax.persistence.*` → `jakarta.persistence.*`.

> 📘 **Decifrando as siglas**
>
> - **J2EE** = *Java 2 Platform, Enterprise Edition*. O "2" vem da marca "Java 2", usada na época (a partir do Java 1.2).
> - **EE** = *Enterprise Edition* ("edição corporativa"). Não é outra linguagem: é um **conjunto de especificações** para sistemas corporativos (aplicações web, transações, persistência, mensageria, segurança...) construído em cima do Java "comum", o **Java SE** (*Standard Edition*, "edição padrão" — a **JVM** (*Java Virtual Machine*, a máquina virtual que executa o código Java compilado), as coleções, `java.time` etc.).
> - **Java EE** = *Java Platform, Enterprise Edition*: o mesmo conjunto, renomeado em 2006 (versões 5, 6, 7 e 8).
> - **Jakarta EE**: em 2017 a Oracle transferiu o Java EE para a **Eclipse Foundation** (uma fundação de software livre sem fins lucrativos — a mesma da IDE Eclipse; *IDE*, *Integrated Development Environment*, é o "ambiente de desenvolvimento integrado", o editor onde você programa). Como a Oracle manteve os direitos sobre a marca "Java", a plataforma precisou de um nome novo. Além disso, o acordo só permitia usar o pacote `javax` *sem modificações*; para poder evoluir as especificações, o Jakarta EE 9 (2020) trocou tudo para `jakarta.*`.
> - **Pacote** (*package*): o "sobrenome" de uma classe Java (em `jakarta.persistence.Entity`, o pacote é `jakarta.persistence`). Ele organiza as classes em grupos e evita conflito de nomes; é o que aparece nos `import`.

Usamos **Jakarta EE 10**. Se na entrevista aparecer código legado com
`javax.*`, os conceitos são exatamente os mesmos — só muda o import.

> 📘 **Código legado** (*legacy code*): código antigo, mas ainda em produção, que precisa ser mantido. Em entrevistas de Java EE é comum aparecer `javax.persistence.Entity`: leia como se fosse `jakarta.persistence.Entity`. O Jakarta EE 10 foi lançado em 2022.

> **Paralelo Spring Boot:** o Spring Boot 3 também migrou para `jakarta.persistence`.
> As anotações JPA que você vê aqui são **literalmente as mesmas** que você usa no Spring.
> O que muda é *quem* fornece a implementação: no Spring Boot é o Hibernate
> embutido no JAR; no Jakarta EE é o servidor de aplicação (o WildFly traz o Hibernate,
> o Payara traz o EclipseLink). Daí o `<scope>provided</scope>` e o `<packaging>war</packaging>`.

> 📘 **Destrinchando o parágrafo acima**
>
> - **Anotação** (*annotation*): aquele `@AlgumaCoisa` colocado em cima de classes, campos ou métodos (`@Entity`, `@Id`...). É um **metadado** — uma "etiqueta" que sozinha não executa nada. Quem lê a etiqueta é um framework, durante a compilação ou em tempo de execução (usando *reflection* — "reflexão", a API do Java que permite a um programa inspecionar classes, campos e anotações enquanto roda), e então decide o que fazer. Ex.: o provedor JPA vê `@Entity` e entende "esta classe corresponde a uma tabela".
> - **Especificação × implementação**: a JPA é uma *especificação* — um documento com regras mais um conjunto de interfaces e anotações, como uma "planta" ou um "contrato". Sozinha ela não faz nada; alguém precisa *implementar* esse contrato. Esse alguém é chamado de **provedor** (*provider*) JPA. Pense na tomada padrão brasileira: a norma é uma só, mas vários fabricantes fazem plugues compatíveis com ela.
> - **Hibernate**: a implementação de JPA mais usada do mercado (é a padrão no Spring Boot e no WildFly). Ele é um **ORM** (*Object-Relational Mapping*, "mapeamento objeto-relacional"): uma ferramenta que traduz automaticamente entre o mundo dos objetos (classes, referências entre objetos) e o mundo relacional (tabelas, linhas, chaves estrangeiras), gerando o **SQL** (*Structured Query Language*, a linguagem padrão dos bancos relacionais) para você. O Hibernate surgiu antes da JPA e inspirou boa parte dela; além do que a especificação define, ele tem recursos próprios.
> - **EclipseLink**: outra implementação de JPA, mantida pela Eclipse Foundation. Historicamente foi a *implementação de referência* da especificação (a que demonstra que a especificação pode ser implementada).
> - **JAR** (*Java ARchive*, "arquivo Java"): um arquivo compactado (formato `.zip`, extensão `.jar`) com classes compiladas (`.class`) e recursos. No Spring Boot, o JAR é "gordo" (*fat JAR*): leva dentro todas as bibliotecas **e** um servidor web embutido (veja a seção 7), e roda sozinho com `java -jar`.
> - **Servidor de aplicação** (*application server*): um programa que fica rodando e **hospeda** aplicações Jakarta EE. Ele já traz prontas as implementações das especificações (persistência, gerenciamento de transações, validação, a parte web, conexões com o banco...). Analogia: um prédio comercial com luz, água, elevador e portaria funcionando — a sua aplicação é só a "mudança" que você instala numa das salas. No Spring Boot é o contrário: a aplicação carrega a própria infraestrutura dentro do JAR.
> - **WildFly**: servidor de aplicação *open source* (código aberto) mantido pela Red Hat, antigo *JBoss AS*; usa o Hibernate como implementação de JPA. É a base da versão comercial JBoss EAP.
> - **Payara**: servidor de aplicação derivado (um *fork* — uma cópia de um projeto que passa a evoluir separadamente) do **GlassFish**, que era o servidor de referência do Java EE. Usa o EclipseLink como implementação de JPA.
> - **`<scope>provided</scope>`**: no Maven, o **escopo** (*scope*) de uma dependência diz em que momentos ela é usada. `provided` ("fornecida") significa: "preciso desta biblioteca para **compilar**, mas **não** a coloque dentro do pacote final, porque o ambiente de execução (o servidor) já a fornece". Empacotar de novo algo que o servidor já tem pode causar conflitos de classes/versões. O escopo padrão, `compile`, empacota a dependência.
> - **`<packaging>war</packaging>`**: diz ao Maven para gerar um **WAR** (*Web Application Archive*, "arquivo de aplicação web"). Também é um `.zip`, mas com uma estrutura padronizada (`WEB-INF/classes`, `WEB-INF/lib`...) e que **não roda sozinho**: ele é *implantado* (*deploy*) dentro de um servidor de aplicação.

## 1. Encapsulamento

> 📘 **O que é encapsulamento?** É um dos pilares da POO: o objeto **esconde** o seu estado interno (os campos) e só permite mexer nele através de métodos que ele mesmo controla. Analogia: um caixa eletrônico — você não abre o cofre para pegar o dinheiro; usa as operações que o caixa oferece, e ele garante as regras (saldo, limite). Assim a classe consegue **proteger as suas regras** e pode mudar a implementação interna sem quebrar quem a usa.

Todos os campos são `private`; o acesso é feito por métodos. O ponto importante
não é "ter getters e setters", é **controlar quais existem**:

> 📘 **`private`, getters e setters**
>
> - **`private`**: modificador de acesso do Java — o campo só é visível dentro da própria classe.
> - **Getter** (de *get*, "obter"): método que **lê** um campo, por convenção `getNome()` (ou `isConcluida()` para `boolean`).
> - **Setter** (de *set*, "definir"): método que **altera** um campo, por convenção `setNome(String nome)`.
>
> Se não existe setter para um campo, o código de fora simplesmente não tem como alterá-lo.

| Campo | Getter | Setter | Por quê |
|---|---|---|---|
| `id` | ✔ | ✘ | Gerado pelo banco. Alterá-lo à mão corromperia a identidade da entidade. |
| `versao` | ✔ | ✘ | Gerenciado pelo JPA para concorrência otimista. |
| `dataCriacao` / `dataAtualizacao` | ✔ | ✘ | Preenchidos pelos callbacks `@PrePersist` / `@PreUpdate`. |
| `obra` (no relatório) | ✔ | ✔ com `requireNonNull` | Invariante: um relatório nunca existe sem obra. |
| `nome`, `localizacao`, `status` | ✔ | ✔ | Necessários para o binding dos formulários JSF (Passo 4). |

> 📘 **Vocabulário da tabela**
>
> - **Concorrência otimista**: *concorrência* é quando dois usuários (ou *threads* — linhas de execução que rodam em paralelo dentro do mesmo programa) mexem no mesmo dado ao mesmo tempo. Há duas estratégias para evitar que um estrague o trabalho do outro. A **pessimista** "tranca" (*lock*) o registro enquanto alguém o edita, e os outros esperam. A **otimista** supõe, "com otimismo", que conflitos são raros: não tranca nada e só **verifica na hora de gravar** se alguém alterou o registro nesse meio-tempo, usando um número de versão. Detalhes em `@Version`, na seção 2.
> - **Callback** ("chamada de volta"): um método que **você não chama**. Você só o marca (aqui com `@PrePersist`/`@PreUpdate`) e o framework o chama no momento certo — o famoso "não nos ligue, nós ligamos para você". `@PrePersist` roda logo antes do `INSERT` e `@PreUpdate` logo antes do `UPDATE`.
> - **`requireNonNull`**: `Objects.requireNonNull(obj, "mensagem")` lança `NullPointerException` com a mensagem se `obj` for `null`; caso contrário, devolve o próprio `obj`. Serve para falhar cedo, com uma mensagem clara.
> - **Invariante**: uma regra que precisa ser **sempre** verdadeira para o objeto, em qualquer momento da vida dele (aqui: "todo relatório tem uma obra").
> - **Binding** ("ligação"): associar um campo da tela a uma propriedade do objeto. Quando a página é montada, o JSF chama o **getter** para preencher o campo; quando o formulário é enviado, chama o **setter** com o valor digitado. Por isso esses campos precisam dos dois métodos.
> - **JSF** (*JavaServer Faces*, hoje chamado oficialmente de *Jakarta Faces*): o framework web do Jakarta EE, baseado em **componentes** (campos, tabelas, botões), que gera as páginas HTML no servidor. É a camada de telas deste projeto (Passo 4).

Também colocamos comportamento na entidade (`isConcluida()`), em vez de espalhar
`obra.getStatus() == StatusObra.CONCLUIDA` pelo código.

**Trade-off honesto:** o JSF (e o JAX-RS/JSON-B) exigem a convenção JavaBeans
(construtor público sem argumentos + getters/setters). Isso nos empurra para um
modelo um tanto "anêmico". Em um sistema maior, seriam usados DTOs na camada de
apresentação para proteger a entidade; aqui mantemos simples e protegemos
apenas o que é crítico.

> 📘 **Destrinchando o parágrafo acima**
>
> - **Trade-off** ("troca", "compromisso"): uma decisão em que você ganha algo abrindo mão de outra coisa. Aqui: ganhamos simplicidade e compatibilidade com os frameworks, perdemos um pouco de proteção da entidade.
> - **JAX-RS** (originalmente *Java API for RESTful Web Services*; hoje *Jakarta RESTful Web Services*): a especificação para criar APIs **REST** (*Representational State Transfer* — o estilo de API sobre HTTP, com recursos e URLs, que você faz com `@RestController` no Spring). É o equivalente Jakarta EE do Spring MVC para APIs.
> - **JSON** (*JavaScript Object Notation*): formato de texto para troca de dados, como `{"nome": "Obra A", "status": "EM_ANDAMENTO"}`.
> - **JSON-B** (*Jakarta JSON Binding*): a especificação que converte objetos Java ↔ JSON automaticamente — o papel que o Jackson faz no Spring Boot.
> - **JavaBeans**: uma **convenção** (não um framework) criada nos anos 1990 para classes "reutilizáveis por ferramentas": construtor **público sem argumentos**, propriedades privadas acessadas por getters/setters com nomes padronizados (`getX`/`isX`/`setX`) e, idealmente, `Serializable`. Os frameworks contam com isso para criar um objeto "vazio" e depois preenchê-lo pelos setters, sem conhecer a classe de antemão. Não confunda com **EJB** (*Enterprise JavaBeans*, explicado na seção 3) nem com os "beans" do Spring/CDI: o nome é parecido, o conceito é outro.
> - **Modelo anêmico** (*Anemic Domain Model*, termo popularizado por Martin Fowler): entidades que são só "sacos de dados" com getters e setters, sem comportamento, com todas as regras espalhadas em classes de serviço. É considerado um antipadrão em projetos orientados ao domínio; por isso colocamos ao menos `isConcluida()` na entidade.
> - **DTO** (*Data Transfer Object*, "objeto de transferência de dados"): um objeto simples, só com dados, feito para **transportar** informação entre camadas — por exemplo, o que a tela ou a API recebe e devolve. Usando DTOs, a tela nunca mexe diretamente na entidade e você escolhe exatamente quais campos expor.
> - **Camada de apresentação**: a parte do sistema que conversa com o usuário ou com o cliente da API (telas JSF, *endpoints* REST — os endereços/URLs que a API expõe).

> **Paralelo Spring Boot:** é igual. A diferença é que no Spring muitas equipes
> usam Lombok (`@Getter @Setter`). Em um projeto Jakarta EE "puro" é comum
> escrever à mão, o que torna explícita a decisão de *não* expor `setId()`.

> 📘 **Lombok**: biblioteca que **gera código repetitivo** (getters, setters, construtores, `equals`, `toString`...) durante a compilação, a partir de anotações como `@Getter` e `@Setter`. O código gerado não aparece no seu arquivo `.java`, mas existe no `.class` compilado.

## 2. Anotações JPA usadas

| Anotação | O que faz | Nota prática |
|---|---|---|
| `@Entity` | Marca a classe como persistível. | Precisa de construtor sem argumentos (public/protected) e não pode ser `final` (o Hibernate cria subclasses proxy). |
| `@Table(name=…, indexes=…)` | Nome da tabela e índices. | O PostgreSQL **não** cria índice automático em FKs; declaramos `idx_relatorio_obra` porque vamos filtrar relatórios por obra. |
| `@Id` + `@GeneratedValue(IDENTITY)` | Chave primária gerada pelo banco. | Veja a nota IDENTITY vs SEQUENCE abaixo. |
| `@Column(nullable, length, updatable)` | Metadados da coluna (DDL). | `updatable = false` em `data_criacao` impede que um `UPDATE` a altere. |
| `@Enumerated(EnumType.STRING)` | Grava o nome da constante (`EM_ANDAMENTO`). | **Nunca** usar `ORDINAL` (default): inserir uma constante no meio do enum desloca todos os valores gravados. |
| `@Version` | Concorrência otimista. | Cada `UPDATE` faz `WHERE versao = ?`; se outro usuário gravou antes, lança `OptimisticLockException`. Essencial em telas de edição com acesso concorrente. |
| `@ManyToOne(fetch = LAZY, optional = false)` | Relação N:1. | Veja a seção 4. |
| `@JoinColumn(name="obra_id", foreignKey=…)` | Nome da coluna FK e da constraint. | Nomes explícitos tornam os erros de banco legíveis (`fk_relatorio_obra` em vez de `FKa3b9…`). |
| `@PrePersist` / `@PreUpdate` | Callbacks do ciclo de vida. | Auditoria automática. |

> 📘 **Vocabulário da tabela, linha por linha**
>
> **`@Entity`**
> - **Persistível**: que pode ser gravado no banco pelo JPA.
> - **Construtor sem argumentos**: ao ler uma linha do banco, o provedor primeiro cria um objeto "vazio" com esse construtor e depois preenche os campos com os valores lidos. Ele pode ser `protected` (não precisa ser público) justamente para que o *seu* código não o use por engano.
> - **`final`**: palavra-chave do Java que, numa classe, **proíbe** que alguém crie subclasses dela.
> - ***Proxy*** ("procurador", "substituto"): um objeto que **se passa pelo objeto real** e controla o acesso a ele. O Hibernate cria, em tempo de execução, uma **subclasse** de `Obra` que fica no lugar da obra verdadeira e só busca os dados no banco quando alguém realmente precisa deles (detalhes na seção 4). Se a classe fosse `final`, essa subclasse não poderia existir.
>
> **`@Table`**
> - **Índice** (*index*): uma estrutura auxiliar que o banco mantém (normalmente uma árvore B, a *B-tree*) para encontrar linhas rapidamente sem ler a tabela inteira — como o índice remissivo no fim de um livro. O custo: ocupa espaço e deixa `INSERT`/`UPDATE` um pouco mais lentos, porque o índice também precisa ser atualizado.
> - **PostgreSQL**: banco de dados relacional de código aberto, muito usado em produção.
> - **FK** (*Foreign Key*, "chave estrangeira"): uma coluna que guarda a chave primária de **outra** tabela, criando o vínculo entre elas (`relatorio_seguranca.obra_id` → `obra.id`). O banco garante que o valor sempre aponte para uma linha que existe. O PostgreSQL cria índice automaticamente para a chave primária e para colunas `UNIQUE`, mas **não** para FKs — por isso declaramos o nosso.
>
> **`@Id` + `@GeneratedValue`**
> - **Chave primária**, ou **PK** (*Primary Key*): a coluna (ou conjunto de colunas) que identifica **unicamente** cada linha da tabela. `@GeneratedValue` ("valor gerado") diz que você não informa o valor: ele é gerado automaticamente. `IDENTITY` e `SEQUENCE` são duas formas de gerar, explicadas logo abaixo.
>
> **`@Column`**
> - **DDL** (*Data Definition Language*, "linguagem de definição de dados"): a parte do SQL que **define a estrutura** do banco — `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`. A outra parte é a **DML** (*Data Manipulation Language*), que mexe nos **dados**: `SELECT` (consultar), `INSERT` (inserir), `UPDATE` (alterar) e `DELETE` (excluir).
> - `nullable` ("aceita nulo?") e `length` (tamanho máximo do texto, que vira `varchar(150)`, por exemplo) servem principalmente para quando o JPA **gera o esquema** do banco — neste projeto o `persistence.xml` manda recriar as tabelas a cada implantação (`drop-and-create`). Já `updatable = false` ("pode ser alterada?") vale também em tempo de execução: a coluna é simplesmente deixada de fora dos `UPDATE` que o Hibernate gera.
>
> **`@Enumerated`**
> - **Default** ("padrão"): o valor usado quando você não especifica nada.
> - **`ORDINAL`** ("ordinal", ou seja, a posição): grava a **posição** da constante no enum, começando em 0 (`PLANEJADA`=0, `EM_ANDAMENTO`=1, `SUSPENSA`=2...). Se alguém inserir uma constante nova no meio, `SUSPENSA` passa a ser 3, e todas as linhas antigas gravadas com 2 passam a "significar" outra coisa — sem nenhum erro, silenciosamente.
> - **`STRING`**: grava o **nome** da constante (`"EM_ANDAMENTO"`), que não depende da ordem. (O único cuidado é não renomear a constante sem migrar os dados.)
>
> **`@Version`**
> - Como funciona: a tabela tem uma coluna `versao`. Você lê a obra com `versao = 3`. Ao gravar, o Hibernate gera algo como `UPDATE obra SET ..., versao = 4 WHERE id = ? AND versao = 3`. Se outro usuário gravou antes, a versão no banco já é 4, o `WHERE` não encontra nenhuma linha (0 linhas afetadas) e o Hibernate lança **`OptimisticLockException`** ("exceção de trava otimista") em vez de sobrescrever.
> - O problema evitado se chama **lost update** ("atualização perdida"): dois usuários abrem o mesmo registro, os dois alteram, e o segundo a salvar apaga sem saber a alteração do primeiro.
>
> **`@ManyToOne` e `@JoinColumn`**
> - **Relação N:1** (*many-to-one*, "muitos-para-um"): **muitos** relatórios pertencem a **uma** obra.
> - **LAZY** ("preguiçoso"): a obra só é carregada do banco quando for realmente usada — explicado na seção 4.
> - *Join column* ("coluna de junção"): a coluna FK usada para "juntar" as duas tabelas.
> - **Constraint** ("restrição"): uma regra que o **próprio banco** impõe, recusando dados que a violem (`NOT NULL`, `UNIQUE`, FK...). Toda constraint tem um nome; se você não der um, o Hibernate gera algo aleatório como `FKa3b9…`, que não diz nada quando aparece numa mensagem de erro.
>
> **`@PrePersist` / `@PreUpdate`**
> - **Ciclo de vida** (*lifecycle*): os estados pelos quais a entidade passa (seção 3).
> - **Auditoria**: registrar automaticamente **quando** (e, em sistemas maiores, **por quem**) cada registro foi criado ou alterado.

**Bean Validation (`@NotBlank`, `@Size`, `@NotNull`, `@PastOrPresent`)** não é JPA,
é outra especificação (Jakarta Validation), mas se integra automaticamente com
três camadas: o **JSF** valida ao submeter o formulário, o **JPA** valida antes do
`INSERT/UPDATE`, e o **JAX-RS** valida o JSON recebido. Uma regra, três pontos
de aplicação — reutilização real.

> 📘 **O que é Bean Validation?** É a especificação de **validação declarativa**: em vez de escrever `if (nome == null || nome.isBlank()) throw ...` em vários lugares, você **declara** a regra uma vez, como anotação no campo, e um validador verifica o objeto quando pedido. *Bean Validation* é o nome histórico; **Jakarta Validation** é o nome atual. A implementação mais usada é o **Hibernate Validator** (projeto da mesma comunidade do Hibernate, mas independente da JPA — é o que vem no `spring-boot-starter-validation`).
>
> As anotações usadas aqui:
> - `@NotNull`: o valor não pode ser `null`.
> - `@NotBlank`: o texto não pode ser `null` e precisa ter pelo menos um caractere que não seja espaço (`""` e `"   "` são rejeitados).
> - `@Size(max = 150)`: o tamanho do texto (ou da coleção) precisa estar dentro dos limites.
> - `@PastOrPresent`: a data precisa estar no passado ou ser a data atual (não pode ser futura).

Repare que a obrigatoriedade da obra está declarada três vezes, de propósito:
- `@NotNull` → falha cedo, na aplicação, com mensagem amigável;
- `optional = false` → o Hibernate pode usar `INNER JOIN` e otimizar;
- `nullable = false` → constraint `NOT NULL` no banco, a última linha de defesa.

> 📘 **`INNER JOIN` e `NOT NULL`**
>
> - Um **JOIN** ("junção") combina linhas de duas tabelas relacionadas numa mesma consulta. O **`INNER JOIN`** só traz as combinações em que existe correspondência dos **dois** lados; o **`LEFT JOIN`** (ou *left outer join*) traz **todas** as linhas da tabela da esquerda, mesmo sem correspondência, preenchendo as colunas do outro lado com `NULL`. Se o Hibernate sabe que todo relatório **sempre** tem obra (`optional = false`), os dois dariam o mesmo resultado — então ele pode usar o `INNER JOIN`, que é mais simples e dá mais liberdade ao otimizador do banco.
> - **`NOT NULL`**: constraint que proíbe valor nulo na coluna. Mesmo que alguém insira dados por fora da aplicação (um script SQL, outro sistema), o banco recusa.

**IDENTITY vs SEQUENCE:** escolhemos `IDENTITY` por simplicidade e porque
funciona igual no H2 e no PostgreSQL. Contudo, com `IDENTITY` o Hibernate precisa
executar o `INSERT` imediatamente para saber o ID, o que **desativa o batching
JDBC**. No PostgreSQL com cargas grandes, `GenerationType.SEQUENCE` com
`allocationSize` é a escolha mais performática. Boa resposta de entrevista.

> 📘 **Entendendo IDENTITY, SEQUENCE, H2, JDBC, batching e allocationSize**
>
> - **`IDENTITY`**: a coluna `id` é autoincrementada **pelo próprio banco** no momento do `INSERT` (no PostgreSQL, uma coluna `GENERATED ... AS IDENTITY` ou `serial`; no MySQL, `AUTO_INCREMENT`). Consequência: o ID só passa a existir **depois** que o `INSERT` é executado.
> - **`SEQUENCE`**: uma *sequence* ("sequência") é um objeto separado no banco, um "contador" que entrega o próximo número quando você pede (no PostgreSQL, `nextval('nome_da_sequence')`). O Hibernate pode pedir o ID **antes** do `INSERT`, guardar o objeto em memória e deixar o `INSERT` para depois.
> - **H2**: banco de dados relacional escrito em Java que pode rodar **em memória**, dentro da própria aplicação — ótimo para desenvolvimento e testes. Neste projeto, o DataSource padrão do servidor é um H2 em memória (veja o `persistence.xml`).
> - **JDBC** (*Java Database Connectivity*): a API de **baixo nível** do Java para falar com bancos de dados: abrir conexão, enviar SQL, ler resultados. JPA e Hibernate usam JDBC por baixo dos panos — é a "camada de transporte".
> - **Batching** (de *batch*, "lote"): em vez de mandar 1.000 `INSERT`s em 1.000 idas e voltas (*round trips*) pela rede até o banco, o JDBC agrupa vários comandos e os envia de uma vez. Com `IDENTITY`, o Hibernate precisa executar cada `INSERT` na hora para descobrir o ID, então não consegue acumulá-los em lote. Analogia: ir ao mercado uma vez com a lista inteira vs. fazer uma viagem por item. (Observação: no Hibernate, o batching também precisa ser ligado na configuração, com a propriedade `hibernate.jdbc.batch_size`; por padrão ele vem desligado.)
> - **`allocationSize`** (atributo de `@SequenceGenerator`, padrão = 50): quantos IDs o Hibernate **reserva** a cada consulta à sequence. Com 50, ele faz um único `nextval` e já tem 50 IDs para distribuir em memória — muito menos idas ao banco. Esse valor precisa ser coerente com o incremento (`INCREMENT BY`) configurado na sequence do banco.

> **Paralelo Spring Boot:** anotações idênticas. A auditoria que no Spring você faria com
> `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` (Spring Data)
> é feita aqui com os callbacks padrão `@PrePersist`/`@PreUpdate` — que também
> funcionam no Spring, porque são JPA puro. A validação que no Spring exige
> `spring-boot-starter-validation` + `@Valid` já vem incluída em qualquer
> servidor Jakarta EE.

> 📘 **Termos do paralelo**
>
> - **Spring Data**: família de projetos do Spring que simplifica o acesso a dados. O **Spring Data JPA** gera a implementação de repositórios a partir de interfaces (`JpaRepository`) e oferece auditoria pronta (`@CreatedDate`, `@LastModifiedDate`, ativada com `@EnableJpaAuditing`).
> - **`@EntityListeners`**: anotação da **própria JPA** que registra uma classe externa — um *listener* ("ouvinte") — cujos callbacks são chamados nos eventos do ciclo de vida da entidade. `AuditingEntityListener` é o *listener* que o Spring Data fornece.
> - **Starter** ("dependência de partida"): no Spring Boot, uma dependência "guarda-chuva" que traz de uma vez um conjunto coerente de bibliotecas; a auto-configuração do Spring Boot detecta a presença delas e configura tudo. Ex.: `spring-boot-starter-validation` traz o Hibernate Validator.
> - **`@Valid`**: anotação da própria Jakarta Validation que diz "valide este objeto" — por exemplo, num parâmetro `@RequestBody @Valid ObraDto dto` de um controller.

## 3. Ciclo de vida da entidade

```
            new Obra()
                │
                ▼
        ┌──────────────┐   persist()    ┌──────────────┐   remove()   ┌──────────────┐
        │ NEW/TRANSIENT│ ─────────────► │   MANAGED    │ ───────────► │   REMOVED    │
        └──────────────┘                └──────────────┘              └──────────────┘
                                         ▲          │
                                 merge() │          │ fim da transação / clear() / close()
                                         │          ▼
                                        ┌──────────────┐
                                        │   DETACHED   │
                                        └──────────────┘
```

> 📘 **Vocabulário do diagrama**
>
> **Ciclo de vida** (*lifecycle*): os estados pelos quais um objeto entidade passa **do ponto de vista do JPA** — se o JPA o conhece e acompanha, e o que vai acontecer com ele no banco.
>
> Os estados, traduzidos:
> - **NEW / TRANSIENT** — "novo" / "transitório" (passageiro: só existe na memória e some quando o programa termina). ⚠️ Não confunda com a palavra-chave `transient` do Java (que exclui um campo da serialização) nem com a anotação `@Transient` do JPA (que exclui um campo da persistência).
> - **MANAGED** — "gerenciado": o JPA está acompanhando o objeto.
> - **DETACHED** — "desanexado", "destacado": o objeto já foi gerenciado, mas não é mais.
> - **REMOVED** — "removido": marcado para exclusão.
>
> Os métodos (todos do `EntityManager`, explicado logo abaixo):
> - **`persist(obj)`** ("persistir"): torna gerenciado um objeto **novo** e agenda o `INSERT`.
> - **`remove(obj)`** ("remover"): marca uma entidade gerenciada para exclusão.
> - **`merge(obj)`** ("mesclar"): copia o estado de um objeto *detached* para uma instância **gerenciada** (buscando-a no banco se for preciso) e **devolve essa instância gerenciada**. Atenção: o objeto que você passou continua *detached*; quem passa a valer é o retorno.
> - **`clear()`** ("limpar"): esvazia o Persistence Context — todas as entidades gerenciadas viram *detached*.
> - **`close()`** ("fechar"): fecha o `EntityManager`; tudo o que ele gerenciava vira *detached*.
>
> **Transação**: um grupo de operações no banco tratado como uma unidade **"tudo ou nada"**: ou todas são confirmadas, ou nenhuma. Exemplo clássico: transferir dinheiro = debitar de uma conta **e** creditar na outra; não pode acontecer só a metade. Confirmar a transação chama-se **commit**; desfazer tudo chama-se **rollback**. Em Jakarta EE, normalmente quem abre e fecha as transações é o próprio servidor, de forma automática (veja "EJB" mais abaixo).
>
> **Analogia do hotel:** *New* é uma pessoa na rua — o hotel não sabe que ela existe. *Managed* é o hóspede com check-in feito: o hotel anota tudo o que ele consome e lança na conta no check-out (o commit). *Detached* é quem já fez check-out mas ainda guarda o número do quarto (o ID): o que ele consumir lá fora não entra na conta; para isso, precisa fazer check-in de novo (o `merge`). *Removed* é a reserva cancelada, que só se efetiva no fechamento.

- **New/Transient:** objeto Java normal; o JPA não o conhece; `id == null`.
- **Managed:** está no *Persistence Context* (o "cache de 1º nível" do
  `EntityManager`). Qualquer alteração feita por um setter é detectada (**dirty checking**)
  e convertida em um `UPDATE` no commit — sem chamar nenhum `save()`.
- **Detached:** tem ID, mas o `EntityManager` que o gerenciou já fechou. Alterações
  **não** são gravadas; é preciso `merge()` para reanexá-lo.
- **Removed:** agendado para `DELETE` no commit.

> 📘 **O que são o `EntityManager` e o Persistence Context?**
>
> O **`EntityManager`** ("gerenciador de entidades") é a interface principal da JPA: é por ele que você faz `persist`, `find`, `merge`, `remove` e consultas. No Hibernate, a versão "nativa" dele se chama `Session` (no Hibernate moderno, `Session` inclusive estende `EntityManager`) — guarde isso para entender o nome "Open Session In View" na seção 4.
>
> Cada `EntityManager` tem um **Persistence Context** ("contexto de persistência"): o conjunto, em memória, das entidades que ele está gerenciando naquele momento, com no máximo **uma instância Java por linha do banco**. Pense nele como a "mesa de trabalho" do `EntityManager`. Ele funciona como:
>
> - **Cache de 1º nível**: *cache* é uma memória de acesso rápido que guarda dados já buscados para não buscá-los de novo. Se você chamar `em.find(Obra.class, 1L)` duas vezes no mesmo contexto, só a primeira chamada vai ao banco; a segunda devolve **o mesmo objeto** que já está na memória. É "de 1º nível" porque existe também um cache de 2º nível, opcional, compartilhado entre vários `EntityManager`s.
> - **Rastreador de mudanças** — o **dirty checking** ("verificação de sujeira"): ao carregar uma entidade, o Hibernate guarda uma "fotografia" (*snapshot*) do estado original dela. No momento do **flush**, ele compara cada entidade gerenciada com a sua fotografia; as que mudaram estão "sujas" (*dirty*) e geram um `UPDATE` automaticamente.
>
> **Flush** ("descarga") é o momento em que o Hibernate **sincroniza** a memória com o banco, enviando os SQLs pendentes. Ele acontece automaticamente logo antes do commit e, por padrão, antes de consultas que possam depender das alterações (ou quando você chama `em.flush()`). Cuidado: **flush ≠ commit**. O flush *envia* os comandos dentro da transação, mas eles ainda podem ser desfeitos por um rollback; só o commit os torna permanentes. Por isso o texto acima diz "no commit": na prática, o `UPDATE` sai no flush que o commit dispara.
>
> `save()` é um método do Spring Data, não da JPA — por isso o texto destaca que aqui ele nem existe.

**Por que isso importa no nosso projeto:** no Passo 4, a `Obra` em edição
vive em um bean JSF `@ViewScoped` durante várias requisições HTTP. Entre requisições, a
transação do EJB já terminou, logo a entidade está **Detached**. Gravar exige
`merge()` — e é o `@Version` que garante que não vamos sobrescrever alterações de outro
usuário feitas nesse meio-tempo.

> 📘 **Destrinchando o parágrafo acima**
>
> - **Bean** (aqui): um objeto cuja criação e tempo de vida são gerenciados por um **contêiner** (*container* — a parte do servidor que cria, injeta e destrói os seus objetos, como o `ApplicationContext` do Spring). Os beans JSF modernos são gerenciados pelo **CDI** (*Contexts and Dependency Injection*, "contextos e injeção de dependências"), o mecanismo de injeção de dependências do Jakarta EE — o equivalente ao contêiner do Spring, com `@Inject` no lugar de `@Autowired`.
> - **`@ViewScoped`**: define o **escopo** (*scope*, o "tempo de vida") do bean como a *view*, ou seja, a página: o bean vive enquanto o usuário permanece **na mesma página**, sobrevivendo às várias requisições feitas a partir dela (cliques, envios de formulário), e é descartado quando ele navega para outra página.
> - **HTTP** (*HyperText Transfer Protocol*): o protocolo da web. Uma **requisição** (*request*) é cada pedido que o navegador faz ao servidor; cada uma é tratada de forma independente.
> - **EJB** (*Enterprise JavaBeans*): componentes de negócio gerenciados pelo servidor de aplicação (neste projeto, as classes `@Stateless` do Passo 3). O ponto que importa aqui: por padrão, cada chamada a um método público de um EJB roda **dentro de uma transação** que o contêiner abre na entrada (ou reaproveita, se já houver uma) e confirma com commit na saída — parecido com colocar `@Transactional` em um `@Service` do Spring. Quando o método retorna, a transação termina, o Persistence Context fecha e as entidades devolvidas viram *detached*.

Os callbacks disponíveis acompanham as transições: `@PrePersist`, `@PostPersist`,
`@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad`.

> 📘 **Lendo os nomes dos callbacks:** *Pre* = antes; *Post* = depois; *Persist* = inserir; *Update* = atualizar; *Remove* = excluir; *Load* = carregar do banco. Ex.: `@PostLoad` roda logo depois que a entidade é lida do banco; `@PreRemove`, logo antes de ela ser excluída.

> **Paralelo Spring Boot:** o ciclo de vida é o mesmo (é JPA). A diferença é que no
> Spring você chama `repository.save()`, que por baixo decide entre `persist()` (ID nulo)
> e `merge()` (ID preenchido) — e esconde o conceito de você. No Jakarta EE você vai usar o
> `EntityManager` diretamente (Passo 2), por isso precisa saber o estado da entidade.

## 4. Por que `FetchType.LAZY` na relação

> 📘 **FetchType, EAGER e LAZY**
>
> **`FetchType`** ("tipo de busca") define **quando** o JPA carrega uma associação, isto é, o objeto (ou a coleção) relacionado:
> - **EAGER** ("ansioso", "apressado"): carrega **junto**, imediatamente, sempre que a entidade principal for carregada.
> - **LAZY** ("preguiçoso"): só carrega **quando alguém realmente acessar** a associação.
>
> Analogia: EAGER é o garçom que traz todos os pratos do cardápio de uma vez, "vai que você queira"; LAZY só traz o prato quando você pede.

**Armadilha clássica de entrevista:** o default de `@ManyToOne` e `@OneToOne` na
especificação JPA é **EAGER**. Só `@OneToMany` e `@ManyToMany` são LAZY por
padrão. Por isso precisamos declarar `LAZY` explicitamente.

> 📘 **Os quatro tipos de relação da JPA**
>
> - `@OneToOne` — **1:1**, "um-para-um" (ex.: pessoa ↔ passaporte).
> - `@ManyToOne` — **N:1**, "muitos-para-um" (ex.: muitos relatórios → uma obra).
> - `@OneToMany` — **1:N**, "um-para-muitos" (o mesmo vínculo visto do outro lado: uma obra → muitos relatórios).
> - `@ManyToMany` — **N:N**, "muitos-para-muitos" (ex.: alunos ↔ disciplinas; no banco exige uma tabela intermediária).
>
> Regra para decorar: as relações que terminam em **"One"** (um objeto só) são EAGER por padrão; as que terminam em **"Many"** (coleções) são LAZY.

**Com EAGER**, cada relatório carregado traz a obra junto. Em uma query JPQL como
`SELECT r FROM RelatorioSeguranca r` que retorne 500 relatórios de 50 obras, o
Hibernate executa 1 query para os relatórios + até 50 queries extras para as obras:
o famoso **problema N+1**. E um EAGER declarado no mapeamento é difícil de
"desligar" em uma query específica, enquanto um LAZY se "liga" facilmente com `JOIN FETCH`.

> 📘 **Query, JPQL, N+1 e JOIN FETCH**
>
> - **Query** = consulta ao banco.
> - **JPQL** (*Java Persistence Query Language*; hoje *Jakarta Persistence Query Language*): a linguagem de consulta da JPA. Parece SQL, mas fala de **entidades e atributos Java** (`RelatorioSeguranca r`, `r.obra.id`), não de tabelas e colunas; o provedor traduz para o SQL do banco que estiver em uso.
> - **Problema N+1**, passo a passo: **1** consulta traz a lista de N relatórios. Depois, para cada relatório cuja obra ainda não foi carregada, o Hibernate dispara **mais uma** consulta para buscá-la — até **N** consultas extras. No exemplo, como relatórios da mesma obra reaproveitam a obra já carregada no Persistence Context, são "só" 1 + 50. Cada consulta é uma ida e volta ao banco; com listas grandes, a tela fica lenta. O pior: **o código parece inocente** — o problema só aparece no log de SQL.
> - **`JOIN FETCH`**: cláusula da JPQL que diz "traga também esta associação **na mesma consulta**", gerando um JOIN no SQL. Ex.: `SELECT r FROM RelatorioSeguranca r JOIN FETCH r.obra` → **uma** consulta só, em vez de N+1.

**Com LAZY**, o campo `obra` é preenchido com um **proxy** (subclasse gerada em
runtime) que só contém o ID. O `SELECT` na tabela `obra` só acontece quando se
chama um método como `getNome()`. Validamos isso: ao executar
`em.find(RelatorioSeguranca.class, id)` o SQL gerado foi apenas

```sql
select rs1_0.id, rs1_0.data_inspecao, rs1_0.descricao, rs1_0.obra_id
from relatorio_seguranca rs1_0 where rs1_0.id = ?
```

sem nenhum join com a `obra`.

> 📘 **Como o proxy funciona na prática**
>
> - **Runtime** ("tempo de execução"): enquanto o programa está rodando — em oposição a *compile time*, o tempo de compilação. A classe do proxy não existe no seu código-fonte: o Hibernate a gera dinamicamente.
> - O proxy é como um **"cartão de visita" da obra**: ele sabe o ID (que já veio na coluna `obra_id` do relatório) e, quando você pede qualquer outra informação (`getNome()`), vai ao banco buscar a obra de verdade. Isso se chama **inicializar** o proxy. Chamar `getId()` normalmente **não** dispara a consulta, porque o ID já está no proxy.
> - `em.find(Classe.class, id)`: `em` é o `EntityManager`; `find` busca uma entidade pela chave primária (consultando primeiro o Persistence Context).
> - No SQL acima, `rs1_0` é só um **alias** (apelido) que o Hibernate 6 gera para a tabela. Repare que `obra_id` é lido — é dele que o proxy tira o ID —, mas nenhuma coluna da tabela `obra` aparece.

**LAZY é o default seguro; quando precisarmos da obra, nós a pedimos
explicitamente** com `JOIN FETCH` na query (Passo 2). Assim cada caso de uso
carrega exatamente o que precisa — é isso que escala.

> 📘 **Escalar** (*to scale*): continuar com bom desempenho quando o volume de dados ou de usuários cresce.

**O custo:** acessar o proxy depois que a transação terminou lança
`LazyInitializationException`. Por isso o `toString()` do relatório **não**
inclui a obra: um simples `log.info(relatorio)` fora da transação quebraria.

> 📘 **`LazyInitializationException`, `toString()` e log**
>
> - **`LazyInitializationException`** ("exceção de inicialização preguiçosa"): lançada pelo Hibernate quando você tenta inicializar um proxy (ou uma coleção LAZY) e o Persistence Context que o criou **já foi fechado** — não há mais sessão aberta para ir buscar os dados. É uma exceção **do Hibernate**: a especificação JPA não define um comportamento único para essa situação, e outros provedores, como o EclipseLink, podem agir de forma diferente.
> - **`toString()`**: o método que gera a representação em texto do objeto. Ele é chamado **implicitamente** em concatenações de `String` e quando você passa o objeto para um logger — por isso é tão fácil disparar um acesso ao proxy sem perceber.
> - **Log** ("registro", "diário de bordo"): as mensagens que a aplicação grava para acompanhar o que está acontecendo; `log.info(...)` grava uma mensagem de nível informativo.

> **Paralelo Spring Boot:** o Spring Boot ativa por padrão o **Open Session In View**
> (`spring.jpa.open-in-view=true`), que mantém a sessão aberta até a view ser
> renderizada, escondendo a `LazyInitializationException` (e gerando queries
> ocultas). No Jakarta EE **não há OSIV por padrão**: o contexto de persistência
> fecha quando a transação do EJB termina. Isso obriga a uma disciplina melhor:
> carregar o que a view precisa dentro da camada de negócio.

> 📘 **O que é Open Session In View (OSIV)?**
>
> **OSIV** (*Open Session In View*, "sessão aberta na view") é um padrão em que a **sessão** do Hibernate (o `EntityManager`) fica aberta durante **toda** a requisição HTTP, até a resposta ser montada — e não só durante o método de serviço transacional. **View** ("visão") é a camada que gera a resposta para o usuário: uma página HTML ou o JSON devolvido por um `@RestController`. *Renderizar* é montar essa resposta.
>
> - **Vantagem:** acessos LAZY feitos na view "simplesmente funcionam".
> - **Desvantagens:** as consultas disparadas na view ficam **escondidas** (acontecem fora da camada de serviço, longe de onde você planejou o acesso a dados) e podem gerar N+1 sem ninguém perceber; além disso, a camada de negócio deixa de ser responsável por decidir o que carrega. Por isso o próprio Spring Boot escreve um aviso (*warning*) no log ao iniciar quando você não configura essa propriedade explicitamente.
>
> **Camada de negócio**: a parte do sistema onde ficam as regras de negócio e onde as transações começam e terminam — no Spring, os `@Service`; aqui, os EJBs do Passo 3.

## 5. Relação unidirecional (sem `@OneToMany` em `Obra`)

Mapeamos apenas o lado `@ManyToOne`. Não colocamos `List<RelatorioSeguranca>` na
`Obra` porque:
- uma obra pode acumular milhares de relatórios ao longo dos anos; uma coleção
  mapeada convida a carregá-los todos sem querer;
- "relatórios da obra X" é uma query (`WHERE r.obra.id = :id`), com paginação,
  que faremos no DAO;
- uma relação bidirecional exige manter os dois lados sincronizados e cuidado
  com ciclos em `toString`/`equals`/serialização JSON.

> 📘 **Destrinchando a lista acima**
>
> - **Unidirecional** ("de uma direção só"): só dá para navegar em um sentido — de `RelatorioSeguranca` para `Obra` (`relatorio.getObra()`), mas não de `Obra` para os seus relatórios.
> - **Bidirecional** ("de duas direções"): os dois lados se referenciam — existiria também `obra.getRelatorios()`.
> - **Coleção mapeada**: uma `List` (ou `Set`) anotada com `@OneToMany`, que o JPA preencheria a partir do banco.
> - `:id` na JPQL é um **parâmetro nomeado**: um "buraco" na consulta preenchido depois com `setParameter("id", valor)` — nunca concatenando texto. Isso também protege contra *SQL injection* ("injeção de SQL"): um ataque em que um texto malicioso digitado pelo usuário, se fosse concatenado na consulta, viraria parte do próprio comando.
> - **Paginação**: dividir o resultado em "páginas" (por exemplo, 20 registros por vez) em vez de trazer tudo de uma vez. Na JPA, isso é feito com `setFirstResult(...)` e `setMaxResults(...)` na query.
> - **DAO** (*Data Access Object*, "objeto de acesso a dados"): a classe responsável por todo o acesso ao banco de uma entidade, isolando o resto do sistema dos detalhes de JPA — o equivalente ao *Repository* do Spring Data (Passo 2).
> - **Serialização**: transformar um objeto da memória em um formato que pode ser gravado ou transmitido (texto JSON, sequência de bytes...); o caminho inverso é a **desserialização**.
> - **Ciclos**: se `obra.toString()` imprime os relatórios e `relatorio.toString()` imprime a obra, um chama o outro sem fim — recursão infinita, que termina em `StackOverflowError`. O mesmo acontece com `equals`/`hashCode` e com um serializador JSON que segue as referências dos dois lados.

O lado `@ManyToOne` é o **owning side**: é a tabela `relatorio_seguranca` que tem
a coluna `obra_id`. Se um dia for preciso o inverso, basta adicionar
`@OneToMany(mappedBy = "obra")` sem alterar o banco.

> 📘 **Owning side ("lado dono") e `mappedBy`**
>
> Em uma relação bidirecional existem **dois** lados no Java, mas **uma única** coluna FK no banco. O JPA precisa saber qual dos lados "manda" nessa coluna: esse é o **owning side**, e **só as mudanças feitas nele são gravadas**. O outro é o **lado inverso** (*inverse side*), marcado com `mappedBy = "obra"`, que significa: "esta relação já está mapeada pelo campo `obra` do outro lado; não crie nada no banco por minha causa".
>
> Pegadinha clássica: se você fizer só `obra.getRelatorios().add(relatorio)` (lado inverso) sem `relatorio.setObra(obra)` (lado dono), **nada** é gravado na FK.
>
> Numa relação `@ManyToOne`/`@OneToMany`, o lado dono é sempre o `@ManyToOne` — o lado "muitos" —, porque é a tabela dele que tem a FK (a anotação `@ManyToOne` nem tem o atributo `mappedBy`).

## 6. `equals`/`hashCode` e `Serializable`

- **`equals` pelo ID** com `instanceof` e `getId()` (não `getClass()` nem
  `outra.id`): o proxy LAZY é uma *subclasse* de `Obra` e seus campos estão
  vazios, só os métodos funcionam.
- **`hashCode` constante:** antes do `persist()` o ID é `null`, depois não. Se o
  hash dependesse do ID, um objeto colocado em um `HashSet` antes de gravar
  "desapareceria" depois de gravar.
- **`Serializable`:** os beans `@ViewScoped` do JSF guardam estado na sessão HTTP,
  que pode ser serializada (replicação em cluster, passivação). Se a entidade lá
  dentro não for serializável, quebra.

> 📘 **`equals`, `hashCode` e `HashSet` em poucos minutos**
>
> - **`equals(outro)`** responde: "estes dois objetos representam a mesma coisa?". **`hashCode()`** devolve um número inteiro usado para **distribuir** objetos em estruturas baseadas em *hash* (`HashSet`, `HashMap`).
> - **O contrato entre os dois**: se `a.equals(b)` é verdadeiro, então obrigatoriamente `a.hashCode() == b.hashCode()`. E, na prática, o `hashCode` de um objeto **não pode mudar** enquanto ele estiver guardado num `HashSet`/`HashMap`.
> - **`HashSet`**: um conjunto (sem elementos repetidos) que guarda os objetos em "gavetas" (*buckets*) escolhidas pelo `hashCode`. Para achar um elemento, ele calcula o hash, abre **só aquela gaveta** e compara com `equals`. Se o hash do objeto muda depois de guardado, o `HashSet` passa a procurar na gaveta errada — é por isso que o objeto "desaparece".
> - **`instanceof` × `getClass()`**: `instanceof Obra` aceita subclasses — e o proxy *é* uma `Obra`. Já `getClass()` compara a classe **exata**, e a classe do proxy é outra (algo como `Obra$HibernateProxy$...`), então a comparação falharia.
> - **Por que `getId()` e não `outra.id`**: acessar o campo diretamente no proxy lê o campo da **subclasse vazia** (vem `null`); já `getId()` é um método, que o proxy sabe responder.
> - **Custo do `hashCode` constante**: todos os objetos da classe caem na mesma gaveta, então em conjuntos muito grandes a busca fica mais lenta (vira uma comparação um a um com `equals`). Para coleções de entidades de tamanho comum, é um preço aceitável em troca de um comportamento correto.

> 📘 **`Serializable`, sessão HTTP, cluster e passivação**
>
> - **Serialização Java**: converte um objeto (e tudo o que ele referencia) numa sequência de **bytes**, para gravá-lo em disco ou enviá-lo pela rede. A interface **`java.io.Serializable`** é uma *marker interface* ("interface marcadora"): não tem nenhum método, só "marca" a classe como autorizada a ser serializada. Sem ela, a tentativa lança `NotSerializableException`. O `serialVersionUID` que você vê nas classes funciona como um "número de versão" do formato, conferido na desserialização.
> - **Sessão HTTP** (`HttpSession`): uma área de memória **no servidor**, uma por usuário, que guarda dados entre requisições. O navegador é reconhecido por um *cookie* — um pequeno dado que o servidor pede para o navegador guardar e reenviar a cada requisição (normalmente chamado `JSESSIONID`). É ali que ficam os beans `@ViewScoped`.
> - **Cluster** ("aglomerado"): vários servidores trabalhando juntos como se fossem um só, para dividir a carga e tolerar falhas. Na **replicação de sessão**, a sessão de cada usuário é copiada (serializada) para outro servidor do cluster: se um cair, outro continua o atendimento sem perder o estado.
> - **Passivação**: quando há muitas sessões paradas, o servidor pode tirá-las da memória e gravá-las em disco (serializando) para liberar espaço, e trazê-las de volta quando o usuário voltar — a **ativação**. É como guardar no depósito os móveis que você não está usando.

## 7. Resumo do paralelo Jakarta EE ↔ Spring Boot (Passo 1)

| Conceito | Jakarta EE (este projeto) | Spring Boot |
|---|---|---|
| Pacote JPA | `jakarta.persistence` | `jakarta.persistence` (igual, desde o Boot 3) |
| Implementação JPA | Fornecida pelo servidor (`provided`) | Hibernate embutido via `spring-boot-starter-data-jpa` |
| Artefato | WAR implantado em um servidor | JAR executável com Tomcat embutido |
| Validação | Incluída na plataforma | `spring-boot-starter-validation` |
| Auditoria | `@PrePersist` / `@PreUpdate` | Idem, ou `@CreatedDate` do Spring Data |
| Configuração do banco | `persistence.xml` + DataSource do servidor (Passo 2) | `application.properties` |
| Lazy fora da transação | `LazyInitializationException` (sem OSIV) | Escondido pelo OSIV por padrão |

> 📘 **Termos da tabela**
>
> - **Artefato** (*artifact*): o arquivo gerado pelo build e entregue para execução (o WAR ou o JAR).
> - **Implantar** (*deploy*): instalar a aplicação no ambiente em que ela vai rodar. No Jakarta EE, é entregar o WAR ao servidor (no WildFly, por exemplo, copiando-o para a pasta `deployments`).
> - **Tomcat**: servidor web da Apache que implementa a **parte web** do Jakarta EE — principalmente a API de **Servlets** (a API Java básica para receber requisições HTTP e gerar respostas, sobre a qual o Spring MVC e o JSF são construídos). Ele **não** é um servidor de aplicação completo (não traz JPA, EJB etc.). No Spring Boot, vem embutido dentro do JAR.
> - **`persistence.xml`**: o arquivo de configuração da JPA (fica em `META-INF/`). Define a **unidade de persistência** (*persistence unit*): um nome, as classes de entidade, o DataSource a usar e propriedades do provedor.
> - **DataSource** ("fonte de dados"): o objeto que entrega **conexões com o banco**, normalmente vindas de um **pool** de conexões. *Pool* é uma "piscina" de conexões já abertas que são emprestadas e devolvidas, porque abrir uma conexão nova a cada operação é caro. No Jakarta EE, o DataSource é configurado **no servidor** e a aplicação o encontra por um nome **JNDI** (*Java Naming and Directory Interface* — o "catálogo de endereços" do servidor, onde os recursos ficam registrados por nome; neste projeto, `java:comp/DefaultDataSource`).
> - **`application.properties`**: o arquivo de configuração do Spring Boot, no formato `chave=valor`.

## 8. Perguntas prováveis na entrevista

1. *Qual é o fetch default de `@ManyToOne`?* → EAGER. Declaramos LAZY e usamos `JOIN FETCH` quando é preciso.
2. *O que é o problema N+1?* → 1 query para a lista + 1 para cada associação. Resolve-se com LAZY + `JOIN FETCH` / Entity Graphs.
3. *Por que `EnumType.STRING`?* → `ORDINAL` corrompe dados se a ordem do enum mudar.
4. *Para que serve `@Version`?* → Concorrência otimista; evita *lost updates* entre usuários.
5. *Diferença entre `persist` e `merge`?* → `persist` torna Managed uma entidade nova; `merge` copia o estado de uma entidade Detached para uma cópia Managed e retorna essa cópia.
6. *Owning side de uma relação?* → O lado que tem a FK (`@ManyToOne`/`@JoinColumn`); o `mappedBy` marca o lado inverso.

> 📘 **Entity Graph** ("grafo de entidades" — *grafo* é um conjunto de objetos ligados entre si): recurso da JPA (desde a versão 2.1) para declarar, **separadamente da query**, quais associações devem ser carregadas junto — com `@NamedEntityGraph` na entidade ou com a API `EntityGraph`. É uma alternativa ao `JOIN FETCH` (no Spring Data, aparece como `@EntityGraph` nos métodos do repositório). *Lost update* foi explicado na seção 2, junto com `@Version`.

## Glossário

| Termo | Significado |
|---|---|
| Alias | "Apelido" dado a uma tabela ou coluna dentro de uma consulta SQL (ex.: `rs1_0`). |
| `allocationSize` | Atributo de `@SequenceGenerator` (padrão 50): quantos IDs o Hibernate reserva a cada consulta à sequence, reduzindo idas ao banco. |
| Anêmico (modelo) | *Anemic Domain Model*: entidades que só têm dados e getters/setters, sem comportamento; as regras ficam espalhadas em serviços. |
| Anotação (*annotation*) | Metadado `@Algo` colocado em classes, campos ou métodos; não faz nada sozinho, é lido por frameworks. |
| API | *Application Programming Interface*: conjunto de interfaces/classes que você usa para conversar com uma biblioteca ou serviço. |
| Artefato (*artifact*) | O arquivo gerado pelo build e entregue para execução (WAR, JAR). |
| Auditoria | Registro automático de quando (e por quem) um dado foi criado ou alterado. |
| Batching (*batch*) | Agrupar vários comandos SQL e enviá-los ao banco de uma vez, em "lote", economizando idas e voltas. |
| Bean | Objeto cuja criação e tempo de vida são gerenciados por um contêiner (CDI, Spring...). |
| Bean Validation | Especificação de validação declarativa por anotações (`@NotNull`, `@NotBlank`, `@Size`, `@PastOrPresent`...); hoje chamada Jakarta Validation. Implementação mais usada: Hibernate Validator. |
| Bidirecional (relação) | Relação navegável nos dois sentidos (`relatorio.getObra()` e `obra.getRelatorios()`). |
| Binding | "Ligação" entre um campo da tela e uma propriedade do objeto (via getter/setter). |
| Cache de 1º nível | O Persistence Context: memória do `EntityManager` que evita buscar de novo a mesma entidade e garante uma instância por linha. |
| Callback | Método que você marca e o framework chama no momento certo (ex.: `@PrePersist`). |
| CDI | *Contexts and Dependency Injection*: mecanismo de injeção de dependências do Jakarta EE (equivalente ao contêiner do Spring). |
| Ciclo de vida (*lifecycle*) | Estados de uma entidade para o JPA: New/Transient, Managed, Detached, Removed. |
| `clear()` / `close()` | Métodos do `EntityManager`: esvaziar o Persistence Context / fechar o `EntityManager`. Em ambos, as entidades viram Detached. |
| Cluster | Vários servidores trabalhando juntos como um só, para dividir carga e tolerar falhas. |
| Código legado (*legacy code*) | Código antigo, ainda em produção, que precisa ser mantido. |
| Commit | Confirmação de uma transação: torna permanentes todas as alterações. |
| Concorrência otimista | Estratégia que não tranca registros e só verifica, ao gravar, se alguém alterou o dado antes (via `@Version`). Oposto da pessimista, que tranca (*lock*). |
| Constraint (restrição) | Regra imposta pelo próprio banco (`NOT NULL`, `UNIQUE`, FK...). |
| Contêiner (*container*) | Parte do servidor (ou do framework) que cria, injeta e gerencia os seus objetos. |
| Cookie | Pequeno dado que o servidor pede ao navegador para guardar e reenviar a cada requisição (identifica a sessão HTTP). |
| DAO | *Data Access Object*: classe que concentra o acesso ao banco de uma entidade (equivalente ao *Repository* do Spring). |
| DataSource | Objeto que fornece conexões com o banco, normalmente de um pool; no Jakarta EE é configurado no servidor e encontrado via JNDI. |
| DDL | *Data Definition Language*: comandos SQL que definem a estrutura (`CREATE`, `ALTER`, `DROP`). |
| Default | Valor padrão, usado quando você não especifica nada. |
| Deploy (implantar) | Instalar a aplicação no ambiente em que vai rodar (ex.: entregar o WAR ao servidor). |
| Detached | Estado "desanexado": a entidade tem ID, mas o `EntityManager` que a gerenciava fechou; alterações não são gravadas sem `merge()`. |
| Dirty checking | "Verificação de sujeira": no flush, o Hibernate compara entidades gerenciadas com a fotografia original e gera `UPDATE` para as que mudaram. |
| DML | *Data Manipulation Language*: comandos SQL que mexem nos dados (`SELECT`, `INSERT`, `UPDATE`, `DELETE`). |
| DTO | *Data Transfer Object*: objeto simples usado só para transportar dados entre camadas. |
| EAGER | "Ansioso": a associação é carregada junto com a entidade, sempre. Default de `@ManyToOne` e `@OneToOne`. |
| Eclipse Foundation | Fundação de software livre que recebeu o Java EE da Oracle e mantém o Jakarta EE. |
| EclipseLink | Implementação de JPA da Eclipse Foundation (usada no Payara); historicamente a implementação de referência. |
| EJB | *Enterprise JavaBeans*: componentes de negócio gerenciados pelo servidor; por padrão, cada método público roda em uma transação. |
| Encapsulamento | Pilar da POO: esconder o estado interno e expor só operações controladas. |
| Endpoint | Endereço (URL) exposto por uma API para receber requisições. |
| Entidade (*entity*) | Classe anotada com `@Entity` cujos objetos são gravados no banco (classe = tabela, objeto = linha). |
| Entity Graph | Recurso da JPA para declarar quais associações carregar junto em uma consulta; alternativa ao `JOIN FETCH`. |
| `EntityManager` | Interface principal da JPA (`persist`, `find`, `merge`, `remove`, consultas); no Hibernate, corresponde à `Session`. |
| Enum | Tipo Java com um conjunto fixo de valores (constantes). |
| `equals` / `hashCode` | Métodos que definem igualdade e o número usado por estruturas de hash; objetos iguais precisam ter o mesmo hashCode. |
| Escalar (*to scale*) | Manter bom desempenho quando o volume de dados ou de usuários cresce. |
| Escopo (*scope*) | Em beans: o tempo de vida do objeto (ex.: `@ViewScoped`). No Maven: em que momentos uma dependência é usada (ex.: `provided`). |
| Especificação × implementação | A especificação é o "contrato" (regras + interfaces); a implementação (provedor) é o código que o cumpre. |
| `FetchType` | Define quando uma associação é carregada: EAGER (já) ou LAZY (sob demanda). |
| `final` | Palavra-chave que, numa classe, impede a criação de subclasses. |
| FK | *Foreign Key* (chave estrangeira): coluna que referencia a PK de outra tabela. |
| Flush | Sincronização do Persistence Context com o banco (envio dos SQLs pendentes); acontece antes do commit, mas não é o commit. |
| Fork | Cópia de um projeto que passa a evoluir separadamente (ex.: Payara é um fork do GlassFish). |
| Getter / Setter | Métodos que leem (`getX`/`isX`) e alteram (`setX`) um campo. |
| GlassFish | Servidor de aplicação que era a referência do Java EE; o Payara deriva dele. |
| H2 | Banco relacional escrito em Java que pode rodar em memória; usado em desenvolvimento e testes. |
| `HashSet` | Conjunto sem repetidos que organiza os elementos em "gavetas" (*buckets*) pelo `hashCode` e confirma com `equals`. |
| Hibernate | A implementação de JPA (ORM) mais usada; padrão no Spring Boot e no WildFly. |
| HTTP | *HyperText Transfer Protocol*: o protocolo da web; cada pedido do navegador é uma requisição (*request*). |
| IDE | *Integrated Development Environment*: ambiente de desenvolvimento integrado (ex.: Eclipse, IntelliJ). |
| IDENTITY | Estratégia de ID autoincrementado pelo banco no `INSERT`; obriga o Hibernate a executar o `INSERT` na hora e impede o batching. |
| Índice (*index*) | Estrutura do banco que acelera buscas por uma coluna, como o índice remissivo de um livro. |
| `INNER JOIN` / `LEFT JOIN` | Junções SQL: o INNER traz só linhas com correspondência dos dois lados; o LEFT mantém todas as linhas da tabela da esquerda. |
| `instanceof` × `getClass()` | `instanceof` aceita subclasses (funciona com proxies); `getClass()` exige a classe exata (falha com proxies). |
| Invariante | Regra que deve ser sempre verdadeira para um objeto. |
| J2EE | *Java 2 Platform, Enterprise Edition*: nome histórico (1999–2006) da plataforma corporativa Java. |
| Jakarta EE | Nome atual da plataforma, mantida pela Eclipse Foundation; pacotes `jakarta.*` a partir da versão 9. |
| Jakarta Validation | Nome atual da especificação Bean Validation. |
| JAR | *Java ARchive*: arquivo `.zip` com classes compiladas; no Spring Boot, um *fat JAR* executável com tudo dentro. |
| Java EE | *Java Platform, Enterprise Edition*: nome da plataforma de 2006 a 2017 (versões 5 a 8). |
| Java SE | *Java Standard Edition*: o Java "comum" (JVM, bibliotecas básicas), sobre o qual o EE é construído. |
| JavaBeans | Convenção: construtor público sem argumentos + getters/setters padronizados (+ `Serializable`). Não confundir com EJB. |
| JAX-RS | *Jakarta RESTful Web Services* (antes *Java API for RESTful Web Services*): especificação para APIs REST. |
| JDBC | *Java Database Connectivity*: API de baixo nível do Java para falar com bancos; usada por baixo pela JPA. |
| JNDI | *Java Naming and Directory Interface*: "catálogo de endereços" do servidor, onde recursos (como DataSources) são registrados por nome. |
| `JOIN FETCH` | Cláusula JPQL que carrega uma associação na mesma consulta, evitando o N+1. |
| JPA | *Java Persistence API* (hoje *Jakarta Persistence*): especificação de mapeamento objeto-relacional do Java. |
| JPQL | *Java/Jakarta Persistence Query Language*: linguagem de consulta da JPA, parecida com SQL, mas sobre entidades e atributos. |
| JSF | *JavaServer Faces* (hoje *Jakarta Faces*): framework web de componentes do Jakarta EE, que gera HTML no servidor. |
| JSON | *JavaScript Object Notation*: formato de texto para troca de dados. |
| JSON-B | *Jakarta JSON Binding*: especificação que converte objetos Java ↔ JSON (papel do Jackson no Spring). |
| JVM | *Java Virtual Machine*: a máquina virtual que executa o código Java compilado. |
| LAZY | "Preguiçoso": a associação só é carregada quando acessada (via proxy). Default de `@OneToMany` e `@ManyToMany`. |
| `LazyInitializationException` | Exceção do Hibernate ao inicializar um proxy/coleção LAZY depois que o Persistence Context fechou. |
| Log | Mensagens que a aplicação grava para registrar o que está acontecendo. |
| Lombok | Biblioteca que gera getters, setters, construtores etc. durante a compilação a partir de anotações. |
| Lost update | "Atualização perdida": o segundo usuário a salvar sobrescreve, sem saber, a alteração do primeiro. Evitado com `@Version`. |
| Managed | Estado "gerenciado": a entidade está no Persistence Context e suas alterações são detectadas e gravadas. |
| `mappedBy` | Atributo que marca o lado inverso de uma relação bidirecional, apontando o campo do lado dono. |
| Marker interface | Interface sem métodos que só "marca" uma classe (ex.: `Serializable`). |
| Maven | Ferramenta de build e gerenciamento de dependências do Java; configurada pelo `pom.xml` (*Project Object Model*). |
| `merge()` | Copia o estado de uma entidade Detached para uma instância Managed e retorna essa instância. |
| N+1 | Problema de desempenho: 1 consulta para a lista + 1 consulta extra para cada associação carregada. |
| New / Transient | Estado de um objeto recém-criado com `new`, que o JPA ainda não conhece. Não confundir com `transient` (Java) nem `@Transient` (JPA). |
| `NOT NULL` | Constraint que proíbe valores nulos em uma coluna. |
| OOP / POO | *Object-Oriented Programming* / Programação Orientada a Objetos: paradigma baseado em objetos que unem dados e comportamento. |
| `OptimisticLockException` | Exceção lançada quando o `UPDATE` com `WHERE versao = ?` não encontra a linha, porque outro usuário gravou antes. |
| ORDINAL / STRING | Formas de gravar um enum: pela posição (perigoso se a ordem mudar) ou pelo nome (seguro). |
| ORM | *Object-Relational Mapping*: técnica/ferramenta que traduz entre objetos e tabelas relacionais. |
| OSIV | *Open Session In View*: manter a sessão do Hibernate aberta durante toda a requisição; ativo por padrão no Spring Boot, ausente no Jakarta EE. |
| Owning side | "Lado dono" da relação: o lado cujas mudanças são gravadas na FK (em N:1, o `@ManyToOne`). |
| Pacote (*package*) | Agrupamento de classes Java que funciona como "sobrenome" (ex.: `jakarta.persistence`). |
| Paginação | Trazer resultados em "páginas" (ex.: 20 por vez), com `setFirstResult`/`setMaxResults`. |
| Passivação | Tirar da memória e gravar em disco (serializando) sessões ou objetos inativos, reativando-os depois. |
| Payara | Servidor de aplicação derivado do GlassFish; usa o EclipseLink. |
| `persist()` | Torna Managed uma entidade nova e agenda o `INSERT`. |
| Persistence Context | Conjunto das entidades gerenciadas por um `EntityManager`; funciona como cache de 1º nível e rastreador de mudanças. |
| Persistence unit | Unidade de persistência: configuração (nome, entidades, DataSource, propriedades) declarada no `persistence.xml`. |
| `persistence.xml` | Arquivo de configuração da JPA, em `META-INF/`. |
| Persistência | Gravar dados em um meio durável (como um banco) para que sobrevivam ao fim do programa. |
| PK | *Primary Key* (chave primária): coluna que identifica unicamente cada linha. |
| Pool | "Piscina" de recursos caros já prontos (ex.: conexões com o banco), emprestados e devolvidos para reuso. |
| PostgreSQL | Banco de dados relacional de código aberto. |
| Provedor (*provider*) | A implementação concreta de uma especificação (ex.: Hibernate e EclipseLink são provedores JPA). |
| `provided` | Escopo Maven: a dependência é usada para compilar, mas não é empacotada, porque o servidor já a fornece. |
| Proxy | Objeto "substituto" que se passa pelo real e controla o acesso a ele; no Hibernate, uma subclasse que carrega os dados sob demanda. |
| Query | Consulta ao banco. |
| Reflection | API do Java que permite a um programa inspecionar classes, campos e anotações em tempo de execução. |
| Relações (1:1, N:1, 1:N, N:N) | `@OneToOne`, `@ManyToOne`, `@OneToMany`, `@ManyToMany`: os quatro tipos de associação da JPA. |
| `remove()` / Removed | Marca uma entidade gerenciada para exclusão; o `DELETE` sai no flush/commit. |
| Replicação de sessão | Copiar a sessão HTTP de um usuário para outro servidor do cluster, para não perdê-la se um servidor cair. |
| `requireNonNull` | `Objects.requireNonNull(obj, msg)`: lança `NullPointerException` com a mensagem se `obj` for nulo. |
| REST | *Representational State Transfer*: estilo de API sobre HTTP baseado em recursos e URLs. |
| Rollback | Desfazer todas as alterações de uma transação. |
| Round trip | Uma "ida e volta" pela rede entre a aplicação e o banco. |
| Runtime | Tempo de execução (enquanto o programa roda), em oposição ao tempo de compilação. |
| SEQUENCE | Estratégia de ID baseada em um contador separado no banco; permite obter o ID antes do `INSERT` e usar batching. |
| `Serializable` | Interface marcadora que autoriza a serialização de uma classe. |
| Serialização | Converter um objeto em bytes ou texto (ex.: JSON) para gravar ou transmitir; o inverso é a desserialização. |
| `serialVersionUID` | "Número de versão" do formato serializado de uma classe, conferido na desserialização. |
| Servidor de aplicação | Programa que hospeda aplicações Jakarta EE e fornece as implementações das especificações (ex.: WildFly, Payara). |
| Servlet | API Java básica para receber requisições HTTP e gerar respostas; base do Spring MVC e do JSF. |
| Sessão HTTP | Área de memória no servidor, uma por usuário, que guarda dados entre requisições (identificada por cookie). |
| Snapshot | "Fotografia" do estado original de uma entidade, usada pelo dirty checking. |
| Spring Data | Família de projetos do Spring para acesso a dados (repositórios gerados a partir de interfaces, auditoria etc.). |
| SQL | *Structured Query Language*: linguagem padrão dos bancos relacionais. |
| SQL injection | Ataque em que texto malicioso concatenado numa consulta vira parte do comando; evitado com parâmetros (`:id`). |
| Starter | Dependência "guarda-chuva" do Spring Boot que traz um conjunto de bibliotecas e ativa a auto-configuração. |
| Thread | Linha de execução; várias threads rodam em paralelo dentro do mesmo programa (ex.: uma por requisição). |
| Tomcat | Servidor web da Apache (contêiner de Servlets); vem embutido no JAR do Spring Boot. Não é um servidor de aplicação completo. |
| `toString()` | Método que gera a representação em texto de um objeto; chamado implicitamente por logs e concatenações. |
| Trade-off | Decisão em que se ganha algo abrindo mão de outra coisa. |
| Transação | Grupo de operações "tudo ou nada": termina em commit (confirma tudo) ou rollback (desfaz tudo). |
| Unidirecional (relação) | Relação navegável em um só sentido (aqui, só de relatório para obra). |
| View | Camada que gera a resposta ao usuário (página HTML ou JSON). |
| `@ViewScoped` | Escopo de bean JSF que vive enquanto o usuário permanece na mesma página. |
| WAR | *Web Application Archive*: pacote de aplicação web que é implantado em um servidor de aplicação (não roda sozinho). |
| WildFly | Servidor de aplicação open source da Red Hat (antigo JBoss AS); usa o Hibernate. |
| XML | *eXtensible Markup Language*: formato de texto estruturado com tags (usado no `pom.xml` e no `persistence.xml`). |
