import { useEffect, useLayoutEffect, useState } from 'react';
import Footer from '../../components/Footer/Footer';
import brazilFlag from '../../imagem/flag_brazil.png';
import defaultHotelImage from '../../imagem/imagem_teste.png';
import { getCatalog } from './catalogApi';
import RoomBooking from './RoomBooking';
import { clearBookingDraft } from './bookingDraft';

const types = { SINGLE: 'Individual', SUITE: 'Suíte', TRIPLE: 'Triplo' };
const statuses = { AVALIABLE: 'Disponível', AVAILABLE: 'Disponível', OCCUPIED: 'Ocupado', MAINTENANCE: 'Em manutenção' };
const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export default function HotelRooms({ hotel, onClose, setPaginaAtual, searchCriteria = {}, initialDraft, onBookingCreated }) {
  const loggedIn = Boolean(localStorage.getItem('accessToken'));
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [selectedRooms, setSelectedRooms] = useState(initialDraft?.rooms || hotel.suggestedRooms || []);
  const [bookingSaving, setBookingSaving] = useState(false);

  useLayoutEffect(() => {
    const scrollPosition = window.scrollY;
    window.scrollTo({ top: 0, behavior: 'instant' });
    return () => window.scrollTo({ top: scrollPosition, behavior: 'instant' });
  }, []);

  function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setPaginaAtual('login');
  }

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    getCatalog(`/hotels/${hotel.hotel_id}/rooms?page=${page}&size=6`, controller.signal)
      .then((result) => { if (!controller.signal.aborted) setData({ ...result, page }); })
      .catch((err) => { if (!controller.signal.aborted) setError(err.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [hotel.hotel_id, page, retry]);

  function toggleRoom(room) {
    if (bookingSaving) return;
    const next = selectedRooms.some((item) => item.room_id === room.room_id)
      ? selectedRooms.filter((item) => item.room_id !== room.room_id)
      : [...selectedRooms, room];
    if (!next.length) clearBookingDraft();
    setSelectedRooms(next);
  }

  function changePage(value) { setLoading(true); setPage(value); }

  return (
    <div className="catalog-page hotel-page">
      <header className="header hotel-page__hero">
        <img className="hotel-page__background" src={hotel.imageUrl || defaultHotelImage} alt=""
          onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = defaultHotelImage; }} />
        <div className="header__upper">
          <button className="header__brand" type="button" onClick={onClose}>Destinify</button>
          <nav className="header__navigator" aria-label="Conta">
            <span className="header__currency"><img className="header__flag" src={brazilFlag} alt="Brasil" /> BRL</span>
            {loggedIn ? <>
              <button className="header__button" type="button" onClick={() => setPaginaAtual('userPanel')}>Minha conta</button>
              <button className="header__button" type="button" onClick={logout}>Sair</button>
            </> : <>
              <button className="header__button" type="button" onClick={() => setPaginaAtual('register')}>Cadastre-se</button>
              <button className="header__button" type="button" onClick={() => setPaginaAtual('login')}>Login</button>
            </>}
          </nav>
        </div>
        <section className="header__title">
          <button className="hotel-page__back" type="button" onClick={onClose}>← Voltar aos hotéis</button>
          <h1>{hotel.name}</h1>
          <p>{[hotel.city, hotel.address].filter(Boolean).join(' · ')}</p>
        </section>
      </header>
      <main className="main">
      {hotel.description && <p className="hotel-page__description">{hotel.description}</p>}
      <h2 className="main__subtitle">Escolha seu quarto</h2>
      {hotel.suggestedRooms?.length > 0 && <p role="status">Sugerimos {hotel.suggestedRooms.length} quarto(s) para {searchCriteria.guests} hóspede(s). Você pode ajustar a seleção abaixo.</p>}
      <p className="catalog-note">Selecione um ou mais quartos para reservar juntos. Valores por noite. O status do quarto não confirma disponibilidade para um período específico.</p>
      {loading && <p role="status">Carregando quartos…</p>}
      {error && <div role="alert"><p>{error}</p><button type="button" onClick={() => setRetry((value) => value + 1)}>Tentar novamente</button></div>}
      <div className="catalog-rooms" aria-busy={loading}>
        {data?.content.map((room) => <article className="catalog-room" key={room.room_id}>
          <img src={room.imgUrl || defaultHotelImage} alt={room.imgUrl ? room.name : 'Imagem ilustrativa de hospedagem'} loading="lazy"
            onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = defaultHotelImage; }} />
          <div><h3>{room.name}</h3><p>{room.description}</p>
            <p>{types[room.roomType] || room.roomType} · Até {room.maxCapacity} hóspedes</p>
            <p>{statuses[room.roomStatus] || 'Status não informado'}</p>
            <div className="hotel-page__room-actions">
              <strong>{room.price != null ? `${currency.format(room.price)} / noite` : 'Preço não informado'}</strong>
              <button className="catalog-button hotel-page__reserve" type="button"
                disabled={bookingSaving} aria-pressed={selectedRooms.some((item) => item.room_id === room.room_id)} onClick={() => toggleRoom(room)}>
                {selectedRooms.some((item) => item.room_id === room.room_id) ? 'Remover' : 'Selecionar'}
              </button>
            </div>
          </div>
        </article>)}
      </div>
      {!loading && !error && data?.content.length === 0 && <p>Nenhum quarto cadastrado neste hotel.</p>}
      {data?.totalPages > 1 && <nav className="catalog-pagination" aria-label="Páginas de quartos">
        <button type="button" disabled={loading || page === 0} onClick={() => changePage(page - 1)}>Anterior</button>
        <span>Página {data.page + 1} de {data.totalPages}</span>
        <button type="button" disabled={loading || page >= data.totalPages - 1} onClick={() => changePage(page + 1)}>Próxima</button>
      </nav>}
      {selectedRooms.length > 0 && <RoomBooking initialDraft={initialDraft} searchCriteria={searchCriteria} hotel={hotel} rooms={selectedRooms} onRemove={toggleRoom}
        onBusyChange={setBookingSaving} setPaginaAtual={setPaginaAtual}
        onCreated={onBookingCreated} />}
      </main>
      <Footer />

    </div>
  );
}
