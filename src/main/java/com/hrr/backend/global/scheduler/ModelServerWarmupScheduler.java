package com.hrr.backend.global.scheduler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicInteger;


@Component
@Slf4j
public class ModelServerWarmupScheduler {

	@Value("${model.api.health-url}")
	private String healthUrl;

	private final RestTemplate restTemplate;
	private final AtomicInteger consecutiveFailures = new AtomicInteger();

	public ModelServerWarmupScheduler(@Qualifier("modelApiRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * 추천/임베딩 모델 서버(Lambda)를 주기적으로 호출해 콜드 스타트로 스케일 인되지 않도록 유지한다.
	 * 스케줄러 공용 스레드를 막지 않도록 @Async로 실행하며, 콜드 스타트에 걸려도
	 * modelApiRestTemplate의 넉넉한 타임아웃 안에서 워밍업이 끝까지 완료되게 둔다.
	 */
	@Async
	@Scheduled(cron = "0 0/5 * * * *", zone = "Asia/Seoul")
	public void warmUpModelServer() {
		try {
			restTemplate.getForObject(healthUrl, String.class);
			int recoveredFailureCount = consecutiveFailures.getAndSet(0);
			if (recoveredFailureCount > 0) {
				log.info("[warmUpModelServer] Model 서버 warmup 요청이 복구되었습니다. previousFailureCount={}",
					recoveredFailureCount);
			}
		} catch (RestClientException e) {
			int failureCount = consecutiveFailures.incrementAndGet();
			log.warn("[warmUpModelServer] Model 서버 warmup 요청에 실패했습니다. exception={}",
				e.getClass().getSimpleName());
			if (failureCount == 3 || failureCount % 12 == 0) {
				log.error("[warmUpModelServer] Model 서버 warmup 요청의 연속 실패가 누적되었습니다. consecutiveFailureCount={}",
					failureCount);
			}
		}
	}
}
