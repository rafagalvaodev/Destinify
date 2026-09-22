package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.TokenResponseDTO;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.servicies.GoogleLoginTicketService;
import com.maisprati.destinify.backend.servicies.LoginGoogleService;
import com.maisprati.destinify.backend.servicies.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.ServerSocket;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/login")
public class GoogleAuthController {


    @Autowired
    private LoginGoogleService loginGoogleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleLoginTicketService googleLoginTicketService;


    @GetMapping
    public ResponseEntity<Void> redirecionarGoogle(){
        String url = loginGoogleService.gerarUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/google/autorizado")
    public ResponseEntity<Void> autenticarUsuarioOAuth(@RequestParam String code) {
        String email = loginGoogleService.obterEmail(code);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(email.substring(0, email.indexOf('@')));
                        newUser.setRole(Role.CLIENT);

                        String randomPassword = UUID.randomUUID().toString();
                        newUser.setPassword(passwordEncoder.encode(randomPassword));
                        return userRepository.save(newUser);
                });
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String tokenAcesso = tokenService.generatToken(user);
        String refreshToken = tokenService.generatRefreshToken(user);

        String ticket = googleLoginTicketService.create(new TokenResponseDTO(tokenAcesso, refreshToken));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("http://localhost:5173/#googleTicket=" + ticket)).build();
    }

    @PostMapping("/google/exchange")
    public ResponseEntity<TokenResponseDTO> trocarTicket(@RequestParam String ticket) {
        return ResponseEntity.ok(googleLoginTicketService.read(ticket));
    }
}
