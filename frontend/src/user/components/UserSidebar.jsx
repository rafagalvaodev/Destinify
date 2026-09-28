import './UserSidebar.css';
export default function UserSidebar({ section, onNavigate, onLogout }) {
  return (
    <aside className="account-sidebar">
      <div className="account-brand">Destinify</div>
      <nav aria-label="Navegação da conta">
        <button className={section === 'bookings' ? 'active' : ''} onClick={() => onNavigate('bookings')}>Minhas reservas</button>
        <button className={section === 'history' ? 'active' : ''} onClick={() => onNavigate('history')}>Histórico</button>
        <button className={section === 'profile' ? 'active' : ''} onClick={() => onNavigate('profile')}>Perfil</button>
      </nav>
      <button className="logout-button" onClick={onLogout}>Sair da conta</button>
    </aside>
  );
}
