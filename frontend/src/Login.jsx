import React, { useState } from 'react';
import './Login.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    console.log('Dados de login:', { email, senha });
    // Lógica de conexão com o Java
  };

  return (
    <div className="bg-container">
      <div className="login-card">
        
        {/* Lado Esquerdo - Formulário */}
        <div className="form-section">
          <div className="form-wrapper">
            <h1 className="logo">Destinify</h1>
            
            <h2 className="title">Login</h2>

            <form onSubmit={handleSubmit} className="login-form">
              <div className="input-group">
                <label htmlFor="email">Email</label>
                <input
                  type="email"
                  id="email"
                  placeholder="exemplo@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="input-group">
                <label htmlFor="senha">Senha</label>
                <input
                  type="password"
                  id="senha"
                  placeholder="Insira sua senha"
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  required
                />
              </div>

              <button type="submit" className="btn-primary">
                Log In
              </button>

              {/* Coloquei o Esqueceu a Senha DENTRO do form para forçar ficar embaixo */}
              <div className="forgot-wrapper">
                <a href="#forgot" className="link-forgot">Esqueceu a senha?</a>
              </div>
            </form>

            <div className="divider">ou</div>

            <button className="btn-google" type="button">
              {/* Imagem direta da web */}
              <img 
                src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" 
                alt="Logo do Google" 
                className="google-icon" 
              />
              Continue com o Google
            </button>

            <p className="signup-text">
              Ainda não tem conta? <a href="#register">Registre-se aqui</a>
            </p>
          </div>
        </div>

        {/* Lado Direito - Imagem da moça no trem */}
        <div className="image-section"></div>

      </div>
    </div>
  );
}