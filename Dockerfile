# Etap 1: Build bez testów
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Buduj tylko .jar bez testów
RUN mvn clean package -DskipTests

# Etap 2: Uruchamialny obraz
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=builder /app/target/cinema-reservation-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]
