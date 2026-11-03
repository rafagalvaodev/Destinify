import { test } from 'node:test';
import assert from 'node:assert/strict';
import { hotelsPath, getCatalog } from './catalogApi.js';

test('lista inicial usa a rota paginada de hotéis', () => {
  assert.equal(hotelsPath(), '/hotels?page=0&size=12');
});
test('pesquisa preserva cidade e nome ao trocar de página', () => {
  const url = new URL(hotelsPath({ name: ' Palace ', city: 'São Paulo', page: 2 }), 'http://test');
  assert.equal(url.pathname, '/hotels/search');
  assert.equal(url.searchParams.get('name'), 'Palace');
  assert.equal(url.searchParams.get('city'), 'São Paulo');
  assert.equal(url.searchParams.get('page'), '2');
});
test('campos em branco não ativam a busca', () => {
  assert.equal(hotelsPath({ name: ' ', city: ' ' }), '/hotels?page=0&size=12');
});
test('consulta pública não envia token e mantém a paginação recebida', async (context) => {
  context.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.ok(url.endsWith('/api/hotels?page=0&size=12'));
    assert.equal(options.headers, undefined);
    return new Response(JSON.stringify({ content: [], totalPages: 0, totalElements: 0 }));
  });
  assert.equal((await getCatalog(hotelsPath())).totalElements, 0);
});
test('erro HTTP não é tratado como resultado vazio', async (context) => {
  context.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 500 }));
  await assert.rejects(getCatalog(hotelsPath()), /HTTP 500/);
});

test('busca envia hóspedes, mínimo de quartos e período junto com a paginação', () => {
  const url = new URL(hotelsPath({ guests: 5, rooms: 1, startDate: '2027-01-10', endDate: '2027-01-12', page: 2 }), 'http://test');
  assert.equal(url.pathname, '/hotels/search');
  assert.equal(url.searchParams.get('guests'), '5');
  assert.equal(url.searchParams.get('rooms'), '1');
  assert.equal(url.searchParams.get('startDate'), '2027-01-10');
  assert.equal(url.searchParams.get('endDate'), '2027-01-12');
  assert.equal(url.searchParams.get('page'), '2');
});
