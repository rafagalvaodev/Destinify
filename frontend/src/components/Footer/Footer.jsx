import './Footer.css'

export default function Footer() {
    return(
        <footer className='footer'>
            <section className='footer__info'>
                <div className="footer__links footer__left">
                    <a href="/">Saiba mais</a>
                    <a href="/">Afilie-se</a>
                    <a href="/">Suporte</a>
                    <a href="/">Trabalhe conosco</a>
                    <a href="/">Política de privacidade</a>
                </div>

                <div className="footer__links footer__center">
                    <a href="/">Home</a>
                    <a href="/">Hotéis</a>
                    <a href="/">Destinos</a>
                    <a href="/">Anuncie aqui</a>
                </div>

                <div className="footer__links footer__right">
                    <a href="/">Instagram</a>
                    <a href="/">Facebook</a>
                    <a href="/">Twitter</a>
                    <a href="/">Blog</a>
                </div>
            </section>

            <p>Destinify - Todos os direitos reservados © - 2026</p>
        </footer>
    );
}