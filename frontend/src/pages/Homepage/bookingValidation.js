export function bookingCapacity(rooms) {
  return rooms.reduce((total, room) => total + Number(room.maxCapacity || 0), 0);
}

export function validateBooking({ rooms, arrival, departure, guests, today }) {
  if (!rooms.length) return 'Selecione pelo menos um quarto.';
  if (!arrival || !departure) return 'Preencha a chegada e a saída.';
  if (arrival < today) return 'A chegada não pode ser no passado.';
  if (departure <= arrival) return 'A saída deve ser posterior à chegada.';
  if (!Number.isInteger(Number(guests)) || Number(guests) < 1) return 'Informe uma quantidade inteira de hóspedes maior que zero.';
  const capacity = bookingCapacity(rooms);
  if (Number(guests) > capacity) return `Os quartos selecionados comportam no máximo ${capacity} hóspedes. Remova hóspedes ou selecione mais quartos.`;
  return '';
}

export function bookingQuote(rooms, arrival, departure) {
  if (!arrival || !departure) return null;
  const nights = (Date.parse(departure) - Date.parse(arrival)) / 86400000;
  if (!Number.isInteger(nights) || nights <= 0) return null;
  if (!rooms.length || rooms.some((room) => room.price == null || !Number.isFinite(Number(room.price)))) return null;
  const dailyPrice = rooms.reduce((total, room) => total + Number(room.price), 0);
  return { nights, dailyPrice, total: nights * dailyPrice };
}
