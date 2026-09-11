package com.maillite.mailliteapi.user.service;

import com.maillite.mailliteapi.config.CurrentUserProvider;
import com.maillite.mailliteapi.user.dto.request.FcmTokenRequestDto;
import com.maillite.mailliteapi.user.dto.response.UserSummaryResponseDto;
import com.maillite.mailliteapi.user.entity.User;
import com.maillite.mailliteapi.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Nested
    class Search {

        @Test
        @DisplayName("Should return user summary list when searching by query excluding current user")
        void shouldReturnUserSummaryListSuccessfully() {
            // Arrange
            var currentUser = User.builder().id(1L).name("Current User").email("current@email.com").build();
            var otherUser = User.builder().id(2L).name("Other User").email("other@email.com").build();
            String query = "Other";

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userRepository.searchByNameOrEmail(eq(query), eq(1L))).thenReturn(List.of(otherUser));

            // Act
            List<UserSummaryResponseDto> result = userService.search(query);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(otherUser.getId(), result.get(0).id());
            assertEquals(otherUser.getName(), result.get(0).name());
            assertEquals(otherUser.getEmail(), result.get(0).email());

            verify(currentUserProvider, times(1)).getCurrentUser();
            verify(userRepository, times(1)).searchByNameOrEmail(query, 1L);
        }
    }

    @Nested
    class UpdateFcmToken {

        @Test
        @DisplayName("Should update current user FCM token successfully")
        void shouldUpdateFcmTokenSuccessfully() {
            // Arrange
            var currentUser = User.builder().id(1L).name("User").fcmToken("old-token").build();
            var input = new FcmTokenRequestDto("new-fcm-token");

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);

            // Act & Assert
            assertDoesNotThrow(() -> userService.updateFcmToken(input));

            verify(currentUserProvider, times(1)).getCurrentUser();
            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            var capturedUser = userArgumentCaptor.getValue();
            assertEquals(1L, capturedUser.getId());
            assertEquals("new-fcm-token", capturedUser.getFcmToken());
        }
    }
}