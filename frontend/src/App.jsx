import React, { useState } from 'react';
import './css/main.css';
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import UserPanel from './user/UserPanel';
import Admin from './admin/Admin';
import Homepage from './pages/Homepage/Homepage';

function App() {
  const [paymentBooking, setPaymentBooking] = useState(null);
  const googleExchange = React.useRef(null);

  // Abre o catálogo público e processa retornos do Google quando necessário.
  const [paginaAtual, setPaginaAtual] = useState(() => {
    const ticket = new URLSearchParams(window.location.hash.slice(1)).get('googleTicket');
    
    return ticket ? 'checking' : 'home';
  });

  React.useEffect(() => {
    if (paginaAtual !== 'checking') return;
    let active = true;

    async function checkSession() {
      try {
        let token = localStorage.getItem('accessToken');
        const ticket = new URLSearchParams(window.location.hash.slice(1)).get('googleTicket');

        if (ticket) {
          if (!googleExchange.current) {
            googleExchange.current = fetch('http://localhost:8080/api/auth/login/google/exchange', {
              method: 'POST',
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
              body: new URLSearchParams({ ticket }),
            }).then(async (response) => {
              if (!response.ok) throw new Error('Login com Google expirou');
              return response.json();
            });
          }

          const tokens = await googleExchange.current;
          if (!active) return;

          localStorage.setItem('accessToken', tokens.accessToken);
          localStorage.setItem('refreshToken', tokens.refreshToken);
          window.history.replaceState(null, '', window.location.pathname + window.location.search);
          token = tokens.accessToken;
        }

        if (!token) throw new Error('Sessão ausente');
        const headers = { Authorization: `Bearer ${token}` };
        const adminResponse = await fetch('http://localhost:8080/api/users/all-users', { headers });
        if (adminResponse.ok) {
          if (active) setPaginaAtual('home');
          return;
        }
        if (adminResponse.status === 403) {
          const userResponse = await fetch('http://localhost:8080/api/users/me', { headers });
          if (userResponse.ok) {
            if (active) setPaginaAtual('home');
            return;
          }
        }
      } catch (error) {
        if (!active) return;
        console.error('Erro ao verificar sessão:', error);
        if (window.location.hash.includes('googleTicket=')) {
          window.history.replaceState(null, '', window.location.pathname + window.location.search);
        }
      }
      if (active) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        setPaginaAtual('login');
      }
    }

    checkSession();
    return () => { active = false; };
  }, [paginaAtual]);

  React.useEffect(() => {
    if (paginaAtual !== 'userPanel' || paymentBooking) return;
    let active = true;
    fetch('http://localhost:8080/api/users/all-users', {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
    }).then((response) => {
      if (active && response.ok) setPaginaAtual('admin');
    }).catch(() => {});
    return () => { active = false; };
  }, [paginaAtual, paymentBooking]);

  function leaveAccount(page) {
    setPaymentBooking(null);
    setPaginaAtual(page);
  }

  function openBookingPayment(booking) {
    setPaymentBooking(booking);
    setPaginaAtual('userPanel');
    window.scrollTo({ top: 0 });
  }

  return (
    <div>
      {paginaAtual === 'checking' && <p role="status">Concluindo login…</p>}
      {paginaAtual === 'home' && (
        <Homepage setPaginaAtual={setPaginaAtual} onBookingCreated={openBookingPayment} />
      )}
      
      {paginaAtual === 'login' && (
        <Login setPaginaAtual={setPaginaAtual} />
      )}

      {paginaAtual === 'register' && (
        <Register setPaginaAtual={setPaginaAtual} />
      )}

      {paginaAtual === 'userPanel' && (
        <UserPanel setPaginaAtual={leaveAccount} paymentBooking={paymentBooking} onPaymentClosed={() => setPaymentBooking(null)} />
      )}
      {paginaAtual === 'admin' && <Admin setPaginaAtual={setPaginaAtual} />}
    </div>
  );
}

export default App;
