package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.dto.LoginDTO.TokenResponseDTO;
import jakarta.persistence.ManyToOne;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleLoginTicketService {
    private final Map<String, Map.Entry<Instant, TokenResponseDTO>> tickets = new ConcurrentHashMap<>();

    public String create(TokenResponseDTO tokenResponseDTO) {
        tickets.entrySet().removeIf(e -> e.getValue().getKey().isBefore(Instant.now()));
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, Map.entry(Instant.now().plusSeconds(60), tokenResponseDTO));
        return ticket;
    }

    public TokenResponseDTO read(String ticket) {
        var entrada = tickets.remove(ticket);
        if (entrada == null || entrada.getKey().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login has expired");
        }
        return entrada.getValue();
    }
}
