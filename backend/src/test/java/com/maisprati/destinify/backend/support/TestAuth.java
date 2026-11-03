package com.maisprati.destinify.backend.support;

import com.maisprati.destinify.backend.domain.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor as (User user) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
