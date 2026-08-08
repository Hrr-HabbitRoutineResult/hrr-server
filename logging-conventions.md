# 로깅 컨벤션

## 왜 이 문서가 필요한가

`log.error(...)`로 찍히는 모든 로그는 즉시 Discord 웹훅으로 전송된다(`global/logging` 패키지, `logback-spring.xml` 참고 — `DISCORD_WEBHOOK_URL` 설정 시 활성화, 5분 내 동일 알림 중복 억제, 분당 20건 상한). 즉 **ERROR 로그 하나하나가 곧 팀 채널에 뜨는 실시간 알림**이다. 이 전제 때문에 일반적인 로깅 습관보다 조금 더 엄격한 규칙이 필요하다:
- 노이즈(예상된/반복되는 실패)를 ERROR로 찍으면 알림 피로가 생긴다.
- 로그 메시지·스택트레이스에 민감정보가 섞이면 그대로 외부 웹훅으로 나간다.

새 코드를 작성하거나 기존 로그를 수정할 때 아래 규칙을 따른다.

## 1. 레벨 선택

- **ERROR** — 진짜 예상하지 못한, 조치가 필요한 실패만. 재시도로 해결되는 중간 단계, 클라이언트 입력에 따른 정상적인 실패(토큰 만료, 검증 실패, 이미 존재함 등)는 ERROR가 아니다.
- **WARN** — 예상된/클라이언트발 실패, 재시도 중간 단계(`@Retryable`의 각 시도 실패 등), 복구 가능한 경합 상황.
- **INFO** — 정상 흐름 추적용. 배치/루프 작업은 시작·종료 요약만 INFO로 남기고 건별 성공은 남기지 않는다.

**예외**: 요청마다(또는 아주 높은 빈도로) 호출되는 코드의 "정상적으로 흔한" 실패는 WARN조차 남기지 않는 게 맞을 수 있다(예: `JwtService.validateToken()` — 매 인증 요청마다 호출되며 토큰 만료는 지극히 흔한 일이라 로그를 넣으면 정상 트래픽만으로 도배된다). 호출 빈도를 먼저 확인할 것.

## 2. 메시지 형식 — `[메소드명]` 접두사

로그만 보고 어느 메소드에서 났는지 바로 알 수 있도록, 메시지 맨 앞에 발생한 메소드명을 대괄호로 붙인다.

```java
log.error("[socialLogin] 카카오 로그인 중 오류 발생: ", e);
log.warn("[extractUserId] 토큰에서 userId 추출 실패: {}", e.getMessage());
```

다른 클래스에 동일한 이름의 메소드가 있어서 헷갈릴 수 있으면 클래스명까지 붙인다:

```java
// NaverAuthService에도 동일한 이름의 fetchUser가 있어 클래스명까지 명시
log.error("[KakaoAuthService.fetchUser] 카카오 사용자 정보 조회 중 네트워크 오류 발생: ", e);
```

기존 스케줄러 코드처럼 `[RoundDrop]`, `[ChallengeStartScheduler]`같은 "작업명" 스타일 접두사가 이미 있는 곳은 그 컨벤션을 유지해도 된다 — 핵심은 "이 로그가 어디서 왔는지 태그 하나로 알 수 있어야 한다"는 것.

## 3. 예외를 잡아서 다시 던질 때

- `GlobalException`으로 감싸 던질 때는 원인을 체이닝한다: `new GlobalException(ErrorCode.X, e)` (단, 4번 보안 규칙에 해당하면 체이닝하지 않는다).
- **이중 로깅 주의**: catch해서 `log.error(...)`로 이미 남긴 뒤 다시 던질 거면, 원본 예외를 그대로 `throw e`하지 말고 `GlobalException`으로 감싸서 던질 것. `ExceptionAdvice`의 catch-all(`exception()`)은 `GlobalException`이 아닌 예외만 다시 ERROR로 로깅하므로, 원본을 그대로 던지면 같은 사고에 대해 Discord 알림이 두 번 뜬다.

## 4. 배치/루프 처리 — 건별 ERROR 금지

루프 안에서 개별 항목 처리가 실패해도 다음 항목을 계속 처리하는 패턴(스케줄러 등)에서는, 건별 실패를 WARN으로 남기고 **루프가 끝난 뒤 실패 건수가 0보다 클 때만 ERROR로 한 번 집계**한다. 하나의 근본 원인으로 루프 전체가 실패하면 N번의 Discord 알림이 뜨는 것을 방지하기 위함.

```java
int failCount = 0;
for (Round round : rounds) {
    try {
        process(round);
    } catch (Exception e) {
        log.warn("[dropNonContinuersAt] 드랍 처리 실패. roundId={}", round.getId(), e);
        failCount++;
    }
}
if (failCount > 0) {
    log.error("[dropNonContinuersAt] 드랍 처리 총 {}건 중 {}건 실패", rounds.size(), failCount);
}
```

## 5. 보안 — 로그에 절대 남기면 안 되는 것

ERROR 로그는 외부 Discord 웹훅으로 나간다는 걸 항상 염두에 둔다.

- **원본 토큰/시크릿/비밀번호/private key**를 로그 메시지에 직접 넣지 않는다.
- **시크릿이 URL 쿼리 파라미터에 포함된 요청에서 발생한 예외는 `e`를 그대로 로깅하거나 체이닝하지 않는다.** Spring의 `RestClientException`(특히 `ResourceAccessException`) 등은 실패한 요청의 **URL 전체**를 예외 메시지에 그대로 담기 때문에, `log.error(msg, e)`로 넘기는 순간 스택트레이스를 통해 시크릿이 그대로 노출된다. 이런 경로에서는 `e.getClass().getSimpleName()`만 안전하게 남긴다.
  - 실제 사례: `NaverAuthService`가 `client_secret`/`access_token`/`refresh_token`을 쿼리 파라미터로 보내는데, 네트워크 에러 시 `e`를 그대로 로깅해서 Discord로 시크릿이 나갈 뻔했다. 가능하면 애초에 시크릿을 URL이 아니라 요청 바디/헤더로 보내는 것(Kakao/Apple 연동이 이 방식)이 근본적으로 더 안전하다.
- **디코딩한 JWT payload 전체를 로깅하지 않는다** — Apple id_token 등은 email 같은 PII를 담고 있을 수 있다. 필요하면 어떤 필드가 있는지(키 목록)만 남긴다.
- **요청 헤더/파라미터를 통째로 덤프하지 않는다.** 디버그 목적으로 꼭 필요하면 `Authorization`/`Cookie`/`token`/`secret`/`code` 등 이름에 민감한 키워드가 들어간 항목은 마스킹한다.

## 6. Discord 알림에 자동으로 실리는 정보

`log.error(...)`를 어디서 호출하든(직접 호출/`ExceptionAdvice`의 catch-all/`AsyncConfig`의 비동기 예외 핸들러 등) 별도 코드 없이 Discord 알림에 다음이 자동으로 포함된다:
- 예외 클래스명(title), 로그 메시지(description), Logger/Thread/발생시간, 스택트레이스(최대 15줄)
- HTTP 요청 스레드에서 난 에러면 호출 API(`RequestContextLoggingFilter`가 MDC에 심어둔 값) — 스케줄러/비동기처럼 요청 컨텍스트가 없으면 안내 문구로 대체됨

그러니 메시지에 굳이 "어느 API에서 났는지"를 반복해서 적을 필요는 없다 — `[메소드명]` 접두사와 함께라면 이미 충분한 컨텍스트가 자동으로 붙는다.
