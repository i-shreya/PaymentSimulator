package com.example.payment.controller;

import com.example.payment.dto.AuthDtos.*;
import com.example.payment.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Operation(summary = "Register merchant", description = "Creates a merchant account with a BCrypt-hashed password.")
    @PostMapping("/register")
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @Operation(summary = "Login", description = "Authenticates a merchant/admin and returns a JWT access token.")
    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
