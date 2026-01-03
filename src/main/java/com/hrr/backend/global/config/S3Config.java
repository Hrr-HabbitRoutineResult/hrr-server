package com.hrr.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

	@Value("${aws.s3.region}")
	private String s3Region;

	@Bean
	public S3Presigner s3Presigner() {
		// IAM의 role로 credentials를 대체
		return S3Presigner.builder()
			.region(Region.of(s3Region))
			.build();
	}

	@Bean
	public S3Client s3Client() {
		// S3Presigner와 마찬가지로 IAM Role을 사용하도록 설정
		return S3Client.builder()
			.region(Region.of(s3Region))
			.build();
	}
}
