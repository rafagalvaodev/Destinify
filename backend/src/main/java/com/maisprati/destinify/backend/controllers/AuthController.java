package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.LoginData;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.RefreshToken;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.TokenResponseDTO;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.servicies.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginData userData) throws Exception {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                userData.email(),
                userData.password());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        User user = (User) authentication.getPrincipal();
        String accessToken = tokenService.generatToken(user);
        String refreshToken = tokenService.generatRefreshToken(user);

        return ResponseEntity.ok(new TokenResponseDTO(accessToken, refreshToken));
    }

    @PostMapping("/update-token")
    public ResponseEntity<TokenResponseDTO> updateToken(@Valid @RequestBody RefreshToken refreshToken) {

        String refreshTokens = refreshToken.refreshToken();

        Long userId = Long.valueOf(tokenService.verifyToken(refreshTokens));
        User user = userRepository.findById(userId).orElseThrow();

        String accessToken = tokenService.generatToken(user);
        String updatedToken = tokenService.generatRefreshToken(user);

        return ResponseEntity.ok(new TokenResponseDTO(accessToken, updatedToken));
    }
}
