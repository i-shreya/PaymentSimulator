package com.example.payment.dto;

import com.example.payment.entity.UserRole;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    public record RegisterRequest(@NotBlank @Size(max = 120) String name, @Email @NotBlank String email, @Size(min = 8, max = 120) String password) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String tokenType, long expiresInMs) {}
    public record UserResponse(UUID id, String name, String email, UserRole role, Instant createdAt) {}
}
