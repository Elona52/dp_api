# 멀티 스테이지 빌드를 사용하여 최적화된 이미지 생성

# Stage 1: 빌드 스테이지
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean bootJar --no-daemon

# Stage 2: 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 실행에 필요한 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 타임존 설정 및 헬스체크 도구 설치 (한국 시간)
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 포트 노출 (Spring Boot 기본 포트)
EXPOSE 8080

# 헬스체크 추가 (기본 엔드포인트로 확인, actuator가 없어도 동작)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]

