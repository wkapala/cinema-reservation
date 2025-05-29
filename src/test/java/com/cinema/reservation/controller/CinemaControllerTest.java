package com.cinema.reservation.controller;

import com.cinema.reservation.config.SecurityConfig;
import com.cinema.reservation.entity.Cinema;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.security.CustomUserDetailsService;
import com.cinema.reservation.service.CinemaService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CinemaController.class)
@Import(SecurityConfig.class)
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CinemaService cinemaService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomUserDetailsService customUserDetailsService;

    private Cinema cinema1;
    private Cinema cinema2;
    private CinemaHall hall1;
    private CinemaHall hall2;

    @BeforeEach
    void setUp() {
        cinema1 = new Cinema();
        cinema1.setId(1L);
        cinema1.setName("Cinema One");
        cinema1.setCity("CityA");

        cinema2 = new Cinema();
        cinema2.setId(2L);
        cinema2.setName("Cinema Two");
        cinema2.setCity("CityB");

        hall1 = new CinemaHall();
        hall1.setId(1L);
        hall1.setName("Hall 1");
        hall1.setCapacity(100);
        hall1.setCinema(cinema1);

        hall2 = new CinemaHall();
        hall2.setId(2L);
        hall2.setName("Hall 2");
        hall2.setCapacity(150);
        hall2.setCinema(cinema1);
    }

    @Test
    @WithMockUser
    void getAllCinemas_ReturnsList() throws Exception {
        List<Cinema> cinemas = Arrays.asList(cinema1, cinema2);
        when(cinemaService.findAll()).thenReturn(cinemas);

        mockMvc.perform(get("/api/cinemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Cinema One")))
                .andExpect(jsonPath("$[1].name", is("Cinema Two")));

        verify(cinemaService).findAll();
    }

    @Test
    @WithMockUser
    void getCinemaById_Existing_ReturnsCinema() throws Exception {
        when(cinemaService.findById(1L)).thenReturn(Optional.of(cinema1));

        mockMvc.perform(get("/api/cinemas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Cinema One")));

        verify(cinemaService).findById(1L);
    }

    @Test
    @WithMockUser
    void getCinemaById_NonExisting_ReturnsNotFound() throws Exception {
        when(cinemaService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cinemas/99"))
                .andExpect(status().isNotFound());

        verify(cinemaService).findById(99L);
    }

    @Test
    @WithMockUser
    void getCinemasByCity_ReturnsList() throws Exception {
        when(cinemaService.findByCity("CityA")).thenReturn(Arrays.asList(cinema1));

        mockMvc.perform(get("/api/cinemas/city/CityA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].city", is("CityA")));

        verify(cinemaService).findByCity("CityA");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCinema_AsAdmin_ReturnsCreated() throws Exception {
        Cinema newCinema = new Cinema();
        newCinema.setName("New Cinem");
        newCinema.setCity("CityC");

        Cinema created = new Cinema();
        created.setId(3L);
        created.setName("New Cinem");
        created.setCity("CityC");

        when(cinemaService.createCinema(any(Cinema.class))).thenReturn(created);

        mockMvc.perform(post("/api/cinemas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCinema)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New Cinem")));

        verify(cinemaService).createCinema(any(Cinema.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCinema_AsUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/cinemas")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(cinemaService, never()).createCinema(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCinema_Existing_ReturnsUpdated() throws Exception {
        Cinema update = new Cinema();
        update.setName("Updated");
        update.setCity("CityX");

        Cinema updated = new Cinema();
        updated.setId(1L);
        updated.setName("Updated");
        updated.setCity("CityX");

        when(cinemaService.updateCinema(eq(1L), any(Cinema.class))).thenReturn(updated);

        mockMvc.perform(put("/api/cinemas/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated")));

        verify(cinemaService).updateCinema(eq(1L), any(Cinema.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCinema_NonExisting_ReturnsNotFound() throws Exception {
        Cinema update = new Cinema();
        update.setName("X");

        when(cinemaService.updateCinema(eq(99L), any(Cinema.class))).thenThrow(new RuntimeException());

        mockMvc.perform(put("/api/cinemas/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCinema_Existing_ReturnsNoContent() throws Exception {
        doNothing().when(cinemaService).deleteCinema(1L);

        mockMvc.perform(delete("/api/cinemas/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(cinemaService).deleteCinema(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCinema_NonExisting_ReturnsNotFound() throws Exception {
        doThrow(new RuntimeException()).when(cinemaService).deleteCinema(99L);

        mockMvc.perform(delete("/api/cinemas/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getCinemaHalls_ReturnsList() throws Exception {
        when(cinemaService.findHallsByCinemaId(1L)).thenReturn(Arrays.asList(hall1, hall2));

        mockMvc.perform(get("/api/cinemas/1/halls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Hall 1")));

        verify(cinemaService).findHallsByCinemaId(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addHallToCinema_Existing_ReturnsCreated() throws Exception {
        CinemaHall newHall = new CinemaHall();
        newHall.setName("New Hall");
        newHall.setCapacity(50);

        CinemaHall created = new CinemaHall();
        created.setId(3L);
        created.setName("New Hall");
        created.setCapacity(50);
        created.setCinema(cinema1);

        when(cinemaService.findById(1L)).thenReturn(Optional.of(cinema1));
        when(cinemaService.createCinemaHall(any(CinemaHall.class))).thenReturn(created);

        mockMvc.perform(post("/api/cinemas/1/halls")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New Hall")));

        verify(cinemaService).findById(1L);
        verify(cinemaService).createCinemaHall(any(CinemaHall.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addHallToCinema_NonExisting_ReturnsNotFound() throws Exception {
        CinemaHall newHall = new CinemaHall();
        newHall.setName("X");

        when(cinemaService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/cinemas/99/halls")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newHall)))
                .andExpect(status().isNotFound());
    }
}