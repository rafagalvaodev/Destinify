import { useEffect, useState } from 'react';
import { apiRequest } from './api/userApi';

const statusMessages = {
  PENDING: 'A reserva está pendente. Se você já pagou, aguarde a confirmação do PagBank.',
  CONFIRMED: 'Pagamento confirmado. Sua reserva está confirmada.',
  CANCELLED: 'Esta reserva foi cancelada.',
  COMPLETED: 'Esta reserva já foi concluída.',
};

export default function PaymentReturn({ bookingId, onLogin, onGoToBookings, onGoHome }) {
  const validId = /^[1-9]\d{0,18}$/.test(bookingId ?? '');
  const hasSession = Boolean(localStorage.getItem('accessToken'));
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(validId && hasSession);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!validId || !hasSession) return;

    const controller = new AbortController();
    let timer;

    async function checkBooking(remainingChecks) {
      try {
        // verifica o dono e devolve o status gravado pelo backend depois do webhook.
        const booking = await apiRequest(`/bookings/${bookingId}`, {
          cache: 'no-store',
          signal: controller.signal,
        });
        if (controller.signal.aborted) return;

        setStatus(booking.status);
        // O navegador pode voltar primeiro. Repetimos por até 20 segundos
        // enquanto a reserva estiver pendente. Depois o usuário pode consultar de novo.
        if (booking.status === 'PENDING' && remainingChecks > 0) {
          timer = window.setTimeout(() => checkBooking(remainingChecks - 1), 4000);
        }
      } catch (requestError) {
        if (!controller.signal.aborted) setError(requestError);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    checkBooking(5);
    return () => {
      controller.abort();
      window.clearTimeout(timer);
    };
  }, [bookingId, validId, hasSession, attempt]);

  function checkAgain() {
    setError(null);
    setLoading(true);
    setAttempt((current) => current + 1);
  }

  const loginRequired = !hasSession || error?.status === 401 || error?.status === 403;

  return (
    <main className="account-state">
      <h1>Retorno do PagBank</h1>
      {!validId && <p>O link de retorno está sem um número de reserva válido.</p>}
      {validId && loginRequired && <p>Entre com a conta que fez a reserva para consultar o pagamento.</p>}
      {validId && !loginRequired && (
        <p role="status">
          {loading ? 'Consultando a reserva…' : error
            ? 'Não foi possível consultar a reserva.'
            : statusMessages[status] || 'A situação da reserva está indisponível.'}
        </p>
      )}
      <div className="modal-actions">
        {validId && loginRequired && <button type="button" onClick={onLogin}>Entrar</button>}
        {validId && !loginRequired && (
          <>
            {(status === 'PENDING' || error) && (
              <button type="button" onClick={checkAgain} disabled={loading}>Consultar novamente</button>
            )}
            <button type="button" onClick={onGoToBookings}>Minhas reservas</button>
          </>
        )}
        <button type="button" onClick={onGoHome}>Voltar ao início</button>
      </div>
    </main>
  );
}
