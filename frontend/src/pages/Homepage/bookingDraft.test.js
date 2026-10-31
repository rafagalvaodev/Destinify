import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readBookingDraft, saveBookingDraft, clearBookingDraft } from './bookingDraft.js';

function storage() {
  const values = new Map();
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
}
const draft = {
  hotel: { hotel_id: 1, name: 'Hotel' },
  rooms: [{ room_id: 2, hotel_id: 1, maxCapacity: 3 }],
  arrival: '2027-01-10', departure: '2027-01-12', guests: 3, adultsCount: 2, childrenAges: [4],
  searchCriteria: { guests: 3, page: 0 }, resumeAfterLogin: true,
};

test('preserva os dados da reserva e a retomada após o login', () => {
  const local = storage();
  saveBookingDraft(draft, local);
  assert.deepEqual(readBookingDraft(local), { ...draft, version: 1 });
  clearBookingDraft(local);
  assert.equal(readBookingDraft(local), null);
});

test('ignora dados corrompidos e quartos de outro hotel', () => {
  const local = storage();
  local.setItem('destinify.bookingDraft', '{invalid');
  assert.equal(readBookingDraft(local), null);
  saveBookingDraft({ ...draft, rooms: [{ room_id: 2, hotel_id: 9 }] }, local);
  assert.equal(readBookingDraft(local), null);
});

test('permite preservar preenchimento parcial sem retomar automaticamente', () => {
  const local = storage();
  saveBookingDraft({ ...draft, arrival: '', departure: '', guests: '', resumeAfterLogin: false }, local);
  assert.equal(readBookingDraft(local).resumeAfterLogin, false);
  assert.equal(readBookingDraft(local).guests, '');
});

test('armazenamento indisponível não quebra a leitura e impede fingir que salvou', () => {
  const local = { getItem() { throw new Error('bloqueado'); }, setItem() { throw new Error('cheio'); } };
  assert.equal(readBookingDraft(local), null);
  assert.throws(() => saveBookingDraft(draft, local), /cheio/);
});
