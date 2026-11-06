/**
 * Prefixo de todas as chamadas à API.
 *
 * Em desenvolvimento, o `proxy.conf.json` troca "/api" por "http://localhost:8082".
 * Em produção, o servidor web (por exemplo, Nginx) faz a mesma troca.
 * Assim o navegador sempre fala com a mesma origem e não precisamos configurar CORS.
 */
export const API_BASE = '/api';
