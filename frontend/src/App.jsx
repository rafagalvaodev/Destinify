import React, { useState } from 'react';
import Login from './Login';
import Register from './Register';
import Dashboard from './dashboard/Dashboard';

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