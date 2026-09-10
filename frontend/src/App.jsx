import React, { useState } from 'react';
import {Routes, Route} from 'react-router-dom'
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import Dashboard from './pages/Dashboard/Dashboard';
import Homepage from './pages/Homepage/Homepage';

function App() {
  // O estado inicial é a tela de 'login'
  const [paginaAtual, setPaginaAtual] = useState(() => {
    const token = localStorage.getItem('accessToken');
    
    return token ? 'register' : 'login';
  });

  return (
    <>
      <Routes>
        <Route path="/" element={ <Homepage/> } />
        <Route path="/register" element={ <Register/> } />
        <Route path="/login" element={ <Login/> } />
        <Route path="/dashboard" element={ <Dashboard/> } />
      </Routes>
    </>
  );
}

export default App;