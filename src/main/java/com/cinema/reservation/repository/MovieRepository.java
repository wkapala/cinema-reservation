package com.cinema.reservation.repository;

import com.cinema.reservation.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Single Responsibility - tylko operacje na Movie
    List<Movie> findByGenre(Movie.Genre genre);

    List<Movie> findByDirectorContainingIgnoreCase(String director);

    List<Movie> findByTitleContainingIgnoreCase(String title);

    // Open/Closed Principle - łatwo dodać nowe query bez modyfikacji
    @Query("SELECT m FROM Movie m WHERE m.rating >= :minRating ORDER BY m.rating DESC")
    List<Movie> findHighRatedMovies(@Param("minRating") Double minRating);

    @Query("SELECT m FROM Movie m WHERE m.releaseDate >= :fromDate ORDER BY m.releaseDate DESC")
    List<Movie> findRecentMovies(@Param("fromDate") LocalDateTime fromDate);

    // Pagination support - nie łamiemy Interface Segregation
    Page<Movie> findByGenre(Movie.Genre genre, Pageable pageable);

    // Business logic encapsulation
    @Query("SELECT m FROM Movie m JOIN m.screenings s WHERE s.startTime >= CURRENT_TIMESTAMP GROUP BY m ORDER BY COUNT(s) DESC")
    List<Movie> findMostPopularMovies();

    // Performance optimization
    @Query("SELECT m FROM Movie m WHERE m.durationMinutes BETWEEN :minDuration AND :maxDuration")
    List<Movie> findByDurationRange(@Param("minDuration") Integer minDuration,
                                    @Param("maxDuration") Integer maxDuration);
}