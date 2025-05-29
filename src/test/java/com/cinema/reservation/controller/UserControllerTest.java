package com.cinema.reservation.controller;

import com.cinema.reservation.config.SecurityConfig;
import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.dto.UserUpdateRequest;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.security.CustomUserDetailsService;
import com.cinema.reservation.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean @SuppressWarnings("unused") private CustomUserDetailsService customUserDetailsService;

    private RegularUser user;
    private UserCreateRequest createRequest;
    private UserUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = new RegularUser();
        user.setId(1L);
        user.setEmail("user@test.com");

        createRequest = new UserCreateRequest();
        createRequest.setEmail("user@test.com");
        createRequest.setPassword("pass");
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setUserType(UserType.REGULAR);
        createRequest.setPhoneNumber("12345");

        updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Doe");
        updateRequest.setPhoneNumber("67890");
    }

    @Test
    void registerUser_WithoutAuth_ReturnsCreated() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("user@test.com")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_AsAdmin_ReturnsOk() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(userService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserByEmail_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/email/user@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByEmail_AsAdmin_ReturnsOk() throws Exception {
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/email/user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("user@test.com")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByEmail_NonExisting_ReturnsNotFound() throws Exception {
        when(userService.findByEmail("none")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/email/none"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_Existing_ReturnsOk() throws Exception {
        when(userService.updateUserProfile(eq(1L), any(UserUpdateRequest.class))).thenReturn(user);

        mockMvc.perform(put("/api/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_NonExisting_ReturnsNotFound() throws Exception {
        when(userService.updateUserProfile(eq(99L), any(UserUpdateRequest.class))).thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/users/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_Existing_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deactivateUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_Error_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException()).when(userService).deactivateUser(1L);

        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
