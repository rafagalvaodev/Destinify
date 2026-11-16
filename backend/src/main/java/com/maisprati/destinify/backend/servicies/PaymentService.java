// CAMINHO: backend/src/main/java/com/maisprati/destinify/backend/servicies/PaymentService.java
package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.CheckoutResponse;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.exceptions.PaymentGatewayException;
import com.maisprati.destinify.backend.exceptions.ResourceNotFoundException;
import com.maisprati.destinify.backend.integrations.pagbank.CheckoutCriado;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankCheckoutRequestFactory;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import com.maisprati.destinify.backend.utils.mappers.PaymentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // Fuso usado para validade do checkout (o PagBank espera data com fuso). Público para os testes usarem.
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PagBankClient pagBankClient;

    @Autowired
    private PagBankCheckoutRequestFactory pagBankCheckoutRequestFactory;

    // Quanto tempo o checkout vale. Padrão 30 min (o do PagBank seria 2h; a reserva PENDING
    // segura o quarto, então um prazo menor libera o quarto mais rápido).
    @Value("${pagbank.checkout-expiration-minutes:30}")
    private long checkoutExpirationMinutes;

    @Transactional
    public PaymentResponse createPayment(Long bookingId, User loggedUser) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUser().getId().equals(loggedUser.getId())) {
            throw new ForbiddenException("This reservation does not belong to this user");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Only pending reservations can receive payment");
        }

        return paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(
                        bookingId, PaymentStatus.PENDING)
                .map(paymentMapper::paymentResponseMapper)
                .orElseGet(() -> {
                    Payment payment = new Payment();
                    payment.setBooking(booking);
                    payment.setAmount(booking.getTotalPrice()
                            .setScale(2, RoundingMode.HALF_UP));
                    payment.setPaymentStatus(PaymentStatus.PENDING);
                    payment.setCreatedAt(LocalDateTime.now());

                    return paymentMapper.paymentResponseMapper(paymentRepository.save(payment));
                });
    }

    @Transactional
    public PaymentResponse processPayment(
            Long bookingId,
            Long paymentId,
            User loggedUser,
            PaymentStatus result) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUser().getId().equals(loggedUser.getId())) {
            throw new ForbiddenException("This reservation does not belong to this user");
        }

        Payment payment = paymentRepository
                .findByIdAndBookingId(paymentId, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found"));

        if (booking.getStatus() != BookingStatus.PENDING
                || payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BookingConflictException("Payment is not pending");
        }

        if (result != PaymentStatus.APPROVED
                && result != PaymentStatus.REJECTED) {
            throw new BookingConflictException("Invalid result");
        }

        payment.setPaymentStatus(result);
        payment.setProcessedAt(LocalDateTime.now());

        if (result == PaymentStatus.APPROVED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        return paymentMapper.paymentResponseMapper(
                paymentRepository.save(payment));
    }


    @Transactional
    public void updatePendingAmount(Booking booking) {
        Payment payment = paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending payment not found"));
        payment.setAmount(booking.getTotalPrice()
                .setScale(2, RoundingMode.HALF_UP));

        // O valor mudou: o checkout antigo ficou com o valor desatualizado. Limpamos os dados dele
        // para que o próximo "pagar" crie um checkout novo, com o valor certo.
        payment.setProviderCheckoutId(null);
        payment.setCheckoutUrl(null);
        payment.setExpiresAt(null);

        paymentRepository.save(payment);
    }

    @Transactional
    public CheckoutResponse startCheckout(Long bookingId, User loggedUser) {
        PaymentResponse pending = createPayment(bookingId, loggedUser);

        Payment payment = paymentRepository.findById(pending.id())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        ZonedDateTime agora = ZonedDateTime.now(ZONA);

        if (checkoutVigente(payment, agora)) {
            return toCheckoutResponse(payment);
        }

        ZonedDateTime expiracao = agora.plusMinutes(checkoutExpirationMinutes)
                .truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> corpo = pagBankCheckoutRequestFactory.montar(payment, payment.getBooking(), expiracao);

        CheckoutCriado checkout;
        try {
            checkout = CheckoutCriado.from(pagBankClient.criarCheckout(corpo));
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("PagBank recusou checkout da reserva {}: HTTP {}", bookingId, status);
            String mensagem = switch (status) {
                case 401 -> "PagBank recusou a autenticação. Verifique o token e o ambiente configurados no servidor.";
                case 403 -> "PagBank negou acesso ao checkout. Verifique as permissões da conta e do token.";
                case 400, 422 -> "PagBank recusou os dados do checkout. Verifique os dados e as URLs configuradas no servidor.";
                case 429 -> "Limite de requisições do PagBank atingido. Tente novamente em alguns instantes.";
                default -> "Não foi possível criar o checkout no PagBank (HTTP " + status + ").";
            };
            throw new PaymentGatewayException(mensagem, ex);
        } catch (RestClientException | IllegalStateException ex) {
            log.error("Falha no checkout PagBank da reserva {}: {}", bookingId, ex.getClass().getSimpleName());
            // RestClientException = erro HTTP/rede (401, 400, 500, timeout ...).
            // IllegalStateException = resposta sem id/link (CheckoutCriado.from).
            throw new PaymentGatewayException("Não foi possível criar o checkout no PagBank", ex);
        }

        payment.setProviderCheckoutId(checkout.id());
        payment.setCheckoutUrl(checkout.payUrl());
        payment.setExpiresAt(expiracao.toLocalDateTime());
        paymentRepository.save(payment);

        return toCheckoutResponse(payment);
    }

    private boolean checkoutVigente(Payment payment, ZonedDateTime agora) {
        return payment.getProviderCheckoutId() != null
                && payment.getCheckoutUrl() != null
                && payment.getExpiresAt() != null
                && payment.getExpiresAt().isAfter(agora.toLocalDateTime());
    }

    private CheckoutResponse toCheckoutResponse(Payment payment) {
        return new CheckoutResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getCheckoutUrl(),
                payment.getExpiresAt());
    }

}
