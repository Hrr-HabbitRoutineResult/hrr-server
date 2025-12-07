package com.hrr.backend.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3UrlUtil {

    private final String s3UrlPrefix;

    public S3UrlUtil(@Value("${aws.s3.url-prefix}") String s3UrlPrefix) {
        this.s3UrlPrefix = s3UrlPrefix;
    }

    /**
     * S3 key를 전체 URL로 변환
     * @param imageKey S3 객체 키 (null 또는 빈 문자열 가능)
     * @return 전체 S3 URL 또는 null
     */
    public String toFullUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        // 이미 전체 URL인 경우 방어 로직 (레거시 데이터 대응)
        if (imageKey.startsWith("http://") || imageKey.startsWith("https://")) {
            return imageKey;
        }
        return s3UrlPrefix + "/" + imageKey;
    }
}