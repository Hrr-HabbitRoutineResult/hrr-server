#!/bin/bash

# --- 환경 설정 ---
# Secrets Manager 정보
SECRET_ID="hrr-official"
REGION="ap-northeast-2"
# Docker Compose 파일 경로
COMPOSE_FILE="docker-compose_prod.yml"
# 프록시 할 포트 정보 담긴 파일
ENV_INC_FILE="./service-env.inc"

set +o histexpand       # 비밀번호의 특수문자 누락 방지

# --- 배포 버전 인자값 확인 ---
# 스크립트 실행 시 첫 번째 인자로 태그를 받습니다. (예: ./deploy.sh v1.0.1)
RELEASE_TAG=$1

if [ -z "$RELEASE_TAG" ]; then
    echo "WARNING: 배포 태그가 지정되지 않았습니다. 기본값(latest)을 사용합니다."
    export IMAGE_TAG="latest"
else
    echo "INFO: ${RELEASE_TAG} 버전을 배포합니다."
    export IMAGE_TAG="${RELEASE_TAG}"
fi

# AWS CLI 설치 및 jq 확인
if ! command -v aws &> /dev/null || ! command -v jq &> /dev/null; then
    echo "ERROR: AWS CLI 또는 jq가 설치되어 있지 않습니다." >&2
    echo "설치 후 다시 시도하세요. (예: sudo apt install awscli jq)" >&2
    exit 1
fi

# --- AWS Secrets Manager에서 Secrets 가져오기 ---
echo "--- 1/6: Secrets Manager에서 정보 가져오기 ---"
SECRET_JSON=$(aws secretsmanager get-secret-value \
    --secret-id "${SECRET_ID}" \
    --region "${REGION}" \
    --query "SecretString" \
    --output text 2>/dev/null)

if [ $? -ne 0 ] || [ -z "$SECRET_JSON" ]; then
    echo "ERROR: Secrets Manager 정보 로드 실패. IAM 권한/Secret ID를 확인하세요." >&2
    exit 1
fi

# --- 환경 변수 설정 (쉘에 Export) ---
echo "--- 2/6: 환경 변수 설정하기 ---"

# 쉘 환경 변수로 export (jq로 JSON 값 추출)
# 모든 ${변수명}을 Secrets Manager의 JSON 키와 매칭하여 정의해야 함
export RDS_URL=$(echo "$SECRET_JSON" | jq -r '.RDS_URL')
export RDS_USERNAME=$(echo "$SECRET_JSON" | jq -r '.RDS_USERNAME')
export RDS_PASSWORD=$(echo "$SECRET_JSON" | jq -r '.RDS_PASSWORD' | tr -d '\n\r'  )
export PROD_BASE_URL=$(echo "$SECRET_JSON" | jq -r '.PROD_BASE_URL')
export JWT_SECRET=$(echo "$SECRET_JSON" | jq -r '.JWT_SECRET')
export KAKAO_CLIENT_ID=$(echo "$SECRET_JSON" | jq -r '.KAKAO_CLIENT_ID')
export KAKAO_CLIENT_SECRET=$(echo "$SECRET_JSON" | jq -r '.KAKAO_CLIENT_SECRET')
export PROD_KAKAO_REDIRECT_URI=$(echo "$SECRET_JSON" | jq -r '.PROD_KAKAO_REDIRECT_URI')
export SPRING_PROFILES_ACTIVE=prod # Spring Profile 설정
export FIREBASE_SERVICE_ACCOUNT_JSON=$(echo "$SECRET_JSON" | jq -r '.FIREBASE_SERVICE_ACCOUNT_JSON')
export KAKAO_APP_REDIRECT_URI=$(echo "$SECRET_JSON" | jq -r '.KAKAO_APP_REDIRECT_URI')
export KAKAO_ADMIN_KEY=$(echo "$SECRET_JSON" | jq -r '.KAKAO_ADMIN_KEY')
export APPLE_TEAM_ID=$(echo "$SECRET_JSON" | jq -r '.APPLE_TEAM_ID')
export APPLE_CLIENT_ID=$(echo "$SECRET_JSON" | jq -r '.APPLE_CLIENT_ID')
export APPLE_KEY_ID=$(echo "$SECRET_JSON" | jq -r '.APPLE_KEY_ID')
export APPLE_P8_KEY=$(echo "$SECRET_JSON" | jq -r '.APPLE_P8_KEY')
export NAVER_CLIENT_ID=$(echo "$SECRET_JSON" | jq -r '.NAVER_CLIENT_ID')
export NAVER_CLIENT_SECRET=$(echo "$SECRET_JSON" | jq -r '.NAVER_CLIENT_SECRET')
export MODEL_SERVER_URL=$(echo "$SECRET_JSON" | jq -r '.MODEL_SERVER_URL')

