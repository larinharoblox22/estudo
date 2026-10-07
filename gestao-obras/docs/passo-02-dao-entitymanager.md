# Passo 2 — O Padrão DAO e o EntityManager

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

Cada camada só conhece a camada imediatamente abaixo e tem **um único motivo
para mudar** (Single Responsibility Principle):

| Se mudar… | Só é preciso alterar… |
|---|---|
| uma query (otimizar, acrescentar `JOIN FETCH`) | o DAO |
| uma regra de negócio ("obra concluída não recebe relatórios") | o Service |
| a tela ou o formato JSON | o Bean JSF / o Resource REST |
| o banco de dados (H2 → PostgreSQL) | o DataSource no servidor (nem o DAO muda) |

Na prática, isso resulta em:
- **Reutilização:** o mesmo `ObraDao` atende a tela JSF e a API REST, via Service.
- **Testabilidade:** o Service pode ser testado com DAOs *mock*, sem banco de dados.
- **Um só lugar para otimizar:** todo o JPQL sobre obras está no `ObraDao`. Um
  problema de performance é procurado em um arquivo, não em 30.

### `GenericDao<T, ID>`: reutilização com generics

O CRUD (`inserir`, `atualizar`, `removerPorId`, `buscarPorId`, `listarTodos`) é
igual para qualquer entidade. Ele é escrito uma vez em uma classe abstrata
parametrizada; cada DAO concreto passa `Obra.class` no construtor e acrescenta
apenas as queries do seu domínio. `ObraDao` sobrescreve `listarTodos()` para impor
ordenação — polimorfismo clássico.

### `@Transactional(MANDATORY)`: quem decide a transação é o negócio

Os DAOs **não abrem transações**. `MANDATORY` faz com que uma chamada sem
transação ativa falhe com `TransactionalException`. Validamos isso em um WildFly
real: chamado a partir de um EJB, funciona; chamado diretamente de um servlet, é
bloqueado.

Por quê? Porque "registrar relatório + atualizar status da obra" precisa ser
**uma** transação. Se cada DAO abrisse a sua, teríamos duas transações
independentes e um estado inconsistente se a segunda falhasse. A fronteira
transacional pertence ao caso de uso, ou seja, ao Service (Passo 3).

### DAO vs Repository

Termos muitas vezes usados como sinônimos. Formalmente:
- **DAO** (Core J2EE Patterns): abstração da *persistência*, orientada a
  tabelas/operações CRUD.
- **Repository** (DDD, Eric Evans): abstração de uma *coleção de agregados* do
  domínio ("me dê as obras em andamento"), sem expor detalhes de persistência.

O nosso `ObraDao` é um híbrido pragmático, como quase todos os projetos reais.
Saber a distinção é uma boa resposta de entrevista.

> **Paralelo Spring Boot:** `@ApplicationScoped` ≈ `@Repository` (ambos singletons).
> A diferença é que o `@Repository` do Spring traduz as exceções JPA para a
> hierarquia `DataAccessException`. No Jakarta EE você recebe as `PersistenceException`
> originais, e o contêiner EJB as encapsula em `EJBException` (Passo 3).
> Não é preciso `beans.xml`: no CDI 4 as classes com anotação de escopo são
> descobertas automaticamente, como o *component scan* do Spring.

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

### JTA vs RESOURCE_LOCAL

- **JTA:** o servidor gerencia as transações (Jakarta Transactions). Uma transação
  pode abranger vários recursos (dois bancos de dados, uma fila JMS) com *two-phase
  commit*. O nosso código nunca chama `begin()`/`commit()`.
- **RESOURCE_LOCAL:** a aplicação gerencia a transação diretamente sobre a conexão
  JDBC (`em.getTransaction().begin()`). É o que o Spring Boot usa por padrão, e o
  que usamos nos testes fora do servidor.

### DataSource por JNDI: a configuração sai do código

O WAR não sabe a URL, o usuário nem a senha do banco de dados. Ele pede ao
servidor um recurso pelo nome JNDI. `java:comp/DefaultDataSource` é obrigatório
em qualquer servidor Jakarta EE (no WildFly e no Payara aponta para um H2 em
memória), por isso a aplicação sobe sem configuração nenhuma.

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

