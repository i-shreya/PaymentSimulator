package com.example.payment.service;

import com.example.payment.dto.AuthDtos.*;
import com.example.payment.entity.User;
import com.example.payment.exception.ApiException;
import com.example.payment.repository.UserRepository;
import com.example.payment.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered.");
        }
        var user = new User();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(encoder.encode(request.password()));
        users.save(user);
        return new UserResponse(user.getPublicId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        var user = users.findByEmail(request.email().toLowerCase()).orElseThrow();
        String token = jwt.generate(new AppUserDetails(user));
        return new AuthResponse(token, "Bearer", jwt.expirationMs());
    }
}
