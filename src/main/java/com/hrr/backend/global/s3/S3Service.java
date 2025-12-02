package com.hrr.backend.global.s3;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Presigner s3Presigner;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.s3.prefix}")
	private String prefix;

	public S3ResponseDto.PostDto getPresignedUrl(String fileName) {

		// uploads/[UUID]_[원본 파일명]
		String s3Key = prefix + "/" + UUID.randomUUID() + "_" + fileName;

		// PutObjectRequest 생성
		PutObjectRequest objectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.build();

		// Presign 요청 생성 (유효 기간 5분)
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(objectRequest)
			.build();

		// URL 발급
		PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
		String url = presignedRequest.url().toString();

		// URL과 S3 Key 담아 반환
		return S3ResponseDto.PostDto.builder()
			.presignedUrl(url)
			.s3Key(s3Key)
			.build();
	}
}
