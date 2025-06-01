package com.cinema.reservation.controller;

import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.Cinema;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.repository.CinemaHallRepository;
import com.cinema.reservation.repository.CinemaRepository;
import com.cinema.reservation.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class CinemaControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("cinema_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private CinemaHallRepository cinemaHallRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminUser adminUser;
    private RegularUser regularUser;
    private Cinema testCinema;
    private Cinema anotherCinema;
    private CinemaHall testHall;
    private String adminEmail;
    private String regularUserEmail;

    @BeforeEach
    void setUp() {
        // Clean data
        cinemaHallRepository.deleteAll();
        cinemaRepository.deleteAll();
        userRepository.deleteAll();

        // Create unique emails for this test class
        String uniqueId = "Cinema-" + System.currentTimeMillis() + "-" + Math.random();
        adminEmail = "admin-" + uniqueId + "@cinema.com";
        regularUserEmail = "user-" + uniqueId + "@cinema.com";

        // Create test users with unique emails
        adminUser = AdminUser.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .department("IT")
                .adminLevel("SUPER_ADMIN")
                .build();

        regularUser = RegularUser.builder()
                .email(regularUserEmail)
                .password(passwordEncoder.encode("user123"))
                .firstName("Regular")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        userRepository.save(adminUser);
        userRepository.save(regularUser);

        // Create test cinemas
        testCinema = new Cinema();
        testCinema.setName("Grand Cinema");
        testCinema.setAddress("123 Main Street");
        testCinema.setCity("New York");
        testCinema.setPhoneNumber("555-0123");
        cinemaRepository.save(testCinema);

        anotherCinema = new Cinema();
        anotherCinema.setName("Royal Theater");
        anotherCinema.setAddress("456 Broadway");
        anotherCinema.setCity("Los Angeles");
        anotherCinema.setPhoneNumber("555-0456");
        cinemaRepository.save(anotherCinema);

        // Create test hall
        testHall = new CinemaHall();
        testHall.setName("Hall 1");
        testHall.setTotalSeats(100);
        testHall.setRows(10);
        testHall.setSeatsPerRow(10);
        testHall.setHallType(CinemaHall.HallType.STANDARD);
        testHall.setCinema(testCinema);
        cinemaHallRepository.save(testHall);
    }

    // ========== PUBLIC ENDPOINTS (No Auth Required) ==========

    @Test
    void shouldGetAllCinemasWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/cinemas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Grand Cinema", "Royal Theater")));
    }

    @Test
    void shouldGetCinemaByIdWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/cinemas/" + testCinema.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Grand Cinema")))
                .andExpect(jsonPath("$.city", is("New York")))
                .andExpect(jsonPath("$.address", is("123 Main Street")));
    }

    @Test
    void shouldReturn404ForNonExistentCinema() throws Exception {
        mockMvc.perform(get("/api/cinemas/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetCinemasByCityWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/cinemas/city/New York"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Grand Cinema")))
                .andExpect(jsonPath("$[0].city", is("New York")));
    }

    @Test
    void shouldReturnEmptyListForNonExistentCity() throws Exception {
        mockMvc.perform(get("/api/cinemas/city/Chicago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldGetCinemaHallsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/cinemas/" + testCinema.getId() + "/halls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Hall 1")))
                .andExpect(jsonPath("$[0].totalSeats", is(100)))
                .andExpect(jsonPath("$[0].hallType", is("STANDARD")));
    }

    @Test
    void shouldReturnEmptyListForCinemaWithNoHalls() throws Exception {
        mockMvc.perform(get("/api/cinemas/" + anotherCinema.getId() + "/halls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ========== ADMIN-ONLY ENDPOINTS ==========

    @Test
    void shouldDenyCinemaCreationForUnauthenticatedUser() throws Exception {
        Cinema newCinema = createTestCinemaData("New Cinema");

        mockMvc.perform(post("/api/cinemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCinema)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyCinemaCreationForRegularUser() throws Exception {
        Cinema newCinema = createTestCinemaData("New Cinema");

        mockMvc.perform(post("/api/cinemas")
                        .with(httpBasic(regularUserEmail, "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCinema)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCinemaCreationForAdmin() throws Exception {
        Cinema newCinema = createTestCinemaData("Admin Created Cinema");

        mockMvc.perform(post("/api/cinemas")
                        .with(httpBasic(adminEmail, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCinema)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Admin Created Cinema")))
                .andExpect(jsonPath("$.city", is("Miami")))
                .andExpect(jsonPath("$.address", is("789 Test Avenue")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldDenyCinemaUpdateForRegularUser() throws Exception {
        Cinema updatedCinema = createTestCinemaData("Updated Cinema");

        mockMvc.perform(put("/api/cinemas/" + testCinema.getId())
                        .with(httpBasic(regularUserEmail, "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCinema)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCinemaUpdateForAdmin() throws Exception {
        Cinema updatedCinema = createTestCinemaData("Updated Grand Cinema");

        mockMvc.perform(put("/api/cinemas/" + testCinema.getId())
                        .with(httpBasic(adminEmail, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCinema)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Grand Cinema")))
                .andExpect(jsonPath("$.city", is("Miami")));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentCinema() throws Exception {
        Cinema updatedCinema = createTestCinemaData("Non-existent Cinema");

        mockMvc.perform(put("/api/cinemas/999")
                        .with(httpBasic(adminEmail, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCinema)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyCinemaDeletionForRegularUser() throws Exception {
        mockMvc.perform(delete("/api/cinemas/" + testCinema.getId())
                        .with(httpBasic(regularUserEmail, "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowCinemaDeletionForAdmin() throws Exception {
        mockMvc.perform(delete("/api/cinemas/" + testCinema.getId())
                        .with(httpBasic(adminEmail, "admin123")))
                .andExpect(status().isNoContent());

        // Verify cinema was deleted
        mockMvc.perform(get("/api/cinemas/" + testCinema.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentCinema() throws Exception {
        mockMvc.perform(delete("/api/cinemas/999")
                        .with(httpBasic(adminEmail, "admin123")))
                .andExpect(status().isNotFound());
    }

    // ========== HALL MANAGEMENT - ADMIN ONLY ==========

    @Test
    void shouldDenyHallCreationForUnauthenticatedUser() throws Exception {
        CinemaHall newHall = createTestHallData("Hall 2");

        mockMvc.perform(post("/api/cinemas/" + testCinema.getId() + "/halls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyHallCreationForRegularUser() throws Exception {
        CinemaHall newHall = createTestHallData("Hall 2");

        mockMvc.perform(post("/api/cinemas/" + testCinema.getId() + "/halls")
                        .with(httpBasic(regularUserEmail, "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowHallCreationForAdmin() throws Exception {
        CinemaHall newHall = createTestHallData("IMAX Hall");

        mockMvc.perform(post("/api/cinemas/" + testCinema.getId() + "/halls")
                        .with(httpBasic(adminEmail, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("IMAX Hall")))
                .andExpect(jsonPath("$.hallType", is("IMAX")))
                .andExpect(jsonPath("$.totalSeats", is(200)));
    }

    @Test
    void shouldReturn404WhenAddingHallToNonExistentCinema() throws Exception {
        CinemaHall newHall = createTestHallData("Orphan Hall");

        mockMvc.perform(post("/api/cinemas/999/halls")
                        .with(httpBasic(adminEmail, "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isNotFound());
    }

    // ========== HELPER METHODS ==========

    private Cinema createTestCinemaData(String name) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setAddress("789 Test Avenue");
        cinema.setCity("Miami");
        cinema.setPhoneNumber("555-9999");
        return cinema;
    }

    private CinemaHall createTestHallData(String name) {
        CinemaHall hall = new CinemaHall();
        hall.setName(name);
        hall.setTotalSeats(200);
        hall.setRows(20);
        hall.setSeatsPerRow(10);
        hall.setHallType(CinemaHall.HallType.IMAX);
        return hall;
    }
}