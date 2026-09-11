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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final FcmPushService fcmPushService;

    /** RF04: envia mensagem, valida destinatário e impede autoenvio; dispara RF09 (push). */
    @Transactional
    public MessageResponseDto send(SendMessageRequestDto dto) {
        User sender = currentUserProvider.getCurrentUser();

        if (sender.getId().equals(dto.recipientId())) {
            throw new ApiException("Não é possível enviar uma mensagem para você mesmo");
        }

        User recipient = userRepository.findById(dto.recipientId())
                .orElseThrow(() -> new NotFoundException("Destinatário não encontrado"));

        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .subject(dto.subject())
                .body(dto.body())
                .build();

        Message saved = messageRepository.save(message);

        fcmPushService.sendNewMessageNotification(recipient.getFcmToken(), sender.getName(), saved.getSubject());

        return MessageResponseDto.fromEntity(saved);
    }

    /** RF05: inbox paginada do usuário autenticado. */
    public Page<MessageResponseDto> getInbox(Pageable pageable) {
        User currentUser = currentUserProvider.getCurrentUser();

        return messageRepository.findByRecipientId(currentUser.getId(), pageable)
                .map(MessageResponseDto::fromEntity);
    }

    /** RF06: retorna detalhes da mensagem e marca automaticamente como lida. */
    @Transactional
    public MessageResponseDto getById(Long id) {
        User currentUser = currentUserProvider.getCurrentUser();

        Message message = messageRepository.findByIdAndRecipientId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada"));

        if (!message.isRead()) {
            message.markAsRead();
        }

        return MessageResponseDto.fromEntity(message);
    }

    /** RF07: remoção (soft delete) da mensagem da caixa de entrada. */
    @Transactional
    public void delete(Long id) {
        User currentUser = currentUserProvider.getCurrentUser();

        Message message = messageRepository.findByIdAndRecipientId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Mensagem não encontrada"));

        message.softDelete();
    }
}