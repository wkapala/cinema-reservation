# 🎬 Cinema Reservation System

> **Zaawansowany system rezerwacji biletów kinowych** zbudowany z wykorzystaniem najnowszych technologii i wzorców projektowych

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Coverage](https://img.shields.io/badge/Coverage-89%25-brightgreen.svg)](target/site/jacoco/index.html)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Spis treści

- [🎯 Opis projektu](#-opis-projektu)
- [🏗️ Architektura i wzorce projektowe](#️-architektura-i-wzorce-projektowe)
- [🔐 System autoryzacji (RBAC)](#-system-autoryzacji-rbac)
- [🚀 Technologie](#-technologie)
- [🐳 Szybki start z Docker](#-szybki-start-z-docker)
- [📚 API Documentation](#-api-documentation)
- [🗄️ Model bazy danych](#️-model-bazy-danych)
- [🔧 Konfiguracja środowiska](#-konfiguracja-środowiska)
- [🧪 Testowanie](#-testowanie)
- [📸 Zrzuty ekranu](#-zrzuty-ekranu)

---

## 🎯 Opis projektu

**Cinema Reservation System** to nowoczesna aplikacja webowa umożliwiająca kompleksowe zarządzanie rezerwacjami w kinach multipleksowych. System został zaprojektowany z myślą o skalowalności, bezpieczeństwie i łatwości użytkowania.

### ✨ Kluczowe funkcjonalności

- 🎫 **Rezerwacja biletów** - zaawansowany system wyboru miejsc i zarządzania rezerwacjami
- 🏢 **Zarządzanie kinami** - obsługa sieci kin z wieloma salami projekcyjnymi
- 🎬 **Katalog filmów** - kompletna baza filmów z metadanymi i seansami
- 👥 **Zarządzanie użytkownikami** - system ról z rozróżnieniem na klientów i administratorów
- 📊 **Raportowanie** - zaawansowane statystyki i analytics
- 🔒 **Bezpieczeństwo** - implementacja Spring Security z autentykacją Basic Auth

---

## 🏗️ Architektura i wzorce projektowe

Projekt został zbudowany zgodnie z **filarami obiektowości** i **zasadami SOLID**, implementując sprawdzone wzorce projektowe:

### 🎨 Wzorce projektowe

#### 1. **Factory Pattern** 🏭
```java
@Component
public class UserFactory {
    public User createUser(UserCreateRequest request) {
        return switch (request.getUserType()) {
            case REGULAR -> RegularUser.builder()
                .email(request.getEmail())
                .build();
            case ADMIN -> AdminUser.builder()
                .email(request.getEmail())
                .department(request.getDepartment())
                .build();
        };
    }
}
```

#### 2. **Strategy Pattern** 🎯
Implementowany w Spring Security dla różnych strategii autoryzacji użytkowników.

#### 3. **Template Method Pattern** 📋
```java
public abstract class User {
    // Template method definiujący wspólny interfejs
    public abstract List<String> getRoles();
    public abstract boolean canMakeReservation();
    public abstract boolean canManageMovies();
}
```

### 🔧 Polimorfizm w akcji

System wykorzystuje **polimorfizm** poprzez hierarchię klas użytkowników:

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type")
public abstract class User {
    // Wspólne pola i metody
}

@Entity
@DiscriminatorValue("REGULAR")
public class RegularUser extends User {
    // Implementacja dla zwykłych użytkowników
}

@Entity  
@DiscriminatorValue("ADMIN")
public class AdminUser extends User {
    // Implementacja dla administratorów
}
```

---

## 🔐 System autoryzacji (RBAC)

System implementuje **Role-Based Access Control** z dwoma poziomami uprawnień:

### 👤 Role użytkowników

| Rola | Opis | Uprawnienia |
|------|------|-------------|
| **USER** | Zwykły klient kina | • Przeglądanie filmów i seansów<br>• Tworzenie rezerwacji<br>• Zarządzanie własnymi rezerwacjami |
| **ADMIN** | Administrator systemu | • Wszystkie uprawnienia USER<br>• Zarządzanie kinami i salami<br>• Zarządzanie filmami i seansami<br>• Potwierdzanie/anulowanie rezerwacji<br>• Dostęp do statystyk |

### 🛡️ Implementacja bezpieczeństwa

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // Publiczne endpointy
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/screenings/**").permitAll()
                
                // Endpointy wymagające autoryzacji
                .requestMatchers("/api/reservations/**").hasAnyRole("USER", "ADMIN")
                
                // Endpointy tylko dla administratorów
                .requestMatchers("/api/movies/**").hasRole("ADMIN")
                .requestMatchers("/api/cinemas/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
```

### 🔑 Proces autoryzacji

1. **Logowanie** - Basic Auth z email/hasło
2. **Weryfikacja** - Spring Security sprawdza dane w bazie
3. **Przyznanie ról** - na podstawie typu użytkownika
4. **Autoryzacja** - dostęp do endpointów zgodnie z rolą

**Przykład autoryzacji:**
```http
Authorization: Basic <base64(email:password)>
```

---

## 🚀 Technologie

### 🎯 Backend Stack
- **Java 21** - najnowsza wersja LTS
- **Spring Boot 3.x** - framework aplikacyjny
- **Spring Security 6** - bezpieczeństwo i autoryzacja
- **Spring Data JPA** - warstwa dostępu do danych
- **Hibernate** - ORM (Object-Relational Mapping)

### 🗄️ Baza danych
- **PostgreSQL 15** - relacyjna baza danych
- **Flyway** - migracje bazy danych
- **HikariCP** - connection pooling

### 🐳 DevOps & Deploy
- **Docker & Docker Compose** - konteneryzacja
- **Maven** - zarządzanie zależnościami i build

### 📚 Dokumentacja & Testowanie
- **Swagger UI (SpringDoc OpenAPI)** - interaktywna dokumentacja API
- **JUnit 5** - testy jednostkowe i integracyjne
- **Testcontainers** - testy integracyjne z prawdziwą bazą
- **JaCoCo** - analiza pokrycia kodu testami (**89% coverage!**)

---

## 🐳 Szybki start z Docker

### Wymagania
- Docker Desktop
- Docker Compose

### 🚀 Uruchomienie

```bash
# Klonowanie repozytorium
git clone <repository-url>
cd cinema-reservation

# Uruchomienie całego stosu
docker-compose up --build

# Aplikacja będzie dostępna na:
# 🌐 API: http://localhost:8080
# 📚 Swagger UI: http://localhost:8080/swagger-ui.html
# 🗄️ PostgreSQL: localhost:5432
```

### 🔧 Konfiguracja Docker

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - database

  database:
    image: postgres:15
    environment:
      POSTGRES_DB: cinema_db
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: secret
    ports:
      - "5432:5432"
```

---

## 📚 API Documentation

### 🎬 Główne moduły API

#### 🎭 Movies Management
```http
GET    /api/movies              # Lista wszystkich filmów (public)
GET    /api/movies/{id}         # Szczegóły filmu (public)
GET    /api/movies/search       # Wyszukiwanie filmów (public)
POST   /api/movies              # Dodanie filmu (admin only)
PUT    /api/movies/{id}         # Edycja filmu (admin only)
DELETE /api/movies/{id}         # Usunięcie filmu (admin only)
```

#### 🏢 Cinema Management
```http
GET    /api/cinemas             # Lista kin (public)
GET    /api/cinemas/{id}        # Szczegóły kina (public)
GET    /api/cinemas/city/{city} # Kina w mieście (public)
POST   /api/cinemas             # Dodanie kina (admin only)
POST   /api/cinemas/{id}/halls  # Dodanie sali (admin only)
```

#### 🎫 Screenings Management
```http
GET    /api/screenings          # Wszystkie seanse (public)
GET    /api/screenings/upcoming # Nadchodzące seanse (public)
GET    /api/screenings/available # Seanse z wolnymi miejscami (public)
POST   /api/screenings          # Dodanie seansu (admin only)
PUT    /api/screenings/{id}     # Edycja seansu (admin only)
```

#### 🎟️ Reservations Management
```http
GET    /api/reservations/{id}           # Szczegóły rezerwacji (authenticated)
GET    /api/reservations/user/{userId}  # Rezerwacje użytkownika (authenticated)
POST   /api/reservations                # Nowa rezerwacja (authenticated)
PUT    /api/reservations/{id}/confirm   # Potwierdzenie (admin only)
PUT    /api/reservations/{id}/cancel    # Anulowanie (admin only)
```

#### 👥 User Management
```http
GET    /api/users/{id}          # Profil użytkownika (authenticated)
POST   /api/users/register      # Rejestracja (public)
PUT    /api/users/{id}          # Edycja profilu (authenticated)
```

### 🔐 Autoryzacja w API

Wszystkie chronione endpointy wymagają nagłówka:
```http
Authorization: Basic <base64(email:password)>
```

**Uwaga:** Musisz najpierw zarejestrować użytkownika przez `POST /api/users/register` lub utworzyć go przez panel administracyjny.

### 📋 Przykład użycia

#### Tworzenie rezerwacji
```http
POST /api/reservations
Authorization: Basic <base64(email:password)>
Content-Type: application/json

{
  "userId": 1,
  "screeningId": 4,
  "seats": [
    {"rowNumber": 5, "seatNumber": 10},
    {"rowNumber": 5, "seatNumber": 11}
  ]
}
```

#### Odpowiedź
```json
{
  "id": 1,
  "reservedSeats": [
    {
      "id": 1,
      "rowNumber": 5,
      "seatNumber": 10,
      "seatDisplay": "E10"
    }
  ],
  "totalPrice": 51.00,
  "status": "PENDING",
  "confirmationCode": "RES1748732897310385",
  "createdAt": "2025-06-01T23:08:17.310956"
}
```

---

## 🗄️ Model bazy danych

### 📊 Diagram ERD

![Zrzut ekranu 2025-06-01 020423](https://github.com/user-attachments/assets/c68cfb9a-09be-4e3a-b666-6ba3ab7ac3c4)

### 🏗️ Struktura tabel

#### 👥 Users (Single Table Inheritance)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_type VARCHAR(50) NOT NULL,     -- 'REGULAR' | 'ADMIN'
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    
    -- RegularUser fields
    phone_number VARCHAR(20),
    
    -- AdminUser fields  
    department VARCHAR(100),
    admin_level VARCHAR(50),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 🎬 Movies
```sql
CREATE TABLE movies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    genre VARCHAR(50),              -- ACTION, COMEDY, DRAMA, etc.
    director VARCHAR(255) NOT NULL,
    poster_url VARCHAR(500),
    rating DECIMAL(3,1),           -- 0.0 - 10.0
    release_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 🏢 Cinemas & Halls
```sql
CREATE TABLE cinemas (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100),
    phone_number VARCHAR(20)
);

CREATE TABLE cinema_halls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    total_seats INTEGER NOT NULL,
    rows INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    hall_type VARCHAR(50),          -- STANDARD, IMAX, VIP, DOLBY_ATMOS
    cinema_id BIGINT NOT NULL,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
);
```

#### 🎫 Screenings & Reservations
```sql
CREATE TABLE screenings (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    available_seats INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (hall_id) REFERENCES cinema_halls(id) ON DELETE CASCADE
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    screening_id BIGINT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, CANCELLED
    confirmation_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (screening_id) REFERENCES screenings(id) ON DELETE CASCADE
);
```

### 🔗 Kluczowe relacje

- **User** 1:N **Reservation** - użytkownik może mieć wiele rezerwacji
- **Cinema** 1:N **CinemaHall** - kino może mieć wiele sal
- **Movie** 1:N **Screening** - film może mieć wiele seansów
- **CinemaHall** 1:N **Screening** - sala może mieć wiele seansów
- **Screening** 1:N **Reservation** - seans może mieć wiele rezerwacji
- **Reservation** 1:N **ReservedSeat** - rezerwacja może obejmować wiele miejsc

---

## 🔧 Konfiguracja środowiska

### 📝 Application Properties

```properties
# --- Database Configuration ---
spring.datasource.url=jdbc:postgresql://database:5432/cinema_db
spring.datasource.username=admin
spring.datasource.password=secret

# --- JPA / Hibernate Configuration ---
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# --- Flyway Configuration ---
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# --- Swagger Configuration ---
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.persist-authorization=false

# --- Security Configuration ---
spring.jackson.serialization.fail-on-empty-beans=false
```

### 🔐 Security Features

- **Password Encoding** - BCrypt hashing
- **CORS** - skonfigurowane dla rozwoju
- **CSRF** - wyłączone dla API REST
- **Session Management** - stateless (JWT-ready)

---

## 🧪 Testowanie

### 📊 Pokrycie testami: **89%** ✅

```bash
# Uruchomienie wszystkich testów
mvn clean test

# Uruchomienie testów z raportem pokrycia
mvn clean verify

# Raport JaCoCo dostępny w:
# target/site/jacoco/index.html
```

### 🏗️ Struktura testów

#### Unit Tests
- **Service Layer Tests** - testowanie logiki biznesowej
- **Controller Tests** - testowanie warstwy API z mockami
- **Repository Tests** - testowanie zapytań do bazy

#### Integration Tests  
- **Controller Integration Tests** - pełne testy API z Testcontainers
- **Database Integration Tests** - testowanie migracji i zapytań
- **Security Integration Tests** - testowanie autoryzacji

### 🐳 Testcontainers

```java
@Testcontainers
class CinemaControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("cinema_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    // Testy używają prawdziwej bazy PostgreSQL w kontenerze
}
```

---

## 📸 Zrzuty ekranu

### 🌐 Swagger UI - Główny panel

![Zrzut ekranu 2025-06-01 023836](https://github.com/user-attachments/assets/d19b25b8-cf86-4a2b-bdda-842f8c8e0a55)

*Interaktywna dokumentacja API z wszystkimi dostępnymi endpointami*

### 🔐 Autoryzacja w Swagger

![Zrzut ekranu 2025-06-01 023906](https://github.com/user-attachments/assets/60348c0c-56ba-40f2-a62a-36430056e72c)

*Panel autoryzacji Basic Auth - wystarczy kliknąć "Authorize" i podać dane logowania*

### 🎬 API Movies - Przeglądanie filmów

![Zrzut ekranu 2025-06-01 023954](https://github.com/user-attachments/assets/8bb0e427-42d4-4952-ade6-9b56fe805a06)

*Endpoint do przeglądania filmów - dostępny publicznie*

### 🎫 API Reservations - Tworzenie rezerwacji

![Zrzut ekranu 2025-06-01 024017](https://github.com/user-attachments/assets/9ad1d2f3-ab9f-4bdc-a308-a9592b4b2ce8)

*Tworzenie nowej rezerwacji - wymaga autoryzacji*

### 📊 Coverage Report

![Zrzut ekranu 2025-06-01 020226](https://github.com/user-attachments/assets/6d83150a-cc51-4334-ba88-7df71bc39e9f)

*Raport pokrycia kodu testami - 89% coverage*

---

## 🎯 Najważniejsze cechy projektu

### ✨ **Wzorce projektowe w akcji**
- **Factory Pattern** - tworzenie użytkowników
- **Strategy Pattern** - różne strategie autoryzacji
- **Template Method** - abstrakcyjna klasa User

### 🔒 **Bezpieczeństwo na najwyższym poziomie**
- Spring Security 6 z Basic Auth
- Role-Based Access Control (RBAC)
- Szyfrowanie haseł BCrypt
- Stateless session management

### 🏗️ **Architektura zgodna z SOLID**
- **Single Responsibility** - każda klasa ma jedną odpowiedzialność
- **Open/Closed** - otwarte na rozszerzenia, zamknięte na modyfikacje
- **Liskov Substitution** - polimorfizm User/RegularUser/AdminUser
- **Interface Segregation** - małe, wyspecjalizowane interfejsy
- **Dependency Inversion** - zależności przez interfejsy

### 📊 **Jakość kodu**
- **89% test coverage** - znacznie powyżej wymaganego 80%
- **245 testów** - unit i integration tests
- **Clean Code** - czytelny, dobrze udokumentowany kod
- **Docker-ready** - gotowe do deployment

---

## 🚀 Uruchomienie projektu

### 1. Wymagania
- Java 17+
- Docker Desktop
- Git

### 2. Instalacja
```bash
git clone <repository-url>
cd cinema-reservation
docker-compose up --build
```

### 3. Dostęp do aplikacji
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Database**: localhost:5432 (admin/secret)

### 4. Pierwsze kroki
1. Otwórz Swagger UI: http://localhost:8080/swagger-ui.html
2. Zarejestruj użytkownika przez `POST /api/users/register`
3. Użyj danych logowania w przycisku "Authorize" w Swagger UI
4. Przetestuj API endpointy

---

## 📞 Kontakt

**Autor**: Wojciech Kapała

**Email**: wokapala@gmail.com  
**GitHub**: https://github.com/wkapala

---

## 📜 Licencja

Ten projekt jest udostępniony na licencji MIT. Zobacz plik `LICENSE` po więcej szczegółów.

---

*Projekt zrealizowany w ramach przedmiotu Programowanie w języku Java na Politechnice Krakowskiej*
