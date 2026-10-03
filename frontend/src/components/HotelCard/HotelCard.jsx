import Rating from '@mui/material/Rating'
import './HotelCard.css'
import '../../css/main.css'

export default function HotelCard({imgURL, URL, ratio, name, location, price})
{

    return (
        <article className='card'>
            <img className='card__img' src={imgURL} alt={name}/>

            <section className='card__body'>
                <div className='card__ratio'>
                    <p>Hotel</p>

                    <div className='card__stars'>
                        <Rating name='size-small-half-read' defaultValue={ratio} precision={0.5} size='small' readOnly/>
                    </div>
                </div>

                <p className='card__name'>{name}</p>
                <p className='card__location'>{location}</p>

                <p className='card__price'>A partir de <span>R$ {price.toFixed(2)}</span></p>
            </section>
            
        </article>
    );
}