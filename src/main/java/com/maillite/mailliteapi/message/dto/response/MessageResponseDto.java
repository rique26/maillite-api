package com.maillite.mailliteapi.message.dto.response;

import com.maillite.mailliteapi.message.entity.Message;
import com.maillite.mailliteapi.user.dto.response.UserSummaryResponseDto;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MessageResponseDto(
        Long id,
        UserSummaryResponseDto sender,
        UserSummaryResponseDto recipient,
        String subject,
        String body,
        LocalDateTime sentAt,
        boolean read
) {
    public static MessageResponseDto fromEntity(Message message) {
        return MessageResponseDto.builder()
                .id(message.getId())
                .sender(UserSummaryResponseDto.fromEntity(message.getSender()))
                .recipient(UserSummaryResponseDto.fromEntity(message.getRecipient()))
                .subject(message.getSubject())
                .body(message.getBody())
                .sentAt(message.getSentAt())
                .read(message.isRead())
                .build();
    }
}