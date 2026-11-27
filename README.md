# FluxoPro

[![CI](https://github.com/vinizx3/FluxoPro/actions/workflows/ci.yml/badge.svg)](https://github.com/vinizx3/FluxoPro/actions/workflows/ci.yml)

> Um funil de vendas que impede a empresa de perder as negociações que já começaram.

**Status:** em desenvolvimento, fases 1 a 6, 8 e 9 de 13 concluídas (backend e frontend funcionando, com mensageria via Kafka e testes de integração com Testcontainers), mais a parte de Docker e CI/CD da fase 7. A publicação na internet (fase 7b) fica para depois (veja o [roadmap](#roadmap)).

## O problema

Empresas que vendem por negociação (engenharia, agências, consultorias, serviços B2B) costumam controlar as oportunidades de cabeça, no WhatsApp ou em planilhas. Quando uma negociação esfria, ninguém percebe até ser tarde demais.

O FluxoPro não gera clientes novos. Ele evita perder quem já demonstrou interesse: um funil visual no formato Kanban, com um termômetro que mostra há quantos dias cada negociação está sem movimento, alertas automáticos e um painel que mostra ao dono quanto dinheiro está esfriando.

É também um projeto de portfólio. O segundo objetivo é praticar o que um sistema de produção precisa além de CRUD: mensageria real, cache medido, testes de integração contra infraestrutura real, observabilidade e auditoria. O projeto é fullstack: backend em Java/Spring Boot e frontend em Angular.

## O que funciona hoje

### Para quem vende

- **Funil Kanban:** Lead, Qualificação, Proposta e Negociação, com as regras de movimentação aplicadas. Negociações encerradas (ganhas e perdidas) ficam numa lista própria.
- **Termômetro de atividade:** cada cartão mostra os dias sem movimento (a última atividade é a mais recente entre a mudança de etapa e o último contato). Passou do limite da empresa (7 dias por padrão, ajustável pelo dono de 1 a 90), a negociação aparece na faixa "Esfriando agora", com o valor total em risco.
- **Contatos e próximo passo:** registro de ligação, WhatsApp, e-mail ou reunião. Cada contato reinicia o termômetro, e o próximo passo agendado aparece no cartão, com destaque quando está vencido.
- **WhatsApp com um clique:** botão que abre a conversa com uma mensagem de acompanhamento pronta, no funil, nos clientes e na negociação.
- **Clientes e projetos:** cadastro de clientes com busca. Quando uma negociação é ganha, o projeto nasce sozinho, ligado ao cliente e à venda de origem.
- **Alertas:** um job diário (8h, horário de Brasília) avisa o responsável de cada negociação parada, uma única vez por período de parada. Cada usuário tem a sua caixa de entrada, com contador de não lidos.
- **Avisos de venda ganha e perdida:** quando uma negociação é ganha ou perdida, o dono da empresa e o responsável recebem uma notificação na caixa de entrada e um e-mail. O envio é feito por mensageria (Kafka), fora da requisição de quem moveu a negociação.

### Para quem gerencia (dono da empresa)

- **Painel:** valor em aberto, valor esfriando, ganhas, taxa de conversão, tempo médio até ganhar, negociações por etapa, motivos de perda e o desempenho de cada vendedor, em períodos de 30 dias, 90 dias ou 12 meses.
- **Equipe:** o dono cria, ativa e desativa vendedores.
- **Transferência de negociação:** o dono passa uma negociação aberta para outro vendedor, e a mudança fica registrada.
- **Configurações:** o dono define depois de quantos dias sem atividade uma negociação é considerada parada. O valor vale para o funil, os alertas e o painel, e é por empresa.
- **Histórico (auditoria):** quem criou, editou, moveu ou arquivou o quê, com o valor antigo e o novo. Mudanças nas configurações da empresa também ficam registradas.

### Segurança e confiabilidade

- **Multi-tenant desde o início:** cada empresa só enxerga os próprios dados. A empresa vem do token assinado, nunca do corpo da requisição.
- **Visibilidade por papel:** o `OWNER` vê tudo da empresa; o `SELLER` vê só as próprias negociações e projetos.
- **Autenticação:** o cadastro cria a empresa e o seu dono; o login devolve um JWT (60 minutos). As senhas usam hash BCrypt. Cada usuário pode trocar a própria senha informando a atual.
- **Edição simultânea segura:** editar ou mover um registro exige a `version` que o cliente leu. Se outra pessoa alterou antes, a API responde `409` em vez de sobrescrever em silêncio.
- **Erros consistentes:** regra de negócio violada devolve `422`, conflito de edição `409` e recurso de outra empresa `404`, todos no mesmo formato.
- **Testes automatizados:** 192 no backend (regras de domínio, services e consumers com Mockito, e testes de integração com Testcontainers) e 67 no frontend.

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
| Mensageria | Apache Kafka (modo KRaft), Spring for Apache Kafka, padrão Outbox |
| E-mail | Spring Mail; Mailpit para ver os e-mails em desenvolvimento |
| Frontend | Angular 22 (componentes standalone, signals, sem zone.js), SCSS, formulários reativos |
| Testes | JUnit 5, Mockito, AssertJ e Testcontainers (backend); Vitest (frontend) |
| Contêineres | Docker (builds multi-stage), Docker Compose, Nginx |
| CI/CD | GitHub Actions |
| Build | Maven (wrapper incluído), Angular CLI |

Planejadas: publicação com HTTPS, envio de e-mail real em produção (SMTP), Redis, Micrometer + Prometheus + Grafana.

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
- **CI que reproduz o ambiente real.** A cada push na `main` e a cada pull request, o GitHub Actions roda os testes do backend contra um PostgreSQL 16 e um Kafka de verdade, em containers (inclusive as migrations do Flyway), os testes e o build do frontend, e confirma que as duas imagens Docker constroem.
- **Venda ganha e projeto na mesma transação.** Ao ganhar uma negociação, a aplicação publica um evento interno e o módulo de projetos o consome de forma síncrona. Se a criação do projeto falhar, a venda não é marcada como ganha. A mensageria da fase 8 cuida só dos efeitos colaterais (veja [Mensageria com Kafka](#mensageria-com-kafka)).
- **Idempotência garantida pelo banco.** Um índice único parcial em `source_opportunity_id` impede dois projetos para a mesma venda.
- **Auditoria somente de inserção, garantida pelo banco.** O histórico é gravado na mesma transação da alteração. Gatilhos (triggers) do PostgreSQL recusam `UPDATE` e `DELETE` nas tabelas de auditoria, então nem um bug na aplicação reescreve o passado.
- **"Parada" é calculada, não gravada.** Uma negociação está parada se estiver aberta e a última atividade for mais antiga que o limite. Não existe flag para ficar desatualizada.
- **Alerta idempotente por período de parada.** Uma restrição única `(oportunidade, tipo, stalled_since)` impede notificações repetidas: o job pode rodar duas vezes, ou em duas instâncias, sem duplicar. Se um contato reinicia o termômetro e a negociação esfria de novo, nasce um alerta novo. O job processa uma empresa por vez, cada uma na sua transação.
- **Frontend sem regra de negócio.** O frontend só decide o que mostrar (por exemplo, esconder o botão "Transferir" de quem não é dono). Quem decide de verdade é sempre a API, que continua validando papel, empresa e versão.
- **Design "temperatura".** A interface usa uma paleta que vai do frio ao quente para comunicar urgência, com contraste verificado, alvos de toque de 44 px, foco visível e navegação por teclado.

## Mensageria com Kafka

Quando uma negociação é criada ou muda de etapa, a aplicação publica um evento. Os efeitos colaterais (notificação, e-mail e métricas) ficam em consumers independentes. A criação do projeto de uma venda ganha continua síncrona, na mesma transação, porque faz parte da regra de negócio e precisa ser consistente.

```
API --> PostgreSQL (negociação + outbox_event, na mesma transação)
                          |
                    relay (a cada 1 s, FOR UPDATE SKIP LOCKED)
                          v
        Kafka: opportunity.events  (chave = id da negociação, 3 partições)
          |-- grupo "notifications"   --> notificação de venda ganha ou perdida
          |-- grupo "emails"          --> e-mail (Mailpit em desenvolvimento)
          `-- grupo "funnel-metrics"  --> tabela funnel_stage_entry
        mensagens que falham --> opportunity.events-dlt (dead-letter)
```

| Consumer (grupo) | O que faz | Como evita duplicar |
|---|---|---|
| `notifications` | Cria a notificação "ganha" ou "perdida" para os donos da empresa e o responsável | Índice único `(source_event_id, user_id)` |
| `emails` | Envia o e-mail aos mesmos destinatários | Tabela `sent_email`, gravada logo depois de cada envio |
| `funnel-metrics` | Conta quantas negociações entraram em cada etapa e o valor na entrada | Tabela `processed_event`, na mesma transação do incremento |

Decisões:

- **Outbox transacional.** O evento é gravado no mesmo banco e na mesma transação da alteração (`Propagation.MANDATORY` impede gravar fora de uma transação). Se o Kafka estiver fora do ar, a venda continua funcionando e o evento é publicado sozinho quando ele voltar.
- **Relay que preserva a ordem.** Ele só marca o evento como publicado depois da confirmação do broker (`acks=all`, produtor idempotente) e para no primeiro erro, para nunca publicar um evento depois de um mais novo. A entrega é "pelo menos uma vez".
- **Chave pelo id da negociação.** Todos os eventos da mesma negociação vão para a mesma partição e chegam em ordem.
- **Consumers idempotentes.** Como o mesmo evento pode chegar duas vezes, cada consumer se protege no banco. Um contador (como o do funil) não é idempotente por natureza, por isso o id do evento é registrado na mesma transação do incremento.
- **Retry e dead-letter.** Falhas passageiras (por exemplo, servidor de e-mail fora do ar) são repetidas com espera crescente (1, 2, 4 e 8 segundos). Depois disso, a mensagem vai para `opportunity.events-dlt` com a causa do erro. Uma mensagem malformada vai direto para lá. A falha do e-mail não afeta a notificação, porque os grupos são independentes.
- **Replay.** A tabela do funil é só uma visão derivada dos eventos: dá para apagá-la, voltar o offset do grupo ao início e reconstruí-la com os mesmos números.
- **Contagem de entradas, não estoque.** A projeção conta as entradas em cada etapa, e não quantas negociações estão em cada uma hoje, porque editar o valor de uma negociação não gera evento.

Limites desta fase: o Kafka roda num único nó, sem cluster, e o volume de eventos é o de um projeto de demonstração. As mensagens na dead-letter não são reprocessadas automaticamente (a recuperação nos testes foi pelo próprio outbox). A tabela do funil ainda não é exibida na interface.

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
3. Suba o PostgreSQL, o Kafka e o Mailpit:
   ```bash
   docker compose up -d
   ```
   O banco é publicado na porta **5434** (para não conflitar com um PostgreSQL local na 5432), o Kafka na **9092** e o Mailpit na **1025** (SMTP) e **8025** (caixa de entrada em `http://localhost:8025`). O Kafka guarda os dados num volume, e os tópicos são criados pela aplicação ao iniciar.
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

> O `docker-compose.prod.yml` ainda não inclui Kafka nem Mailpit. Eles entram junto com a publicação (fase 7b), e até lá a mensageria roda só no ambiente de desenvolvimento.

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

### Ver a mensageria funcionando

1. Mova uma negociação até **Ganha** na interface. O e-mail aparece em `http://localhost:8025` e a notificação, na página de alertas.
2. Para ver as mensagens no Kafka:
   ```bash
   docker exec fluxopro-kafka /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 --topic opportunity.events --from-beginning \
     --property print.key=true --property print.headers=true
   ```
3. Para ver o Outbox em ação, pare o Kafka (`docker stop fluxopro-kafka`), mova uma negociação (a venda funciona normalmente) e suba o Kafka de novo (`docker start fluxopro-kafka`). O evento, guardado em `outbox_event`, é publicado sozinho.

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
| GET | `/company/settings` | Configurações da empresa: dias para uma negociação ser considerada parada (qualquer usuário logado) |
| PUT | `/company/settings` | Altera os dias para parada, de 1 a 90 (somente `OWNER`; a mudança vai para o histórico) |
| GET | `/dashboard?days=` | Números do painel da empresa (somente `OWNER`; de 7 a 365 dias) |
| GET | `/audit-log?entityType=&entityId=&page=&size=` | Histórico de alterações, mais recentes primeiro (somente `OWNER`) |
| GET | `/notifications?unreadOnly=&page=&size=` | Notificações do usuário logado (negociação parada, ganha ou perdida) |
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

Os testes de integração do backend usam Testcontainers: eles sobem o próprio PostgreSQL 16 e Kafka em containers descartáveis, então precisam apenas do **Docker em execução**, sem `docker compose up` e sem arquivo `.env`. Eles cobrem o contexto completo da aplicação (com as migrations do Flyway), a projeção do funil (SQL com `ON CONFLICT` e deduplicação por evento), o caminho de uma venda pelo Outbox, pelo Kafka e até a notificação e o funil, e a dead-letter (mensagem malformada vai direto para `opportunity.events-dlt`, com a causa do erro). Os testes dos consumers e do relay são unitários, com Mockito.

### Integração contínua

O arquivo `.github/workflows/ci.yml` roda a cada push na `main` e a cada pull request, com três jobs:

| Job | O que faz |
|---|---|
| `backend` | Roda `mvn verify`; os testes sobem o próprio PostgreSQL e Kafka com Testcontainers (testes e migrations) |
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
| 5 | Alertas de negociação parada (job diário e notificações), com limite configurável por empresa | Concluída |
| 6 | Frontend em Angular: funil, clientes, projetos, equipe, histórico, alertas, painel, contatos e transferência | Concluída |
| 7a | Docker (imagens, Compose de produção) e CI com GitHub Actions | Concluída |
| 7b | Publicação com HTTPS, e-mail real em produção (resumo diário e "esqueci minha senha") e contas de demonstração | Adiada |
| 8 | Kafka com Outbox transacional: eventos da negociação, consumers independentes (notificação, e-mail e métricas do funil), retry com dead-letter e replay | Concluída |
| 9 | Testes de integração com Testcontainers (PostgreSQL e Kafka): contexto, projeção do funil, fluxo ponta a ponta do Outbox e dead-letter | Concluída |
| 10 | Cache com Redis, com latência medida antes e depois | Próxima |
| 11 | Observabilidade: Actuator, Micrometer, Prometheus, Grafana | Planejada |
| 12 | Upload de documentos | Planejada |
| 13 | Resumo de negociação com IA (opcional) | Planejada |

## Autor

Vinícius Fernandes, desenvolvedor de software em Brasília-DF, em busca da primeira oportunidade como desenvolvedor júnior (Java / fullstack).
GitHub: [@vinizx3](https://github.com/vinizx3). Outro projeto: [HR Management System](https://github.com/vinizx3/HR-Management-System).
