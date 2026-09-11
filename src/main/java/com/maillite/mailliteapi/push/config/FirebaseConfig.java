package com.maillite.mailliteapi.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Inicializa o Firebase Admin SDK a partir do arquivo de credenciais (Console do Firebase >
 * Configurações do projeto > Contas de serviço > Gerar nova chave privada).
 *
 * Se o arquivo não existir, o boot NÃO falha — apenas loga um aviso e o FcmPushService
 * ignora silenciosamente o envio de push (RF09 desabilitada), sem travar o envio de
 * mensagens (RF04), que é a funcionalidade principal.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-path:firebase-service-account.json}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            Resource resource = new FileSystemResource(credentialsPath);

            if (!resource.exists()) {
                log.warn("Credenciais do Firebase não encontradas em '{}'. Push notifications (RF09) desabilitadas.", credentialsPath);
                return;
            }

            try (FileInputStream serviceAccount = new FileInputStream(resource.getFile())) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK inicializado com sucesso.");
                }
            }
        } catch (IOException e) {
            log.warn("Falha ao inicializar Firebase Admin SDK. Push notifications desabilitadas.", e);
        }
    }
}