// Endereço da API do backend. Em produção vem da variável VITE_API_URL (definida na Vercel).
// Sem a variável, usa o backend local. A barra final é removida para não gerar "//".
export const API_URL = (import.meta.env?.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '');