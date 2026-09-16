# ── 빌드 단계 ──
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 관련 파일만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ── 실행 단계 ──
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]