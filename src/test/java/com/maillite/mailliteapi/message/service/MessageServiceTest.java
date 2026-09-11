package com.maillite.mailliteapi.message.service;

import com.maillite.mailliteapi.config.CurrentUserProvider;
import com.maillite.mailliteapi.exception.ApiException;
import com.maillite.mailliteapi.exception.NotFoundException;
import com.maillite.mailliteapi.message.dto.request.SendMessageRequestDto;
import com.maillite.mailliteapi.message.dto.response.MessageResponseDto;
import com.maillite.mailliteapi.message.entity.Message;
import com.maillite.mailliteapi.message.repository.MessageRepository;
import com.maillite.mailliteapi.push.service.FcmPushService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private FcmPushService fcmPushService;

    @InjectMocks
    private MessageService messageService;

    @Captor
    private ArgumentCaptor<Message> messageArgumentCaptor;

    @Nested
    class Send {

        @Test
        @DisplayName("Should send message successfully and trigger push notification")
        void shouldSendMessageWithSuccess() {
            // Arrange
            var sender = User.builder().id(1L).name("Sender User").fcmToken("sender-token").build();
            var recipient = User.builder().id(2L).name("Recipient User").fcmToken("recipient-token").build();
            var input = new SendMessageRequestDto(2L, "Assunto Teste", "Corpo da mensagem");

            when(currentUserProvider.getCurrentUser()).thenReturn(sender);
            when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                return Message.builder()
                        .id(10L)
                        .sender(msg.getSender())
                        .recipient(msg.getRecipient())
                        .subject(msg.getSubject())
                        .body(msg.getBody())
                        .build();
            });

            // Act
            MessageResponseDto response = messageService.send(input);

            // Assert
            assertNotNull(response);
            assertEquals(10L, response.id());
            assertEquals("Assunto Teste", response.subject());
            assertEquals("Corpo da mensagem", response.body());

            verify(messageRepository, times(1)).save(messageArgumentCaptor.capture());
            var capturedMessage = messageArgumentCaptor.getValue();
            assertEquals(sender, capturedMessage.getSender());
            assertEquals(recipient, capturedMessage.getRecipient());

            verify(fcmPushService, times(1)).sendNewMessageNotification(
                    eq("recipient-token"),
                    eq("Sender User"),
                    eq("Assunto Teste")
            );
        }

        @Test
        @DisplayName("Should throw ApiException when trying to send message to self")
        void shouldThrowExceptionWhenSendingToSelf() {
            // Arrange
            var sender = User.builder().id(1L).name("Sender User").build();
            var input = new SendMessageRequestDto(1L, "Assunto", "Corpo");

            when(currentUserProvider.getCurrentUser()).thenReturn(sender);

            // Act & Assert
            var exception = assertThrows(
                    ApiException.class,
                    () -> messageService.send(input)
            );

            assertEquals("Não é possível enviar uma mensagem para você mesmo", exception.getMessage());
            verify(messageRepository, never()).save(any());
            verify(fcmPushService, never()).sendNewMessageNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw NotFoundException when recipient does not exist")
        void shouldThrowExceptionWhenRecipientNotFound() {
            // Arrange
            var sender = User.builder().id(1L).name("Sender User").build();
            var input = new SendMessageRequestDto(99L, "Assunto", "Corpo");

            when(currentUserProvider.getCurrentUser()).thenReturn(sender);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            var exception = assertThrows(
                    NotFoundException.class,
                    () -> messageService.send(input)
            );

            assertEquals("Destinatário não encontrado", exception.getMessage());
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    class GetInbox {

        @Test
        @DisplayName("Should return list of messages for current user inbox")
        void shouldReturnInboxSuccessfully() {
            // Arrange
            var currentUser = User.builder().id(1L).build();
            var sender = User.builder().id(2L).name("Sender").build();
            var message = Message.builder()
                    .id(1L)
                    .sender(sender)
                    .recipient(currentUser)
                    .subject("Inbox Subject")
                    .body("Inbox Body")
                    .build();

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(messageRepository.findInboxByRecipientId(1L)).thenReturn(List.of(message));

            // Act
            List<MessageResponseDto> inbox = messageService.getInbox();

            // Assert
            assertNotNull(inbox);
            assertEquals(1, inbox.size());
            assertEquals("Inbox Subject", inbox.get(0).subject());
            verify(messageRepository, times(1)).findInboxByRecipientId(1L);
        }
    }

    @Nested
    class GetById {

        @Test
        @DisplayName("Should return message details and mark as read when unread")
        void shouldGetMessageAndMarkAsRead() {
            // Arrange
            var currentUser = User.builder().id(1L).build();
            var sender = User.builder().id(2L).name("Sender").build();
            var message = Message.builder()
                    .id(5L)
                    .sender(sender)
                    .recipient(currentUser)
                    .subject("Test")
                    .body("Body")
                    .read(false)
                    .build();

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(messageRepository.findByIdAndRecipientId(5L, 1L)).thenReturn(Optional.of(message));

            // Act
            MessageResponseDto response = messageService.getById(5L);

            // Assert
            assertNotNull(response);
            assertTrue(message.isRead());
            verify(messageRepository, times(1)).findByIdAndRecipientId(5L, 1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when message does not exist or does not belong to user")
        void shouldThrowNotFoundWhenMessageDoesNotExist() {
            // Arrange
            var currentUser = User.builder().id(1L).build();

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(messageRepository.findByIdAndRecipientId(99L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            var exception = assertThrows(
                    NotFoundException.class,
                    () -> messageService.getById(99L)
            );

            assertEquals("Mensagem não encontrada", exception.getMessage());
        }
    }

    @Nested
    class Delete {

        @Test
        @DisplayName("Should soft delete message successfully when found")
        void shouldSoftDeleteMessageSuccessfully() {
            // Arrange
            var currentUser = User.builder().id(1L).build();
            var message = Message.builder()
                    .id(3L)
                    .recipient(currentUser)
                    .build();

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(messageRepository.findByIdAndRecipientId(3L, 1L)).thenReturn(Optional.of(message));

            // Act & Assert
            assertDoesNotThrow(() -> messageService.delete(3L));
            verify(messageRepository, times(1)).findByIdAndRecipientId(3L, 1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when trying to delete non-existent message")
        void shouldThrowNotFoundWhenDeletingNonExistentMessage() {
            // Arrange
            var currentUser = User.builder().id(1L).build();

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(messageRepository.findByIdAndRecipientId(99L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            var exception = assertThrows(
                    NotFoundException.class,
                    () -> messageService.delete(99L)
            );

            assertEquals("Mensagem não encontrada", exception.getMessage());
        }
    }
}