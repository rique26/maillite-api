package com.maillite.mailliteapi.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequestDto(
        @NotBlank(message = "O token FCM é obrigatório")
        String fcmToken
) {}