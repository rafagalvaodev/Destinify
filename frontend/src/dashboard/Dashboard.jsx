import React, { useState } from 'react';
import fotinear from '../../imagem/fotinear.gif';
import './Dashboard.css';

export default function Dashboard({ setPaginaAtual }) {


  return (
    <div className="minitainer">
      <img src={ fotinear } alt="" />
    </div>
  );
}