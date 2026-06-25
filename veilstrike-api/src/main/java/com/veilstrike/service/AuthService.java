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
}
