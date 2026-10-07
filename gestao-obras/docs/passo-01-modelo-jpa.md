# Passo 1 — O Modelo (JPA & OOP)

## Ficheiros deste passo

```
gestao-obras/
├── pom.xml
└── src/main/java/pt/exemplo/gestaoobras/model/
    ├── StatusObra.java
    ├── Obra.java
    └── RelatorioSeguranca.java
```

## 0. Contexto: J2EE → Java EE → Jakarta EE

"J2EE" é o nome histórico (1999–2006). Passou a **Java EE** (5 a 8) e, desde
que a Oracle doou a plataforma à Eclipse Foundation, chama-se **Jakarta EE**.
A grande diferença prática é o pacote: `javax.persistence.*` → `jakarta.persistence.*`.

Usamos **Jakarta EE 10**. Se na entrevista aparecer código legado com
`javax.*`, os conceitos são exatamente os mesmos — só muda o import.

> **Paralelo Spring Boot:** o Spring Boot 3 também migrou para `jakarta.persistence`.
> As anotações JPA que vês aqui são **literalmente as mesmas** que usas no Spring.
> O que muda é *quem* fornece a implementação: no Spring Boot é o Hibernate
> embutido no JAR; em Jakarta EE é o servidor aplicacional (WildFly traz Hibernate,
> Payara traz EclipseLink). Daí o `<scope>provided</scope>` e o `<packaging>war</packaging>`.

## 1. Encapsulamento

Todos os campos são `private`; o acesso é feito por métodos. O ponto importante
não é "ter getters e setters", é **controlar quais existem**:

| Campo | Getter | Setter | Porquê |
|---|---|---|---|
| `id` | ✔ | ✘ | Gerado pela BD. Alterá-lo à mão corromperia a identidade da entidade. |
| `versao` | ✔ | ✘ | Gerido pelo JPA para concorrência otimista. |
| `dataCriacao` / `dataAtualizacao` | ✔ | ✘ | Preenchidos pelos callbacks `@PrePersist` / `@PreUpdate`. |
| `obra` (no relatório) | ✔ | ✔ com `requireNonNull` | Invariante: um relatório nunca existe sem obra. |
| `nome`, `localizacao`, `status` | ✔ | ✔ | Necessários para o binding dos formulários JSF (Passo 4). |

Também colocámos comportamento na entidade (`isConcluida()`), em vez de espalhar
`obra.getStatus() == StatusObra.CONCLUIDA` pelo código.

**Trade-off honesto:** o JSF (e o JAX-RS/JSON-B) exigem a convenção JavaBeans
(construtor público sem argumentos + getters/setters). Isto empurra-nos para um
modelo algo "anémico". Num sistema maior usar-se-iam DTOs na camada de
apresentação para proteger a entidade; aqui mantemos simples e protegemos
apenas o que é crítico.

> **Paralelo Spring Boot:** é igual. A diferença é que no Spring muitas equipas
> usam Lombok (`@Getter @Setter`). Num projeto Jakarta EE "puro" é comum
> escrever à mão, o que torna explícita a decisão de *não* expor `setId()`.

## 2. Anotações JPA usadas

| Anotação | O que faz | Nota prática |
|---|---|---|
| `@Entity` | Marca a classe como persistível. | Precisa de construtor sem argumentos (public/protected) e não pode ser `final` (o Hibernate cria subclasses proxy). |
| `@Table(name=…, indexes=…)` | Nome da tabela e índices. | O PostgreSQL **não** cria índice automático em FKs; declaramos `idx_relatorio_obra` porque vamos filtrar relatórios por obra. |
| `@Id` + `@GeneratedValue(IDENTITY)` | Chave primária gerada pela BD. | Ver nota IDENTITY vs SEQUENCE abaixo. |
| `@Column(nullable, length, updatable)` | Metadados da coluna (DDL). | `updatable = false` em `data_criacao` impede que um `UPDATE` a altere. |
| `@Enumerated(EnumType.STRING)` | Grava o nome da constante (`EM_CURSO`). | **Nunca** usar `ORDINAL` (default): inserir uma constante a meio do enum desloca todos os valores gravados. |
| `@Version` | Concorrência otimista. | Cada `UPDATE` faz `WHERE versao = ?`; se outro utilizador gravou antes, lança `OptimisticLockException`. Essencial em ecrãs de edição concorridos. |
| `@ManyToOne(fetch = LAZY, optional = false)` | Relação N:1. | Ver secção 4. |
| `@JoinColumn(name="obra_id", foreignKey=…)` | Nome da coluna FK e da constraint. | Nomes explícitos tornam os erros de BD legíveis (`fk_relatorio_obra` em vez de `FKa3b9…`). |
| `@PrePersist` / `@PreUpdate` | Callbacks do ciclo de vida. | Auditoria automática. |

