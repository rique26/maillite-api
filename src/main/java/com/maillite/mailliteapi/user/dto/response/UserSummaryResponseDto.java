package com.maillite.mailliteapi.user.dto.response;

import com.maillite.mailliteapi.user.entity.User;
import lombok.Builder;

@Builder
public record UserSummaryResponseDto(
        Long id,
        String name,
        String email
) {
    public static UserSummaryResponseDto fromEntity(User user) {
        return UserSummaryResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}