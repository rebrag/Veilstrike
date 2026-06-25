package com.veilstrike.service;

import com.veilstrike.config.JwtProperties;
import com.veilstrike.model.Session;
import com.veilstrike.model.User;
import com.veilstrike.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SessionRepository sessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getAccessTokenExpiration())))
                .signWith(getSigningKey())
                .compact();
    }

    @Transactional
    public String generateRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Session session = new Session();
        session.setUser(user);
        session.setRefreshTokenHash(tokenHash);
        session.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()));
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        sessionRepository.save(session);

        return rawToken;
    }

    public UUID validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            return null;
        }
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawRefreshToken, String userAgent, String ipAddress) {
        String tokenHash = hashToken(rawRefreshToken);
        Session session = sessionRepository.findByRefreshTokenHash(tokenHash).orElse(null);

        if (session == null || session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }

        // Revoke the old session
        session.setRevoked(true);
        sessionRepository.save(session);

        // Issue a new refresh token under a new session row
        User user = session.getUser();
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        Session newSession = new Session();
        newSession.setUser(user);
        newSession.setRefreshTokenHash(newTokenHash);
        newSession.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()));
        newSession.setUserAgent(userAgent);
        newSession.setIpAddress(ipAddress);
        sessionRepository.save(newSession);

        String newAccessToken = generateAccessToken(user);
        return new RotationResult(newAccessToken, newRawToken);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public record RotationResult(String accessToken, String refreshToken) {}
}
