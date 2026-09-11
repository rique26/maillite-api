package com.maillite.mailliteapi.push.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @InjectMocks
    private FcmPushService fcmPushService;

    @Nested
    class SendNewMessageNotification {

        @Test
        @DisplayName("Should return early and not send when recipient token is blank or null")
        void shouldReturnEarlyWhenTokenIsBlank() {
            try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
                // Act & Assert
                assertDoesNotThrow(() -> fcmPushService.sendNewMessageNotification("", "Sender", "Subject"));
                assertDoesNotThrow(() -> fcmPushService.sendNewMessageNotification(null, "Sender", "Subject"));

                firebaseAppMock.verifyNoInteractions();
            }
        }

        @Test
        @DisplayName("Should return early and not send when Firebase app is not initialized")
        void shouldReturnEarlyWhenFirebaseAppIsEmpty() {
            try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class)) {
                firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of());

                // Act & Assert
                assertDoesNotThrow(() -> fcmPushService.sendNewMessageNotification("token-123", "Sender", "Subject"));

                firebaseAppMock.verify(FirebaseApp::getApps, times(1));
            }
        }

        @Test
        @DisplayName("Should send push notification successfully when token is valid and Firebase is initialized")
        void shouldSendPushSuccessfully() {
            String token = "token-123";
            String senderName = "Sender User";
            String subject = "Test Subject";

            try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {

                firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));

                FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
                firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);
                when(messagingMock.send(any(Message.class))).thenReturn("projects/test/messages/123");

                // Act & Assert
                assertDoesNotThrow(() -> fcmPushService.sendNewMessageNotification(token, senderName, subject));

                firebaseMessagingMock.verify(FirebaseMessaging::getInstance, times(1));
                verify(messagingMock, times(1)).send(any(Message.class));
            } catch (FirebaseMessagingException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Should catch FirebaseMessagingException and not throw when sending push fails")
        void shouldCatchExceptionWhenSendFails() {
            String token = "token-123";

            try (MockedStatic<FirebaseApp> firebaseAppMock = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> firebaseMessagingMock = mockStatic(FirebaseMessaging.class)) {

                firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));

                FirebaseMessaging messagingMock = mock(FirebaseMessaging.class);
                firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(messagingMock);

                FirebaseMessagingException exceptionMock = mock(FirebaseMessagingException.class);
                when(exceptionMock.getMessage()).thenReturn("Messaging error");

                // Correção: Uso de doThrow para evitar erro de compilação com Checked Exception
                doThrow(exceptionMock).when(messagingMock).send(any(Message.class));

                // Act & Assert (Best-effort: deve apenas logar e não lançar exceção)
                assertDoesNotThrow(() -> fcmPushService.sendNewMessageNotification(token, "Sender", "Subject"));

                verify(messagingMock, times(1)).send(any(Message.class));
            } catch (FirebaseMessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}