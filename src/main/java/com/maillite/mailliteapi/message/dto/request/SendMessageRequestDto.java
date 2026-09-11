package com.maillite.mailliteapi.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequestDto(
        @NotNull(message = "O destinatário é obrigatório")
        Long recipientId,

        @NotBlank(message = "O assunto é obrigatório")
        @Size(max = 255, message = "O assunto deve ter no máximo 255 caracteres")
        String subject,

        @NotBlank(message = "O corpo da mensagem é obrigatório")
        String body
) {}