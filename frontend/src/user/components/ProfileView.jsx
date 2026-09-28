import './ProfileView.css';
import { formatDate } from '../api/userApi';

export default function ProfileView({ user, profile, setProfile, password, setPassword, saving, onSaveProfile, onChangePassword, onDelete }) {
  return (
    <>
      <div className="profile-overview">
        <div className="profile-avatar" aria-hidden="true">{user?.name?.charAt(0).toUpperCase()}</div>
        <div className="profile-identity"><span>Conta pessoal</span><h2>{user?.name}</h2><p>{user?.email}</p></div>
        <div className="profile-meta"><small>Data de nascimento</small><strong>{profile.birthDate ? formatDate(profile.birthDate) : 'Data não informada'}</strong></div>
      </div>
      <div className="profile-settings-grid">
        <form className="profile-form" onSubmit={onSaveProfile}>
          <div className="section-heading"><span className="section-number">01</span><div><h2>Dados pessoais</h2><p>Informações usadas na sua conta e nas reservas.</p></div></div>
          <div className="profile-fields">
            <label>Nome completo<input value={profile.name} onChange={(event) => setProfile({ ...profile, name: event.target.value })} required /></label>
            <label>E-mail<input type="email" value={profile.email} onChange={(event) => setProfile({ ...profile, email: event.target.value })} required /></label>
            <label>Data de nascimento<input type="date" value={profile.birthDate} onChange={(event) => setProfile({ ...profile, birthDate: event.target.value })} required /></label>
            <label>Tipo de conta<input value={user?.role === 'ADMIN' ? 'Administrador' : 'Cliente'} disabled /></label>
          </div>
          <div className="form-actions"><button className="primary-button" disabled={saving}>{saving ? 'Salvando...' : 'Salvar dados'}</button></div>
        </form>
        <form className="profile-form security-profile-form" onSubmit={onChangePassword}>
          <div className="section-heading"><span className="section-number">02</span><div><h2>Senha e acesso</h2><p>Atualize sua senha regularmente para proteger a conta.</p></div></div>
          <div className="profile-fields">
            <label>Senha atual<input type="password" value={password.currentPassword} onChange={(event) => setPassword({ ...password, currentPassword: event.target.value })} required /></label>
            <label>Nova senha<input type="password" minLength="8" value={password.newPassword} onChange={(event) => setPassword({ ...password, newPassword: event.target.value })} required /></label>
            <label>Confirmar nova senha<input type="password" minLength="8" value={password.confirmNewPassword} onChange={(event) => setPassword({ ...password, confirmNewPassword: event.target.value })} required /></label>
          </div>
          <div className="form-actions"><button className="primary-button" disabled={saving}>{saving ? 'Alterando...' : 'Atualizar senha'}</button></div>
        </form>
      </div>
      <div className="danger-zone profile-danger">
        <div><span>Zona de risco</span><h2>Excluir conta</h2><p>Esta ação remove permanentemente seus dados e reservas vinculadas.</p></div>
        <button className="danger-button" onClick={onDelete}>Excluir minha conta</button>
      </div>
    </>
  );
}
