FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
RUN mkdir -p /app/secrets
ARG JAR_FILE=build/libs/*SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
