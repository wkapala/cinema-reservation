package com.cinema.reservation.controller;

import com.cinema.reservation.config.SecurityConfig;
import com.cinema.reservation.dto.ReservationCreateRequest;
import com.cinema.reservation.dto.SeatRequest;
import com.cinema.reservation.entity.Reservation;
import com.cinema.reservation.security.CustomUserDetailsService;
import com.cinema.reservation.service.ReservationService;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReservationController.class)
@Import(SecurityConfig.class)
class ReservationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReservationService reservationService;
    @MockitoBean @SuppressWarnings("unused") private CustomUserDetailsService customUserDetailsService;

    private Reservation reservation;
    private Reservation reservation2;
    private ReservationCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        reservation.setId(1L);
        reservation2 = new Reservation();
        reservation2.setId(2L);

        SeatRequest seat = new SeatRequest();
        seat.setRowNumber(1);
        seat.setSeatNumber(1);

        createRequest = new ReservationCreateRequest(1L, 1L, List.of(seat));
    }

    @Test
    void createReservation_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReservation_AsUser_ReturnsCreated() throws Exception {
        when(reservationService.createReservation(any(ReservationCreateRequest.class)))
                .thenReturn(reservation);

        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createReservation_AsAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReservationById_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getReservationById_Existing_ReturnsOk() throws Exception {
        when(reservationService.findById(1L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser
    void getReservationById_NonExisting_ReturnsNotFound() throws Exception {
        when(reservationService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reservations/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getReservationByCode_Existing_ReturnsOk() throws Exception {
        when(reservationService.findByConfirmationCode("code123"))
                .thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/api/reservations/confirmation/code123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser
    void getReservationByCode_NonExisting_ReturnsNotFound() throws Exception {
        when(reservationService.findByConfirmationCode("none"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reservations/confirmation/none"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getUserReservations_ReturnsList() throws Exception {
        when(reservationService.findByUserId(1L))
                .thenReturn(Arrays.asList(reservation, reservation2));

        mockMvc.perform(get("/api/reservations/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void confirmReservation_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/reservations/1/confirm").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirmReservation_AsAdmin_ReturnsOk() throws Exception {
        when(reservationService.confirmReservation(1L)).thenReturn(reservation);

        mockMvc.perform(put("/api/reservations/1/confirm").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void confirmReservation_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/reservations/1/confirm").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void confirmReservation_Error_ReturnsBadRequest() throws Exception {
        when(reservationService.confirmReservation(1L))
                .thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/reservations/1/confirm").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelReservation_AsAdmin_ReturnsOk() throws Exception {
        when(reservationService.cancelReservation(1L)).thenReturn(reservation);

        mockMvc.perform(put("/api/reservations/1/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelReservation_Error_ReturnsBadRequest() throws Exception {
        when(reservationService.cancelReservation(1L))
                .thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/reservations/1/cancel").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancelReservation_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/reservations/1/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
