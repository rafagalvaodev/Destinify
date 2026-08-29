package com.maisprati.destinify.backend.security;

import com.maisprati.destinify.backend.servicies.AccessTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AccessTokenFilter accessTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        return httpSecurity
                .authorizeHttpRequests(
                        request -> {
                            request.requestMatchers(HttpMethod.POST,
                                    "/api/auth/login").permitAll()
                                    .requestMatchers(HttpMethod.POST,
                                    "/api/users/register").permitAll();
                            request.requestMatchers(HttpMethod.GET,
                                    "/api/users/all-users").hasRole("ADMIN");
                            request.requestMatchers(
                                    "/api/auth/update-token",
                                    "/api/users/{id}", // mudar depois
                                    "/api/users/{id}/password",
                                    "/api/users/user/{id}").hasRole("CLIENT");

                            // Listar/ ver hotel: público
                            request.requestMatchers(HttpMethod.GET, "/api/hotels", "/api/hotels/**").permitAll();

                            // Criar, editar, excluir hotel: ADMIN
                            request.requestMatchers(HttpMethod.POST, "/api/hotels", "/api/hotels/**").hasRole("ADMIN");
                            request.requestMatchers(HttpMethod.PUT, "/api/hotels", "/api/hotels/**").hasRole("ADMIN");
                            request.requestMatchers(HttpMethod.DELETE, "/api/hotels", "/api/hotels/**").hasRole("ADMIN");

                            request.anyRequest().authenticated();
                        })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(smc ->
                        smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}