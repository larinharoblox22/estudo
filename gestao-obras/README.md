# Gestão de Obras — Jakarta EE (exemplo de estudo)

Aplicação de exemplo para preparação de entrevista técnica: CRUD de **Obras** e
**Relatórios de Segurança** com a stack Jakarta EE (antigo J2EE), sempre com
paralelos ao Spring Boot.

**Stack:** Java 17 · Jakarta EE 10 (JPA, EJB, CDI, JSF + PrimeFaces, JAX-RS) · H2/PostgreSQL · WildFly

## Progresso

| Passo | Tema | Estado | Relatório |
|---|---|---|---|
| 1 | Modelo (JPA & OOP) | ✅ | [docs/passo-01-modelo-jpa.md](docs/passo-01-modelo-jpa.md) |
| 2 | DAO e EntityManager | ⏳ | |
| 3 | Camada de negócio (EJB) | ⏳ | |
| 4 | Frontend (JSF & PrimeFaces) | ⏳ | |
| 5 | Integração (API REST JAX-RS) | ⏳ | |

## Compilar

```bash
mvn package    # gera target/gestao-obras.war
```
