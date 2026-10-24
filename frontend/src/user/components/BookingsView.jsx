import './BookingsView.css';
import AccountIcon from './AccountIcon';
import { bookingStatusLabel, formatCurrency, formatDate } from '../api/userApi';

function BookingCard({ booking, active, saving, onDetails, onEdit, onPay, onCancel }) {
  const rooms = booking.rooms?.length
    ? booking.rooms.map((room) => room.name || room.roomName || `Quarto #${room.room_id || room.id}`).join(', ')
    : 'Quarto não informado';

  return (
    <article className="booking-card">
      <div className="booking-card-heading">
        <div className="booking-hotel"><span className="booking-hotel-icon"><AccountIcon name="stay" /></span><div><small>Reserva #{booking.id}</small><h3>{booking.hotelName || 'Sua hospedagem'}</h3></div></div>
        <span className={`booking-status status-${String(booking.status || '').toLowerCase()}`}>{bookingStatusLabel[booking.status] || booking.status}</span>
      </div>
      <div className="booking-stay">
        <div className="booking-details">
          <div><small>CHECK-IN</small><strong>{formatDate(booking.checkInDate)}</strong></div>
          <span className="booking-date-arrow" aria-hidden="true">→</span>
          <div><small>CHECK-OUT</small><strong>{formatDate(booking.checkOutDate)}</strong></div>
        </div>
        <p className="booking-room-info">{rooms}</p>
        <p className="booking-guests">{booking.guestsCount} {Number(booking.guestsCount) === 1 ? 'hóspede' : 'hóspedes'}</p>
      </div>
      <div className="booking-price"><span>Total da estadia</span><strong>{formatCurrency(booking.totalPrice)}</strong></div>
      <div className="booking-footer">
        <div className="booking-actions">
          <button type="button" onClick={() => onDetails(booking)}>Detalhes</button>
          {active && booking.status === 'PENDING' && (
            <>
              <button type="button" onClick={() => onEdit(booking)}>Editar</button>
              <button className="pay-booking" type="button" disabled={saving} onClick={() => onPay(booking)}>Pagar</button>
            </>
          )}
          {active && ['PENDING', 'CONFIRMED'].includes(booking.status) && (
            <button className="cancel-booking" type="button" disabled={saving} onClick={() => onCancel(booking)}>Cancelar</button>
          )}
        </div>
      </div>
    </article>
  );
}

export default function BookingsView({ section, bookings, loading, saving, page, totalPages, onPage, onDetails, onEdit, onPay, onCancel }) {
  const active = section === 'bookings';
  return (
    <div>
      <div className="section-heading">
        <h2>{active ? 'Próximas hospedagens' : 'Reservas anteriores'}</h2>
        <p>{active ? 'Acompanhe os detalhes das suas reservas ativas.' : 'Consulte hospedagens concluídas e reservas canceladas.'}</p>
      </div>
      {loading ? (
        <div className="booking-empty">Carregando reservas...</div>
      ) : bookings.length === 0 ? (
        <div className="booking-empty">
          <span className="booking-empty-icon"><AccountIcon name="stay" /></span>
          <strong>{active ? 'Nenhuma reserva ativa' : 'Seu histórico está vazio'}</strong>
          <span>{active ? 'Suas próximas hospedagens aparecerão aqui.' : 'Reservas concluídas ou canceladas aparecerão aqui.'}</span>
        </div>
      ) : (
        <div className={`booking-list ${active ? 'booking-grid' : 'history-grid'}`}>
          {bookings.map((booking) => <BookingCard key={booking.id} booking={booking} active={active} saving={saving} onDetails={onDetails} onEdit={onEdit} onPay={onPay} onCancel={onCancel} />)}
        </div>
      )}
      {totalPages > 1 && (
        <div className="booking-pagination" aria-label="Paginação das reservas">
          <button type="button" disabled={page === 0} onClick={() => onPage(page - 1)}>Anterior</button>
          <span>Página {page + 1} de {totalPages}</span>
          <button type="button" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Próxima</button>
        </div>
      )}
    </div>
  );
}