**Bean Validation (`@NotBlank`, `@Size`, `@NotNull`, `@PastOrPresent`)** não é JPA,
é outra especificação (Jakarta Validation), mas integra-se automaticamente com
três camadas: o **JSF** valida ao submeter o formulário, o **JPA** valida antes do
`INSERT/UPDATE`, e o **JAX-RS** valida o JSON recebido. Uma regra, três pontos
de aplicação — reutilização real.

Repara que a obrigatoriedade da obra está declarada três vezes, de propósito:
- `@NotNull` → falha cedo, na aplicação, com mensagem amigável;
- `optional = false` → o Hibernate pode usar `INNER JOIN` e otimizar;
- `nullable = false` → constraint `NOT NULL` na BD, a última linha de defesa.

**IDENTITY vs SEQUENCE:** escolhemos `IDENTITY` por simplicidade e porque
funciona igual em H2 e PostgreSQL. Contudo, com `IDENTITY` o Hibernate tem de
executar o `INSERT` imediatamente para saber o ID, o que **desativa o batching
JDBC**. Em PostgreSQL com cargas grandes, `GenerationType.SEQUENCE` com
`allocationSize` é a escolha mais performante. Boa resposta de entrevista.

> **Paralelo Spring Boot:** anotações idênticas. A auditoria que no Spring farias com
> `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` (Spring Data)
> faz-se aqui com os callbacks standard `@PrePersist`/`@PreUpdate` — que também
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
- **Managed:** está no *Persistence Context* (a "cache de 1.º nível" do
  `EntityManager`). Qualquer alteração a um setter é detetada (**dirty checking**)
  e convertida num `UPDATE` no commit — sem chamar nenhum `save()`.
- **Detached:** tem ID, mas o `EntityManager` que o geriu já fechou. Alterações
  **não** são gravadas; é preciso `merge()` para o reanexar.
- **Removed:** agendado para `DELETE` no commit.

**Porque é que isto importa no nosso projeto:** no Passo 4, a `Obra` em edição
vive num bean JSF `@ViewScoped` durante vários pedidos HTTP. Entre pedidos, a
transação do EJB já terminou, logo a entidade está **Detached**. Gravar exige
`merge()` — e é o `@Version` que garante que não esmagamos alterações de outro
utilizador feitas entretanto.

Os callbacks disponíveis acompanham as transições: `@PrePersist`, `@PostPersist`,
`@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad`.

> **Paralelo Spring Boot:** o ciclo de vida é o mesmo (é JPA). A diferença é que no
> Spring chamas `repository.save()`, que por baixo decide entre `persist()` (ID nulo)
> e `merge()` (ID preenchido) — esconde-te o conceito. Em Jakarta EE vais usar o
> `EntityManager` diretamente (Passo 2), por isso precisas de saber o estado da entidade.

## 4. Porquê `FetchType.LAZY` na relação

**Armadilha clássica de entrevista:** o default de `@ManyToOne` e `@OneToOne` na
especificação JPA é **EAGER**. Só `@OneToMany` e `@ManyToMany` são LAZY por
defeito. Por isso temos de declarar `LAZY` explicitamente.

**Com EAGER**, cada relatório carregado traz a obra atrás. Numa query JPQL como
`SELECT r FROM RelatorioSeguranca r` que devolva 500 relatórios de 50 obras, o
Hibernate executa 1 query para os relatórios + até 50 queries extra para as obras:
o famoso **problema N+1**. E um EAGER declarado no mapeamento é difícil de
"desligar" numa query concreta, enquanto um LAZY se "liga" facilmente com `JOIN FETCH`.

**Com LAZY**, o campo `obra` é preenchido com um **proxy** (subclasse gerada em
runtime) que só contém o ID. O `SELECT` à tabela `obra` só acontece quando se
chama um método como `getNome()`. Validámos isto: ao fazer
`em.find(RelatorioSeguranca.class, id)` o SQL gerado foi apenas

```sql
select rs1_0.id, rs1_0.data_inspecao, rs1_0.descricao, rs1_0.obra_id
from relatorio_seguranca rs1_0 where rs1_0.id = ?
```

