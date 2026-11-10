# FluxoPro

[![CI](https://github.com/vinizx3/FluxoPro/actions/workflows/ci.yml/badge.svg)](https://github.com/vinizx3/FluxoPro/actions/workflows/ci.yml)

> Um funil de vendas que impede a empresa de perder as negociações que já começaram.

**Status:** em desenvolvimento, fases 1 a 6 de 13 concluídas (backend e frontend funcionando), mais a parte de Docker e CI/CD da fase 7. A publicação na internet é a próxima etapa (veja o [roadmap](#roadmap)).

## O problema

Empresas que vendem por negociação (engenharia, agências, consultorias, serviços B2B) costumam controlar as oportunidades de cabeça, no WhatsApp ou em planilhas. Quando uma negociação esfria, ninguém percebe até ser tarde demais.

O FluxoPro não gera clientes novos. Ele evita perder quem já demonstrou interesse: um funil visual no formato Kanban, com um termômetro que mostra há quantos dias cada negociação está sem movimento, alertas automáticos e um painel que mostra ao dono quanto dinheiro está esfriando.

É também um projeto de portfólio. O segundo objetivo é praticar o que um sistema de produção precisa além de CRUD: mensageria real, cache medido, testes de integração contra infraestrutura real, observabilidade e auditoria. O projeto é fullstack: backend em Java/Spring Boot e frontend em Angular.

## O que funciona hoje

### Para quem vende

- **Funil Kanban:** Lead, Qualificação, Proposta e Negociação, com as regras de movimentação aplicadas. Negociações encerradas (ganhas e perdidas) ficam numa lista própria.
- **Termômetro de atividade:** cada cartão mostra os dias sem movimento (a última atividade é a mais recente entre a mudança de etapa e o último contato). Passou de 7 dias, a negociação aparece na faixa "Esfriando agora", com o valor total em risco.
- **Contatos e próximo passo:** registro de ligação, WhatsApp, e-mail ou reunião. Cada contato reinicia o termômetro, e o próximo passo agendado aparece no cartão, com destaque quando está vencido.
- **WhatsApp com um clique:** botão que abre a conversa com uma mensagem de acompanhamento pronta, no funil, nos clientes e na negociação.
- **Clientes e projetos:** cadastro de clientes com busca. Quando uma negociação é ganha, o projeto nasce sozinho, ligado ao cliente e à venda de origem.
- **Alertas:** um job diário (8h, horário de Brasília) avisa o responsável de cada negociação parada, uma única vez por período de parada. Cada usuário tem a sua caixa de entrada, com contador de não lidos.

### Para quem gerencia (dono da empresa)

- **Painel:** valor em aberto, valor esfriando, ganhas, taxa de conversão, tempo médio até ganhar, negociações por etapa, motivos de perda e o desempenho de cada vendedor, em períodos de 30 dias, 90 dias ou 12 meses.
- **Equipe:** o dono cria, ativa e desativa vendedores.
- **Transferência de negociação:** o dono passa uma negociação aberta para outro vendedor, e a mudança fica registrada.
- **Histórico (auditoria):** quem criou, editou, moveu ou arquivou o quê, com o valor antigo e o novo.

### Segurança e confiabilidade

- **Multi-tenant desde o início:** cada empresa só enxerga os próprios dados. A empresa vem do token assinado, nunca do corpo da requisição.
- **Visibilidade por papel:** o `OWNER` vê tudo da empresa; o `SELLER` vê só as próprias negociações e projetos.
- **Autenticação:** o cadastro cria a empresa e o seu dono; o login devolve um JWT (60 minutos). As senhas usam hash BCrypt. Cada usuário pode trocar a própria senha informando a atual.
- **Edição simultânea segura:** editar ou mover um registro exige a `version` que o cliente leu. Se outra pessoa alterou antes, a API responde `409` em vez de sobrescrever em silêncio.
- **Erros consistentes:** regra de negócio violada devolve `422`, conflito de edição `409` e recurso de outra empresa `404`, todos no mesmo formato.
- **Testes automatizados:** 133 no backend (regras de domínio, services com Mockito e contexto da aplicação) e 54 no frontend.

### Regras do funil

```
LEAD <-> QUALIFICATION <-> PROPOSAL <-> NEGOTIATION --> WON
qualquer etapa aberta ----------------------------> LOST (motivo obrigatório)
```

- O cartão anda para a etapa vizinha, para frente ou para trás, e não pode pular etapas.
- `WON` só é alcançado a partir de `NEGOTIATION`.
- `WON` e `LOST` são estados finais; só negociações finalizadas podem ser arquivadas.
- Registros arquivados não são editados nem movidos, e não aparecem no quadro.
- Contato e transferência só valem para negociações abertas. O próximo passo não pode estar no passado.
- Os contatos não podem ser editados nem apagados: são o histórico do relacionamento.

### Regras dos projetos

```
PLANNING --> IN_PROGRESS --> DELIVERED
PLANNING / IN_PROGRESS ----> CANCELLED
```

- O projeto só anda para frente, sem pular etapas. `DELIVERED` e `CANCELLED` são finais.
- Só projetos finalizados podem ser arquivados, e projetos arquivados não são editados nem movidos.
- O responsável do projeto é quem ganhou a venda.

## Tecnologias

| Área | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4 |
| Segurança | Spring Security, OAuth2 Resource Server (JWT, HS256), BCrypt |
| Persistência | PostgreSQL 16, Spring Data JPA (Hibernate), Flyway |
| Frontend | Angular 22 (componentes standalone, signals, sem zone.js), SCSS, formulários reativos |
| Testes | JUnit 5, Mockito, AssertJ (backend); Vitest (frontend) |
| Contêineres | Docker (builds multi-stage), Docker Compose, Nginx |
| CI/CD | GitHub Actions |
| Build | Maven (wrapper incluído), Angular CLI |

Planejadas: publicação com HTTPS, e-mail (SMTP), Kafka, Redis, Testcontainers, Micrometer + Prometheus + Grafana.

## Decisões de arquitetura

- **Isolamento entre empresas em dois níveis.** Toda consulta de dados de negócio recebe o `companyId` na assinatura do método, então ele não pode ser esquecido. Além disso, chaves estrangeiras compostas no banco (`(customer_id, company_id)`) impedem ligar registros de empresas diferentes, mesmo que o código tivesse um bug.
- **Regras de negócio no domínio.** A tabela de transições de etapa faz parte do enum `OpportunityStatus` e da entidade. Por isso é testada sem Spring e sem banco, e vale para qualquer porta de entrada.
- **Schema versionado.** O Flyway é dono do banco. O Hibernate roda com `ddl-auto: validate`, então problemas de schema aparecem na inicialização.
- **404 em vez de 403.** Acessar um recurso de outra empresa devolve `404`, para a API nunca confirmar que um ID existe.
- **Relógio injetável.** O tempo vem de um bean `Clock`, o que mantém as regras que dependem de data testáveis.
- **Sem N+1 no quadro.** Cliente e responsável são carregados com um entity graph, numa única consulta.
- **Arquivar em vez de apagar.** Registros arquivados mantêm o histórico e saem das telas. Os índices são parciais (`WHERE archived_at IS NULL`).
- **Concorrência otimista.** Cada registro tem uma `version`; a API compara a versão que o cliente leu com a gravada, e o `@Version` do JPA cobre a janela entre a checagem e a gravação.
- **Paginação controlada pela API.** Tamanho máximo de página, ordenação fixa com desempate por `id` e resposta num formato próprio.
- **Sem segredos no repositório.** As credenciais do banco e o segredo do JWT vêm de variáveis de ambiente.
- **Imagens Docker enxutas e seguras.** O backend usa build em duas etapas (Maven na primeira, só o JRE na segunda) e roda com usuário sem privilégios. O `Dockerfile` copia apenas `pom.xml` e `src`, então o `.env` nunca entra na imagem. O endereço do banco (`DB_URL`) e a porta (`PORT`) podem ser trocados por variável de ambiente, e o padrão continua funcionando no desenvolvimento local.
- **Mesma origem para o navegador.** Em produção, o Nginx serve o Angular e encaminha `/api` para o backend, então o navegador fala com um único endereço e não precisa de CORS.
- **CI que reproduz o ambiente real.** A cada push na `main` e a cada pull request, o GitHub Actions roda os testes do backend contra um PostgreSQL 16 de verdade (inclusive as migrations do Flyway), os testes e o build do frontend, e confirma que as duas imagens Docker constroem.
- **Venda ganha e projeto na mesma transação.** Ao ganhar uma negociação, a aplicação publica um evento interno e o módulo de projetos o consome de forma síncrona. Se a criação do projeto falhar, a venda não é marcada como ganha. O evento carrega só IDs, então na fase 8 ele pode virar uma mensagem Kafka sem mudar a regra de negócio.
- **Idempotência garantida pelo banco.** Um índice único parcial em `source_opportunity_id` impede dois projetos para a mesma venda.
- **Auditoria somente de inserção, garantida pelo banco.** O histórico é gravado na mesma transação da alteração. Gatilhos (triggers) do PostgreSQL recusam `UPDATE` e `DELETE` nas tabelas de auditoria, então nem um bug na aplicação reescreve o passado.
- **"Parada" é calculada, não gravada.** Uma negociação está parada se estiver aberta e a última atividade for mais antiga que o limite. Não existe flag para ficar desatualizada.
- **Alerta idempotente por período de parada.** Uma restrição única `(oportunidade, tipo, stalled_since)` impede notificações repetidas: o job pode rodar duas vezes, ou em duas instâncias, sem duplicar. Se um contato reinicia o termômetro e a negociação esfria de novo, nasce um alerta novo. O job processa uma empresa por vez, cada uma na sua transação.
- **Frontend sem regra de negócio.** O frontend só decide o que mostrar (por exemplo, esconder o botão "Transferir" de quem não é dono). Quem decide de verdade é sempre a API, que continua validando papel, empresa e versão.
- **Design "temperatura".** A interface usa uma paleta que vai do frio ao quente para comunicar urgência, com contraste verificado, alvos de toque de 44 px, foco visível e navegação por teclado.

## Como rodar

### Pré-requisitos

- JDK 21
- Docker (com Docker Compose)
- Node.js 22 ou superior (para o frontend)

### Backend

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

### Frontend

Em outro terminal, com o backend no ar:

```bash
cd frontend
npm install
npm start
```

A interface abre em `http://localhost:4200`. O servidor de desenvolvimento encaminha as chamadas de `/api` para `http://localhost:8082`. Na tela de entrada, use a aba **Criar empresa** para cadastrar a empresa e o seu dono, e depois entre com o e-mail e a senha cadastrados.

### Tudo junto, com Docker

Para subir banco, backend e frontend como em produção, sem instalar JDK nem Node:

1. Crie o `.env` como no passo 2 do backend (precisa de `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `JWT_SECRET`).
2. Rode:
   ```bash
   docker compose -f docker-compose.prod.yml up --build
   ```
3. Abra `http://localhost`. O banco começa vazio: crie a empresa pela aba **Criar empresa**.

Nessa configuração, o banco não é publicado fora da rede do Docker, e o Nginx encaminha `/api` para o backend.

### Experimente a API

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
| PUT | `/me/password` | Troca a própria senha (exige a senha atual) |
| GET | `/users` | Equipe da empresa (somente `OWNER`) |
| POST | `/users` | Cria um vendedor (somente `OWNER`) |
| POST | `/users/{id}/activate` · `/deactivate` | Ativa ou desativa um usuário (somente `OWNER`) |
| POST | `/customers` | Cria um cliente |
| GET | `/customers?search=&page=&size=` | Lista paginada dos clientes ativos, com busca |
| GET | `/customers/{id}` | Consulta um cliente |
| PUT | `/customers/{id}` | Edita um cliente (exige `version`) |
| POST | `/customers/{id}/archive` | Arquiva um cliente sem negociações nem projetos abertos |
| POST | `/opportunities` | Cria uma negociação (começa em `LEAD`) |
| GET | `/opportunities/board` | Negociações abertas agrupadas por etapa |
| GET | `/opportunities/closed?page=&size=` | Negociações encerradas (ganhas e perdidas) |
| GET | `/opportunities/{id}` | Consulta uma negociação |
| PUT | `/opportunities/{id}` | Edita título e valor (exige `version`) |
| PATCH | `/opportunities/{id}/status` | Move para outra etapa (exige `version`; `lostReason` obrigatório para `LOST`) |
| PATCH | `/opportunities/{id}/owner` | Transfere para outro vendedor (somente `OWNER`; exige `version`) |
| POST | `/opportunities/{id}/contacts` | Registra um contato e, opcionalmente, o próximo passo |
| GET | `/opportunities/{id}/contacts` | Linha do tempo de contatos da negociação |
| POST | `/opportunities/{id}/archive` | Arquiva uma negociação `WON` ou `LOST` |
| GET | `/opportunities/stalled?days=&page=&size=` | Negociações abertas sem atividade há N dias (padrão 7); o `SELLER` vê só as dele |
| POST | `/projects` | Cria um projeto direto para um cliente |
| GET | `/projects?status=&page=&size=` | Lista paginada dos projetos ativos, com filtro por etapa |
| GET | `/projects/{id}` | Consulta um projeto |
| PUT | `/projects/{id}` | Edita nome, descrição e data prevista (exige `version`) |
| PATCH | `/projects/{id}/status` | Move para outra etapa (exige `version`) |
| POST | `/projects/{id}/archive` | Arquiva um projeto entregue ou cancelado |
| GET | `/dashboard?days=` | Números do painel da empresa (somente `OWNER`; de 7 a 365 dias) |
| GET | `/audit-log?entityType=&entityId=&page=&size=` | Histórico de alterações, mais recentes primeiro (somente `OWNER`) |
| GET | `/notifications?unreadOnly=&page=&size=` | Notificações do usuário logado |
| GET | `/notifications/unread-count` | Quantidade de notificações não lidas |
| POST | `/notifications/{id}/read` | Marca uma notificação como lida |
| POST | `/notifications/read-all` | Marca todas como lidas |
| GET | `/actuator/health` | Verificação de saúde (público) |

Tudo, exceto `/auth/**` e `/actuator/health`, exige o cabeçalho `Authorization: Bearer <token>`.

## Testes

Backend:

```bash
./mvnw test
```

Frontend:

```bash
cd frontend
npm test
```

O teste de contexto do backend precisa do banco rodando (`docker compose up -d`). Na fase 9, ele passa a usar Testcontainers: os testes sobem o próprio PostgreSQL e Kafka.

### Integração contínua

O arquivo `.github/workflows/ci.yml` roda a cada push na `main` e a cada pull request, com três jobs:

| Job | O que faz |
|---|---|
| `backend` | Sobe um PostgreSQL 16 descartável e roda `mvn verify` (testes e migrations) |
| `frontend` | Roda `npm ci`, os testes (Vitest) e o build de produção do Angular |
| `docker` | Constrói as imagens do backend e do frontend (só roda se os dois anteriores passarem) |

Os valores usados no CI (senha do banco e segredo do JWT) existem só nessa execução, contra um banco descartável. Nenhum segredo real fica no repositório.

## Roadmap

| Fase | Escopo | Status |
|---|---|---|
| 1 | Fundação: Docker, Flyway, JWT, papéis, multi-tenant | Concluída |
| 2 | Funil comercial (Kanban), edição, arquivamento e paginação | Concluída |
| 3 | Projetos criados a partir de oportunidades ganhas | Concluída |
| 4 | Auditoria (quem alterou o quê e quando) | Concluída |
| 5 | Alertas de negociação parada (job diário e notificações) | Concluída |
| 6 | Frontend em Angular: funil, clientes, projetos, equipe, histórico, alertas, painel, contatos e transferência | Concluída |
| 7a | Docker (imagens, Compose de produção) e CI com GitHub Actions | Concluída |
| 7b | Publicação com HTTPS, e-mail (resumo diário e "esqueci minha senha") e contas de demonstração | Próxima |
| 8 | Kafka: o evento "negociação ganha" (hoje interno) vira mensagem, com Outbox | Planejada |
| 9 | Testes de integração com Testcontainers (PostgreSQL e Kafka) | Planejada |
| 10 | Cache com Redis, com latência medida antes e depois | Planejada |
| 11 | Observabilidade: Actuator, Micrometer, Prometheus, Grafana | Planejada |
| 12 | Upload de documentos | Planejada |
| 13 | Resumo de negociação com IA (opcional) | Planejada |

## Autor

Vinícius Fernandes, desenvolvedor de software em Brasília-DF, em busca da primeira oportunidade como desenvolvedor júnior (Java / fullstack).
GitHub: [@vinizx3](https://github.com/vinizx3). Outro projeto: [HR Management System](https://github.com/vinizx3/HR-Management-System).
