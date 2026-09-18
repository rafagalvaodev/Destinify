import './HotelCard.css'
import '../../css/main.css'

export default function HotelCard({imgURL, URL, ratio, name, location, price})
{
    let stars = 0;

    return (
        <article className='card'>
            <img className='card__img' src={imgURL} alt={name}/>

            <section className='card__body'>
                <div className='card__ratio'>
                    <p>Hotel</p>

                    <div className='card__stars'>
                        {ratio.map((star) => {
                            stars++;
                            if(star === 0)
                                return <img key={stars} src='../../imagem/blank_star.png' alt="sem estrela"/>
                            else if(star > 0 && star < 1)
                                return <img key={stars} src='../../imagem/half_star.png' alt="meia estrela"/>
                            else if(star === 1)
                                return <img key={stars} src='../../imagem/star.png' alt ="estrela"/>
                        })}
                    </div>
                </div>

                <p className='card__name'>{name}</p>
                <p className='card__location'>{location}</p>

                <p className='card__price'>A partir de <span>R$ {price}</span></p>
            </section>
            
        </article>
    );
}