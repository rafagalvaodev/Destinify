import React, { useState } from 'react';
import './Register.css';

export default function Register({ setPaginaAtual }) {
  // Controle de Etapas (1 ou 2)
  const [etapa, setEtapa] = useState(1);
  const [erro, setErro] = useState('');

  // Estados da Etapa 1
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [repetirSenha, setRepetirSenha] = useState('');

  // Estados da Etapa 2
  const [nome, setNome] = useState('');
  const [dataNascimento, setDataNascimento] = useState('');
  const [cpf, setCpf] = useState('');
  // Máscara de CPF: 000.000.000-00
  const handleCpfChange = (e) => {
    let valor = e.target.value.replace(/\D/g, ''); // Remove tudo o que não for número
    if (valor.length > 11) valor = valor.slice(0, 11); // Limita a 11 números
    
    valor = valor.replace(/(\d{3})(\d)/, '$1.$2');
    valor = valor.replace(/(\d{3})(\d)/, '$1.$2');
    valor = valor.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    setCpf(valor);
  };
  const [telefone, setTelefone] = useState('');
  // Máscara de Telefone: (00) 00000-0000
  const handleTelefoneChange = (e) => {
    let valor = e.target.value.replace(/\D/g, '');
    if (valor.length > 11) valor = valor.slice(0, 11);

    valor = valor.replace(/^(\d{2})(\d)/g, '($1) $2');
    valor = valor.replace(/(\d)(\d{4})$/, '$1-$2');
    setTelefone(valor);
  };
  const [termos, setTermos] = useState(false);

  // Avança para a próxima etapa
  const handleProximaEtapa = (e) => {
    e.preventDefault(); // Impede o recarregamento da página
    setErro('');

    if (senha !== repetirSenha) {
      setErro('As senhas não coincidem. Tente novamente.');
      return;
    }
    
    // Se estiver tudo certo, vai para a etapa 2
    setEtapa(2);
  };

  // Envia todos os dados para o Backend
  const handleFinalizarRegistro = async (e) => {
    e.preventDefault();
    setErro('');

    if (!termos) {
      setErro('Você precisa concordar com os Termos e Política de Privacidade.');
      return;
    }

    const dadosUsuario = {
      email,
      senha,
      nome,
      dataNascimento,
      cpf,
      telefone
    };

    console.log('Dados completos prontos para o Java:', dadosUsuario);
    // Aqui entrará o fetch() para enviar ao backend
  };

  return (
    <div className="bg-container">
      <div className="register-card">
        
        {/* Lado Esquerdo - Imagem da Montanha */}
        <div className="image-section register-image"></div>

        {/* Lado Direito - Formulário Dinâmico */}
        <div className="form-section">
          <div className="form-wrapper">
            
{/* Renderização Condicional: ETAPA 1 */}
            {etapa === 1 && (
              <>
                <h1 className="logo">Destinify</h1>
                <h2 className="title">Registro</h2>

                {/* A mensagem de erro acima dos inputs */}
                {erro && <p className="error-message">{erro}</p>}

                <form onSubmit={handleProximaEtapa} className="auth-form">
                  <div className="input-group">
                    <label htmlFor="email">Email</label>
                    <input 
                      type="email" 
                      id="email" 
                      placeholder="exemplo@email.com" 
                      value={email} 
                      onChange={(e) => { setEmail(e.target.value); setErro(''); }} 
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
                      onChange={(e) => { setSenha(e.target.value); setErro(''); }} 
                      required 
                    />
                  </div>

                  <div className="input-group">
                    <label htmlFor="repetirSenha">Repetir senha</label>
                    <input 
                      type="password" 
                      id="repetirSenha" 
                      placeholder="Insira sua senha novamente" 
                      value={repetirSenha} 
                      onChange={(e) => { setRepetirSenha(e.target.value); setErro(''); }} 
                      required 
                    />
                  </div>

                  <button type="submit" className="btn-primary">
                    Registrar
                  </button>
                </form>

                <div className="divider">ou</div>

                <button className="btn-google" type="button">
                  <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Logo do Google" className="google-icon" />
                  Continue com o Google
                </button>

                <p className="signup-text">
                  Já tem conta?{' '}
                  <a href="#login" onClick={(e) => { e.preventDefault(); setPaginaAtual('login'); }}>
                    Faça login
                  </a>
                </p>
              </>
            )}

            {/* Renderização Condicional: ETAPA 2 */}
            {etapa === 2 && (
              <>
                <h1 className="logo title-step2">Informações pessoais</h1>

                <form onSubmit={handleFinalizarRegistro} className="auth-form">
                  <div className="input-group step2-group">
                    <label htmlFor="nome">Nome completo</label>
                    <input type="text" id="nome" placeholder="Insira seu nome completo" value={nome} onChange={(e) => setNome(e.target.value)} required />
                  </div>

                  <div className="input-group step2-group">
                    <label htmlFor="dataNascimento">Data de nascimento</label>
                    <input type="text" id="dataNascimento" placeholder="00/00/0000" value={dataNascimento} onChange={(e) => setDataNascimento(e.target.value)} required />
                  </div>

                  <div className="input-group step2-group">
                    <label htmlFor="cpf">CPF</label>
                    <input type="text" id="cpf" placeholder="000.000.000-00" value={cpf} onChange={handleCpfChange} required />
                  </div>

                  <div className="input-group step2-group">
                    <label htmlFor="telefone">Telefone (Opcional)</label>
                    <input type="text" id="telefone" placeholder="(DDD) 00000-0000" value={telefone} onChange={handleTelefoneChange} />
                  </div>

                  <div className="checkbox-group">
                    <input type="checkbox" id="termos" checked={termos} onChange={(e) => setTermos(e.target.checked)} />
                    <label htmlFor="termos">Li e concordo com os Termos de Serviço e a Política de Privacidade.</label>
                  </div>

                  <button type="submit" className="btn-primary">
                    Finalizar registro
                  </button>
                  
                  {/* Botão opcional para voltar caso o usuário queira corrigir o email */}
                  <button type="button" className="btn-back" onClick={() => setEtapa(1)}>
                    Voltar
                  </button>
                </form>
              </>
            )}

          </div>
        </div>
      </div>
    </div>
  );
}