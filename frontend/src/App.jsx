import React, { useState } from 'react';
import Login from './Login';
import Register from './Register';
import Dashboard from './dashboard/Dashboard';
import Admin from './admin/Admin';

function App() {
  const googleExchange = React.useRef(null);

  // O estado inicial é a tela de 'login'
  const [paginaAtual, setPaginaAtual] = useState(() => {
    const token = localStorage.getItem('accessToken');
    const ticket = new URLSearchParams(window.location.hash.slice(1)).get('googleTicket');
    
    return token || ticket ? 'checking' : 'login';
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
          if (active) setPaginaAtual('admin');
          return;
        }
        if (adminResponse.status === 403) {
          const userResponse = await fetch('http://localhost:8080/api/users/me', { headers });
          if (userResponse.ok) {
            if (active) setPaginaAtual('dashboard');
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
    if (paginaAtual !== 'dashboard') return;
    let active = true;
    fetch('http://localhost:8080/api/users/all-users', {
      headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
    }).then((response) => {
      if (active && response.ok) setPaginaAtual('admin');
    }).catch(() => {});
    return () => { active = false; };
  }, [paginaAtual]);

  return (
    <div>
      {/* Exibe o Login ou o Registro dependendo do estado atual */}
      {paginaAtual === 'login' && (
        <Login setPaginaAtual={setPaginaAtual} />
      )}

      {paginaAtual === 'register' && (
        <Register setPaginaAtual={setPaginaAtual} />
      )}

      {paginaAtual === 'dashboard' && (
        <Dashboard setPaginaAtual={setPaginaAtual} />
      )}
      {paginaAtual === 'admin' && <Admin setPaginaAtual={setPaginaAtual} />}
    </div>
  );
}

export default App;
