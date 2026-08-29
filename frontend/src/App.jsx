import React, { useState } from 'react';
import Login from './Login';
import Register from './Register';

function App() {
  // O estado inicial é a tela de 'login'
  const [paginaAtual, setPaginaAtual] = useState('login');

  return (
    <div>
      {/* Exibe o Login ou o Registro dependendo do estado atual */}
      {paginaAtual === 'login' ? (
        <Login setPaginaAtual={setPaginaAtual} />
      ) : (
        <Register setPaginaAtual={setPaginaAtual} />
      )}
    </div>
  );
}

export default App;