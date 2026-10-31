import { useEffect, useRef, useState } from 'react';
import { apiRequest, formatCurrency } from '../../user/api/userApi';
import { saveBookingDraft, clearBookingDraft } from './bookingDraft';
import { bookingCapacity, bookingQuote, validateBooking } from './bookingValidation';

export default function RoomBooking({ hotel, rooms, onRemove, onCreated, onBusyChange, setPaginaAtual, searchCriteria = {}, initialDraft }) {
  const submitting = useRef(false);
  const [arrival, setArrival] = useState(initialDraft?.arrival ?? searchCriteria.startDate ?? '');
  const [departure, setDeparture] = useState(initialDraft?.departure ?? searchCriteria.endDate ?? '');
  const [adultsCount, setAdultsCount] = useState(initialDraft?.adultsCount ?? searchCriteria.adultsCount ?? initialDraft?.guests ?? searchCriteria.guests ?? 1);
  const [childrenAges, setChildrenAges] = useState(initialDraft?.childrenAges ?? Array.from({ length: searchCriteria.childrenCount || 0 }, () => ''));
  const guests = Number(adultsCount) + childrenAges.length;
  const [summary, setSummary] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const today = new Date().toLocaleDateString('en-CA');
  const capacity = bookingCapacity(rooms);
  const quote = bookingQuote(rooms, arrival, departure);
  const guestError = !Number.isInteger(Number(adultsCount)) || Number(adultsCount) < 1
    ? 'Informe pelo menos 1 hóspede, sem casas decimais.'
    : childrenAges.some((age) => age === '' || !Number.isInteger(Number(age)) || Number(age) < 0 || Number(age) > 17)
      ? 'Informe a idade de cada criança, de 0 a 17 anos, na data da chegada.'
    : Number(guests) > capacity ? `Capacidade máxima: ${capacity} hóspedes. Selecione mais quartos ou reduza a quantidade.` : '';

  const summaryKey = JSON.stringify({ hotelId: hotel.hotel_id, roomIds: rooms.map((room) => room.room_id),
    checkInDate: arrival, checkOutDate: departure, guestsCount: guests,
    adultsCount: Number(adultsCount), childrenAges: childrenAges.map(Number) });
  const currentSummary = summary?.key === summaryKey ? summary.data : null;
  const validSummary = !guestError && !validateBooking({ rooms, arrival, departure, guests, today });

  useEffect(() => {
    if (!validSummary || !localStorage.getItem('accessToken')) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      apiRequest('/bookings/summary', { method: 'POST', body: summaryKey, signal: controller.signal })
        .then((data) => { if (!controller.signal.aborted) setSummary({ key: summaryKey, data }); })
        .catch((err) => { if (!controller.signal.aborted) setSummary({ key: summaryKey, error: err.message }); });
    }, 300);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [summaryKey, validSummary]);

  const resumed = useRef(false);

  function persistDraft(resumeAfterLogin = false) {
    saveBookingDraft({ hotel, rooms, arrival, departure, guests, adultsCount, childrenAges, searchCriteria, resumeAfterLogin });
  }

  useEffect(() => {
    try {
      saveBookingDraft({ hotel, rooms, arrival, departure, guests, adultsCount, childrenAges, searchCriteria, resumeAfterLogin: false });
    } catch {
      setError('Não foi possível salvar a reserva neste navegador. Verifique o armazenamento antes de fazer login.');
    }
  }, [hotel, rooms, arrival, departure, guests, adultsCount, childrenAges, searchCriteria]);

  useEffect(() => {
    if (!initialDraft?.resumeAfterLogin || !localStorage.getItem('accessToken') || resumed.current) return;
    resumed.current = true;
    reserve();
  });

  async function reserve(event) {
    event?.preventDefault();
    if (submitting.current) return;
    if (guestError) { setError(guestError); return; }
    const validation = validateBooking({ rooms, arrival, departure, guests, today });
    if (validation) { setError(validation); return; }
    if (!localStorage.getItem('accessToken')) {
      try { persistDraft(true); } catch {
        setError('Não foi possível salvar a reserva. Libere o armazenamento do navegador para continuar após o login.');
        return;
      }
      setPaginaAtual('login');
      return;
    }
    submitting.current = true;
    setSaving(true);
    onBusyChange(true);
    setError('');
    try {
      const result = await apiRequest('/bookings', {
        method: 'POST',
        body: JSON.stringify({ hotelId: hotel.hotel_id, roomIds: rooms.map((room) => room.room_id), checkInDate: arrival, checkOutDate: departure, guestsCount: Number(guests), adultsCount: Number(adultsCount), childrenAges: childrenAges.map(Number) }),
      });
      // A reserva já existe no servidor: nunca repetir a criação se a limpeza local falhar.
      try { clearBookingDraft(); } catch { /* A reserva criada segue para o pagamento. */ }
      onCreated(result);
    } catch (requestError) {
      if (requestError.status === 401) {
        try { persistDraft(true); } catch { setError('Não foi possível salvar a reserva para renovar sua sessão.'); return; }
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setPaginaAtual('login');
      } else {
        setError(requestError.message);
      }
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
          <label htmlFor="booking-adults">Adultos
            <input id="booking-adults" type="number" min="1" max="60" step="1" required value={adultsCount}
              onChange={(event) => { setAdultsCount(event.target.value); setError(''); }} />
          </label>
          <label htmlFor="booking-children">Crianças
            <input id="booking-children" type="number" min="0" max="59" step="1" required value={childrenAges.length}
              onChange={(event) => {
                const count = Number(event.target.value);
                if (Number.isInteger(count) && count >= 0 && count <= 59)
                  setChildrenAges((ages) => Array.from({ length: count }, (_, index) => ages[index] ?? ''));
                setError('');
              }} />
          </label>
          {childrenAges.map((age, index) => <label key={index} htmlFor={`booking-child-${index}`}>
            Idade da criança {index + 1} na chegada
            <input id={`booking-child-${index}`} type="number" min="0" max="17" step="1" required value={age}
              onChange={(event) => { setChildrenAges((ages) => ages.map((value, i) => i === index ? event.target.value : value)); setError(''); }} />
          </label>)}
        </div>
        <p>Total: {guests} hóspedes. Com pelo menos uma criança de 0 a 6 anos, a reserva recebe 10% de desconto adicional, aplicado uma única vez sobre o saldo após os demais descontos.</p>
        <p id="booking-capacity">Capacidade total: {capacity} hóspedes.</p>
        {guestError && <p id="booking-guest-error" className="catalog-error" role="alert">{guestError}</p>}
        <div className="hotel-page__booking-total" aria-live="polite" aria-atomic="true">
          {quote ? <>
            <p>{quote.nights} {quote.nights === 1 ? 'diária' : 'diárias'} × {formatCurrency(quote.dailyPrice)} por noite</p>
            {currentSummary ? <>
              <p>Valor original: {formatCurrency(currentSummary.basePrice)}</p>
              {currentSummary.promotions.map((promotion) => <p key={promotion.code}>
                {promotion.name}{Number(promotion.amount) > 0 ? `: −${formatCurrency(promotion.amount)}` : ''}
              </p>)}
              <p>Descontos: {formatCurrency(currentSummary.discountTotal)}</p>
              <p>Total da estadia: <strong>{formatCurrency(currentSummary.totalPrice)}</strong></p>
            </> : <p>Estimativa sem promoções: <strong>{formatCurrency(quote.total)}</strong></p>}
            {summary?.key === summaryKey && summary.error && <p role="alert">{summary.error}</p>}
            {!localStorage.getItem('accessToken') && <p>Entre na sua conta para consultar as promoções aplicáveis.</p>}
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
