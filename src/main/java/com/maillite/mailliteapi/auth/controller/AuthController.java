package com.maillite.mailliteapi.auth.controller;

import com.maillite.mailliteapi.auth.dto.request.LoginRequestDto;
import com.maillite.mailliteapi.auth.dto.request.RegisterUserRequestDto;
import com.maillite.mailliteapi.auth.dto.response.TokenResponseDto;
import com.maillite.mailliteapi.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDto login(@RequestBody @Valid LoginRequestDto loginRequestDto) {
        return authService.login(loginRequestDto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid RegisterUserRequestDto registerUserRequestDto) {
        authService.register(registerUserRequestDto);
    }
}