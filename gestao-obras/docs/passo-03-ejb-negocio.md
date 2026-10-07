# Passo 3 — A Camada de Negócio (EJB)

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

## 1. O padrão Session Facade

O Service é uma **fachada**: oferece aos clientes (tela JSF no Passo 4, API REST
no Passo 5) **operações de negócio completas** — "excluir obra", "registrar
relatório com interdição" — e esconde os DAOs, as queries e as transações.

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

**Session Facade** é um dos *Core J2EE Patterns*. A ideia original era reduzir o
número de chamadas remotas (cada uma cara); hoje o benefício principal é ter
**uma operação = um caso de uso = uma transação**, em um único lugar.

> **Paralelo Spring Boot:** é a sua camada `@Service`. A diferença está no que vem
> "de graça" do contêiner, como você verá a seguir.

## 2. `@Stateless`: o que é e por que usar

Um *session bean* `@Stateless` é um componente gerenciado pelo contêiner EJB que
**não guarda estado de conversa com o cliente** entre chamadas.

### Ciclo de vida

```
 (não existe) ──construtor → injeções → @PostConstruct──► [ pool de instâncias prontas ]
                                                                 │      ▲
                                                   chamada chega │      │ chamada termina
                                                                 ▼      │
                                                          [ executando o método ]
 (não existe) ◄──@PreDestroy── (contêiner decide encolher o pool)
```

O contêiner mantém um **pool** de instâncias. Cada chamada recebe uma instância
**exclusiva** durante a execução do método. Consequências:
- **Thread-safety sem `synchronized`:** duas threads nunca usam a mesma instância ao mesmo tempo.
- **Nenhum estado de cliente nos campos:** a próxima chamada do mesmo cliente
  pode cair em outra instância. Campos só para dependências injetadas.
- **Escalabilidade:** o pool cresce e encolhe com a carga; o tamanho é configuração
  do servidor, não do código.

### Os tipos de session bean

| EJB | Comportamento | Equivalente aproximado no Spring |
|---|---|---|
| `@Stateless` | Pool, sem estado de conversa | `@Service` (singleton sem estado) |
| `@Stateful` | Uma instância por cliente, mantém estado (ex.: carrinho), pode ser passivada em disco | Bean `@SessionScope` |
| `@Singleton` | Uma instância por aplicação; concorrência controlada por `@Lock(READ/WRITE)` | Singleton padrão (sem locks automáticos) |
| `@MessageDriven` | Consome mensagens JMS | `@JmsListener` / `@KafkaListener` |

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

No Spring você monta isso à la carte (starters + anotações). No EJB tudo já faz
parte da plataforma.

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

As três "visões" de um EJB:

| Visão | Como declarar | Quem pode chamar |
|---|---|---|
| **No-interface view** | Só `@Stateless` na classe (desde o EJB 3.1) | Código na mesma aplicação |
| **`@Local`** | Interface de negócio local | Código na mesma JVM/aplicação (passagem por referência) |
| **`@Remote`** | Interface de negócio remota | Outras JVMs (chamada remota; passagem por valor, com serialização) |

**História que cai em entrevista:** no EJB 2.x as interfaces eram obrigatórias
(Home + Remote/Local para cada bean), com muito código repetitivo. O EJB 3.0
(Java EE 5) trouxe as anotações, e o EJB 3.1 (Java EE 6) tornou a interface
opcional com a *no-interface view*.

### Nomes JNDI portáveis

Ao implantar, o WildFly registrou (log real):

```
java:global/gestao-obras/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
java:app/gestao-obras/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
java:module/ObraServiceBean!br.com.exemplo.gestaoobras.service.ObraService
```

No J2EE clássico, o cliente fazia `new InitialContext().lookup("java:global/...")`
(padrão *Service Locator*). Hoje a injeção (`@EJB` / `@Inject`) faz esse lookup
por você. No Spring, o equivalente seria `applicationContext.getBean(...)`.

## 4. Injeção de dependências: `@Inject` vs `@EJB`

Usamos as duas de propósito em `RelatorioSegurancaServiceBean`:

```java
@EJB
private ObraService obraService;          // outro EJB

@Inject
private RelatorioSegurancaDao relatorioDao; // bean CDI
```

| | `@Inject` (CDI) | `@EJB` |
|---|---|---|
| Injeta | Qualquer bean CDI, **inclusive EJBs** | Só EJBs |
| Resolução | Por tipo + qualificadores (`@Named`, anotações próprias) | Por tipo, `beanName` ou `lookup` JNDI |
| EJB remoto | Não | Sim |
| Quando usar | Padrão moderno, para quase tudo | EJB remoto, lookup por nome, código legado |

`@Inject ObraService obraService` funcionaria igual aqui. O `@EJB` deixa explícito
que estamos chamando **outro EJB**, e que a chamada passa pelo contêiner
(transação, segurança, interceptadores).

> **Paralelo Spring Boot:** `@Inject` ≈ `@Autowired`. No Spring, a recomendação é
> injeção pelo **construtor**. Em EJB, a injeção por **campo** é o padrão, porque a
> especificação exige um construtor público sem argumentos.

## 5. Transações ACID com CMT

**CMT (Container-Managed Transactions):** todo método público de um EJB é
transacional **sem nenhuma anotação**. O contêiner abre a transação JTA ao
entrar no método e faz commit ao sair (ou rollback, conforme a exceção). O
`@TransactionAttribute(REQUIRED)` no `ObraServiceBean` está lá só por didática —
é o padrão.

> ⚠️ **Diferença importante para quem vem do Spring:** no Spring, um método de
> `@Service` **sem** `@Transactional` não tem transação. No EJB, tem.

### Atributos de transação

