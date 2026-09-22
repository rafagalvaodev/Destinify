package com.maisprati.destinify.backend.utils.OpenAPI;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPIDocumentation() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole apenas o accessToken retornado pelo login, sem o prefixo Bearer.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(
                        new Info().title("Destinify Documentation")
                                .description("API Destinify, Gerenciamento de reserva de hoteis")
                                .version("V1")
                                .contact(new Contact()
                                        .name("Destinify Team")
                                        .email("destinifyTeam@email.com"))
                );
    }
}
