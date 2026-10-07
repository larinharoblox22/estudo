# Passo 1 — O Modelo (JPA & OOP)

## Arquivos deste passo

```
gestao-obras/
├── pom.xml
└── src/main/java/br/com/exemplo/gestaoobras/model/
    ├── StatusObra.java
    ├── Obra.java
    └── RelatorioSeguranca.java
```

## 0. Contexto: J2EE → Java EE → Jakarta EE

"J2EE" é o nome histórico (1999–2006). Passou a se chamar **Java EE** (5 a 8) e,
desde que a Oracle doou a plataforma à Eclipse Foundation, se chama **Jakarta EE**.
A grande diferença prática é o pacote: `javax.persistence.*` → `jakarta.persistence.*`.

Usamos **Jakarta EE 10**. Se na entrevista aparecer código legado com
`javax.*`, os conceitos são exatamente os mesmos — só muda o import.

> **Paralelo Spring Boot:** o Spring Boot 3 também migrou para `jakarta.persistence`.
> As anotações JPA que você vê aqui são **literalmente as mesmas** que você usa no Spring.
> O que muda é *quem* fornece a implementação: no Spring Boot é o Hibernate
> embutido no JAR; no Jakarta EE é o servidor de aplicação (o WildFly traz o Hibernate,
> o Payara traz o EclipseLink). Daí o `<scope>provided</scope>` e o `<packaging>war</packaging>`.

## 1. Encapsulamento

Todos os campos são `private`; o acesso é feito por métodos. O ponto importante
não é "ter getters e setters", é **controlar quais existem**:

| Campo | Getter | Setter | Por quê |
|---|---|---|---|
| `id` | ✔ | ✘ | Gerado pelo banco. Alterá-lo à mão corromperia a identidade da entidade. |
| `versao` | ✔ | ✘ | Gerenciado pelo JPA para concorrência otimista. |
| `dataCriacao` / `dataAtualizacao` | ✔ | ✘ | Preenchidos pelos callbacks `@PrePersist` / `@PreUpdate`. |
| `obra` (no relatório) | ✔ | ✔ com `requireNonNull` | Invariante: um relatório nunca existe sem obra. |
| `nome`, `localizacao`, `status` | ✔ | ✔ | Necessários para o binding dos formulários JSF (Passo 4). |

Também colocamos comportamento na entidade (`isConcluida()`), em vez de espalhar
`obra.getStatus() == StatusObra.CONCLUIDA` pelo código.

**Trade-off honesto:** o JSF (e o JAX-RS/JSON-B) exigem a convenção JavaBeans
(construtor público sem argumentos + getters/setters). Isso nos empurra para um
modelo um tanto "anêmico". Em um sistema maior, seriam usados DTOs na camada de
apresentação para proteger a entidade; aqui mantemos simples e protegemos
apenas o que é crítico.

> **Paralelo Spring Boot:** é igual. A diferença é que no Spring muitas equipes
> usam Lombok (`@Getter @Setter`). Em um projeto Jakarta EE "puro" é comum
> escrever à mão, o que torna explícita a decisão de *não* expor `setId()`.

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

**Bean Validation (`@NotBlank`, `@Size`, `@NotNull`, `@PastOrPresent`)** não é JPA,
é outra especificação (Jakarta Validation), mas se integra automaticamente com
três camadas: o **JSF** valida ao submeter o formulário, o **JPA** valida antes do
`INSERT/UPDATE`, e o **JAX-RS** valida o JSON recebido. Uma regra, três pontos
de aplicação — reutilização real.

Repare que a obrigatoriedade da obra está declarada três vezes, de propósito:
- `@NotNull` → falha cedo, na aplicação, com mensagem amigável;
- `optional = false` → o Hibernate pode usar `INNER JOIN` e otimizar;
- `nullable = false` → constraint `NOT NULL` no banco, a última linha de defesa.

**IDENTITY vs SEQUENCE:** escolhemos `IDENTITY` por simplicidade e porque
funciona igual no H2 e no PostgreSQL. Contudo, com `IDENTITY` o Hibernate precisa
executar o `INSERT` imediatamente para saber o ID, o que **desativa o batching
JDBC**. No PostgreSQL com cargas grandes, `GenerationType.SEQUENCE` com
`allocationSize` é a escolha mais performática. Boa resposta de entrevista.

> **Paralelo Spring Boot:** anotações idênticas. A auditoria que no Spring você faria com
> `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` (Spring Data)
> é feita aqui com os callbacks padrão `@PrePersist`/`@PreUpdate` — que também
> funcionam no Spring, porque são JPA puro. A validação que no Spring exige
> `spring-boot-starter-validation` + `@Valid` já vem incluída em qualquer
> servidor Jakarta EE.

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

- **New/Transient:** objeto Java normal; o JPA não o conhece; `id == null`.
- **Managed:** está no *Persistence Context* (o "cache de 1º nível" do
  `EntityManager`). Qualquer alteração feita por um setter é detectada (**dirty checking**)
  e convertida em um `UPDATE` no commit — sem chamar nenhum `save()`.