| EJB (`@TransactionAttribute`) | Comportamento | Spring (`propagation`) |
|---|---|---|
| `REQUIRED` (padrão) | Entra na transação existente ou cria uma nova | `REQUIRED` (padrão) |
| `REQUIRES_NEW` | Sempre cria uma nova, suspendendo a atual | `REQUIRES_NEW` |
| `MANDATORY` | Exige uma transação existente, senão falha | `MANDATORY` |
| `SUPPORTS` | Usa a transação se houver; senão roda sem | `SUPPORTS` |
| `NOT_SUPPORTED` | Suspende a transação atual e roda sem | `NOT_SUPPORTED` |
| `NEVER` | Falha se houver transação | `NEVER` |
| — | — | `NESTED` (savepoints; não existe em EJB) |

No projeto:
- **Services:** `REQUIRED` → cada operação de negócio é uma transação.
- **DAOs:** `MANDATORY` (Passo 2) → nunca abrem transação própria.
- **Chamada entre EJBs:** `RelatorioSegurancaServiceBean.registrar` chama
  `obraService.buscarPorId`, que é `REQUIRED` e **entra na mesma transação**.
- **`REQUIRES_NEW`** (não usado aqui) é o caso clássico de log de auditoria: o
  registro "tentativa de excluir obra X" deve ser gravado mesmo que a operação
  principal sofra rollback.

> 💡 EJB não tem `readOnly` como o `@Transactional(readOnly = true)` do Spring.
> Quando necessário, usa-se uma hint do provider na query (ex.: `org.hibernate.readOnly`).

### A — Atomicidade: tudo ou nada

`ObraServiceBean.excluir` faz duas escritas em uma única transação:

```java
relatorioDao.removerPorObra(obra.getId());   // DELETE relatórios
obraDao.removerPorId(obra.getId());          // DELETE obra
```

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

- ⚠️ **Limite honesto:** `validarNomeUnico` é um *check-then-act*. Duas transações
  simultâneas podem passar pela verificação antes de qualquer uma gravar. A
  garantia definitiva é um índice único no banco, criado via migração
  (ex.: `CREATE UNIQUE INDEX ux_obra_nome ON obra (LOWER(nome))` no PostgreSQL).
  A verificação no Service continua útil para dar uma mensagem amigável.

### D — Durabilidade: depois do commit, o dado sobrevive

No PostgreSQL, o commit só retorna depois de gravado no log (WAL). ⚠️ O nosso H2
**em memória** não é durável: os dados somem quando o servidor reinicia. Serve
para desenvolvimento, não para produção.

### Persistence Context compartilhado e dirty checking

```java
Obra obra = obraService.buscarPorId(obraId);   // outro EJB, MESMA transação
...
obra.setStatus(StatusObra.SUSPENSA);           // nenhum update() explícito
```

Como as duas classes estão na mesma transação JTA, compartilham o mesmo
Persistence Context: a obra retornada está **managed**, e o dirty checking gera
o `UPDATE` no commit. Confirmado no teste 08 (`status=SUSPENSA`).

## 6. Exceções e rollback (a pergunta mais traiçoeira sobre EJB)

O contêiner EJB divide as exceções em dois tipos:

| | Exceção de **sistema** | Exceção de **aplicação** |
|---|---|---|
| O que é | `RuntimeException` sem `@ApplicationException` (ex.: `OptimisticLockException`, `ConstraintViolationException`, `NullPointerException`) | Exceção *checked*, ou `RuntimeException` anotada com `@ApplicationException` |
| Rollback | **Sempre** | **Só** se `rollback = true` (ou `setRollbackOnly()`) |
| O cliente recebe | `EJBException` embrulhando a original (ou `EJBTransactionRolledbackException` se o cliente já tinha transação) | A própria exceção, sem embrulho |
| Instância do bean | **Descartada** do pool | Continua no pool |
| Log | O contêiner registra como erro | Não registra (é esperada) |

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

### Auto-invocação (self-invocation)

Se um método do EJB chama outro método **do mesmo bean** com `this.metodo()`, a
chamada **não passa pelo contêiner**: o `@TransactionAttribute` do segundo método
é ignorado. No Spring acontece exatamente o mesmo com `@Transactional` (o proxy é
contornado). A solução é chamar via outro bean (como fizemos com `@EJB ObraService`)
ou via `sessionContext.getBusinessObject(ObraService.class)`.

## 7. Testabilidade: EJBs são POJOs

Desde o EJB 3, um session bean é uma classe Java comum com anotações. As regras de
negócio podem ser testadas **sem servidor**, com mocks (exemplo com JUnit 5 + Mockito):

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

O comportamento do contêiner (transações, rollback, JNDI) se testa com testes de
integração (Arquillian ou implantação real), como fizemos no WildFly.

## 8. SOLID neste passo

| Princípio | Onde |
|---|---|
| **S** | Service = regras + fronteira transacional; DAO = acesso a dados; `StatusObra` = regra de transição; exceções = sinalização. |
| **O** | Nova regra de transição se adiciona no `StatusObra`, sem mexer no Service. Novos tipos de erro estendem `RegraNegocioException` sem mudar quem já trata a classe-mãe. |
| **L** | `EntidadeNaoEncontradaException` substitui `RegraNegocioException` em qualquer `catch` e herda o rollback. Os contratos (`@throws`) estão documentados na interface: qualquer implementação deve honrá-los. |
| **I** | Dois services pequenos (por agregado) em vez de um `GestaoObrasService` gigante. A API REST receberá um contrato só de leitura no Passo 5. |
| **D** | Clientes dependem de `ObraService` / `RelatorioSegurancaService` (`@Local`), nunca dos `*Bean`. |

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
