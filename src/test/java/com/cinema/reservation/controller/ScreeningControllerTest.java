package com.cinema.reservation.controller;

import com.cinema.reservation.config.SecurityConfig;
import com.cinema.reservation.dto.ScreeningCreateRequest;
import com.cinema.reservation.entity.Screening;
import com.cinema.reservation.security.CustomUserDetailsService;
import com.cinema.reservation.service.ScreeningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScreeningController.class)
@Import(SecurityConfig.class)
class ScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScreeningService screeningService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomUserDetailsService customUserDetailsService;

    private Screening screening1;
    private Screening screening2;

    @BeforeEach
    void setUp() {
        screening1 = new Screening();
        screening1.setId(1L);
        screening2 = new Screening();
        screening2.setId(2L);
    }

    @Test
    void getAllScreenings_WithoutAuth_ReturnsList() throws Exception {
        List<Screening> list = Arrays.asList(screening1, screening2);
        when(screeningService.findAll()).thenReturn(list);

        mockMvc.perform(get("/api/screenings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(screeningService).findAll();
    }

    @Test
    void getScreeningById_Existing_ReturnsOk() throws Exception {
        when(screeningService.findById(1L)).thenReturn(Optional.of(screening1));

        mockMvc.perform(get("/api/screenings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(screeningService).findById(1L);
    }

    @Test
    void getScreeningById_NonExisting_ReturnsNotFound() throws Exception {
        when(screeningService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/screenings/99"))
                .andExpect(status().isNotFound());

        verify(screeningService).findById(99L);
    }

    @Test
    void getScreeningsByMovie_ReturnsList() throws Exception {
        when(screeningService.findByMovieId(5L)).thenReturn(Arrays.asList(screening1));

        mockMvc.perform(get("/api/screenings/movie/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(screeningService).findByMovieId(5L);
    }

    @Test
    void getUpcomingScreenings_ReturnsList() throws Exception {
        when(screeningService.findAvailableScreenings()).thenReturn(Arrays.asList(screening1));

        mockMvc.perform(get("/api/screenings/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(screeningService).findAvailableScreenings();
    }

    @Test
    void getAvailableScreenings_DefaultMinSeats_ReturnsList() throws Exception {
        when(screeningService.findScreeningsWithAvailableSeats(1)).thenReturn(Arrays.asList(screening1));

        mockMvc.perform(get("/api/screenings/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(screeningService).findScreeningsWithAvailableSeats(1);
    }

    @Test
    void getAvailableScreenings_CustomMinSeats_ReturnsList() throws Exception {
        when(screeningService.findScreeningsWithAvailableSeats(5)).thenReturn(Arrays.asList(screening2));

        mockMvc.perform(get("/api/screenings/available").param("minSeats", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(2)));

        verify(screeningService).findScreeningsWithAvailableSeats(5);
    }

    @Test
    void createScreening_WithoutAuth_ReturnsUnauthorized() throws Exception {
        ScreeningCreateRequest request = new ScreeningCreateRequest();

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createScreening_AsUser_ReturnsForbidden() throws Exception {
        ScreeningCreateRequest request = new ScreeningCreateRequest();

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(screeningService, never()).createScreeningFromRequest(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createScreening_AsAdmin_ReturnsCreated() throws Exception {
        ScreeningCreateRequest request = new ScreeningCreateRequest();
        request.setMovieId(1L);
        request.setHallId(2L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(BigDecimal.valueOf(29.99));

        Screening created = new Screening();
        created.setId(3L);

        when(screeningService.createScreeningFromRequest(any(ScreeningCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)));

        verify(screeningService).createScreeningFromRequest(any(ScreeningCreateRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createScreening_Error_ReturnsBadRequest() throws Exception {
        ScreeningCreateRequest request = new ScreeningCreateRequest();
        request.setMovieId(1L);
        request.setHallId(2L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(BigDecimal.valueOf(29.99));

        when(screeningService.createScreeningFromRequest(any(ScreeningCreateRequest.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled
    @WithMockUser(roles = "ADMIN")
    void updateScreening_Existing_ReturnsUpdated() throws Exception {
        // Używamy pełnego obiektu Screening
        Screening updateScreening = new Screening();
        updateScreening.setId(1L);
        updateScreening.setPrice(new BigDecimal("25.99"));

        Screening updated = new Screening();
        updated.setId(1L);

        when(screeningService.updateScreening(eq(1L), any(Screening.class))).thenReturn(updated);

        mockMvc.perform(put("/api/screenings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateScreening)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @Disabled
    @WithMockUser(roles = "ADMIN")
    void updateScreening_NonExisting_ReturnsNotFound() throws Exception {
        // Używamy pełnego obiektu Screening
        Screening updateScreening = new Screening();
        updateScreening.setId(99L);
        updateScreening.setPrice(new BigDecimal("25.99"));

        when(screeningService.updateScreening(eq(99L), any(Screening.class))).thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/screenings/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateScreening)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteScreening_Existing_ReturnsNoContent() throws Exception {
        doNothing().when(screeningService).deleteScreening(1L);

        mockMvc.perform(delete("/api/screenings/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(screeningService).deleteScreening(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteScreening_NonExisting_ReturnsNotFound() throws Exception {
        doThrow(new RuntimeException()).when(screeningService).deleteScreening(99L);

        mockMvc.perform(delete("/api/screenings/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}