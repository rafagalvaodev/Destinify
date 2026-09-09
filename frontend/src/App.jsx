import React, { useState } from 'react';
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import Dashboard from './pages/Dashboard/Dashboard';

function App() {
  // O estado inicial é a tela de 'login'
  const [paginaAtual, setPaginaAtual] = useState(() => {
    const token = localStorage.getItem('accessToken');
    
    return token ? 'register' : 'login';
  });

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
    </div>
  );
}

export default App;