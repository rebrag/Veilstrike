package com.veilstrike.dto;

import com.veilstrike.model.AuthProvider;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        AuthProvider authProvider) {}
