# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY services/circleguard-identity-service services/circleguard-identity-service

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-identity-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S circleguard && adduser -S circleguard -G circleguard
USER circleguard

COPY --from=builder /workspace/services/circleguard-identity-service/build/libs/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
