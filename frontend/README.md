# FluxoPro — Frontend

Interface web do FluxoPro, o funil de vendas que avisa quando uma negociação esfria.
Feito em **Angular** (componentes standalone, sem zone.js, signals), **SCSS** e **Vitest**.

O backend (Spring Boot) fica na raiz do repositório. Este projeto só consome a API dele.

## Requisitos

- Node.js **22.22.3 ou superior** (ou 24.15 ou superior)
- Backend rodando em `http://localhost:8082`

## Como rodar

```powershell
cd frontend
npm install
npm start        # abre em http://localhost:4200
```

Outros comandos:

```powershell
npm test         # testes unitários (Vitest)
npm run build    # build de produção em dist/
```

Em desenvolvimento, as chamadas para `/api/*` são redirecionadas para o backend pelo
`proxy.conf.json` (o prefixo `/api` é removido). Assim o navegador sempre fala com a mesma
origem e não é preciso configurar CORS. Em produção, o servidor web faz o mesmo papel.

## Telas

| Tela | Rota | Quem vê |
| --- | --- | --- |
| Entrar / Criar empresa | `/entrar` | visitante |
| Funil (com "esfriando agora" e encerradas) | `/funil` | dono e vendedor |
| Detalhe da negociação | `/negociacoes/:id` | dono e vendedor |
| Clientes | `/clientes` | dono e vendedor |
| Projetos e detalhe | `/projetos`, `/projetos/:id` | dono e vendedor |
| Alertas | `/alertas` | dono e vendedor |
| Equipe (adicionar e desativar vendedores) | `/equipe` | **só dono** |
| Histórico de auditoria | `/historico` | **só dono** |

O vendedor enxerga apenas as próprias negociações e projetos; quem aplica essa regra é o backend.

## Estrutura

```
src/app/
  core/       serviços e regras sem tela: autenticação, interceptor, guardas de rota,
              clientes HTTP de cada área, mensagens de erro, regras do termômetro
  layout/     casca da aplicação (menu e cabeçalho)
  shared/     peças reutilizáveis: termômetro de dias parados e comportamento de janela modal
  features/   uma pasta por tela (auth, funnel, customers, opportunity, projects,
              alerts, audit, team)
src/styles.scss   cores, tipografia e classes globais (design system)
```

## Decisões importantes

- **Papéis na interface são só conveniência.** O token JWT é lido no navegador apenas para
  decidir o que mostrar (por exemplo, esconder "Equipe" de um vendedor). A autorização de
  verdade é do backend (403 e 404).
- **Token no `localStorage`.** É simples, mas pode ser lido por um script injetado na página.
  Uma evolução futura é guardá-lo em cookie `httpOnly`.
- **Token vencido ou inválido** nunca é enviado ao login e ao cadastro, porque o Spring Security
  responde 401 até em rotas públicas quando recebe um token ruim. Uma resposta 401 nas demais
  chamadas leva de volta para `/entrar`.
- **Mensagens em português.** A API responde em inglês; a interface traduz a partir do status
  HTTP (400, 403, 404, 409, 422) e cada tela pode trocar a frase de um status.
- **Conflito de edição (409).** Os registros têm `version`. Se outra pessoa alterou antes, a
  tela avisa e recarrega os dados.
- **Etapas e transições** (negociações e projetos) espelham as regras do backend só para
  mostrar as opções válidas. O backend continua decidindo.

## Design

A identidade segue a ideia de **temperatura**: laranja quando a negociação está andando, azul
frio quando esfria (o azul não é cor de marca). O termômetro de 10 tracinhos perde um por dia
parado, e a partir de 7 dias o cartão fica azul-gelo. Fontes: Bricolage Grotesque (títulos) e
Instrument Sans (texto).

Acessibilidade: janelas com foco preso, Esc para fechar e foco devolvido; link "Pular para o
conteúdo"; página atual marcada no menu; alvos de toque de 44 px; funciona no celular.

## Testes

Os testes cobrem as regras que não dependem de tela: leitura do token e validade, guardas de
rota por papel, termômetro e dias parados, transições de etapa, formatação do histórico e
contador de alertas.

```powershell
npm test
```
