package com.cinema.reservation.controller;

import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.repository.MovieRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class MovieControllerIntegrationTest {

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
    private MovieRepository movieRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminUser adminUser;
    private RegularUser regularUser;
    private Movie testMovie;

    @BeforeEach
    void setUp() {
        // Clean data - important for TestContainers
        movieRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        adminUser = AdminUser.builder()
                .email("test.admin@cinema.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Test")
                .lastName("Admin")
                .department("IT")
                .adminLevel("SUPER_ADMIN")
                .build();

        regularUser = RegularUser.builder()
                .email("test.user@cinema.com")
                .password(passwordEncoder.encode("user123"))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        userRepository.save(adminUser);
        userRepository.save(regularUser);

        // Create test movie
        testMovie = new Movie();
        testMovie.setTitle("Test Movie");
        testMovie.setDescription("A test movie for integration testing");
        testMovie.setDurationMinutes(120);
        testMovie.setGenre(Movie.Genre.ACTION);
        testMovie.setDirector("Test Director");
        testMovie.setRating(8.5);
        testMovie.setReleaseDate(LocalDateTime.now().plusDays(30));

        movieRepository.save(testMovie);
    }

    // ========== PUBLIC ENDPOINTS (No Auth Required) ==========

    @Test
    void shouldGetAllMoviesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")))
                .andExpect(jsonPath("$[0].genre", is("ACTION")));
    }

    @Test
    void shouldGetMovieByIdWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/movies/" + testMovie.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is("Test Movie")))
                .andExpect(jsonPath("$.director", is("Test Director")));
    }

    @Test
    void shouldReturn404ForNonExistentMovie() throws Exception {
        mockMvc.perform(get("/api/movies/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchMoviesByTitleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/movies/search")
                        .param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Test")));
    }

    @Test
    void shouldGetMoviesByGenreWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/movies/genre/ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].genre", is("ACTION")));
    }

    // ========== ADMIN-ONLY ENDPOINTS ==========

    @Test
    void shouldDenyMovieCreationForUnauthenticatedUser() throws Exception {
        Movie newMovie = createTestMovieData("New Movie");

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovie)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyMovieCreationForRegularUser() throws Exception {
        Movie newMovie = createTestMovieData("New Movie");

        mockMvc.perform(post("/api/movies")
                        .with(httpBasic("test.user@cinema.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovie)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMovieCreationForAdmin() throws Exception {
        Movie newMovie = createTestMovieData("Admin Created Movie");

        mockMvc.perform(post("/api/movies")
                        .with(httpBasic("test.admin@cinema.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovie)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Admin Created Movie")))
                .andExpect(jsonPath("$.director", is("Test Director")))
                .andExpect(jsonPath("$.genre", is("DRAMA")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldDenyMovieUpdateForRegularUser() throws Exception {
        Movie updatedMovie = createTestMovieData("Updated Title");

        mockMvc.perform(put("/api/movies/" + testMovie.getId())
                        .with(httpBasic("test.user@cinema.com", "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMovie)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMovieUpdateForAdmin() throws Exception {
        Movie updatedMovie = createTestMovieData("Updated Movie Title");

        mockMvc.perform(put("/api/movies/" + testMovie.getId())
                        .with(httpBasic("test.admin@cinema.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMovie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Movie Title")))
                .andExpect(jsonPath("$.genre", is("DRAMA")));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentMovie() throws Exception {
        Movie updatedMovie = createTestMovieData("Non-existent Movie");

        mockMvc.perform(put("/api/movies/999")
                        .with(httpBasic("test.admin@cinema.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMovie)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyMovieDeletionForRegularUser() throws Exception {
        mockMvc.perform(delete("/api/movies/" + testMovie.getId())
                        .with(httpBasic("test.user@cinema.com", "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowMovieDeletionForAdmin() throws Exception {
        mockMvc.perform(delete("/api/movies/" + testMovie.getId())
                        .with(httpBasic("test.admin@cinema.com", "admin123")))
                .andExpect(status().isNoContent());

        // Verify movie was deleted
        mockMvc.perform(get("/api/movies/" + testMovie.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentMovie() throws Exception {
        mockMvc.perform(delete("/api/movies/999")
                        .with(httpBasic("test.admin@cinema.com", "admin123")))
                .andExpect(status().isNotFound());
    }

    // ========== EDGE CASES ==========

    @Test
    void shouldReturnEmptyListWhenSearchingNonExistentTitle() throws Exception {
        mockMvc.perform(get("/api/movies/search")
                        .param("title", "NonExistentMovie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnEmptyListForNonExistentGenre() throws Exception {
        mockMvc.perform(get("/api/movies/genre/HORROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ========== HELPER METHODS ==========

    private Movie createTestMovieData(String title) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription("A test movie for integration testing");
        movie.setDurationMinutes(150);
        movie.setGenre(Movie.Genre.DRAMA);
        movie.setDirector("Test Director");
        movie.setRating(9.0);
        movie.setReleaseDate(LocalDateTime.now().plusDays(60));
        return movie;
    }
}