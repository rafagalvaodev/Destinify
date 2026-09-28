import './BookingsView.css';
import { bookingStatusLabel, formatCurrency, formatDate } from '../api/userApi';

function BookingCard({ booking, active, saving, onDetails, onEdit, onPay, onCancel }) {
  const rooms = booking.rooms?.length
    ? booking.rooms.map((room) => room.name || room.roomName || `Quarto #${room.room_id || room.id}`).join(', ')
    : 'Quarto não informado';

  return (
    <article className="booking-card">
      <div className="booking-card-heading">
        <div><small>Reserva #{booking.id}</small><h3>{booking.hotelName}</h3></div>
        <span className={`booking-status status-${booking.status.toLowerCase()}`}>{bookingStatusLabel[booking.status] || booking.status}</span>
      </div>
      <div className="booking-details">
        <div><small>Entrada</small><strong>{formatDate(booking.checkInDate)}</strong></div>
        <div><small>Saída</small><strong>{formatDate(booking.checkOutDate)}</strong></div>
        <div><small>Hóspedes</small><strong>{booking.guestsCount}</strong></div>
        <div><small>Valor total</small><strong>{formatCurrency(booking.totalPrice)}</strong></div>
      </div>
      <div className="booking-footer">
        <span>{rooms}</span>
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
