FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S circleguard && adduser -S circleguard -G circleguard
USER circleguard
COPY services/circleguard-notification-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]