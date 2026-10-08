# Gestão de Obras — Jakarta EE (exemplo de estudo)

Aplicação de exemplo para preparação de entrevista técnica: CRUD de **Obras** e
**Relatórios de Segurança** com a stack Jakarta EE (antigo J2EE), sempre com
paralelos ao Spring Boot.

**Stack:** Java 17 · Jakarta EE 10 (JPA, EJB, CDI, JSF + PrimeFaces, JAX-RS) · H2/PostgreSQL · WildFly

## Progresso

| Passo | Tema | Status | Relatório |
|---|---|---|---|
| 1 | Modelo (JPA & OOP) | ✅ | [docs/passo-01-modelo-jpa.md](docs/passo-01-modelo-jpa.md) |
| 2 | DAO e EntityManager | ✅ | [docs/passo-02-dao-entitymanager.md](docs/passo-02-dao-entitymanager.md) |
| 3 | Camada de negócio (EJB) | ✅ | [docs/passo-03-ejb-negocio.md](docs/passo-03-ejb-negocio.md) |
| 4 | Frontend (JSF & PrimeFaces) | ✅ | [docs/passo-04-jsf-primefaces.md](docs/passo-04-jsf-primefaces.md) |
| 5 | Integração (API REST JAX-RS) | ✅ | [docs/passo-05-api-rest-jaxrs.md](docs/passo-05-api-rest-jaxrs.md) |

Transversal: [docs/solid.md](docs/solid.md) — onde cada princípio SOLID aparece no código e por quê.

## Compilar

```bash
mvn package    # gera target/gestao-obras.war
```

## Executar (WildFly)

Não é preciso configurar banco de dados: a aplicação usa o `java:comp/DefaultDataSource`
do servidor (H2 em memória no WildFly) e carrega dados de demonstração na inicialização.

```bash
cp target/gestao-obras.war $WILDFLY_HOME/standalone/deployments/
$WILDFLY_HOME/bin/standalone.sh
```

Depois, abra http://localhost:8080/gestao-obras/ no navegador.

![Tela de obras](docs/img/passo-04/01-lista.png)

## API REST

Base: `http://localhost:8080/gestao-obras/api`

| Método e URL | O que faz |
|---|---|
| `GET /obras?nome=&status=` | Lista obras (filtros opcionais e combináveis) |
| `GET /obras/{id}` | Uma obra |
| `GET /obras/{id}/relatorios` | Relatórios da obra |
| `POST /obras/{id}/relatorios` | Registra relatório (201 + `Location`) |
| `GET /relatorios/recentes?limite=` | Últimos relatórios |
| `GET /relatorios/{id}` | Um relatório |
| `DELETE /relatorios/{id}` | Exclui relatório (204) |

```bash
curl http://localhost:8080/gestao-obras/api/obras?status=EM_ANDAMENTO
```

### Postman

Importe [`postman/gestao-obras-api.postman_collection.json`](postman/gestao-obras-api.postman_collection.json)
no Postman (**Import**): 24 requisições com testes automáticos e explicações, incluindo os casos
de erro (400, 404, 405, 406, 415, 422). Para rodar pela linha de comando, com o servidor no ar:

```bash
npx newman run postman/gestao-obras-api.postman_collection.json
```