sem qualquer join à `obra`.

**LAZY é o default seguro; quando precisarmos da obra, pedimo-la
explicitamente** com `JOIN FETCH` na query (Passo 2). Assim cada caso de uso
carrega exatamente o que precisa — é isto que escala.

**O custo:** aceder ao proxy depois de a transação terminar lança
`LazyInitializationException`. Por isso o `toString()` do relatório **não**
inclui a obra: um simples `log.info(relatorio)` fora da transação rebentaria.

> **Paralelo Spring Boot:** o Spring Boot ativa por defeito o **Open Session In View**
> (`spring.jpa.open-in-view=true`), que mantém a sessão aberta até a view ser
> renderizada, escondendo a `LazyInitializationException` (e gerando queries
> escondidas). Em Jakarta EE **não há OSIV por defeito**: o contexto de persistência
> fecha quando a transação do EJB termina. Isto obriga a uma disciplina melhor:
> carregar o que a view precisa dentro da camada de negócio.

## 5. Relação unidirecional (sem `@OneToMany` em `Obra`)

Mapeámos apenas o lado `@ManyToOne`. Não pusemos `List<RelatorioSeguranca>` na
`Obra` porque:
- uma obra pode acumular milhares de relatórios ao longo dos anos; uma coleção
  mapeada convida a carregá-los todos sem querer;
- "relatórios da obra X" é uma query (`WHERE r.obra.id = :id`), com paginação,
  que faremos no DAO;
- uma relação bidirecional exige manter os dois lados sincronizados e cuidado
  com ciclos em `toString`/`equals`/serialização JSON.

O lado `@ManyToOne` é o **owning side**: é a tabela `relatorio_seguranca` que tem
a coluna `obra_id`. Se um dia for preciso o inverso, adiciona-se
`@OneToMany(mappedBy = "obra")` sem alterar a BD.

## 6. `equals`/`hashCode` e `Serializable`

- **`equals` pelo ID** com `instanceof` e `getId()` (não `getClass()` nem
  `outra.id`): o proxy LAZY é uma *subclasse* de `Obra` e os seus campos estão
  vazios, só os métodos funcionam.
- **`hashCode` constante:** antes do `persist()` o ID é `null`, depois não. Se o
  hash dependesse do ID, um objeto colocado num `HashSet` antes de gravar
  "desapareceria" depois de gravar.
- **`Serializable`:** os beans `@ViewScoped` do JSF guardam estado na sessão HTTP,
  que pode ser serializada (replicação em cluster, passivação). Se a entidade lá
  dentro não for serializável, rebenta.

## 7. Resumo do paralelo Jakarta EE ↔ Spring Boot (Passo 1)

| Conceito | Jakarta EE (este projeto) | Spring Boot |
|---|---|---|
| Pacote JPA | `jakarta.persistence` | `jakarta.persistence` (igual, desde o Boot 3) |
| Implementação JPA | Fornecida pelo servidor (`provided`) | Hibernate embutido via `spring-boot-starter-data-jpa` |
| Artefacto | WAR implantado num servidor | JAR executável com Tomcat embutido |
| Validação | Incluída na plataforma | `spring-boot-starter-validation` |
| Auditoria | `@PrePersist` / `@PreUpdate` | Idem, ou `@CreatedDate` do Spring Data |
| Configuração da BD | `persistence.xml` + DataSource do servidor (Passo 2) | `application.properties` |
| Lazy fora da transação | `LazyInitializationException` (sem OSIV) | Escondido pelo OSIV por defeito |

## 8. Perguntas prováveis na entrevista

1. *Qual é o fetch default de `@ManyToOne`?* → EAGER. Declaramos LAZY e usamos `JOIN FETCH` quando é preciso.
2. *O que é o problema N+1?* → 1 query para a lista + 1 por cada associação. Resolve-se com LAZY + `JOIN FETCH` / Entity Graphs.
3. *Porquê `EnumType.STRING`?* → `ORDINAL` corrompe dados se a ordem do enum mudar.
4. *Para que serve `@Version`?* → Concorrência otimista; evita *lost updates* entre utilizadores.
5. *Diferença entre `persist` e `merge`?* → `persist` torna Managed uma entidade nova; `merge` copia o estado de uma entidade Detached para uma cópia Managed e devolve essa cópia.
6. *Owning side de uma relação?* → O lado que tem a FK (`@ManyToOne`/`@JoinColumn`); o `mappedBy` marca o lado inverso.
