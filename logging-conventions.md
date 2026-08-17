# 로깅 컨벤션

## 왜 이 문서가 필요한가

`log.error(...)`로 찍히는 모든 로그는 비동기 queue를 거쳐 Discord 웹훅으로 전송된다(`global/logging` 패키지, `logback-spring.xml` 참고 — `DISCORD_WEBHOOK_URL` 설정 시 활성화, 전송 성공한 동일 알림은 5분간 중복 억제, 분당 20건 상한). 즉 **ERROR 로그 하나하나가 팀 채널의 실시간 알림 대상**이다. 이 전제 때문에 일반적인 로깅 습관보다 조금 더 엄격한 규칙이 필요하다:
- 노이즈(예상된/반복되는 실패)를 ERROR로 찍으면 알림 피로가 생긴다.
- 로그 메시지·스택트레이스에 민감정보가 섞이면 그대로 외부 웹훅으로 나간다.

새 코드를 작성하거나 기존 로그를 수정할 때 아래 규칙을 따른다.

## 1. 레벨 선택

- **ERROR** — 즉시 조치가 필요한 단일 실패 또는 WARN 대상 실패가 한 작업 안에서 누적된 최종 집계만 남긴다. 재시도로 해결되는 중간 단계, 클라이언트 입력에 따른 정상적인 실패는 ERROR가 아니다.
- **WARN** — 당장 처리를 중단할 필요는 없지만 추후 빈도와 추세를 확인해야 하는 실패, 재시도 중간 단계(`@Retryable`의 각 시도 실패 등), 복구 가능한 경합 상황. 추후 Sentry에서 WARN 누적을 집계할 수 있도록 안전한 식별자와 실패 유형을 포함한다.
- **INFO** — 정상 처리 흐름 중 장애 분석이나 운영 상태 확인에 필요한 이벤트를 남긴다. 배치 실행 결과, 외부 연동 결과, 비동기 이벤트 수신, 주요 데이터 상태 변경이 대상이다.

**예외**: 요청마다(또는 아주 높은 빈도로) 호출되는 코드의 "정상적으로 흔한" 실패는 WARN조차 남기지 않는 게 맞을 수 있다(예: `JwtService.validateToken()` — 매 인증 요청마다 호출되며 토큰 만료는 지극히 흔한 일이라 로그를 넣으면 정상 트래픽만으로 도배된다). 호출 빈도를 먼저 확인할 것.

다음 상황은 기본적으로 로그를 남기지 않는다.

- 조회 결과 없음, 중복 요청 차단, 이미 처리된 상태처럼 정상 분기에 해당하는 경우
- 인증 실패, 만료된 token, 외부 API의 예상 가능한 4xx처럼 호출자에게 오류 응답으로 충분한 경우
- 단순 CRUD 요청의 진입·종료나 목록 조회처럼 호출량에 비례하지만 추가 진단 정보가 없는 정상 흐름
- 상위 처리 경로에서 최종 실패를 기록하는 재시도·fallback의 중간 성공 또는 중간 실패

배치·비동기·외부 연동은 실행 여부 자체가 장애 분석에 필요할 수 있다. 이 경우 시작과 건별 성공을 여러 줄 남기기보다 `targetCount`, `successCount`, `failedCount`가 포함된 완료 요약을 우선한다. 다만 트랜잭션 commit 이후 이벤트 수신, fallback 적용처럼 실행 경계 확인이 중요한 지점은 개별 INFO를 남길 수 있다.

## 2. 메시지 형식 — `[methodName] 한글 설명. key={}`

모든 신규 로그는 아래 형식을 사용한다.

```java
log.error("[socialLogin] Kakao 로그인 중 예상하지 못한 오류가 발생했습니다.", e);
log.warn("[dropNonContinuersAt] 드랍 처리에 실패했습니다. roundId={}", roundId, e);
```

