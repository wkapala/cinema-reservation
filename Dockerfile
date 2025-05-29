# Etap 1: Build + Test + JaCoCo
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Skopiuj tylko pliki niezbędne do budowania
COPY pom.xml .
COPY src ./src

# Uruchom budowanie z testami i raportem pokrycia
RUN mvn clean verify

# Etap 2: Runtime – tylko JAR
FROM openjdk:21-jdk-slim
WORKDIR /app

# Kopiuj tylko wynikowy JAR (nie całe target/)
COPY --from=builder /app/target/cinema-reservation-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]
