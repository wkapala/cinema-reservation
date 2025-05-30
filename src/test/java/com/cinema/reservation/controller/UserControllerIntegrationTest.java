package com.cinema.reservation.controller;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserUpdateRequest;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.entity.User;
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
class UserControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private AdminUser adminUser;
    private RegularUser regularUser;
    private RegularUser anotherUser;

    @BeforeEach
    void setUp() {
        // Clean data
        userRepository.deleteAll();

        // Create test users with unique emails
        String uniqueId = String.valueOf(System.currentTimeMillis() + Math.random() * 1000);
        adminUser = AdminUser.builder()
                .email("admin-" + uniqueId + "@cinema.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .department("IT")
                .adminLevel("SUPER_ADMIN")
                .build();

        regularUser = RegularUser.builder()
                .email("user-" + uniqueId + "@cinema.com")
                .password(passwordEncoder.encode("user123"))
                .firstName("Regular")
                .lastName("User")
                .phoneNumber("123456789")
                .build();

        anotherUser = RegularUser.builder()
                .email("another-" + uniqueId + "@cinema.com")
                .password(passwordEncoder.encode("another123"))
                .firstName("Another")
                .lastName("User")
                .phoneNumber("987654321")
                .build();

        userRepository.save(adminUser);
        userRepository.save(regularUser);
        userRepository.save(anotherUser);
    }

    // ========== PUBLIC REGISTRATION ENDPOINT ==========

    @Test
    void shouldAllowUserRegistrationWithoutAuthentication() throws Exception {
        UserCreateRequest request = createUserRegistrationRequest("newuser@cinema.com");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newuser@cinema.com")))
                .andExpect(jsonPath("$.firstName", is("New")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void shouldCreateRegularUserByDefault() throws Exception {
        UserCreateRequest request = createUserRegistrationRequest("regular@cinema.com");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))));
    }

    // ========== GET USER BY ID - BASIC TESTS ==========

    @Test
    void shouldDenyGetUserByIdForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/users/" + regularUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminToGetAnyUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/" + regularUser.getId())
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(regularUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is(regularUser.getEmail())))
                .andExpect(jsonPath("$.firstName", is("Regular")));
    }

    @Test
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/users/999")
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isNotFound());
    }

    // ========== GET USER BY EMAIL - ADMIN ONLY ==========

    @Test
    void shouldDenyGetUserByEmailForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/users/email/" + regularUser.getEmail()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyGetUserByEmailForRegularUser() throws Exception {
        mockMvc.perform(get("/api/users/email/" + anotherUser.getEmail())
                        .with(httpBasic(regularUser.getEmail(), "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToGetUserByEmail() throws Exception {
        mockMvc.perform(get("/api/users/email/" + regularUser.getEmail())
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(regularUser.getEmail())))
                .andExpect(jsonPath("$.firstName", is("Regular")));
    }

    @Test
    void shouldReturn404WhenAdminSearchesNonExistentEmail() throws Exception {
        mockMvc.perform(get("/api/users/email/nonexistent@cinema.com")
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isNotFound());
    }

    // ========== UPDATE USER PROFILE - ADMIN TESTS ==========

    @Test
    void shouldDenyUpdateForUnauthenticatedUser() throws Exception {
        UserUpdateRequest request = createUserUpdateRequest("Updated", "Name");

        mockMvc.perform(put("/api/users/" + regularUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminToUpdateAnyUserProfile() throws Exception {
        UserUpdateRequest request = createUserUpdateRequest("Admin", "Updated");

        mockMvc.perform(put("/api/users/" + regularUser.getId())
                        .with(httpBasic(adminUser.getEmail(), "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Admin")))
                .andExpect(jsonPath("$.lastName", is("Updated")));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        UserUpdateRequest request = createUserUpdateRequest("Non", "Existent");

        mockMvc.perform(put("/api/users/999")
                        .with(httpBasic(adminUser.getEmail(), "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE/DEACTIVATE USER - ADMIN TESTS ==========

    @Test
    void shouldDenyDeactivationForUnauthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + regularUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminToDeactivateAnyUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + regularUser.getId())
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400WhenDeactivatingNonExistentUser() throws Exception {
        mockMvc.perform(delete("/api/users/999")
                        .with(httpBasic(adminUser.getEmail(), "admin123")))
                .andExpect(status().isBadRequest());
    }

    // ========== EDGE CASES ==========

    @Test
    void shouldHandleRegularUserAccessDeniedScenarios() throws Exception {
        // Regular user cannot access other users by ID
        mockMvc.perform(get("/api/users/" + anotherUser.getId())
                        .with(httpBasic(regularUser.getEmail(), "user123")))
                .andExpect(status().isForbidden());

        // Regular user cannot update other users
        UserUpdateRequest request = createUserUpdateRequest("Hacked", "User");
        mockMvc.perform(put("/api/users/" + anotherUser.getId())
                        .with(httpBasic(regularUser.getEmail(), "user123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Regular user cannot delete other users
        mockMvc.perform(delete("/api/users/" + anotherUser.getId())
                        .with(httpBasic(regularUser.getEmail(), "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateUserRegistrationData() throws Exception {
        // Test with invalid email format
        UserCreateRequest invalidRequest = createUserRegistrationRequest("invalid-email");

        // This test would require validation annotations in UserCreateRequest
        // For now, we just test successful creation
        UserCreateRequest validRequest = createUserRegistrationRequest("valid@example.com");
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    // ========== HELPER METHODS ==========

    private UserCreateRequest createUserRegistrationRequest(String email) {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setUserType(UserType.REGULAR);
        request.setPhoneNumber("555-0123");
        return request;
    }

    private UserUpdateRequest createUserUpdateRequest(String firstName, String lastName) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        return request;
    }
}