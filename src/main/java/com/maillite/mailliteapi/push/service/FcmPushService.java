package com.maillite.mailliteapi.push.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Disparo assíncrono de push (RF09). É best-effort: qualquer falha aqui é apenas logada,
 * nunca deve derrubar o fluxo de envio de mensagem (RF04).
 */
@Slf4j
@Service
public class FcmPushService {

    @Async
    public void sendNewMessageNotification(String recipientFcmToken, String senderName, String subject) {
        if (!StringUtils.hasText(recipientFcmToken)) {
            log.debug("Destinatário sem token FCM registrado; push ignorado.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase Admin SDK não inicializado; push ignorado.");
            return;
        }

        Message message = Message.builder()
                .setToken(recipientFcmToken)
                .setNotification(Notification.builder()
                        .setTitle("Nova mensagem de " + senderName)
                        .setBody(subject)
                        .build())
                .putData("type", "NEW_MESSAGE")
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push enviado com sucesso: {}", response);
        } catch (FirebaseMessagingException e) {
            log.warn("Falha ao enviar push notification: {}", e.getMessage());
        }
    }
}