FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S circleguard && adduser -S circleguard -G circleguard
USER circleguard
COPY services/circleguard-promotion-service/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]