package com.hrr.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

	// 운영 환경에서는 환경 변수에서 JSON 문자열 자체를 주입
	@Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
	private String firebaseServiceAccountJson;

    @Value("${firebase.config.path:firebase/hrr-server-firebase-adminsdk-fbsvc-4630feb.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options;

                // 운영 환경일 경우 (환경 변수 기반)
                if ("prod".equalsIgnoreCase(activeProfile)) {
					if (firebaseServiceAccountJson == null || firebaseServiceAccountJson.isBlank()) {
						// JSON 문자열이 없으면 예외 발생
						throw new IllegalStateException("Environment variable 'FIREBASE_SERVICE_ACCOUNT_JSON' is not set or empty.");
					}

					// 문자열을 바이트 배열로 변환하여 InputStream으로 사용
					try (InputStream serviceAccount = new ByteArrayInputStream(firebaseServiceAccountJson.getBytes(
						StandardCharsets.UTF_8))) {
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
