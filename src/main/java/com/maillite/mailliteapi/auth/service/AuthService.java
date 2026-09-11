package com.maillite.mailliteapi.auth.service;

import com.maillite.mailliteapi.auth.dto.request.LoginRequestDto;
import com.maillite.mailliteapi.auth.dto.request.RegisterUserRequestDto;
import com.maillite.mailliteapi.auth.dto.response.TokenResponseDto;
import com.maillite.mailliteapi.auth.entity.User;
import com.maillite.mailliteapi.auth.repository.UserRepository;
import com.maillite.mailliteapi.common.util.SanitizationUtil;
import com.maillite.mailliteapi.config.TokenProvider;
import com.maillite.mailliteapi.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public TokenResponseDto login(LoginRequestDto dto) throws ApiException {
        try {
            String sanitizedEmail = SanitizationUtil.email(dto.email());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(sanitizedEmail, dto.password())
            );

            String token = tokenProvider.generateToken(authentication);

            return TokenResponseDto.builder()
                    .token(token)
                    .type("Bearer")
                    .expiration(expirationTime)
                    .build();
        } catch (BadCredentialsException e) {
            throw new ApiException("E-mail ou senha inválidos");
        }
    }

    @Transactional
    public void register(RegisterUserRequestDto dto) throws ApiException {
        String sanitizedEmail = SanitizationUtil.email(dto.email());

        validateEmailUniqueness(sanitizedEmail);

        User user = User.builder()
                .name(dto.name().trim())
                .email(sanitizedEmail)
                .password(passwordEncoder.encode(dto.password()))
                .fcmToken(dto.fcmToken())
                .build();

        userRepository.save(user);
    }

    private void validateEmailUniqueness(String email) throws ApiException {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email já cadastrado");
        }
    }

}