package com.maillite.mailliteapi.user.service;

import com.maillite.mailliteapi.config.CurrentUserProvider;
import com.maillite.mailliteapi.user.dto.request.FcmTokenRequestDto;
import com.maillite.mailliteapi.user.dto.response.UserSummaryResponseDto;
import com.maillite.mailliteapi.user.entity.User;
import com.maillite.mailliteapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    /** RF03. */
    public List<UserSummaryResponseDto> search(String query) {
        User currentUser = currentUserProvider.getCurrentUser();

        return userRepository.searchByNameOrEmail(query, currentUser.getId()).stream()
                .map(UserSummaryResponseDto::fromEntity)
                .toList();
    }

    /** RF08. */
    @Transactional
    public void updateFcmToken(FcmTokenRequestDto dto) {
        User currentUser = currentUserProvider.getCurrentUser();
        currentUser.setFcmToken(dto.fcmToken());
        userRepository.save(currentUser);
    }
}