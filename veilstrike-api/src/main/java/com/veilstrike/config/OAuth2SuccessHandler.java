package com.veilstrike.config;

import com.veilstrike.dto.AuthResponse;
import com.veilstrike.exception.ApiException;
import com.veilstrike.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Runs after Google has authenticated the user. It pulls the Google profile from the
 * OAuth2 principal, runs our find-or-create logic, mints the SAME internal JWT pair as
 * local login, and redirects the browser back to the PWA frontend with those tokens.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        try {
            AuthResponse tokens = authService.processGoogleLogin(
                    googleId,
                    email,
                    name,
                    Boolean.TRUE.equals(emailVerified),
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());

            String target = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", tokens.accessToken())
                    .queryParam("refreshToken", tokens.refreshToken())
                    .build()
                    .toUriString();
            response.sendRedirect(target);

        } catch (ApiException e) {
            String target = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", e.getMessage())
                    .build()
                    .toUriString();
            response.sendRedirect(target);
        }
    }
}
