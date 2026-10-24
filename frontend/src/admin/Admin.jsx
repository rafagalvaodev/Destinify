import { useCallback, useEffect, useRef, useState } from 'react';
import './Admin.css';

const API = 'http://localhost:8080/api';
const tabs = ['hotels', 'rooms', 'bookings', 'users'];
const titles = { hotels: 'Hotéis', rooms: 'Quartos', bookings: 'Reservas', users: 'Usuários' };
const paths = { hotels: '/hotels', rooms: '/rooms/all', bookings: '/bookings/all', users: '/users/all-users' };
const emptyHotel = { name: '', city: '', address: '', description: '', pricePerNight: '', stars: 3, imageUrl: '' };
const emptyRoom = { name: '', hotelId: '', description: '', roomType: 'SINGLE', price: '', imgUrl: '', roomStatus: 'AVALIABLE' };

async function request(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('accessToken')}`,
      ...options.headers,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message || body?.detail || `Erro ${response.status} ao consultar a API`);
  }
  if (response.status === 204) return null;
  return response.headers.get('content-type')?.includes('application/json')
    ? response.json()
    : response.text();
}

function Field({ label, name, value, onChange, type = 'text', required = false, children }) {
  return <label className="admin-field"><span>{label}</span>{children || <input name={name} type={type} value={value ?? ''} onChange={onChange} required={required} />}</label>;
}

function HotelCard({ hotel, onOpen, onEdit, onDelete }) {
  const [imageFailed, setImageFailed] = useState(false);

  return <article className="admin-hotel-card">
    <button className="admin-hotel-open" type="button" onClick={() => onOpen(hotel)} aria-label={`Ver quartos de ${hotel.name}`}>
      <span className="admin-hotel-photo">
        {hotel.imageUrl && !imageFailed
          ? <img src={hotel.imageUrl} alt="" loading="lazy" onError={() => setImageFailed(true)} />
          : <span>Imagem indisponível</span>}
      </span>
      <span className="admin-hotel-details">
        <span className="admin-hotel-title"><span className="admin-card-heading">{hotel.name}</span><span aria-label={`${hotel.stars} estrelas`}>★ {hotel.stars}</span></span>
        <span className="admin-hotel-location">{hotel.city} · {hotel.address}</span>
        {hotel.description && <span className="admin-hotel-description">{hotel.description}</span>}
        <span className="admin-open-label">Ver quartos</span>
      </span>
    </button>
    <div className="admin-hotel-footer"><small>Hotel #{hotel.hotel_id}</small><div>
      <button type="button" onClick={() => onEdit(hotel)}>Editar</button>
      <button type="button" className="danger" onClick={() => onDelete(hotel)}>Excluir</button>
    </div>
    </div>
  </article>;
}

function RoomCard({ room, hotelName, onEdit, onDelete }) {
  const [imageFailed, setImageFailed] = useState(false);
  const status = { AVALIABLE: 'Disponível', OCCUPIED: 'Ocupado', MAINTENANCE: 'Manutenção' };
  const type = { SINGLE: 'Individual', SUITE: 'Suíte', TRIPLE: 'Triplo' };

  return <article className="admin-hotel-card">
    <div className="admin-hotel-photo">
      {room.imgUrl && !imageFailed
        ? <img src={room.imgUrl} alt={room.name} loading="lazy" onError={() => setImageFailed(true)} />
        : <span>Imagem indisponível</span>}
    </div>
    <div className="admin-hotel-details">
      <div className="admin-hotel-title"><h3>{room.name}</h3><span>{status[room.roomStatus] || room.roomStatus}</span></div>
      <p className="admin-hotel-location">{hotelName && `${hotelName} · `}{type[room.roomType] || room.roomType} · {room.maxCapacity} hóspedes</p>
      {room.description && <p className="admin-hotel-description">{room.description}</p>}
      <strong className="admin-room-price">{Number(room.price).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} <small>/ noite</small></strong>
      <div className="admin-hotel-footer"><small>Quarto #{room.room_id}</small><div>
        <button type="button" onClick={() => onEdit(room)}>Editar</button>
        <button type="button" className="danger" onClick={() => onDelete(room)}>Excluir</button>
      </div></div>
    </div>
  </article>;
}

export default function Admin({ setPaginaAtual }) {
  const [tab, setTab] = useState('hotels');
  const [page, setPage] = useState(0);
  const [hotelListPage, setHotelListPage] = useState(0);
  const [selectedHotel, setSelectedHotel] = useState(null);
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [adminName, setAdminName] = useState('');
  const [searchDraft, setSearchDraft] = useState({ name: '', city: '' });
  const [hotelSearch, setHotelSearch] = useState({ name: '', city: '' });
  const loadRequest = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++loadRequest.current;
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page: String(page), size: '12' });
      if (tab === 'hotels') {
        if (hotelSearch.name) params.set('name', hotelSearch.name);
        if (hotelSearch.city) params.set('city', hotelSearch.city);
      }
      const path = tab === 'hotels' && (hotelSearch.name || hotelSearch.city)
        ? '/hotels/search'
        : tab === 'rooms' && selectedHotel
          ? `/hotels/${selectedHotel.hotel_id}/rooms`
          : paths[tab];
      const data = await request(`${path}${tab === 'users' ? '' : `?${params}`}`);
      if (requestId !== loadRequest.current) return;
      setItems(Array.isArray(data) ? data : data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err) {
      if (requestId === loadRequest.current) {
        setItems([]);
        setTotalPages(1);
        setError(err.message);
      }
    } finally {
      if (requestId === loadRequest.current) setLoading(false);
    }
  }, [tab, page, hotelSearch, selectedHotel]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    request('/hotels?size=200').then((data) => setHotels(data.content || [])).catch(() => {});
  }, []);
  useEffect(() => {
    request('/users/me').then((name) => setAdminName(name.trim())).catch(() => {});
  }, []);

  function switchTab(next) {
    if (next === 'hotels' && selectedHotel) {
      backToHotels();
      return;
    }
    setSelectedHotel(null);
    setTab(next);
    setPage(0);
    setForm(null);
    setPendingDelete(null);
    setNotice('');
    setLoading(true);
  }
  function openHotel(hotel) {
    setHotelListPage(page);
    setSelectedHotel(hotel);
    setTab('rooms');
    setPage(0);
    setForm(null);
    setNotice('');
    setLoading(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  function backToHotels() {
    setSelectedHotel(null);
    setTab('hotels');
    setPage(hotelListPage);
    setForm(null);
    setPendingDelete(null);
    setNotice('');
    setLoading(true);
  }
  function newItem() {
    setForm(tab === 'hotels' ? { ...emptyHotel } : { ...emptyRoom, hotelId: selectedHotel?.hotel_id || '' });
    setError('');
  }
  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setHotelSearch({ name: searchDraft.name.trim(), city: searchDraft.city.trim() });
  }
  function clearSearch() {
    setSearchDraft({ name: '', city: '' });
    setHotelSearch({ name: '', city: '' });
    setPage(0);
  }
  function updateForm(event) { setForm((current) => ({ ...current, [event.target.name]: event.target.value })); }
  function beginEdit(item) {
    setForm(tab === 'hotels'
      ? { ...emptyHotel, ...item, id: item.hotel_id, pricePerNight: '' }
      : { ...emptyRoom, ...item, id: item.room_id, hotelId: item.hotel_id });
    setNotice('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function save(event) {
    event.preventDefault();
    setSaving(true); setError(''); setNotice('');
    try {
      const isHotel = tab === 'hotels';
      const body = isHotel
        ? { name: form.name, city: form.city, address: form.address, description: form.description, stars: Number(form.stars), imageUrl: form.imageUrl,
            ...(!form.id ? { pricePerNight: Number(form.pricePerNight) } : {}) }
        : { name: form.name, description: form.description, roomType: form.roomType, price: Number(form.price), imgUrl: form.imgUrl, roomStatus: form.roomStatus,
            ...(!form.id ? { hotelId: Number(form.hotelId) } : {}) };
      const path = isHotel ? `/hotels${form.id ? `/${form.id}` : ''}` : form.id ? `/rooms/${form.id}` : '/rooms/newRoom';
      await request(path, { method: form.id ? isHotel ? 'PUT' : 'PATCH' : 'POST', body: JSON.stringify(body) });
      setForm(null); setNotice('Registro salvo.'); await load();
      if (isHotel) request('/hotels?size=200').then((data) => setHotels(data.content || [])).catch(() => {});
    } catch (err) { setError(err.message); }
    finally { setSaving(false); }
  }

  async function remove() {
    if (!pendingDelete) return;
    const { id, type } = pendingDelete;
    setDeleting(true);
    setError(''); setNotice('');
    try {
      await request(`/${type}/${id}`, { method: 'DELETE' });
      setPendingDelete(null);
      setNotice('Registro excluído.'); await load();
    } catch (err) { setPendingDelete(null); setError(err.message); }
    finally { setDeleting(false); }
  }

  function askToRemove(item) {
    const id = tab === 'hotels' ? item.hotel_id : tab === 'rooms' ? item.room_id : item.id;
    setPendingDelete({ id, type: tab, name: item.name || item.hotelName || `#${id}` });
  }

  function logout() {
    localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken'); setPaginaAtual('login');
  }

  return <div className="admin-shell">
    <aside className="admin-sidebar">
      <div className="admin-brand">Destinify
        <small>ADMIN</small>
      </div>
      <nav aria-label="Administração">{tabs.map((name) => <button key={name} type="button" className={tab === name ? 'active' : ''} onClick={() => switchTab(name)}>{titles[name]}</button>)}</nav>
      <button className="admin-logout" type="button" onClick={logout}>Sair</button>
    </aside>
    <main className="admin-main">
      <header className="admin-header"><div>{selectedHotel ? <nav className="admin-breadcrumb" aria-label="Caminho"><button type="button" onClick={backToHotels}>Hotéis</button><span>/</span><span>{selectedHotel.name}</span></nav> : <span>Administração</span>}<h1>{selectedHotel ? `Quartos de ${selectedHotel.name}` : titles[tab]}</h1></div><div className="admin-header-actions"><div className="admin-user"><small>Conta admin</small><strong>{adminName || 'Administrador'}</strong></div>{['hotels', 'rooms'].includes(tab) && <button className="admin-primary" type="button" onClick={newItem}>+ Novo {tab === 'hotels' ? 'hotel' : 'quarto'}</button>}</div></header>
      {error && <p className="admin-alert" role="alert">{error}</p>}{notice && <p className="admin-notice" role="status">{notice}</p>}
      {form && <section className="admin-editor"><div className="admin-section-heading"><h2>{form.id ? 'Editar' : 'Cadastrar'} {tab === 'hotels' ? 'hotel' : 'quarto'}</h2><button type="button" onClick={() => setForm(null)} aria-label="Fechar formulário">×</button></div><form onSubmit={save} className="admin-form">
        <Field label="Nome" name="name" value={form.name} onChange={updateForm} required />
        {tab === 'hotels' ? <>
          <Field label="Cidade" name="city" value={form.city} onChange={updateForm} required />
          <Field label="Endereço" name="address" value={form.address} onChange={updateForm} required />
          {!form.id && <Field label="Preço por noite" name="pricePerNight" value={form.pricePerNight} onChange={updateForm} type="number" required />}
          <Field label="Estrelas" name="stars" value={form.stars} onChange={updateForm} type="number" required />
          <Field label="URL da imagem" name="imageUrl" value={form.imageUrl} onChange={updateForm} />
        </> : <>
          {!form.id && <Field label="Hotel" name="hotelId">{selectedHotel ? <input value={selectedHotel.name} readOnly /> : <select name="hotelId" value={form.hotelId} onChange={updateForm} required><option value="">Selecione</option>{hotels.map((hotel) => <option key={hotel.hotel_id} value={hotel.hotel_id}>{hotel.name}</option>)}</select>}</Field>}
          <Field label="Tipo" name="roomType"><select name="roomType" value={form.roomType} onChange={updateForm}><option value="SINGLE">Individual</option><option value="SUITE">Suíte</option><option value="TRIPLE">Triplo</option></select></Field>
          <Field label="Preço" name="price" value={form.price} onChange={updateForm} type="number" required />
          <Field label="Status" name="roomStatus"><select name="roomStatus" value={form.roomStatus} onChange={updateForm}><option value="AVALIABLE">Disponível</option><option value="OCCUPIED">Ocupado</option><option value="MAINTENANCE">Manutenção</option></select></Field>
          <Field label="URL da imagem" name="imgUrl" value={form.imgUrl} onChange={updateForm} />
        </>}
        <label className="admin-field admin-wide"><span>Descrição</span><textarea name="description" value={form.description || ''} onChange={updateForm} rows="3" /></label>
        <div className="admin-form-actions"><button type="button" onClick={() => setForm(null)}>Cancelar</button><button className="admin-primary" type="submit" disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</button></div>
      </form></section>}
      <section className={`admin-list ${['hotels', 'rooms'].includes(tab) ? 'admin-list-gallery' : ''}`}><div className="admin-section-heading"><h2>{tab === 'hotels' ? 'Hotéis cadastrados' : tab === 'rooms' ? 'Quartos cadastrados' : 'Registros'}</h2><button type="button" onClick={load} aria-label="Atualizar lista" title="Atualizar lista">↻</button></div>
        {tab === 'hotels' && <form className="admin-search" onSubmit={submitSearch} role="search">
          <label>Nome do hotel<input type="search" value={searchDraft.name} onChange={(event) => setSearchDraft((current) => ({ ...current, name: event.target.value }))} placeholder="Ex.: Grand Plaza" /></label>
          <label>Cidade<input type="search" value={searchDraft.city} onChange={(event) => setSearchDraft((current) => ({ ...current, city: event.target.value }))} placeholder="Ex.: São Paulo" /></label>
          <button className="admin-primary" type="submit">Buscar</button>
          <button type="button" onClick={clearSearch}>Limpar</button>
        </form>}
        {loading ? <p className="admin-empty">Carregando...</p> : items.length === 0 ? <p className="admin-empty">Nenhum registro encontrado.</p> : tab === 'hotels' ? <div className="admin-item-grid">{items.map((hotel) => <HotelCard key={`${hotel.hotel_id}-${hotel.imageUrl}`} hotel={hotel} onOpen={openHotel} onEdit={beginEdit} onDelete={askToRemove} />)}</div> : tab === 'rooms' ? <div className="admin-item-grid">{items.map((room) => <RoomCard key={`${room.room_id}-${room.imgUrl}`} room={room} hotelName={selectedHotel ? '' : hotels.find((hotel) => hotel.hotel_id === room.hotel_id)?.name || `Hotel #${room.hotel_id}`} onEdit={beginEdit} onDelete={askToRemove} />)}</div> : <div className="admin-table-wrap"><table><thead><tr>{(tab === 'bookings' ? ['Reserva', 'Cliente', 'Hotel / quarto', 'Período', 'Status', 'Ações'] : ['Usuário', 'Email', 'Nascimento', 'Perfil']).map((heading) => <th key={heading}>{heading}</th>)}</tr></thead><tbody>{items.map((item) => <tr key={item.id}>
          {tab === 'bookings' && <><td><strong>#{item.id}</strong><small>{item.createdAt?.slice(0, 10)}</small></td><td>{item.userName}</td><td>{item.hotelName}<small>{item.roomName}</small></td><td>{item.checkInDate} a {item.checkOutDate}</td><td>{item.status}</td></>}
          {tab === 'users' && <><td><strong>{item.name}</strong><small>#{item.id}</small></td><td>{item.email}</td><td>{item.birthDate}</td><td>{item.role}</td></>}
          {tab !== 'users' && <td className="admin-actions">{tab !== 'bookings' && <button type="button" onClick={() => beginEdit(item)}>Editar</button>}<button type="button" className="danger" onClick={() => askToRemove(item)}>Excluir</button></td>}
        </tr>)}</tbody></table></div>}
        {tab !== 'users' && totalPages > 1 && <div className="admin-pagination"><button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button><span>Página {page + 1} de {totalPages}</span><button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Próxima</button></div>}
      </section>
    </main>
    {pendingDelete && <div className="admin-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget && !deleting) setPendingDelete(null); }}>
      <div className="admin-modal" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description" onKeyDown={(event) => { if (event.key === 'Escape' && !deleting) setPendingDelete(null); }}>
        <h2 id="delete-title">Excluir {pendingDelete.type === 'hotels' ? 'hotel' : pendingDelete.type === 'rooms' ? 'quarto' : 'reserva'}?</h2>
        <p id="delete-description">{pendingDelete.type === 'bookings' ? `A reserva #${pendingDelete.id}` : `“${pendingDelete.name}”`} será excluída permanentemente.</p>
        <div className="admin-modal-actions">
          <button type="button" autoFocus disabled={deleting} onClick={() => setPendingDelete(null)}>Cancelar</button>
          <button type="button" className="admin-delete-button" disabled={deleting} onClick={remove}>{deleting ? 'Excluindo...' : 'Excluir'}</button>
        </div>
      </div>
    </div>}
  </div>;
}
