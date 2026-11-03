import './BookingModals.css';
import { bookingStatusLabel, formatCurrency, formatDate } from '../api/userApi';

const roomNames = (booking) => booking.rooms?.map((room) => room.name || room.roomName || `Quarto #${room.room_id || room.id}`).join(', ') || 'Não informado';

function BookingDiscounts({ booking }) {
  if (booking.basePrice == null) return null;
  return <div className="payment-summary">
    <div><small>Valor original</small><strong>{formatCurrency(booking.basePrice)}</strong></div>
    {booking.promotions?.map((promotion) => <div key={promotion.code}>
      <small>{promotion.name}</small><strong>{Number(promotion.amount) > 0 ? `−${formatCurrency(promotion.amount)}` : 'Sem cobrança extra'}</strong>
    </div>)}
    <div><small>Total de descontos</small><strong>{formatCurrency(booking.discountTotal)}</strong></div>
  </div>;
}

export function BookingDetailsModal({ booking, onClose }) {
  if (!booking) return null;
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="booking-modal" role="dialog" aria-modal="true" aria-labelledby="booking-detail-title">
    <div className="modal-heading"><div><small>Reserva #{booking.id}</small><h2 id="booking-detail-title">{booking.hotelName}</h2></div><button type="button" aria-label="Fechar" onClick={onClose}>×</button></div>
    <div className="modal-detail-grid">
      <div><small>Entrada</small><strong>{formatDate(booking.checkInDate)}</strong></div><div><small>Saída</small><strong>{formatDate(booking.checkOutDate)}</strong></div>
      <div><small>Hóspedes</small><strong>{booking.guestsCount}</strong></div><div><small>Valor total</small><strong>{formatCurrency(booking.totalPrice)}</strong></div>
      <div><small>Status</small><strong>{bookingStatusLabel[booking.status]}</strong></div><div><small>Reservada em</small><strong>{booking.createdAt ? new Date(booking.createdAt).toLocaleDateString('pt-BR') : 'Não informado'}</strong></div>
    </div>
    <BookingDiscounts booking={booking} />
    <div className="modal-rooms"><small>Quartos</small><strong>{roomNames(booking)}</strong></div>
  </div></div>;
}

export function BookingEditModal({ booking, values, setValues, saving, onSubmit, onClose }) {
  if (!booking) return null;
  return <div className="modal-backdrop" role="presentation"><form className="booking-modal" onSubmit={onSubmit}>
    <div className="modal-heading"><div><small>Reserva #{booking.id}</small><h2>Editar reserva</h2></div><button type="button" aria-label="Fechar" onClick={onClose}>×</button></div>
    <div className="modal-form-grid">
      <label>Entrada<input type="date" value={values.checkInDate} onChange={(event) => setValues({ ...values, checkInDate: event.target.value })} required /></label>
      <label>Saída<input type="date" value={values.checkOutDate} min={values.checkInDate} onChange={(event) => setValues({ ...values, checkOutDate: event.target.value })} required /></label>
      <label>Adultos<input type="number" min="1" max="60" step="1" value={values.adultsCount} onChange={(event) => setValues({ ...values, adultsCount: event.target.value })} required /></label>
      <label>Crianças<input type="number" min="0" max="59" step="1" value={values.childrenAges?.length || 0} onChange={(event) => {
        const count = Number(event.target.value);
        if (Number.isInteger(count) && count >= 0 && count <= 59) setValues({ ...values, childrenAges: Array.from({ length: count }, (_, i) => values.childrenAges?.[i] ?? '') });
      }} required /></label>
      {values.childrenAges?.map((age, index) => <label key={index}>Idade da criança {index + 1} na chegada
        <input type="number" min="0" max="17" step="1" value={age} required onChange={(event) => setValues({ ...values, childrenAges: values.childrenAges.map((value, i) => i === index ? event.target.value : value) })} />
      </label>)}
    </div>
    <div className="modal-actions"><button className="secondary-button" type="button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={saving}>{saving ? 'Salvando...' : 'Salvar alterações'}</button></div>
  </form></div>;
}

export function PaymentModal({ booking, saving, onConfirm, onClose }) {
  if (!booking) return null;
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="booking-modal payment-modal" role="dialog" aria-modal="true" aria-labelledby="payment-title">
    <div className="modal-heading"><div><small>Reserva #{booking.id}</small><h2 id="payment-title">Confirmar pagamento</h2></div><button type="button" aria-label="Fechar" onClick={onClose}>×</button></div>
    <div className="payment-summary"><div><small>Hotel</small><strong>{booking.hotelName}</strong></div><div><small>Período</small><strong>{formatDate(booking.checkInDate)} a {formatDate(booking.checkOutDate)}</strong></div><div className="payment-total"><small>Valor total</small><strong>{formatCurrency(booking.totalPrice)}</strong></div></div>
    <BookingDiscounts booking={booking} />
    <p className="payment-note">Ao confirmar, o pagamento será processado e a reserva passará para o status confirmado.</p>
    <div className="modal-actions"><button className="secondary-button" type="button" onClick={onClose} disabled={saving}>Cancelar</button><button className="primary-button" type="button" onClick={() => onConfirm(booking.id)} disabled={saving}>{saving ? 'Processando...' : 'Confirmar pagamento'}</button></div>
  </div></div>;
}

export function ConfirmModal({ open, title, description, confirmLabel, busyLabel, saving, onConfirm, onClose }) {
  if (!open) return null;
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><div className="delete-modal" role="dialog" aria-modal="true">
    <h2>{title}</h2><p>{description}</p>
    <div className="modal-actions"><button className="secondary-button" onClick={onClose} disabled={saving}>Cancelar</button><button className="danger-button" onClick={onConfirm} disabled={saving}>{saving ? busyLabel : confirmLabel}</button></div>
  </div></div>;
}