- 접두사는 실제 메서드명을 영어 `camelCase`로 적는다. 같은 이름이 여러 클래스에 있어 구분이 어려울 때만 `[ClassName.methodName]`을 사용한다.
- 설명은 한글 완결형 문장으로 작성하고 마침표로 끝낸다.
- 메서드명, 클래스명, entity/field 이름과 기술 용어는 원래 영어 표기를 유지한다. 예: `DB`, `Redis`, `API`, `FCM`, `S3`, `JWT`, `User`, `NotificationEvent`, `userId`.
- `타깃`, `타켓`, `target`을 설명 문장에 혼용하지 않고 **대상**으로 통일한다. 구조화된 key 이름(`targetDate`)은 코드의 영어 이름을 유지한다.
- 동적 값은 문장에 이어 `key={}` 형태로 적고, `ID: {}`, `(원인: {})`, `실패 -` 같은 변형은 사용하지 않는다.
- 예외 객체를 마지막 인자로 전달했다면 `error={}`에 `e.getMessage()`를 다시 넣지 않는다. 스택 트레이스에 같은 정보가 포함된다.

```java
log.error("[migrateRedisToLogTable] Redis 데이터를 DB로 마이그레이션하는 중 오류가 발생했습니다. targetHour={}",
        targetHourKey, e);
```

나쁜 예:

```java
log.error("[Retry Failed] migration error - Target: {}, reason: {}", target, e.getMessage(), e);
```

## 3. 한 실패는 한 곳에서만 ERROR로 기록

- `GlobalException`으로 감싸 던질 때는 원인을 체이닝한다: `new GlobalException(ErrorCode.X, e)` (단, 5번 보안 규칙에 해당하면 체이닝하지 않는다).
- 하위 계층이 원인을 충분히 알고 있으면 그곳에서 한 번 기록하고 `GlobalException`으로 변환한다. 상위 계층은 `catch (GlobalException e) { throw e; }`로 전달만 한다.
- 하위 계층이 기록하지 않은 예상 밖 예외만 상위 계층에서 ERROR로 기록한다.
- ERROR로 기록한 원본 예외를 그대로 `throw e`하면 framework 또는 `ExceptionAdvice`가 다시 기록할 수 있다. 중복되지 않도록 예외 소유 지점을 명확히 정한다.
- 외부 API client와 이를 호출하는 service가 같은 실패를 각각 기록하지 않는다. client가 네트워크 실패를 기록했다면 service는 해당 `GlobalException`을 다시 기록하지 않는다.

## 4. 배치/루프 처리 — 건별 ERROR 금지

루프 안에서 개별 항목 처리가 실패해도 다음 항목을 계속 처리하는 패턴(스케줄러 등)에서는, 건별 실패를 WARN으로 남기고 **루프가 끝난 뒤 실패 건수가 0보다 클 때만 ERROR로 한 번 집계**한다. 최종 ERROR에는 Discord에서 원인을 확인할 수 있도록 첫 번째 대표 예외를 마지막 인자로 전달한다.

```java
int failCount = 0;
Exception firstFailure = null;
for (Round round : rounds) {
    try {
        process(round);
    } catch (Exception e) {
        log.warn("[dropNonContinuersAt] 드랍 처리에 실패했습니다. roundId={}", round.getId(), e);
        failCount++;
        if (firstFailure == null) {
            firstFailure = e;
        }
    }
}
if (failCount > 0) {
    log.error("[dropNonContinuersAt] 드랍 처리 대상 총 {}건 중 {}건을 실패했습니다.",
            rounds.size(), failCount, firstFailure);
}
```

## 5. 보안 — 로그에 절대 남기면 안 되는 것

ERROR 로그는 외부 Discord 웹훅으로 나간다는 걸 항상 염두에 둔다.

