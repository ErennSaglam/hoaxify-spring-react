# Kursundaki Dockerfile'ın aynısı: önce ./gradlew bootJar, sonra docker build
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY build/libs/config-server-0.0.1.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
