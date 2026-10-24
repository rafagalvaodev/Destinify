const configured = import.meta.env?.VITE_API_URL || 'http://localhost:8080/api';
const base = configured.replace(/\/+$/, '');
const apiBase = base.endsWith('/api') ? base : `${base}/api`;

export function hotelsPath({ name = '', city = '', page = 0 } = {}) {
  const params = new URLSearchParams({ page: String(page), size: '12' });
  if (name.trim()) params.set('name', name.trim());
  if (city.trim()) params.set('city', city.trim());
  return `/hotels${name.trim() || city.trim() ? '/search' : ''}?${params}`;
}

export async function getCatalog(path, signal) {
  let response;
  try {
    response = await fetch(`${apiBase}${path}`, { signal });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.');
  }
  if (!response.ok) throw new Error(`Não foi possível carregar os dados (HTTP ${response.status}).`);
  const result = await response.json();
  if (!Array.isArray(result.content)) throw new Error('O servidor retornou uma listagem inválida.');
  return result;
}
