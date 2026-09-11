package com.maillite.mailliteapi.auth.service;

import com.maillite.mailliteapi.auth.dto.request.LoginRequestDto;
import com.maillite.mailliteapi.auth.dto.request.RegisterUserRequestDto;
import com.maillite.mailliteapi.auth.dto.response.TokenResponseDto;
import com.maillite.mailliteapi.user.entity.User;
import com.maillite.mailliteapi.user.repository.UserRepository;
import com.maillite.mailliteapi.config.TokenProvider;
import com.maillite.mailliteapi.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationTime", 86400L);
    }

    @Nested
    class Login {

        @Test
        @DisplayName("Should authenticate and return token when valid credentials are provided")
        void shouldLoginWithSuccess() {
            // Arrange
            var input = new LoginRequestDto("usuario@email.com", "123456");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("fake-jwt-token");

            // Act
            TokenResponseDto response = authService.login(input);

            // Assert
            assertNotNull(response);
            assertEquals("fake-jwt-token", response.token());
            assertEquals("Bearer", response.type());
            assertEquals(86400L, response.expiration());
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider, times(1)).generateToken(authentication);
        }

        @Test
        @DisplayName("Should throw ApiException when invalid credentials are provided")
        void shouldThrowExceptionWhenBadCredentials() {
            // Arrange
            var input = new LoginRequestDto("usuario@email.com", "senhaerrada");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            var exception = assertThrows(
                    ApiException.class,
                    () -> authService.login(input)
            );

            assertEquals("E-mail ou senha inválidos", exception.getMessage());
            verify(tokenProvider, never()).generateToken(any());
        }
    }

    @Nested
    class Register {

        @Test
        @DisplayName("Should register user with success when valid request is provided")
        void shouldRegisterUserWithSuccess() {
            // Arrange
            var input = new RegisterUserRequestDto(
                    "  User Test  ",
                    "usuario@email.com",
                    "123456",
                    "fcm-token-123"
            );

            when(userRepository.existsByEmail("usuario@email.com")).thenReturn(false);
            when(passwordEncoder.encode("123456")).thenReturn("encoded_password");

            // Act & Assert
            assertDoesNotThrow(() -> authService.register(input));

            // Verify User save
            verify(userRepository, times(1)).save(userArgumentCaptor.capture());
            var capturedUser = userArgumentCaptor.getValue();
            assertEquals("User Test", capturedUser.getName());
            assertEquals("usuario@email.com", capturedUser.getEmail());
            assertEquals("encoded_password", capturedUser.getPassword());
            assertEquals("fcm-token-123", capturedUser.getFcmToken());
        }

        @Test
        @DisplayName("Should throw ApiException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Arrange
            var input = new RegisterUserRequestDto(
                    "User Test",
                    "existente@email.com",
                    "123456",
                    "fcm-token-123"
            );

            when(userRepository.existsByEmail("existente@email.com")).thenReturn(true);

            // Act & Assert
            var exception = assertThrows(
                    ApiException.class,
                    () -> authService.register(input)
            );

            assertEquals("Email já cadastrado", exception.getMessage());

            verify(userRepository, times(1)).existsByEmail("existente@email.com");
            verify(userRepository, never()).save(any());
        }
    }
}