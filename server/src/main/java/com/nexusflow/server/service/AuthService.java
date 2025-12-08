package com.nexusflow.server.service;

import com.nexusflow.server.config.JwtService;
import com.nexusflow.server.dto.AuthDto;
import com.nexusflow.server.entity.Role;
import com.nexusflow.server.entity.User;
import com.nexusflow.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository repository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
                if (repository.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("Username already exists"); // Simple exception
                }
                var user = User.builder()
                                .username(request.getUsername())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(request.getRole() != null ? request.getRole() : Role.ROLE_CUSTOMER)
                                .build();
                if (user == null)
                        throw new IllegalStateException("User creation failed");
                repository.save(user);

                var userDetails = org.springframework.security.core.userdetails.User.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .roles(user.getRole().name().substring(5))
                                .build();

                var jwtToken = jwtService.generateToken(userDetails);

                return AuthDto.AuthResponse.builder()
                                .token(jwtToken)
                                .username(user.getUsername())
                                .role(user.getRole())
                                .build();
        }

        public AuthDto.AuthResponse authenticate(AuthDto.LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));
                var user = repository.findByUsername(request.getUsername())
                                .orElseThrow();

                var userDetails = org.springframework.security.core.userdetails.User.builder()
                                .username(user.getUsername())
                                .password(user.getPassword())
                                .roles(user.getRole().name().substring(5))
                                .build();

                var jwtToken = jwtService.generateToken(userDetails);
                return AuthDto.AuthResponse.builder()
                                .token(jwtToken)
                                .username(user.getUsername())
                                .role(user.getRole())
                                .build();
        }
}
