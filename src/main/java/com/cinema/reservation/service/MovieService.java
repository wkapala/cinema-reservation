package com.cinema.reservation.service;

import com.cinema.reservation.entity.Movie;
import com.cinema.reservation.exception.InvalidMovieDataException;
import com.cinema.reservation.exception.MovieNotFoundException;
import com.cinema.reservation.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MovieService {

    // Dependency Inversion - zależy od interface
    private final MovieRepository movieRepository;

    // Single Responsibility - tylko operacje na filmach

    public Movie createMovie(Movie movie) {
        log.info("Creating new movie: {}", movie.getTitle());

        validateMovieData(movie);
        Movie savedMovie = movieRepository.save(movie);

        log.info("Movie created with ID: {}", savedMovie.getId());
        return savedMovie;
    }

    public Optional<Movie> findById(Long id) {
        return movieRepository.findById(id);
    }

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    public List<Movie> findByGenre(Movie.Genre genre) {
        return movieRepository.findByGenre(genre);
    }

    public Page<Movie> findByGenre(Movie.Genre genre, Pageable pageable) {
        return movieRepository.findByGenre(genre, pageable);
    }

    public List<Movie> searchByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Movie> searchByDirector(String director) {
        return movieRepository.findByDirectorContainingIgnoreCase(director);
    }

    public List<Movie> findHighRatedMovies(Double minRating) {
        return movieRepository.findHighRatedMovies(minRating);
    }

    public List<Movie> findRecentMovies() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        return movieRepository.findRecentMovies(threeMonthsAgo);
    }

    public List<Movie> findMostPopularMovies() {
        return movieRepository.findMostPopularMovies();
    }

    public List<Movie> findByDurationRange(Integer minDuration, Integer maxDuration) {
        return movieRepository.findByDurationRange(minDuration, maxDuration);
    }

    public Movie updateMovie(Long id, Movie movieUpdates) {
        Movie existingMovie = movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException("Movie not found with ID: " + id));

        // Update fields
        existingMovie.setTitle(movieUpdates.getTitle());
        existingMovie.setDescription(movieUpdates.getDescription());
        existingMovie.setDurationMinutes(movieUpdates.getDurationMinutes());
        existingMovie.setGenre(movieUpdates.getGenre());
        existingMovie.setDirector(movieUpdates.getDirector());
        existingMovie.setPosterUrl(movieUpdates.getPosterUrl());
        existingMovie.setRating(movieUpdates.getRating());
        existingMovie.setReleaseDate(movieUpdates.getReleaseDate());

        return movieRepository.save(existingMovie);
    }

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new MovieNotFoundException("Movie not found with ID: " + id);
        }

        movieRepository.deleteById(id);
        log.info("Movie deleted with ID: {}", id);
    }

    // Business logic validation
    private void validateMovieData(Movie movie) {
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            throw new InvalidMovieDataException("Movie title cannot be empty");
        }

        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
            throw new InvalidMovieDataException("Movie duration must be positive");
        }

        if (movie.getDirector() == null || movie.getDirector().trim().isEmpty()) {
            throw new InvalidMovieDataException("Movie director cannot be empty");
        }
    }
}