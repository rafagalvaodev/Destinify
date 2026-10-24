import { useRef, useState } from 'react';
import { apiRequest, formatCurrency } from '../../user/api/userApi';
import { bookingCapacity, bookingQuote, validateBooking } from './bookingValidation';

export default function RoomBooking({ hotel, rooms, onRemove, onCreated, onBusyChange, setPaginaAtual }) {
  const submitting = useRef(false);
  const [arrival, setArrival] = useState('');
  const [departure, setDeparture] = useState('');
  const [guests, setGuests] = useState(1);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const today = new Date().toLocaleDateString('en-CA');
  const capacity = bookingCapacity(rooms);
  const quote = bookingQuote(rooms, arrival, departure);
  const guestError = !Number.isInteger(Number(guests)) || Number(guests) < 1
    ? 'Informe pelo menos 1 hóspede, sem casas decimais.'
    : Number(guests) > capacity ? `Capacidade máxima: ${capacity} hóspedes. Selecione mais quartos ou reduza a quantidade.` : '';

  async function reserve(event) {
    event.preventDefault();
    if (submitting.current) return;
    const validation = validateBooking({ rooms, arrival, departure, guests, today });
    if (validation) { setError(validation); return; }
    if (!localStorage.getItem('accessToken')) { setPaginaAtual('login'); return; }
    submitting.current = true;
    setSaving(true);
    onBusyChange(true);
    setError('');
    try {
      const result = await apiRequest('/bookings', {
        method: 'POST',
        body: JSON.stringify({ hotelId: hotel.hotel_id, roomIds: rooms.map((room) => room.room_id), checkInDate: arrival, checkOutDate: departure, guestsCount: Number(guests) }),
      });
      onCreated(result);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      submitting.current = false;
      setSaving(false);
      onBusyChange(false);
    }
  }

  return (
    <form className="hotel-page__selection" aria-label="Reserva dos quartos selecionados" onSubmit={reserve}>
      <fieldset disabled={saving}>
        <legend>{rooms.length} {rooms.length === 1 ? 'quarto selecionado' : 'quartos selecionados'}</legend>
        <ul>{rooms.map((room) => <li key={room.room_id}>
          <span>{room.name} · Até {room.maxCapacity} hóspedes</span>
          <button type="button" onClick={() => onRemove(room)} aria-label={`Remover ${room.name}`}>Remover</button>
        </li>)}</ul>
        <div className="hotel-page__selection-dates">
          <label htmlFor="booking-arrival">Chegada<input id="booking-arrival" type="date" required min={today} value={arrival} onChange={(event) => {
            setArrival(event.target.value); setError('');
            if (departure && event.target.value >= departure) setDeparture('');
          }} /></label>
          <label htmlFor="booking-departure">Saída<input id="booking-departure" type="date" required min={arrival || today} value={departure} onChange={(event) => { setDeparture(event.target.value); setError(''); }} /></label>
        </div>
        <div className="hotel-page__guests">
          <label htmlFor="booking-guests">Total de hóspedes</label>
          <div className="hotel-page__guest-counter">
            <button type="button" aria-label="Diminuir hóspedes" disabled={saving || Number(guests) <= 1}
              onClick={() => { setGuests(Math.max(1, Math.min(capacity, (Number(guests) || 1) - 1))); setError(''); }}>−</button>
            <input id="booking-guests" type="number" required min="1" max={capacity} step="1" value={guests}
              aria-invalid={Boolean(guestError)} aria-describedby={`booking-capacity${guestError ? ' booking-guest-error' : ''}`}
              onChange={(event) => { setGuests(event.target.value); setError(''); }} />
            <button type="button" aria-label="Aumentar hóspedes" disabled={saving || Number(guests) >= capacity}
              onClick={() => { setGuests(Math.min(capacity, Math.max(1, (Number(guests) || 0) + 1))); setError(''); }}>+</button>
          </div>
        </div>
        <p id="booking-capacity">Capacidade total: {capacity} hóspedes.</p>
        {guestError && <p id="booking-guest-error" className="catalog-error" role="alert">{guestError}</p>}
        <div className="hotel-page__booking-total" aria-live="polite" aria-atomic="true">
          {quote ? <>
            <p>{quote.nights} {quote.nights === 1 ? 'diária' : 'diárias'} × {formatCurrency(quote.dailyPrice)} por noite</p>
            <p>Total da estadia: <strong>{formatCurrency(quote.total)}</strong></p>
          </> : <p>Selecione chegada e saída válidas para calcular o total da estadia.</p>}
        </div>
        {arrival && departure && departure <= arrival && <p role="alert">A saída deve ser posterior à chegada.</p>}
        <p>Todos os quartos serão reservados para as mesmas datas.</p>
      </fieldset>
      {error && <p className="catalog-error" role="alert">{error}</p>}
      <button className="catalog-button" type="submit" disabled={saving || Boolean(guestError)}>{saving ? 'Reservando…' : rooms.length === 1 ? 'Reservar quarto' : 'Reservar quartos'}</button>
    </form>
  );
}
