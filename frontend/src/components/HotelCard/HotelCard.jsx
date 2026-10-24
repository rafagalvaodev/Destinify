import Rating from '@mui/material/Rating';
import './HotelCard.css';
import defaultHotelImage from '../../imagem/imagem_teste.png';

export default function HotelCard({ hotel, onSelect }) {
  return (
    <article className="card hotel-card">
      <div className="catalog-image">
        <img key={hotel.imageUrl} className="card__img" src={hotel.imageUrl || defaultHotelImage}
          alt={hotel.imageUrl ? hotel.name : 'Imagem ilustrativa de hospedagem'} loading="lazy"
          onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = defaultHotelImage; }} />
      </div>
      <section className="card__body">
        <div className="card__ratio"><p>Hotel</p>
          <Rating value={Number(hotel.stars) || 0} precision={0.1} size="small" readOnly />
        </div>
        <h3 className="card__name">{hotel.name}</h3>
        <p className="card__location">{hotel.city}</p>
        <button className="hotel-card__open" type="button" aria-label={`Ver quartos de ${hotel.name}`} onClick={() => onSelect(hotel)} />
      </section>
    </article>
  );
}
