package com.hrr.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Value("${aws.s3.region}")
	private String s3Region;

	/**
	 * Presigned URL 발급을 위한 S3Presigner Bean 등록
	 * * AWS SDK는 EC2에 연결된 IAM 역할(Role)이나
	 * 환경 변수에서 자격 증명을 자동으로 찾기 때문에 별도의 .credentialsProvider() 설정이 필요 없습니다.
	 */
	@Bean
	public S3Presigner s3Presigner() {
		// IAM의 role로 credentials를 대체
		return S3Presigner.builder()
			.region(Region.of(s3Region))
			.build();
	}
}
