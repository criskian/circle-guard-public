FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S circleguard && adduser -S circleguard -G circleguard
USER circleguard
COPY services/circleguard-auth-service/build/libs/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java", "-jar", "/app/app.jar"]