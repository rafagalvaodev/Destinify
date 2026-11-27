package com.maisprati.destinify.backend.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o "tradutor de exceções em respostas HTTP" (@RestControllerAdvice).

 * Cada método do GlobalExceptionHandler recebe uma exceção e devolve um ResponseEntity com
 * o status HTTP certo e um corpo ApiError. Como são métodos Java comuns, dá para chamá-los
 * diretamente, sem subir Spring nem MockMvc — basta uma requisição de mentira
 * (MockHttpServletRequest) para o handler ler a URL (request.getRequestURI()).

 * Mapa que este teste garante:
 *   ResourceNotFoundException (e filhas: Hotel/Room/User/BookingNotFound) -> 404
 *   ForbiddenException                                                    -> 403
 *   BookingConflictException                                              -> 409
 *   BookingCancellationDeadlineException                                  -> 400

 * Os testes de controller (pasta controllers) também exercitam o handler "de ponta a ponta".
 */

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {
 private GlobalExceptionHandler handler;
 private MockHttpServletRequest request;

 @BeforeEach
 void setUp() {
  handler = new GlobalExceptionHandler();
  request = new MockHttpServletRequest("GET", "/api/teste/1");
 }

 private void assertApiError(ResponseEntity<ApiError> response, HttpStatus status, String message) {
  assertThat(response.getStatusCode()).isEqualTo(status);
  ApiError body = response.getBody();
  assertThat(body).isNotNull();
  assertThat(body.status()).isEqualTo(status.value());
  assertThat(body.error()).isEqualTo(status.getReasonPhrase());
  assertThat(body.message()).isEqualTo(message);
  assertThat(body.path()).isEqualTo("/api/teste/1");
  assertThat(body.timestamp()).isNotNull();
 }

 @Test
 @DisplayName("ResourceNotFoundException vira 404")
 void resourceNotFound_vira404() {
  ResponseEntity<ApiError> response = handler.handlerResourceNotFound(
          new ResourceNotFoundException("Algo não achado"), request);

  assertApiError(response, HttpStatus.NOT_FOUND, "Algo não achado");
 }

 @Test
 @DisplayName("exceções filhas de ResourceNotFound (ex.: HotelNotFound) também viram 404")
 void hotelNotFound_vira404() {
  ResponseEntity<ApiError> response = handler.handlerResourceNotFound(new HotelNotFoundException(3L), request);

  assertApiError(response, HttpStatus.NOT_FOUND, "Hotel with id 3 not found");
 }

 @Test
 @DisplayName("ForbiddenException vira 403")
 void forbidden_vira403() {
  ResponseEntity<ApiError> response = handler.handlerForbddenAccess(new ForbiddenException("Sem permissão"), request);

  assertApiError(response, HttpStatus.FORBIDDEN, "Sem permissão");
 }

 @Test
 @DisplayName("BookingConflictException vira 409")
 void bookingConflict_vira409() {
  ResponseEntity<ApiError> response = handler.handlerBookingConflict(new BookingConflictException("Conflito"), request);

  assertApiError(response, HttpStatus.CONFLICT, "Conflito");
 }

 @Test
 @DisplayName("BookingCancellationDeadlineException vira 400 com a mensagem padrão de 72h")
 void cancellationDeadline_vira400() {
  ResponseEntity<ApiError> response = handler.handleCancellationDeadline(
          new BookingCancellationDeadlineException(), request);

  assertApiError(response, HttpStatus.BAD_REQUEST, "O cancelamento exige mais de 72 horas de antecedência");
 }
}