O *pool* de conexões também é do servidor (no Spring Boot é o HikariCP, embutido
na aplicação). Benefício corporativo: o mesmo WAR é promovido de DEV → QA → PROD
sem ser recompilado; cada ambiente tem o seu próprio DataSource, e as credenciais nunca
passam pelo repositório Git.

> ⚠️ `drop-and-create` e o script de dados iniciais servem só para
> desenvolvimento. Em produção se usa `none` e um gerenciador de migrações
> (Flyway/Liquibase), assim como no Spring Boot.

### Detalhe: `hibernate.hbm2ddl.charset_name`

O Hibernate lê o `dados-iniciais.sql` com o charset padrão da JVM. No
Windows com Java 17 esse charset é `Cp1252`, e "Edifício" ficaria corrompido.
Fixamos `UTF-8` explicitamente. (A partir do Java 18 o default já é UTF-8.)

## 3. Como funciona o `EntityManager`

### Três peças

| Peça | O que é | Quantas | Thread-safe? |
|---|---|---|---|
| `EntityManagerFactory` | Objeto pesado: metadados, pool, caches | 1 por Persistence Unit | Sim |
| `EntityManager` | Sessão de trabalho com o banco | 1 por transação | **Não** |
| Persistence Context | O conjunto de entidades *managed* dentro do `EntityManager` | 1 por `EntityManager` | — |

### O que é injetado com `@PersistenceContext`

O `EntityManager` real **não é thread-safe**. Mas o que o contêiner injeta é um
**proxy**: a cada chamada, o proxy procura o `EntityManager` associado à
transação JTA atual (ou o cria). Por isso o DAO pode ser um singleton
(`@ApplicationScoped`) compartilhado por todas as requisições: cada transação recebe o
seu próprio Persistence Context. Quando a transação termina, o Persistence
Context fecha e as entidades ficam *detached* (aquele estado do Passo 1).

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

Vimos isso no teste: um `persist` seguido de `setLocalizacao()` na mesma
transação produziu

```sql
insert into obra (...) values (...)
update obra set ..., versao=? where id=? and versao=?
```

sem nenhum `save()` explícito. O `where ... and versao=?` é o `@Version` do
Passo 1 em ação.

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

## 4. JPQL, Criteria API e parâmetros

- **JPQL** consulta *entidades e atributos Java* (`Obra`, `o.nome`), não tabelas
  e colunas. O provider traduz para o SQL do dialeto do banco de dados — por isso
  o mesmo DAO funciona no H2 e no PostgreSQL.
- **`r.obra.id`** navega até o ID da associação e é traduzido diretamente para
  a coluna FK, sem `JOIN`:
  ```sql
  select ... from relatorio_seguranca rs1_0 where rs1_0.obra_id=? order by rs1_0.data_inspecao desc
  ```
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

**Trade-offs:** o DAO manual é mais verboso, mas não tem "mágica" — o que você lê é o
que executa, e você tem controle total sobre cada query. O Spring Data elimina o
*boilerplate*, mas esconde conceitos (`persist` vs `merge`, *flush*) que
continuam existindo e causando bugs.

> **Para mencionar na entrevista:** o **Jakarta Data** (Jakarta EE 11) traz
> repositórios declarativos no estilo Spring Data para a plataforma padrão.
> Em projetos Java EE legados você vai encontrar quase sempre o padrão DAO com
> `EntityManager`, como aqui.

## 6. Escalabilidade

- **DAOs sem estado**: um singleton atende todas as requisições em paralelo; nada
  para sincronizar.
- **Pool de conexões gerenciado pelo servidor**, dimensionável sem recompilar.
- **Carregar só o necessário**: LAZY por padrão, `JOIN FETCH` por caso de uso,
  `setMaxResults` para limitar, `COUNT` em vez de carregar listas para contar.
- **Bulk operations** para operações em massa (1 SQL em vez de N).
- **Índice em `obra_id`** (Passo 1), que sustenta `listarPorObra` e `contarPorObra`.

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
| Descoberta de beans | CDI (sem `beans.xml` desde o CDI 4) | Component scan |

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
