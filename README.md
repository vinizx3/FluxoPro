# FluxoPro

> Um funil de vendas que impede a empresa de perder as negociações que já começaram.

**Status:** em desenvolvimento, fase 2 de 12 (veja o [roadmap](#roadmap)).

## O problema

Empresas que vendem por negociação (engenharia, agências, consultorias, serviços B2B) costumam controlar as oportunidades de cabeça, no WhatsApp ou em planilhas. Quando uma negociação esfria, ninguém percebe até ser tarde demais.

O FluxoPro não gera clientes novos. Ele evita perder quem já demonstrou interesse: um funil visual no formato Kanban, com reativação automática de oportunidades paradas (planejada).

É também um projeto de portfólio. O segundo objetivo é praticar o que um backend de produção precisa além de CRUD: mensageria real, cache medido, testes de integração contra infraestrutura real, observabilidade e auditoria. O projeto é fullstack: backend em Java/Spring Boot e frontend em Angular (planejado para a fase 10).

## O que funciona hoje

- **Multi-tenant desde o início:** cada empresa só enxerga os próprios dados. A empresa vem do token assinado, nunca do corpo da requisição.
- **Autenticação:** o cadastro cria a empresa e o seu dono; o login devolve um JWT. As senhas são armazenadas com hash BCrypt.
- **Clientes:** criar, listar e consultar, sempre dentro da empresa.
- **Oportunidades (Kanban):** criar, ver o quadro agrupado por etapa e mover o cartão com as regras de negócio aplicadas.
- **Visibilidade por papel:** o `OWNER` vê todas as oportunidades da empresa; o `SELLER` vê só as dele.
- **Erros consistentes:** violação de regra de negócio devolve `422`, e recurso de outra empresa devolve `404`, ambos no mesmo formato de resposta.
- **43 testes automatizados** (regras de domínio, lógica dos services com Mockito e contexto da aplicação).

### Regras do funil

```
LEAD <-> QUALIFICATION <-> PROPOSAL <-> NEGOTIATION --> WON
qualquer etapa aberta ----------------------------> LOST (motivo obrigatório)
```

- O cartão anda para a etapa vizinha, para frente ou para trás, e não pode pular etapas.
- `WON` só é alcançado a partir de `NEGOTIATION`.
- `WON` e `LOST` são estados finais.
- Toda oportunidade registra quando mudou de etapa pela última vez (`status_changed_at`). É esse dado que a detecção de oportunidades paradas vai usar.

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem / framework | Java 21, Spring Boot 4 |
| Segurança | Spring Security, OAuth2 Resource Server (JWT, HS256), BCrypt |
| Persistência | PostgreSQL 16, Spring Data JPA (Hibernate), Flyway |
| Ambiente | Docker Compose |
| Testes | JUnit 5, Mockito, AssertJ |
| Build | Maven (wrapper incluído) |

Planejadas: Kafka, Redis, Testcontainers, Actuator + Micrometer + Prometheus + Grafana, Angular, GitHub Actions.

## Decisões de arquitetura

- **Isolamento entre empresas em dois níveis.** Toda consulta de dados de negócio recebe o `companyId` na assinatura do método, então ele não pode ser esquecido. Além disso, chaves estrangeiras compostas no banco (`(customer_id, company_id)`) impedem ligar registros de empresas diferentes, mesmo que o código tivesse um bug.
- **Regras de negócio no domínio.** A tabela de transições de etapa faz parte do enum `OpportunityStatus` e da entidade. Por isso é testada sem Spring e sem banco, e vale para qualquer porta de entrada.
- **Schema versionado.** O Flyway é dono do banco. O Hibernate roda com `ddl-auto: validate` e o Flyway recusa migrations com nome fora do padrão, então problemas de schema aparecem na inicialização.
- **404 em vez de 403.** Acessar um recurso de outra empresa devolve `404`, para a API nunca confirmar que um ID existe.
- **Relógio injetável.** O tempo vem de um bean `Clock`, o que mantém as regras que dependem de data testáveis.
- **Sem N+1 no quadro.** Cliente e responsável são carregados com um entity graph, numa única consulta.
- **Sem segredos no repositório.** As credenciais do banco e o segredo do JWT vêm de variáveis de ambiente.

## Como rodar

### Pré-requisitos

- JDK 21
- Docker (com Docker Compose)

### Passo a passo

1. Clone o repositório:
   ```bash
   git clone https://github.com/vinizx3/FluxoPro.git
   cd FluxoPro
   ```
2. Crie o seu arquivo de ambiente a partir do modelo e preencha os valores:
   ```bash
   cp .env.example .env
   ```
   Gere um segredo aleatório para o JWT (no mínimo 32 bytes) e coloque em `JWT_SECRET`:
   ```bash
   openssl rand -base64 48
   ```
   No PowerShell (Windows):
   ```powershell
   $b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
   ```
3. Suba o PostgreSQL:
   ```bash
   docker compose up -d
   ```
   O container é publicado na porta **5434** do computador, para não conflitar com um PostgreSQL local na 5432.
4. Suba a aplicação (o Flyway aplica as migrations na inicialização):
   ```bash
   ./mvnw spring-boot:run
   ```
   No Windows: `.\mvnw spring-boot:run`. A API fica em `http://localhost:8082`.

### Experimente

```bash
# 1. Cadastre uma empresa e o seu dono (devolve um token de acesso)
curl -X POST http://localhost:8082/auth/register \
  -H "Content-Type: application/json" \
  -d '{"companyName":"Acme","name":"Jane","email":"jane@acme.com","password":"uma-senha-forte"}'

# 2. Use o token nos outros endpoints
TOKEN=<cole aqui o accessToken>

curl -X POST http://localhost:8082/customers \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Cliente grande"}'

curl -X POST http://localhost:8082/opportunities \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"customerId":"<id do cliente>","title":"Redesign do site","amount":15000}'

curl http://localhost:8082/opportunities/board -H "Authorization: Bearer $TOKEN"
```

## Visão geral da API

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/auth/register` | Cria uma empresa e o seu dono, devolve um token |
| POST | `/auth/login` | Devolve um token |
| GET | `/me` | Identidade que consta no token |
| POST | `/customers` | Cria um cliente |
| GET | `/customers` | Lista os clientes da empresa |
| GET | `/customers/{id}` | Consulta um cliente |
| POST | `/opportunities` | Cria uma oportunidade (começa em `LEAD`) |
| GET | `/opportunities/board` | Oportunidades abertas agrupadas por etapa |
| GET | `/opportunities/{id}` | Consulta uma oportunidade |
| PATCH | `/opportunities/{id}/status` | Move para outra etapa (`lostReason` obrigatório para `LOST`) |
| GET | `/actuator/health` | Verificação de saúde (público) |

Tudo, exceto `/auth/**` e `/actuator/health`, exige o cabeçalho `Authorization: Bearer <token>`.

## Testes

```bash
./mvnw test
```

O teste de contexto da aplicação precisa do banco rodando (`docker compose up -d`). Na fase 6 ele passa a usar Testcontainers, e os testes sobem o próprio PostgreSQL e Kafka.

## Roadmap

| Fase | Escopo | Status |
|---|---|---|
| 1 | Fundação: Docker, Flyway, JWT, papéis, multi-tenant | Concluída |
| 2 | Funil comercial (Kanban) | Núcleo concluído; edição, arquivamento e paginação pendentes |
| 3 | Projetos criados a partir de oportunidades ganhas | Planejada |
| 4 | Auditoria (quem alterou o quê e quando) | Planejada |
| 5 | Kafka: o evento "oportunidade ganha" cria o projeto; lembretes de oportunidades paradas | Planejada |
| 6 | Testes de integração com Testcontainers (PostgreSQL e Kafka) | Planejada |
| 7 | Cache com Redis, com latência medida antes e depois | Planejada |
| 8 | Observabilidade: Actuator, Micrometer, Prometheus, Grafana | Planejada |
| 9 | Upload de documentos | Planejada |
| 10 | Frontend em Angular | Planejada |
| 11 | Resumo de oportunidade com IA (opcional) | Planejada |
| 12 | Deploy, CI/CD com GitHub Actions e documentação | Planejada |

## Autor

Vinícius Fernandes, desenvolvedor de software em Brasília-DF, em busca da primeira oportunidade como desenvolvedor júnior (Java / fullstack).
GitHub: [@vinizx3](https://github.com/vinizx3). Outro projeto: [HR Management System](https://github.com/vinizx3/HR-Management-System).
