# FluxoPro — Frontend

Angular (standalone, zoneless) + SCSS + Vitest.

## Como rodar

Requisitos: Node 22.22.3+ ou 24.15+ e o backend rodando em `localhost:8082`.

```powershell
npm install
npm start      # http://localhost:4200
npm test
```

Em desenvolvimento, `/api/*` é redirecionado para o backend (`proxy.conf.json`), sem CORS.
