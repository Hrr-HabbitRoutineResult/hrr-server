package com.hrr.backend.global.s3;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

	private final S3Client s3Client;
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
			.acl(ObjectCannedACL.PUBLIC_READ)
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

	public void deleteFileByKey(String key) {
		if (key == null || key.isBlank() || key.startsWith("http")) return;
		String keyFingerprint = Integer.toHexString(key.hashCode());

		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build();

			s3Client.deleteObject(deleteObjectRequest);
			log.info("[deleteFileByKey] S3 파일 삭제를 완료했습니다. keyFingerprint={}", keyFingerprint);
		} catch (S3Exception e) {
			String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
			log.error("[deleteFileByKey] S3 파일 삭제에 실패했습니다. keyFingerprint={}, status={}, errorCode={}, requestId={}",
				keyFingerprint, e.statusCode(), errorCode, e.requestId());
		} catch (Exception e) {
			log.error("[deleteFileByKey] S3 파일 삭제 중 예상하지 못한 오류가 발생했습니다. keyFingerprint={}, exception={}",
				keyFingerprint, e.getClass().getSimpleName());
		}
	}
}
