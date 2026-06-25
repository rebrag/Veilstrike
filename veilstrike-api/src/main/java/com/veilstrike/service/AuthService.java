package com.veilstrike.service;

import com.veilstrike.dto.*;
import com.veilstrike.exception.ApiException;
import com.veilstrike.model.AuthProvider;
import com.veilstrike.model.User;
import com.veilstrike.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setDisplayName(request.getDisplayName());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, userAgent, ipAddress);
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "This account uses Google sign-in");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, userAgent, ipAddress);
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request, String userAgent, String ipAddress) {
        JwtService.RotationResult result = jwtService.rotateRefreshToken(
                request.getRefreshToken(), userAgent, ipAddress);

        if (result == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        return new AuthResponse(result.accessToken(), result.refreshToken());
    }

    /**
     * Find-or-create flow for a verified Google identity, then converge on the SAME
     * JWT issuance path as local login. Returns the standard access + refresh token pair.
     */
    @Transactional
    public AuthResponse processGoogleLogin(
            String googleId,
            String email,
            String displayName,
            boolean emailVerified,
            String userAgent,
            String ipAddress) {

        User user = userRepository.findByGoogleId(googleId).orElse(null);

        if (user == null) {
            // No account is linked to this Google id yet. See if the email is already taken.
            User existing = userRepository.findByEmail(email).orElse(null);

            if (existing != null) {
                // The email belongs to an existing account (typically a LOCAL one).
                // Only link the Google identity if Google has VERIFIED the email — that
                // proves the person signing in actually controls this address. Without
                // that proof, linking would let anyone with a matching Google email
                // hijack the local account.
                if (!emailVerified) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "This email is already registered. Sign in with your password.");
                }
                // Link: attach the google_id to the existing account. We keep the
                // original auth_provider (e.g. LOCAL) so the user can still sign in with
                // their password as well — the DB constraint is satisfied because a LOCAL
                // account already has a password_hash.
                existing.setGoogleId(googleId);
                user = userRepository.save(existing);
            } else {
                // Brand-new user: create a GOOGLE account (no password_hash).
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setGoogleId(googleId);
                newUser.setAuthProvider(AuthProvider.GOOGLE);
                newUser.setDisplayName(normalizeDisplayName(displayName, email));
                user = userRepository.save(newUser);
            }
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, userAgent, ipAddress);
        return new AuthResponse(accessToken, refreshToken);
    }

    private String normalizeDisplayName(String displayName, String email) {
        String name = (displayName == null || displayName.isBlank()) ? email : displayName;
        return name.length() > 50 ? name.substring(0, 50) : name;
    }
}
