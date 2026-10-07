# SOLID no projeto Gestão de Obras

Explicação detalhada de onde cada princípio SOLID aparece no código, por que foi
aplicado (ou não) e quais são os trade-offs. Os exemplos citam arquivos e métodos
reais do projeto.

## Situação atual (após o Passo 3)

| Princípio | Situação |
|---|---|
| **S** — Responsabilidade Única | ✅ Aplicado, com dois pontos de tensão conscientes |
| **O** — Aberto/Fechado | ✅ Aplicado |
| **L** — Substituição de Liskov | ✅ Aplicado (o caso dos proxies do Hibernate é o mais interessante) |
| **I** — Segregação de Interfaces | 🟡 Parcial: services separados por agregado; contrato só de leitura previsto para o Passo 5 |
| **D** — Inversão de Dependência | ✅ Entre apresentação e negócio (interfaces `@Local`, Passo 3); 🟡 entre negócio e DAO, por escolha pragmática |

---

## S — Single Responsibility Principle (Responsabilidade Única)

**Definição (Robert C. Martin):** *"uma classe deve ter um, e somente um, motivo
para mudar"*. O "motivo para mudar" é um ator ou interesse do negócio, não
"fazer uma coisa só".

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

Exemplo concreto: se o DBA pedir para otimizar a busca por nome, você mexe **só**
no `ObraDao`. A `Obra`, o `GenericDao`, o Service, a tela JSF e a API REST não
são tocados.

**2. O DAO não decide transação** (`GenericDao`):

```java
@Transactional(Transactional.TxType.MANDATORY)
public abstract class GenericDao<T, ID> {
```

Esse é o ponto mais sutil. Decidir **onde uma transação começa e termina**
depende do **caso de uso**. Por exemplo, "excluir obra + excluir os relatórios
dela" tem de ser uma única transação.

Se o DAO abrisse a própria transação, ele teria **dois** motivos para mudar:
- a forma de acessar os dados;
- as regras de atomicidade do negócio.

Com `MANDATORY`, o DAO só acessa dados, e a decisão transacional fica no Service
(Passo 3). Testamos isso no WildFly: uma chamada sem transação é bloqueada com
`TransactionalException`.

### Onde o SRP está "esticado" (sendo honesto)

**a) A `Obra` mistura três interesses através de anotações:**

```java
@NotBlank(...)                                         // validação
@Column(name = "nome", nullable = false, length = 150) // mapeamento do banco
private String nome;                                    // domínio
```

Um purista de Clean Architecture ou de arquitetura hexagonal separaria isso em
três peças: uma `Obra` de domínio pura, uma `ObraEntity` JPA e um *mapper* entre
elas. Optamos pelo pragmatismo, por dois motivos:
- anotações são **metadados**, não comportamento;
- separar dobraria o número de classes sem ganho real em um CRUD.

Na entrevista, a resposta madura é: *"sei que existe a alternativa e sei quando
vale a pena: quando o domínio é rico ou o modelo do banco diverge muito do modelo
de negócio"*.

**b) O `StatusObra` guarda texto de tela:**

```java
EM_ANDAMENTO("Em andamento"),
```

Isso é uma violação real, ainda que pequena. Se amanhã a aplicação precisar de
inglês, o enum muda por um motivo de **apresentação**, e esse é um segundo motivo
para mudar. O lugar certo desse texto é um arquivo de mensagens
(`messages.properties`) do JSF. **Planejado para o Passo 4**, quando a tela existir.

---

## O — Open/Closed Principle (Aberto/Fechado)

**Definição:** *"entidades de software devem estar abertas para extensão, mas
fechadas para modificação"*. Você acrescenta comportamento novo **escrevendo
código novo**, não editando código que já funciona e já foi testado.

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

O `GenericDao` **não é alterado em nenhuma linha**, e o `Equipamento` já ganha
`inserir`, `atualizar`, `buscarPorId` e os outros. O que torna isso possível:
- os **generics** `<T, ID>`, que deixam o tipo em aberto;
- o `Class<T> classeEntidade` recebido no construtor, que é usado no
  `em.find(classeEntidade, id)`.

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

**3. Comportamentos transversais adicionados por anotação**

