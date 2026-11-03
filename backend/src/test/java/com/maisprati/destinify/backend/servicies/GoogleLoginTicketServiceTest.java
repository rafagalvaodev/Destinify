package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.dto.LoginDTO.TokenResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GoogleLoginTicketService")
class GoogleLoginTicketServiceTest {

    private GoogleLoginTicketService service;
    private TokenResponseDTO tokens;

    @BeforeEach
    void setUp() {
        service = new GoogleLoginTicketService();
        tokens = new TokenResponseDTO("access-token", "refresh-token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map.Entry<Instant, TokenResponseDTO>> ticketsInternos() {
        return (Map<String, Map.Entry<Instant, TokenResponseDTO>>) ReflectionTestUtils.getField(service, "tickets");
    }

    @Test
    @DisplayName("create gera um ticket não vazio e read devolve os tokens guardados")
    void createERead_devolveOsTokens() {
        String ticket = service.create(tokens);

        assertThat(ticket).isNotBlank();
        assertThat(service.read(ticket)).isSameAs(tokens); // isSameAs = mesma instância (não só igual)
    }

    @Test
    @DisplayName("cada chamada de create gera um ticket diferente")
    void create_geraTicketsUnicos() {
        String primeiro = service.create(tokens);
        String segundo = service.create(tokens);

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("o ticket só pode ser usado uma vez")
    void read_segundaLeituraFalha() {
        String ticket = service.create(tokens);
        service.read(ticket); // primeira leitura consome o ticket

        assertThatThrownBy(() -> service.read(ticket))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException e = (ResponseStatusException) ex;
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getReason()).isEqualTo("Login has expired");
                });
    }

    @Test
    @DisplayName("ticket inexistente é rejeitado com 400")
    void read_ticketInexistente_falha() {
        assertThatThrownBy(() -> service.read("ticket-que-nao-existe"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Login has expired");
    }

    @Test
    @DisplayName("ticket vencido (mais de 60s) é rejeitado")
    void read_ticketVencido_falha() {
        // Arrange: inserimos "na marra" um ticket que venceu 1 segundo atrás
        ticketsInternos().put("vencido", Map.entry(Instant.now().minusSeconds(1), tokens));

        // Act + Assert
        assertThatThrownBy(() -> service.read("vencido"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Login has expired");
    }

    @Test
    @DisplayName("create limpa os tickets vencidos que ficaram na memória")
    void create_removeTicketsVencidos() {
        ticketsInternos().put("vencido", Map.entry(Instant.now().minusSeconds(5), tokens));

        service.create(tokens); // a limpeza acontece no início do create

        assertThat(ticketsInternos()).doesNotContainKey("vencido");
    }
}