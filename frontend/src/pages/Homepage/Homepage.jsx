import React, { useState } from 'react';
import './Homepage.css'
import '../../css/main.css'
import HotelCard from '../../components/HotelCard/HotelCard'
import Footer from '../../components/Footer/Footer'

export default function Homepage() {

    // Variáveis para serem usadas no mecanismo de pesquisa
    const [destination, setDestination] = useState('')
    const [arrival, setArrival] = useState('')
    const [departure, setDeparture] = useState('')
    const [adults, setAdults] = useState(0)
    const [childs, setChilds] = useState(0)
    const [rooms, setRooms] = useState(0)

    return(
        <>
            <header className="header">
                {/* Container supeiror do header */}
                <div className="header__upper">
                    <h3 className="header__brand">
                        Destinify
                    </h3>

                    <nav className="header__navigator">
                        <div className="header__accordion">
                            <img className="header__flag" src="../../../imagem/flag_brazil.png" alt="brazil" />
                            <button>BRL</button>
                        </div>
                        <p className="header__link"><a>Anuncie sua propriedadade</a></p>
                        <p className="header__button">Cadastre-se</p>
                        <p className="header__button">Login</p>
                    </nav>
                </div>

                {/* Título do meio do header */}
                <section className="header__title">
                    <h1>Encontre o lugar perfeito para sua estadia!</h1>
                    <h2>Reserve já o seu lugar ideal.</h2>
                </section>

                {/* Formulário do mecanismo de pesquisa do header */}
                <form className="header__search">
                    <div className='header__input'>
                        <label for="destination-input">Escolha seu destino</label>
                        <input type="text" name="destination" id="destination-input" placeholder='Para onde planeja ir?'/>
                    </div>

                    <div className='header__input'>
                        <label for="arrival-input">Selecione a data</label>
                        <div className='header__date'>
                            <input type="date" name="arrival-date" id="arrival-input"/>
                            <span>—</span>
                            <input type="date" name="departure-date" id="departure-input"/>
                        </div>
                    </div>

                    <div className="header__input">
                        <label for="people-button">Selecione a ocupação</label>
                        <input type="text" name="people" id ="people-button" placeholder='Ocupação'/>
                    </div>

                    <button type='submit'>Pesquisar</button>
                </form>
            </header>
            
            <main className='main'>

                <p className='main__subtitle'>Conheça seu próximo destino!</p>

                <section className='card__layout'>
                    <HotelCard imgURL='../../../imagem/imagem_teste.png' URL='/' ratio={[1,1,1,0.5,0]} name='Hotel Teste' location="Teste - Teste" price={100}></HotelCard>
                    <HotelCard imgURL='../../../imagem/imagem_teste.png' URL='/' ratio={[1,1,1,0.5,0]} name='Hotel Teste' location="Teste - Teste" price={100}></HotelCard>
                    <HotelCard imgURL='../../../imagem/imagem_teste.png' URL='/' ratio={[1,1,1,0.5,0]} name='Hotel Teste' location="Teste - Teste" price={100}></HotelCard>
                    <HotelCard imgURL='../../../imagem/imagem_teste.png' URL='/' ratio={[1,1,1,0.5,0]} name='Hotel Teste' location="Teste - Teste" price={100}></HotelCard>
                </section>
            </main>

            <Footer />
        </>
    );
} 