`@Transactional`, `@PersistenceContext`, `@Stateless` e as anotações de Bean
Validation acrescentam comportamento (transação, injeção, pool, validação)
**sem modificar o código** das classes. O contêiner "embrulha" a classe com um
interceptador. Em Spring é o mesmo mecanismo, a AOP. Para criar uma regra de
validação nova, você adiciona uma anotação; o motor de validação não muda.

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

**Definição (Barbara Liskov):** *se S é subtipo de T, objetos do tipo T podem ser
substituídos por objetos do tipo S sem quebrar o programa*.

Na prática, a subclasse precisa honrar o **contrato** da classe-mãe:
- não pode exigir mais (pré-condições mais fortes);
- não pode entregar menos (pós-condições mais fracas);
- não pode lançar exceções inesperadas.

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

**2. Os proxies do Hibernate (o caso mais interessante do projeto)**

Com o `FetchType.LAZY` do Passo 1, quando você chama `relatorio.getObra()`,
**não recebe uma `Obra`**. Recebe uma **subclasse gerada em tempo de execução**,
algo como `Obra$HibernateProxy$xyz`. O sistema inteiro trata esse objeto como uma
`Obra` normal. **Isso é LSP acontecendo de verdade**, e o código foi escrito para
não quebrá-lo:

| Decisão | Onde | O que quebraria sem ela |
|---|---|---|
| `equals` usa `instanceof` | `Obra.equals` | Com `getClass()`, `obra.equals(proxy)` daria `false`, porque as classes são diferentes. O proxy não seria um substituto válido. |
| `equals` usa `outra.getId()` | `Obra.equals` | Com `outra.id`, o resultado seria `null`: os **campos** do proxy ficam vazios e só os **métodos** são delegados. |
| A classe **não** é `final` | `Obra` | O Hibernate nem conseguiria criar a subclasse. |
| `toString` não inclui a obra | `RelatorioSeguranca.toString` | Um simples log lançaria `LazyInitializationException` fora da transação. |

Isso foi validado no teste do Passo 1: `assertEquals(obra, relatorio.getObra())`
passou com o proxy **ainda não inicializado**.

> ⚠️ **Limite a conhecer:** um `equals` com `instanceof` pode quebrar a simetria se
> um dia existir uma subclasse "de verdade" com o próprio `equals` (por exemplo,
> herança JPA com `ObraPublica extends Obra`). Hoje não existe; se surgir, esse
> ponto precisa ser reavaliado.

**3. (Passo 3) Hierarquia de exceções e contratos documentados**

- `EntidadeNaoEncontradaException` é uma `RegraNegocioException`: qualquer
  `catch (RegraNegocioException e)` continua funcionando, e ela herda o
  `@ApplicationException(rollback = true)`.
- As interfaces `ObraService` e `RelatorioSegurancaService` documentam o contrato
  com `@throws`. Qualquer implementação (inclusive um mock) deve honrá-lo.

---

## I — Interface Segregation Principle (Segregação de Interfaces)

**Definição:** *"clientes não devem ser forçados a depender de métodos que não
usam"*. É melhor ter várias interfaces pequenas e específicas do que uma única
interface "gorda".

### Situação

**Onde já aparece (Passo 3):** em vez de um único `GestaoObrasService` com tudo,
há dois contratos separados por agregado: `ObraService` e
`RelatorioSegurancaService`. Quem só lida com relatórios não depende das
operações de obra, e vice-versa.

**Onde ainda não está aplicado:** o `GenericDao` entrega o CRUD **completo** a
todos os DAOs. Veja onde isso pode doer no nosso domínio.

Em obras, um relatório de inspeção de segurança costuma ter valor **legal e de
auditoria**: depois de registrado, não deveria ser editado. Se essa for uma regra
do negócio, o `RelatorioSegurancaDao` **herda um `atualizar()` que nunca deveria
existir para ele**. Esse é o *refused bequest* (a "herança recusada"), sintoma
clássico de violação do ISP.

Outro caso virá no Passo 5: a API REST só **lista** obras. Se ela depender do
contrato completo, passa a enxergar métodos de escrita que nunca usa.

