package com.cinema.reservation.controller;

import com.cinema.reservation.config.SecurityConfig;
import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.security.CustomUserDetailsService;
import com.cinema.reservation.service.MovieService;
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

@WebMvcTest(controllers = MovieController.class)
@Import(SecurityConfig.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomUserDetailsService customUserDetailsService;

    private Movie testMovie;
    private Movie testMovie2;

    @BeforeEach
    void setUp() {
        testMovie = new Movie();
        testMovie.setId(1L);
        testMovie.setTitle("Test Movie");
        testMovie.setDescription("Test Description");
        testMovie.setDurationMinutes(120);
        testMovie.setGenre(Movie.Genre.ACTION);
        testMovie.setDirector("Test Director");
        testMovie.setRating(8.5);
        testMovie.setReleaseDate(LocalDateTime.now());

        testMovie2 = new Movie();
        testMovie2.setId(2L);
        testMovie2.setTitle("Another Movie");
        testMovie2.setDescription("Another Description");
        testMovie2.setDurationMinutes(90);
        testMovie2.setGenre(Movie.Genre.COMEDY);
        testMovie2.setDirector("Another Director");
        testMovie2.setRating(7.0);
    }

    @Test
    @WithMockUser
    void getAllMovies_ReturnsMoviesList() throws Exception {
        List<Movie> movies = Arrays.asList(testMovie, testMovie2);
        when(movieService.findAll()).thenReturn(movies);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")))
                .andExpect(jsonPath("$[1].title", is("Another Movie")));

        verify(movieService).findAll();
    }

    @Test
    @WithMockUser
    void getMovieById_ExistingMovie_ReturnsMovie() throws Exception {
        when(movieService.findById(1L)).thenReturn(Optional.of(testMovie));

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Test Movie")))
                .andExpect(jsonPath("$.director", is("Test Director")))
                .andExpect(jsonPath("$.durationMinutes", is(120)));

        verify(movieService).findById(1L);
    }

    @Test
    @WithMockUser
    void getMovieById_NonExistingMovie_ReturnsNotFound() throws Exception {
        when(movieService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/movies/99"))
                .andExpect(status().isNotFound());

        verify(movieService).findById(99L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMovie_ValidMovie_ReturnsCreatedMovie() throws Exception {
        Movie newMovie = new Movie();
        newMovie.setTitle("New Movie");
        newMovie.setDescription("New Description");
        newMovie.setDurationMinutes(100);
        newMovie.setGenre(Movie.Genre.DRAMA);
        newMovie.setDirector("New Director");

        Movie createdMovie = new Movie();
        createdMovie.setId(3L);
        createdMovie.setTitle("New Movie");
        createdMovie.setDescription("New Description");
        createdMovie.setDurationMinutes(100);
        createdMovie.setGenre(Movie.Genre.DRAMA);
        createdMovie.setDirector("New Director");

        when(movieService.createMovie(any(Movie.class))).thenReturn(createdMovie);

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovie)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.title", is("New Movie")))
                .andExpect(jsonPath("$.director", is("New Director")));

        verify(movieService).createMovie(any(Movie.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createMovie_AsRegularUser_ReturnsForbidden() throws Exception {
        Movie newMovie = new Movie();
        newMovie.setTitle("New Movie");

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovie)))
                .andExpect(status().isForbidden());

        verify(movieService, never()).createMovie(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMovie_ExistingMovie_ReturnsUpdatedMovie() throws Exception {
        Movie updateData = new Movie();
        updateData.setTitle("Updated Movie");
        updateData.setDescription("Updated Description");
        updateData.setDurationMinutes(130);
        updateData.setGenre(Movie.Genre.THRILLER);
        updateData.setDirector("Updated Director");

        Movie updatedMovie = new Movie();
        updatedMovie.setId(1L);
        updatedMovie.setTitle("Updated Movie");
        updatedMovie.setDescription("Updated Description");
        updatedMovie.setDurationMinutes(130);
        updatedMovie.setGenre(Movie.Genre.THRILLER);
        updatedMovie.setDirector("Updated Director");

        when(movieService.updateMovie(eq(1L), any(Movie.class))).thenReturn(updatedMovie);

        mockMvc.perform(put("/api/movies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Movie")))
                .andExpect(jsonPath("$.durationMinutes", is(130)));

        verify(movieService).updateMovie(eq(1L), any(Movie.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMovie_NonExistingMovie_ReturnsNotFound() throws Exception {
        Movie updateData = new Movie();
        updateData.setTitle("Updated Movie");

        when(movieService.updateMovie(eq(99L), any(Movie.class)))
                .thenThrow(new RuntimeException("Movie not found"));

        mockMvc.perform(put("/api/movies/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMovie_ExistingMovie_ReturnsNoContent() throws Exception {
        doNothing().when(movieService).deleteMovie(1L);

        mockMvc.perform(delete("/api/movies/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(movieService).deleteMovie(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMovie_NonExistingMovie_ReturnsNotFound() throws Exception {
        doThrow(new RuntimeException("Movie not found"))
                .when(movieService).deleteMovie(99L);

        mockMvc.perform(delete("/api/movies/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void searchMovies_ReturnsMatchingMovies() throws Exception {
        when(movieService.searchByTitle("Test")).thenReturn(List.of(testMovie));

        mockMvc.perform(get("/api/movies/search").param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));

        verify(movieService).searchByTitle("Test");
    }

    @Test
    @WithMockUser
    void getMoviesByGenre_ReturnsMoviesOfGenre() throws Exception {
        when(movieService.findByGenre(Movie.Genre.ACTION)).thenReturn(List.of(testMovie));

        mockMvc.perform(get("/api/movies/genre/ACTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre", is("ACTION")));

        verify(movieService).findByGenre(Movie.Genre.ACTION);
    }

    @Test
    void getAllMovies_WithoutAuthentication_ReturnsOk() throws Exception {
        // Bez logowania lista jest publiczna
        List<Movie> movies = Arrays.asList(testMovie, testMovie2);
        when(movieService.findAll()).thenReturn(movies);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Test Movie")));

        verify(movieService).findAll();
    }

}