- **Detached:** tem ID, mas o `EntityManager` que o gerenciou já fechou. Alterações
  **não** são gravadas; é preciso `merge()` para reanexá-lo.
- **Removed:** agendado para `DELETE` no commit.

**Por que isso importa no nosso projeto:** no Passo 4, a `Obra` em edição
vive em um bean JSF `@ViewScoped` durante várias requisições HTTP. Entre requisições, a
transação do EJB já terminou, logo a entidade está **Detached**. Gravar exige
`merge()` — e é o `@Version` que garante que não vamos sobrescrever alterações de outro
usuário feitas nesse meio-tempo.

Os callbacks disponíveis acompanham as transições: `@PrePersist`, `@PostPersist`,
`@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad`.

> **Paralelo Spring Boot:** o ciclo de vida é o mesmo (é JPA). A diferença é que no
> Spring você chama `repository.save()`, que por baixo decide entre `persist()` (ID nulo)
> e `merge()` (ID preenchido) — e esconde o conceito de você. No Jakarta EE você vai usar o
> `EntityManager` diretamente (Passo 2), por isso precisa saber o estado da entidade.

## 4. Por que `FetchType.LAZY` na relação

**Armadilha clássica de entrevista:** o default de `@ManyToOne` e `@OneToOne` na
especificação JPA é **EAGER**. Só `@OneToMany` e `@ManyToMany` são LAZY por
padrão. Por isso precisamos declarar `LAZY` explicitamente.

**Com EAGER**, cada relatório carregado traz a obra junto. Em uma query JPQL como
`SELECT r FROM RelatorioSeguranca r` que retorne 500 relatórios de 50 obras, o
Hibernate executa 1 query para os relatórios + até 50 queries extras para as obras:
o famoso **problema N+1**. E um EAGER declarado no mapeamento é difícil de
"desligar" em uma query específica, enquanto um LAZY se "liga" facilmente com `JOIN FETCH`.

**Com LAZY**, o campo `obra` é preenchido com um **proxy** (subclasse gerada em
runtime) que só contém o ID. O `SELECT` na tabela `obra` só acontece quando se
chama um método como `getNome()`. Validamos isso: ao executar
`em.find(RelatorioSeguranca.class, id)` o SQL gerado foi apenas

```sql
select rs1_0.id, rs1_0.data_inspecao, rs1_0.descricao, rs1_0.obra_id
from relatorio_seguranca rs1_0 where rs1_0.id = ?
```

sem nenhum join com a `obra`.

**LAZY é o default seguro; quando precisarmos da obra, nós a pedimos
explicitamente** com `JOIN FETCH` na query (Passo 2). Assim cada caso de uso
carrega exatamente o que precisa — é isso que escala.

**O custo:** acessar o proxy depois que a transação terminou lança
`LazyInitializationException`. Por isso o `toString()` do relatório **não**
inclui a obra: um simples `log.info(relatorio)` fora da transação quebraria.

> **Paralelo Spring Boot:** o Spring Boot ativa por padrão o **Open Session In View**
> (`spring.jpa.open-in-view=true`), que mantém a sessão aberta até a view ser
> renderizada, escondendo a `LazyInitializationException` (e gerando queries
> ocultas). No Jakarta EE **não há OSIV por padrão**: o contexto de persistência
> fecha quando a transação do EJB termina. Isso obriga a uma disciplina melhor:
> carregar o que a view precisa dentro da camada de negócio.

## 5. Relação unidirecional (sem `@OneToMany` em `Obra`)

Mapeamos apenas o lado `@ManyToOne`. Não colocamos `List<RelatorioSeguranca>` na
`Obra` porque:
- uma obra pode acumular milhares de relatórios ao longo dos anos; uma coleção
  mapeada convida a carregá-los todos sem querer;
- "relatórios da obra X" é uma query (`WHERE r.obra.id = :id`), com paginação,
  que faremos no DAO;
- uma relação bidirecional exige manter os dois lados sincronizados e cuidado
  com ciclos em `toString`/`equals`/serialização JSON.

O lado `@ManyToOne` é o **owning side**: é a tabela `relatorio_seguranca` que tem
a coluna `obra_id`. Se um dia for preciso o inverso, basta adicionar
`@OneToMany(mappedBy = "obra")` sem alterar o banco.

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

## 8. Perguntas prováveis na entrevista

1. *Qual é o fetch default de `@ManyToOne`?* → EAGER. Declaramos LAZY e usamos `JOIN FETCH` quando é preciso.
2. *O que é o problema N+1?* → 1 query para a lista + 1 para cada associação. Resolve-se com LAZY + `JOIN FETCH` / Entity Graphs.
3. *Por que `EnumType.STRING`?* → `ORDINAL` corrompe dados se a ordem do enum mudar.
4. *Para que serve `@Version`?* → Concorrência otimista; evita *lost updates* entre usuários.
5. *Diferença entre `persist` e `merge`?* → `persist` torna Managed uma entidade nova; `merge` copia o estado de uma entidade Detached para uma cópia Managed e retorna essa cópia.
6. *Owning side de uma relação?* → O lado que tem a FK (`@ManyToOne`/`@JoinColumn`); o `mappedBy` marca o lado inverso.