- **원본 토큰/시크릿/비밀번호/private key**를 로그 메시지에 직접 넣지 않는다.
- **DB 원문, 외부 API 응답 body, 요청 DTO/비동기 메서드 인자 전체, S3 object key**를 통째로 남기지 않는다. 필요한 경우 상태 코드, 개수, boolean 존재 여부, 내부 식별자만 남긴다.
- **시크릿이 URL 쿼리 파라미터에 포함된 요청에서 발생한 예외는 `e`를 그대로 로깅하거나 체이닝하지 않는다.** Spring의 `RestClientException`(특히 `ResourceAccessException`) 등은 실패한 요청의 **URL 전체**를 예외 메시지에 그대로 담기 때문에, `log.error(msg, e)`로 넘기는 순간 스택트레이스를 통해 시크릿이 그대로 노출된다. 이런 경로에서는 `e.getClass().getSimpleName()`만 안전하게 남긴다.
  - 실제 사례: `NaverAuthService`가 `client_secret`/`access_token`/`refresh_token`을 쿼리 파라미터로 보내는데, 네트워크 에러 시 `e`를 그대로 로깅해서 Discord로 시크릿이 나갈 뻔했다. 가능하면 애초에 시크릿을 URL이 아니라 요청 바디/헤더로 보내는 것(Kakao/Apple 연동이 이 방식)이 근본적으로 더 안전하다.
- **디코딩한 JWT payload 전체를 로깅하지 않는다** — Apple id_token 등은 email 같은 PII를 담고 있을 수 있다. 필요하면 어떤 필드가 있는지(키 목록)만 남긴다.
- **요청 헤더/파라미터를 통째로 덤프하지 않는다.** 디버그 목적으로 꼭 필요하면 값 공개가 안전하다고 명시한 allowlist 외에는 전부 마스킹한다.
- 외부 시스템이 반환한 `error_description`이나 예외 메시지는 사용자 입력을 포함할 수 있으므로 그대로 출력하지 않는다. 허용된 error code, HTTP status, 예외 클래스명처럼 범위가 정해진 값만 사용한다.

## 6. Discord 알림에 자동으로 실리는 정보

`log.error(...)`를 어디서 호출하든(직접 호출/`ExceptionAdvice`의 catch-all/`AsyncConfig`의 비동기 예외 핸들러 등) 별도 코드 없이 Discord 알림에 다음이 자동으로 포함된다:
- 예외 클래스명(title), 로그 메시지(description), Logger/Thread/발생시간, 스택트레이스(최대 15줄)
- HTTP 요청 스레드에서 난 에러면 HTTP method와 controller handler 이름(예: `POST ChallengeController.joinChallenge`) — raw URL, route, path/query 값은 외부로 보내지 않음
- 최종 payload 생성 시 token/secret/password와 주요 PII, Authorization/Cookie, JWT, private key, Discord webhook URL 패턴을 한 번 더 마스킹함
- 예외 메시지는 사용자 입력이나 외부 응답을 포함할 수 있으므로 Discord stack trace에서는 제외하고 예외 클래스와 frame만 전송함

최종 마스킹은 실수에 대한 방어선일 뿐이므로 민감정보를 로그 인자로 넘겨도 된다는 뜻은 아니다. 메시지에 굳이 "어느 API에서 났는지"를 반복해서 적을 필요도 없다. `[methodName]` 접두사와 자동 요청 컨텍스트면 충분하다.

Discord가 429·5xx로 응답하거나 일시적인 네트워크 오류가 나면 최대 3회까지 재시도한다. 429는 Discord가 지정한 `Retry-After` 전체 시간을 지키며, 401·403·404처럼 webhook 설정이 복구되지 않는 응답은 appender를 재시작할 때까지 전송 회로를 차단한다. 최종 실패, 분당 상한 초과, queue 포화로 전송하지 못한 이벤트는 dedup 예약을 해제하여 다음 동일 오류가 즉시 다시 시도할 수 있게 한다. 각 유실 유형의 누적 건수는 `DiscordDeliveryMonitor` 내부 logger에 남기며, 재귀 알림을 막기 위해 이 logger 자체는 Discord 전송 대상에서 제외한다.
