import test from 'node:test';
import assert from 'node:assert/strict';
import { bookingCapacity, bookingQuote, validateBooking } from './bookingValidation.js';

const base = { rooms: [{ maxCapacity: 2 }], arrival: '2026-11-01', departure: '2026-11-03', today: '2026-10-24', guests: 2 };
test('validates the capacity of one room and the sum across rooms', () => {
  assert.equal(validateBooking(base), '');
  assert.match(validateBooking({ ...base, guests: 3 }), /no máximo 2/);
  const rooms = [{ maxCapacity: 2 }, { maxCapacity: 3 }];
  assert.equal(bookingCapacity(rooms), 5);
  assert.equal(validateBooking({ ...base, rooms, guests: 5 }), '');
  assert.match(validateBooking({ ...base, rooms, guests: 6 }), /no máximo 5/);
  assert.match(validateBooking({ ...base, rooms: [rooms[0]], guests: 5 }), /no máximo 2/);
});
test('rejects missing dates, invalid periods and invalid guest counts', () => {
  for (const guests of ['', 0, -1, 1.5, 'abc']) assert.match(validateBooking({ ...base, guests }), /quantidade inteira/);
  assert.match(validateBooking({ ...base, arrival: '' }), /Preencha/);
  assert.match(validateBooking({ ...base, arrival: '2026-10-23' }), /passado/);
  assert.match(validateBooking({ ...base, departure: base.arrival }), /posterior/);
  assert.match(validateBooking({ ...base, rooms: [] }), /Selecione/);
});

test('only calculates the stay total for valid dates and updates nights and rooms', () => {
  const rooms = [{ price: 100 }, { price: 150 }];
  assert.equal(bookingQuote(rooms, '', ''), null);
  assert.equal(bookingQuote(rooms, '2026-11-01', ''), null);
  assert.equal(bookingQuote(rooms, '2026-11-03', '2026-11-01'), null);
  assert.equal(bookingQuote(rooms, '2026-11-01', '2026-11-01'), null);
  assert.equal(bookingQuote(rooms, 'invalid', '2026-11-03'), null);
  assert.deepEqual(bookingQuote(rooms, '2026-11-01', '2026-11-03'), { nights: 2, dailyPrice: 250, total: 500 });
  assert.equal(bookingQuote(rooms, '2026-11-01', '2026-11-04').total, 750);
  assert.equal(bookingQuote([rooms[0]], '2026-11-01', '2026-11-04').total, 300);
  assert.equal(bookingQuote([{ price: null }], '2026-11-01', '2026-11-04'), null);
});
