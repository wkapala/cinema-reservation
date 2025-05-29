package com.cinema.reservation.service;

import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.exception.InvalidMovieDataException;
import com.cinema.reservation.exception.MovieNotFoundException;
import com.cinema.reservation.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie;

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
    }

    @Test
    void createMovie_ValidMovie_Success() {
        // Given
        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        // When
        Movie result = movieService.createMovie(testMovie);

        // Then
        assertNotNull(result);
        assertEquals("Test Movie", result.getTitle());
        verify(movieRepository).save(testMovie);
    }

    @Test
    void createMovie_NullTitle_ThrowsException() {
        // Given
        testMovie.setTitle(null);

        // When & Then
        assertThrows(InvalidMovieDataException.class,
                () -> movieService.createMovie(testMovie));
        verify(movieRepository, never()).save(any());
    }

    @Test
    void createMovie_EmptyTitle_ThrowsException() {
        // Given
        testMovie.setTitle("   ");

        // When & Then
        assertThrows(InvalidMovieDataException.class,
                () -> movieService.createMovie(testMovie));
    }

    @Test
    void createMovie_InvalidDuration_ThrowsException() {
        // Given
        testMovie.setDurationMinutes(0);

        // When & Then
        assertThrows(InvalidMovieDataException.class,
                () -> movieService.createMovie(testMovie));
    }

    @Test
    void createMovie_NullDirector_ThrowsException() {
        // Given
        testMovie.setDirector(null);

        // When & Then
        assertThrows(InvalidMovieDataException.class,
                () -> movieService.createMovie(testMovie));
    }

    @Test
    void findById_ExistingMovie_ReturnsMovie() {
        // Given
        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));

        // When
        Optional<Movie> result = movieService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Movie", result.get().getTitle());
    }

    @Test
    void findAll_ReturnsAllMovies() {
        // Given
        List<Movie> movies = Arrays.asList(testMovie, new Movie());
        when(movieRepository.findAll()).thenReturn(movies);

        // When
        List<Movie> result = movieService.findAll();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void findByGenre_ReturnsMoviesOfGenre() {
        // Given
        when(movieRepository.findByGenre(Movie.Genre.ACTION))
                .thenReturn(Arrays.asList(testMovie));

        // When
        List<Movie> result = movieService.findByGenre(Movie.Genre.ACTION);

        // Then
        assertEquals(1, result.size());
        assertEquals(Movie.Genre.ACTION, result.get(0).getGenre());
    }

    @Test
    void searchByTitle_ReturnsMatchingMovies() {
        // Given
        when(movieRepository.findByTitleContainingIgnoreCase("Test"))
                .thenReturn(Arrays.asList(testMovie));

        // When
        List<Movie> result = movieService.searchByTitle("Test");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTitle().contains("Test"));
    }

    @Test
    void updateMovie_ExistingMovie_Success() {
        // Given
        Movie updates = new Movie();
        updates.setTitle("Updated Title");
        updates.setDescription("Updated Description");
        updates.setDurationMinutes(150);
        updates.setGenre(Movie.Genre.DRAMA);
        updates.setDirector("Updated Director");
        updates.setPosterUrl("http://poster.url");
        updates.setRating(9.0);
        updates.setReleaseDate(LocalDateTime.now());

        when(movieRepository.findById(1L)).thenReturn(Optional.of(testMovie));
        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        // When
        Movie result = movieService.updateMovie(1L, updates);

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", testMovie.getTitle());
        verify(movieRepository).save(testMovie);
    }

    @Test
    void updateMovie_NonExistingMovie_ThrowsException() {
        // Given
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MovieNotFoundException.class,
                () -> movieService.updateMovie(99L, new Movie()));
    }

    @Test
    void deleteMovie_ExistingMovie_Success() {
        // Given
        when(movieRepository.existsById(1L)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> movieService.deleteMovie(1L));

        // Then
        verify(movieRepository).deleteById(1L);
    }

    @Test
    void deleteMovie_NonExistingMovie_ThrowsException() {
        // Given
        when(movieRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(MovieNotFoundException.class,
                () -> movieService.deleteMovie(99L));
        verify(movieRepository, never()).deleteById(any());
    }
}