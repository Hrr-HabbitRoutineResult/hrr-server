package com.hrr.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${firebase.config.path:firebase/hrr-server-firebase-adminsdk-fbsvc-4630feb.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options;

                // 운영 환경일 경우 (환경 변수 기반)
                if ("prod".equalsIgnoreCase(activeProfile)) {
                    String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                    if (credentialsPath == null || credentialsPath.isBlank()) {
                        throw new IllegalStateException("Environment variable 'GOOGLE_APPLICATION_CREDENTIALS' is not set");
                    }

                    try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                        options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();
                    }
                }
                // 로컬/개발 환경일 경우 (classpath 기반)
                else {
                    try (InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream()) {
                        options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .build();
                    }
                }

                FirebaseApp.initializeApp(options);
                log.info("Firebase App initialized successfully (profile: {}).", activeProfile);
            } else {
                log.warn("Firebase App already initialized. Skipping re-initialization.");
            }
        } catch (IOException e) {
            log.error("Firebase initialization failed (profile: {}): {}", activeProfile, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize Firebase", e);
        }
    }
}
