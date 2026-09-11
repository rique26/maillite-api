package com.maillite.mailliteapi.auth.dto.response;

import lombok.Builder;

@Builder
public record TokenResponseDto(
        String token,
        String type,
        Long expiration
) { }