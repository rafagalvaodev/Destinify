package com.maisprati.destinify.backend.domain;

import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE_TIME;

/**
 * Testa o método de ciclo de vida @PrePersist de entidade Booking.
 * O @PrePersist é chamado pelo Hibernate logo antes do INSERT. Aqui NÃO temos banco,
 * então chamamos prePersist() "na mão" para provar a regra: toda reserva nasce com
 * data de criação e, se ninguém definir o status, ele vira PENDING.
 */

@DisplayName("Booking (entidade)")
class BookingTest {

    @Test
    @DisplayName("prePersist define createdAt e status PENDING quando o status é nulo")
    void prePersist_semStatus_defineCreatedAtEPending() {
        // Arrange
        Booking booking = new Booking(); // status == null
        LocalDateTime antes = LocalDateTime.now();

        // Act
        booking.prePersist();

        // Assert
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        // "ifAfterOrEqualTo(antes)": createdAt foi gerado agora, nunca antes do início do teste
        assertThat(booking.getCreatedAt()).isAfterOrEqualTo(antes);
    }

    @Test
    @DisplayName("prePersist não sobrescreve um status já definido")
    void prePersist_comStatus_mantemOStatus() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);

        booking.prePersist();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("equals/hashCode comparam só o id")
    void equals_comparaPeloId() {
        Booking a = new Booking();
        a.setId(1L);
        Booking b = new Booking();
        b.setId(1L);
        Booking c = new Booking();
        c.setId(2L);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}