# --- 3/6: 배포 대상(Blue/Green) 결정 ---
echo "--- 3/6: 배포 대상 확인 ---"
# 파일이 없으면 기본값으로 생성
if ! [ -f "$ENV_INC_FILE" ]; then
    echo "set \$service_url http://app-blue:8080;" > "$ENV_INC_FILE"
fi

# 안전하게 현재 서비스 판별
CURRENT_SERVICE="$(grep -o -m1 'app-blue\|app-green' "$ENV_INC_FILE" || true)"

if [ -z "$CURRENT_SERVICE" ]; then
  echo "ERROR: $ENV_INC_FILE 에서 현재 서비스(app-blue/app-green)를 판별하지 못했습니다." >&2
  echo "       파일 내용: $(sed -n '1,5p' "$ENV_INC_FILE")" >&2
  exit 1
fi

if [ "$CURRENT_SERVICE" == "app-blue" ]; then
    TARGET_COLOR="green"
    TARGET_SERVICE="app-green"
    OLD_SERVICE="app-blue"
else
    TARGET_COLOR="blue"
    TARGET_SERVICE="app-blue"
    OLD_SERVICE="app-green"
fi

echo "현재 서비스: $OLD_SERVICE -> 신규 배포 대상: $TARGET_SERVICE"

# --- 4/6: 신규 버전 배포 및 헬스 체크 ---
echo "--- 4/6: $TARGET_SERVICE 이미지 Pull 및 실행 ---"
docker compose -f "${COMPOSE_FILE}" pull "$TARGET_SERVICE" || {
       echo "ERROR: docker compose pull 실패. 로그를 확인하세요." >&2
       exit 1
     }
if ! docker compose -f "${COMPOSE_FILE}" up -d "$TARGET_SERVICE"; then
  echo "ERROR: docker compose up 실패. 로그를 확인하세요." >&2
  exit 1
fi

echo "새 컨테이너($TARGET_SERVICE) 헬스 체크 중..."
for i in {1..25}; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' "hrr_app_$TARGET_COLOR" 2>/dev/null)
    if [ "$STATUS" == "healthy" ]; then
        echo "SUCCESS: 새 컨테이너가 정상 실행되었습니다."
        break
    fi
    echo "대기 중... ($i/25)"
    sleep 5
    if [ $i -eq 25 ]; then
        echo "ERROR: 헬스 체크 실패. 배포를 중단합니다."
        exit 1
    fi
done

# --- 5/6: 트래픽 전환 (Nginx Reload) ---
echo "--- 5/6: 트래픽 전환 ---"
echo "set \$service_url http://$TARGET_SERVICE:8080;" > "$ENV_INC_FILE"
docker exec hrr_nginx_prod nginx -s reload

# --- 6/6: 이전 컨테이너 정리 (디스크 공간 확보; 실패해도 배포 자체는 성공으로 간주) ---
echo "--- 6/6: 이전 버전($OLD_SERVICE) 종료 및 정리 ---"
docker compose -f "${COMPOSE_FILE}" stop "$OLD_SERVICE"
docker image prune -f || echo "WARN: docker image prune 실패(디스크 정리만 실패)."

echo "배포 완료! 현재 활성 서비스: $TARGET_SERVICE (Tag: $IMAGE_TAG)"
echo "상태 확인: sudo docker ps"