**Por que ainda não foi aplicado:** o ISP é sobre **clientes**. Separar interfaces
sem saber quem vai usá-las é especulação, e acabaríamos com interfaces que ninguém
consome (viola o YAGNI, *"you aren't gonna need it"*). **Planejado para o Passo 5**,
quando o cliente só de leitura existir.

**Como fica quando aplicado:**

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

> 🟢 **Paralelo com Spring:** o Spring Data é um ótimo exemplo de ISP feito pelo
> framework. A hierarquia vai de `Repository` (zero métodos) para `CrudRepository`,
> depois `ListCrudRepository`, `PagingAndSortingRepository` e por fim
> `JpaRepository`. Você pode estender só o `Repository<Obra, Long>` e declarar
> apenas o `findAll()` de que precisa.

---

## D — Dependency Inversion Principle (Inversão de Dependência)

**Definição, em duas partes:**
- (a) módulos de alto nível (negócio) não devem depender de módulos de baixo nível
  (detalhes técnicos); ambos devem depender de **abstrações**;
- (b) abstrações não devem depender de detalhes; os detalhes é que dependem das
  abstrações.

### ⭐ A distinção que mais cai em entrevista

Três conceitos parecidos que **não são a mesma coisa**:

| Conceito | O que é | No nosso código |
|---|---|---|
| **IoC** (Inversão de Controle) | Quem cria e gerencia os objetos é o **contêiner**, não o seu código | O WildFly cria o `ObraDao` e o `ObraServiceBean` |
| **DI** (Injeção de Dependência) | O **mecanismo** pelo qual a dependência chega pronta | `@PersistenceContext`, `@Inject`, `@EJB` |
| **DIP** (Inversão de Dependência) | Um **princípio de design** sobre a **direção** das dependências | Depender de abstrações, não de implementações |

Usar `@Inject` **não garante** DIP. Dá para injetar uma classe concreta, e aí você
tem DI sem DIP. Saber explicar isso impressiona o entrevistador.

### Onde está aplicado

**1. Os DAOs dependem da especificação JPA, não do Hibernate**

```java
protected EntityManager em;   // jakarta.persistence.EntityManager → INTERFACE da especificação
```

Não existe **nenhum** `import org.hibernate` no código da aplicação. Quem fornece a
implementação é o servidor: Hibernate no WildFly, EclipseLink no GlassFish. É
exatamente o diagrama do DIP: o detalhe (Hibernate) implementa a abstração
(`EntityManager`), e o nosso código só conhece a abstração.

```
ObraDao  ──depende de──►  EntityManager (interface)  ◄──implementa──  Hibernate
```

**2. O código não sabe qual é o banco** (`persistence.xml`)

O código depende de um `DataSource` obtido por **nome JNDI**, não de um driver
concreto. Trocar H2 por PostgreSQL é configuração do servidor; nenhuma classe Java
muda.

**3. O DAO não cria as próprias dependências**

Não existe `new EntityManager()` nem `Persistence.createEntityManagerFactory(...)`
no código. A dependência chega injetada (DI + IoC), e é isso que **permite**
depender só da abstração.

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

**Por que muitos projetos (Jakarta EE e Spring) aceitam a classe concreta:**
- quase sempre só existe **uma** implementação;
- o Mockito consegue fazer mock de classes concretas, então os testes não sofrem;
- os proxies do CDI funcionam com classes;
- uma interface para uma implementação única vira cerimônia.

**Contra-argumento:** em código de negócio crítico ou de longa duração, a interface
protege contra uma troca de tecnologia (por exemplo, ler obras de um serviço
externo em vez do banco).

**Curiosidade histórica do J2EE, que costuma cair em entrevista:** no EJB 2.x, as
interfaces eram **obrigatórias** (Home, Remote e Local para cada bean). A
plataforma forçava o DIP, à custa de muito código repetitivo. O EJB 3.1
(Java EE 6) criou a *no-interface view* e as tornou opcionais.

---

## Planejamento das próximas melhorias

| Passo | Princípio | O que será feito |
|---|---|---|
| 3 ✅ | D | Interfaces de negócio `@Local` (`ObraService`, `RelatorioSegurancaService`) |
| 4 | S | Tirar o texto de tela ("Em andamento") do enum e levar para o `messages.properties` |
| 5 | I | A API REST passa a depender de um contrato só de leitura |
