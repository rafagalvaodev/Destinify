import { useEffect, useState } from 'react';
import { apiRequest } from './api/userApi';
import BookingsView from './components/BookingsView';
import { BookingDetailsModal, BookingEditModal, ConfirmModal, PaymentModal } from './components/BookingModals';
import ProfileView from './components/ProfileView';
import UserSidebar from './components/UserSidebar';
import './UserPanel.css';

const emptyPassword = { currentPassword: '', newPassword: '', confirmNewPassword: '' };

export default function UserPanel({ setPaginaAtual }) {
  const [section, setSection] = useState('bookings');
  const [user, setUser] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [bookingsLoading, setBookingsLoading] = useState(true);
  const [profile, setProfile] = useState({ name: '', email: '', birthDate: '' });
  const [password, setPassword] = useState(emptyPassword);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [editingBooking, setEditingBooking] = useState(null);
  const [editBooking, setEditBooking] = useState({ checkInDate: '', checkOutDate: '', guestsCount: 1 });
  const [pendingCancel, setPendingCancel] = useState(null);
  const [pendingPayment, setPendingPayment] = useState(null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    apiRequest('/users/me')
      .then((data) => {
        setUser(data);
        setProfile({ name: data.name || '', email: data.email || '', birthDate: data.birthDate || '' });
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
    apiRequest('/bookings/me?size=100&sort=checkInDate,desc')
      .then((data) => setBookings(data.content || []))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setBookingsLoading(false));
  }, []);

  function clearMessages() {
    setError('');
    setNotice('');
  }

  function navigate(nextSection) {
    setSection(nextSection);
    setPage(0);
    clearMessages();
  }

  function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setPaginaAtual('login');
  }

  async function runAction(action) {
    clearMessages();
    setSaving(true);
    try {
      await action();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  function saveProfile(event) {
    event.preventDefault();
    runAction(async () => {
      const emailChanged = profile.email.trim().toLowerCase() !== user.email.toLowerCase();
      const updatedUser = await apiRequest('/users/me', {
        method: 'PATCH',
        body: JSON.stringify({ name: profile.name.trim(), email: profile.email.trim(), birthDate: profile.birthDate || null }),
      });
      setUser(updatedUser);
      setProfile({ name: updatedUser.name, email: updatedUser.email, birthDate: updatedUser.birthDate || '' });
      if (emailChanged) {
        const tokens = await apiRequest('/auth/update-token', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') }),
        });
        localStorage.setItem('accessToken', tokens.accessToken);
        localStorage.setItem('refreshToken', tokens.refreshToken);
      }
      setNotice(emailChanged ? 'Dados atualizados e sessão renovada.' : 'Seus dados foram atualizados.');
    });
  }

  function changePassword(event) {
    event.preventDefault();
    if (password.newPassword !== password.confirmNewPassword) {
      setError('A confirmação não corresponde à nova senha.');
      return;
    }
    runAction(async () => {
      await apiRequest('/users/me/password', { method: 'PATCH', body: JSON.stringify(password) });
      setPassword(emptyPassword);
      setNotice('Senha alterada com sucesso.');
    });
  }

  function deleteAccount() {
    runAction(async () => {
      await apiRequest('/users/me', { method: 'DELETE' });
      logout();
    });
  }

  function cancelBooking(id) {
    runAction(async () => {
      const updated = await apiRequest(`/bookings/${id}/cancel`, { method: 'PATCH' });
      setBookings((current) => current.map((booking) => booking.id === id ? updated : booking));
      setPendingCancel(null);
      setNotice(`A reserva #${id} foi cancelada.`);
    });
  }

  function payBooking(id) {
    runAction(async () => {
      await apiRequest(`/bookings/${id}/pay`, { method: 'POST' });
      setBookings((current) => current.map((booking) => booking.id === id ? { ...booking, status: 'CONFIRMED' } : booking));
      setPendingPayment(null);
    });
  }

  function openEditBooking(booking) {
    setEditingBooking(booking);
    setEditBooking({ checkInDate: booking.checkInDate, checkOutDate: booking.checkOutDate, guestsCount: booking.guestsCount });
  }

  function updateBooking(event) {
    event.preventDefault();
    runAction(async () => {
      const updated = await apiRequest(`/bookings/${editingBooking.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ ...editBooking, guestsCount: Number(editBooking.guestsCount) }),
      });
      setBookings((current) => current.map((booking) => booking.id === updated.id ? updated : booking));
      setEditingBooking(null);
      setNotice(`A reserva #${updated.id} foi atualizada.`);
    });
  }

  const today = new Date().toISOString().slice(0, 10);
  const isHistory = (booking) => ['CANCELLED', 'COMPLETED'].includes(booking.status) || booking.checkOutDate < today;
  const visibleBookings = bookings.filter((booking) => section === 'history' ? isHistory(booking) : !isHistory(booking));
  const itemsPerPage = section === 'bookings' ? 6 : 8;
  const totalPages = Math.max(1, Math.ceil(visibleBookings.length / itemsPerPage));
  const paginatedBookings = visibleBookings.slice(page * itemsPerPage, (page + 1) * itemsPerPage);
  const titles = { bookings: 'Minhas reservas', history: 'Histórico', profile: 'Perfil e segurança' };

  return (
    <main className="account-page">
      <UserSidebar section={section} onNavigate={navigate} onLogout={logout} />
      <section className="account-content">
        <header className="account-header">
          <div><span>Minha conta</span><h1>{titles[section]}</h1></div>
          {user && <div className="user-initial" aria-hidden="true">{user.name?.charAt(0).toUpperCase()}</div>}
        </header>
        {loading ? <div className="account-state">Carregando seus dados...</div> : error && !user ? <div className="account-state error-message">{error}</div> : (
          <div className={`account-panel ${section === 'profile' ? 'profile-panel' : ''}`}>
            {notice && <div className="feedback success-message">{notice}</div>}
            {error && <div className="feedback error-message">{error}</div>}
            {section === 'profile' ? (
              <ProfileView user={user} profile={profile} setProfile={setProfile} password={password} setPassword={setPassword} saving={saving} onSaveProfile={saveProfile} onChangePassword={changePassword} onDelete={() => setConfirmDelete(true)} />
            ) : (
              <BookingsView section={section} bookings={paginatedBookings} loading={bookingsLoading} saving={saving} page={page} totalPages={totalPages} onPage={setPage} onDetails={setSelectedBooking} onEdit={openEditBooking} onPay={setPendingPayment} onCancel={setPendingCancel} />
            )}
          </div>
        )}
      </section>

      <BookingDetailsModal booking={selectedBooking} onClose={() => setSelectedBooking(null)} />
      <BookingEditModal booking={editingBooking} values={editBooking} setValues={setEditBooking} saving={saving} onSubmit={updateBooking} onClose={() => setEditingBooking(null)} />
      <PaymentModal booking={pendingPayment} saving={saving} onConfirm={payBooking} onClose={() => setPendingPayment(null)} />
      <ConfirmModal open={pendingCancel} title={pendingCancel ? `Cancelar reserva #${pendingCancel.id}?` : ''} description="O cancelamento só é permitido com mais de 72 horas de antecedência do check-in." confirmLabel="Confirmar cancelamento" busyLabel="Cancelando..." saving={saving} onConfirm={() => cancelBooking(pendingCancel.id)} onClose={() => setPendingCancel(null)} />
      <ConfirmModal open={confirmDelete} title="Excluir sua conta?" description="Seus dados serão removidos e esta ação não poderá ser desfeita." confirmLabel="Sim, excluir conta" busyLabel="Excluindo..." saving={saving} onConfirm={deleteAccount} onClose={() => setConfirmDelete(false)} />
    </main>
  );
}
