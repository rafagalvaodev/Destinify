import './UserHeader.css';
import brazilFlag from '../../imagem/flag_brazil.png';

export default function UserHeader({ section, onNavigate, onLogout, onHome, title, user }) {
  return (
    <header className="account-topbar">
      <div className="account-topbar__upper">
        <button className="account-brand" type="button" onClick={onHome}>Destinify</button>
        <div className="account-topbar__actions">
          <span className="account-currency"><img src={brazilFlag} alt="Brasil" /> BRL</span>
          <button className="account-home" type="button" onClick={onHome}>Explorar hotéis</button>
          <button className="logout-button" type="button" onClick={onLogout}>Sair</button>
        </div>
      </div>
      <div className="account-topbar__title">
        <div><p>Minha conta{user?.name ? ` · Olá, ${user.name}` : ''}</p><h1>{title}</h1></div>
        {user && <div className="user-initial" aria-hidden="true">{user.name?.charAt(0).toUpperCase()}</div>}
      </div>
      <nav className="account-navigation" aria-label="Navegação da conta">
        <button type="button" className={section === 'bookings' ? 'active' : ''} aria-current={section === 'bookings' ? 'page' : undefined} onClick={() => onNavigate('bookings')}>Minhas reservas</button>
        <button type="button" className={section === 'history' ? 'active' : ''} aria-current={section === 'history' ? 'page' : undefined} onClick={() => onNavigate('history')}>Histórico</button>
        <button type="button" className={section === 'profile' ? 'active' : ''} aria-current={section === 'profile' ? 'page' : undefined} onClick={() => onNavigate('profile')}>Perfil e segurança</button>
      </nav>
    </header>
  );
}
