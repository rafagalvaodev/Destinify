import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import './Homepage.css';
import '../../css/main.css';
import HotelCard from '../../components/HotelCard/HotelCard';
import Footer from '../../components/Footer/Footer';
import brazilFlag from '../../imagem/flag_brazil.png';
import HotelRooms from './HotelRooms';
import { getCatalog, hotelsPath } from './catalogApi';

export default function Homepage({ setPaginaAtual }) {
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [adults, setAdults] = useState(2);
  const [children, setChildren] = useState(0);
  const [rooms, setRooms] = useState(1);
  const [arrival, setArrival] = useState('');
  const [departure, setDeparture] = useState('');
  const [query, setQuery] = useState({ name: '', city: '', page: 0 });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [selected, setSelected] = useState(null);
  const scrollOnSuccess = useRef(false);
  const results = useRef(null);
  const loggedIn = Boolean(localStorage.getItem('accessToken'));

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    getCatalog(hotelsPath(query), controller.signal)
      .then((result) => { if (!controller.signal.aborted) setData({ ...result, page: query.page }); })
      .catch((err) => { if (!controller.signal.aborted) setError(err.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [query, retry]);

  useLayoutEffect(() => {
    if (data && scrollOnSuccess.current) {
      results.current?.scrollIntoView({ block: 'start', behavior: 'instant' });
      scrollOnSuccess.current = false;
    }
  }, [data]);

  function search(event) {
    event.preventDefault();
    scrollOnSuccess.current = true;
    setLoading(true);
    setQuery({ name: name.trim(), city: city.trim(), page: 0 });
  }
  function clear() {
    setName(''); setCity(''); setLoading(true);
    setAdults(2); setChildren(0); setRooms(1);
    setArrival(''); setDeparture('');
    setQuery({ name: '', city: '', page: 0 });
  }
  function changePage(page) {
    scrollOnSuccess.current = true;
    setLoading(true);
    setQuery((previous) => ({ ...previous, page }));
  }
  function logout() {
    localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken');
    setPaginaAtual('login');
  }

  if (selected) {
    return <HotelRooms key={selected.hotel_id} hotel={selected} onClose={() => setSelected(null)} setPaginaAtual={setPaginaAtual} />;
  }

  return (
    <div className="catalog-page">
      <header className="header">
        <div className="header__upper">
          <button className="header__brand" type="button" onClick={() => { clear(); window.scrollTo({ top: 0 }); }}>Destinify</button>
          <nav className="header__navigator" aria-label="Conta">
            <span className="header__currency"><img className="header__flag" src={brazilFlag} alt="Brasil" /> BRL</span>
            {loggedIn ? <><button type="button" className="header__button" onClick={() => setPaginaAtual('userPanel')}>Minha conta</button>
              <button type="button" className="header__button" onClick={logout}>Sair</button></> : <>
              <button type="button" className="header__button" onClick={() => setPaginaAtual('register')}>Cadastre-se</button>
              <button type="button" className="header__button" onClick={() => setPaginaAtual('login')}>Login</button></>}
          </nav>
        </div>
        <section className="header__title"><h1>Encontre o lugar perfeito para sua estadia!</h1><h2>Conheça os hotéis e escolha seu próximo destino.</h2></section>
        
        <form className="header__search" onSubmit={search}>
          <div className="header__input"><label htmlFor="catalog-city">Escolha seu destino</label>
            <input id="catalog-city" type="text" placeholder="Para onde planeja ir?" value={city} onChange={(event) => setCity(event.target.value)} /></div>
          <div className='header__input'><label htmlFor="arrival-input">Selecione a data</label>
              <div className='header__date'><input type="date" name="arrival-date" id="arrival-input" aria-label="Data de chegada" value={arrival} onChange={(event) => { setArrival(event.target.value); if (departure && event.target.value > departure) setDeparture(''); }}/>
                  <span>—</span>
                  <input type="date" name="departure-date" id="departure-input" aria-label="Data de saída" min={arrival || undefined} value={departure} onChange={(event) => setDeparture(event.target.value)}/>
              </div>
          </div>
          <div className="header__input header__occupancy">
            <span className="header__field-label" id="occupancy-label">Selecione a ocupação</span>
            <details className="occupancy-picker">
              <summary aria-describedby="occupancy-label">{adults} {adults === 1 ? 'adulto' : 'adultos'} · {children} {children === 1 ? 'criança' : 'crianças'} · {rooms} {rooms === 1 ? 'quarto' : 'quartos'}</summary>
              <div className="occupancy-picker__fields">
                <label htmlFor="catalog-adults">Adultos<input id="catalog-adults" type="number" min="1" max="30" required value={adults} onChange={(event) => setAdults(event.target.value === '' ? '' : Number(event.target.value))} /></label>
                <label htmlFor="catalog-children">Crianças<input id="catalog-children" type="number" min="0" max="30" required value={children} onChange={(event) => setChildren(event.target.value === '' ? '' : Number(event.target.value))} /></label>
                <label htmlFor="catalog-rooms">Quartos<input id="catalog-rooms" type="number" min="1" max="30" required value={rooms} onChange={(event) => setRooms(event.target.value === '' ? '' : Number(event.target.value))} /></label>
              </div>
            </details>
          </div>
          <div className="header__input header__hotel-name"><label htmlFor="catalog-name">Nome do hotel</label>
            <input id="catalog-name" type="text" placeholder="Qual hotel procura?" value={name} onChange={(event) => setName(event.target.value)} /></div>
          <button type="submit" disabled={loading}>Pesquisar</button>
          <button type="button" className="catalog-clear" onClick={clear} disabled={loading}>Limpar</button>
        </form>

      </header>
      <main className="main" ref={results}>
        <p className="main__subtitle">Conheça seu próximo destino!</p>
        {loading && <p role="status">Carregando hotéis…</p>}
        {error && <div className="catalog-error" role="alert"><p>{error}</p>
          <button className="catalog-button" type="button" onClick={() => setRetry((value) => value + 1)}>Tentar novamente</button></div>}

        {!loading && !error && data?.content.length === 0 && <p>Nenhum hotel encontrado. Tente outra cidade ou nome.</p>}
        <section className="card__layout" aria-label="Hotéis" aria-busy={loading}>
          {data?.content.map((hotel) => <HotelCard key={hotel.hotel_id} hotel={hotel} onSelect={setSelected} />)}
        </section>
        {data?.totalPages > 0 && <nav className="catalog-pagination" aria-label="Páginas de hotéis">
          <button type="button" disabled={loading || query.page === 0} onClick={() => changePage(query.page - 1)}>Anterior</button>
          <span aria-live="polite">Página {data.page + 1} de {data.totalPages}</span>
          <button type="button" disabled={loading || query.page >= data.totalPages - 1} onClick={() => changePage(query.page + 1)}>Próxima</button>
        </nav>}
      </main>
      <Footer />
    </div>
  );
}
