package com.maisprati.destinify.backend.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerGatewayTest {

    @Test
    void paymentGatewayException_vira502() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/booking/7/checkout");

        ResponseEntity<ApiError> response = handler.handlePaymentGateway(
                new PaymentGatewayException("Falha no PagBank", new RuntimeException("causa")), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(502);
        assertThat(body.error()).isEqualTo("Bad Gateway");
        assertThat(body.message()).isEqualTo("Falha no PagBank");
        assertThat(body.path()).isEqualTo("/api/booking/7/checkout");
    }
}
