package com.veilstrike.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Holds the {@link PasswordEncoder} bean on its own so that beans needing it (e.g.
 * AuthService) don't have to depend on SecurityConfig — which would create a cycle now
 * that SecurityConfig wires in the OAuth2 success handler (handler -> AuthService ->
 * PasswordEncoder -> SecurityConfig -> handler).
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
