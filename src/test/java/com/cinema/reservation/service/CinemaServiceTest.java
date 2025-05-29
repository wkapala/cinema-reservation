package com.cinema.reservation.service;

import com.cinema.reservation.entity.Cinema;
import com.cinema.reservation.entity.CinemaHall;
import com.cinema.reservation.exception.CinemaNotFoundException;
import com.cinema.reservation.exception.CinemaHallNotFoundException;
import com.cinema.reservation.exception.InvalidCinemaDataException;
import com.cinema.reservation.exception.InvalidCinemaHallDataException;
import com.cinema.reservation.repository.CinemaHallRepository;
import com.cinema.reservation.repository.CinemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private CinemaHallRepository cinemaHallRepository;

    @InjectMocks
    private CinemaService cinemaService;

    private Cinema testCinema;
    private CinemaHall testHall;

    @BeforeEach
    void setUp() {
        // Setup test cinema
        testCinema = new Cinema();
        testCinema.setId(1L);
        testCinema.setName("Test Cinema");
        testCinema.setAddress("Test Address 123");
        testCinema.setCity("Warsaw");
        testCinema.setPhoneNumber("+48123456789");

        // Setup test hall
        testHall = new CinemaHall();
        testHall.setId(1L);
        testHall.setName("Hall 1");
        testHall.setTotalSeats(100);
        testHall.setRows(10);
        testHall.setSeatsPerRow(10);
        testHall.setHallType(CinemaHall.HallType.STANDARD);
        testHall.setCinema(testCinema);
    }

    // Cinema Tests
    @Test
    void createCinema_ValidData_Success() {
        // Given
        when(cinemaRepository.save(any(Cinema.class))).thenReturn(testCinema);

        // When
        Cinema result = cinemaService.createCinema(testCinema);

        // Then
        assertNotNull(result);
        assertEquals("Test Cinema", result.getName());
        assertEquals("Warsaw", result.getCity());
        verify(cinemaRepository).save(testCinema);
    }

    @Test
    void createCinema_NullName_ThrowsException() {
        // Given
        testCinema.setName(null);

        // When & Then
        assertThrows(InvalidCinemaDataException.class,
                () -> cinemaService.createCinema(testCinema));
        verify(cinemaRepository, never()).save(any());
    }

    @Test
    void createCinema_EmptyName_ThrowsException() {
        // Given
        testCinema.setName("   ");

        // When & Then
        assertThrows(InvalidCinemaDataException.class,
                () -> cinemaService.createCinema(testCinema));
    }

    @Test
    void createCinema_NullAddress_ThrowsException() {
        // Given
        testCinema.setAddress(null);

        // When & Then
        assertThrows(InvalidCinemaDataException.class,
                () -> cinemaService.createCinema(testCinema));
    }

    @Test
    void findById_ExistingCinema_ReturnsCinema() {
        // Given
        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(testCinema));

        // When
        Optional<Cinema> result = cinemaService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Cinema", result.get().getName());
    }

    @Test
    void findById_NonExistingCinema_ReturnsEmpty() {
        // Given
        when(cinemaRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<Cinema> result = cinemaService.findById(99L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_ReturnsAllCinemas() {
        // Given
        List<Cinema> cinemas = Arrays.asList(testCinema, new Cinema());
        when(cinemaRepository.findAll()).thenReturn(cinemas);

        // When
        List<Cinema> result = cinemaService.findAll();

        // Then
        assertEquals(2, result.size());
        verify(cinemaRepository).findAll();
    }

    @Test
    void findByCity_ReturnsCinemasInCity() {
        // Given
        List<Cinema> warsawCinemas = Arrays.asList(testCinema);
        when(cinemaRepository.findByCity("Warsaw")).thenReturn(warsawCinemas);

        // When
        List<Cinema> result = cinemaService.findByCity("Warsaw");

        // Then
        assertEquals(1, result.size());
        assertEquals("Warsaw", result.get(0).getCity());
    }

    @Test
    void searchByName_ReturnsCinemasWithName() {
        // Given
        when(cinemaRepository.findByNameContainingIgnoreCase("Test")).thenReturn(Arrays.asList(testCinema));

        // When
        List<Cinema> result = cinemaService.searchByName("Test");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getName().contains("Test"));
    }

    @Test
    void updateCinema_ExistingCinema_Success() {
        // Given
        Cinema updates = new Cinema();
        updates.setName("Updated Cinema");
        updates.setAddress("New Address");
        updates.setCity("Krakow");
        updates.setPhoneNumber("+48987654321");

        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(testCinema));
        when(cinemaRepository.save(any(Cinema.class))).thenReturn(testCinema);

        // When
        Cinema result = cinemaService.updateCinema(1L, updates);

        // Then
        assertNotNull(result);
        assertEquals("Updated Cinema", testCinema.getName());
        assertEquals("New Address", testCinema.getAddress());
        assertEquals("Krakow", testCinema.getCity());
        verify(cinemaRepository).save(testCinema);
    }

    @Test
    void updateCinema_NonExistingCinema_ThrowsException() {
        // Given
        when(cinemaRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CinemaNotFoundException.class,
                () -> cinemaService.updateCinema(99L, new Cinema()));
    }

    @Test
    void deleteCinema_ExistingCinema_Success() {
        // Given
        when(cinemaRepository.existsById(1L)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> cinemaService.deleteCinema(1L));

        // Then
        verify(cinemaRepository).deleteById(1L);
    }

    @Test
    void deleteCinema_NonExistingCinema_ThrowsException() {
        // Given
        when(cinemaRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(CinemaNotFoundException.class,
                () -> cinemaService.deleteCinema(99L));
        verify(cinemaRepository, never()).deleteById(any());
    }

    // Cinema Hall Tests
    @Test
    void createCinemaHall_ValidData_Success() {
        // Given
        when(cinemaHallRepository.save(any(CinemaHall.class))).thenReturn(testHall);

        // When
        CinemaHall result = cinemaService.createCinemaHall(testHall);

        // Then
        assertNotNull(result);
        assertEquals("Hall 1", result.getName());
        assertEquals(100, result.getTotalSeats());
        verify(cinemaHallRepository).save(testHall);
    }

    @Test
    void createCinemaHall_NullName_ThrowsException() {
        // Given
        testHall.setName(null);

        // When & Then
        assertThrows(InvalidCinemaHallDataException.class,
                () -> cinemaService.createCinemaHall(testHall));
    }

    @Test
    void createCinemaHall_InvalidTotalSeats_ThrowsException() {
        // Given
        testHall.setTotalSeats(0);

        // When & Then
        assertThrows(InvalidCinemaHallDataException.class,
                () -> cinemaService.createCinemaHall(testHall));
    }

    @Test
    void createCinemaHall_InvalidRows_ThrowsException() {
        // Given
        testHall.setRows(-1);

        // When & Then
        assertThrows(InvalidCinemaHallDataException.class,
                () -> cinemaService.createCinemaHall(testHall));
    }

    @Test
    void createCinemaHall_InvalidSeatsPerRow_ThrowsException() {
        // Given
        testHall.setSeatsPerRow(0);

        // When & Then
        assertThrows(InvalidCinemaHallDataException.class,
                () -> cinemaService.createCinemaHall(testHall));
    }

    @Test
    void createCinemaHall_TotalSeatsMismatch_ThrowsException() {
        // Given
        testHall.setRows(10);
        testHall.setSeatsPerRow(10);
        testHall.setTotalSeats(99); // Should be 100

        // When & Then
        InvalidCinemaHallDataException exception = assertThrows(InvalidCinemaHallDataException.class,
                () -> cinemaService.createCinemaHall(testHall));
        assertTrue(exception.getMessage().contains("Total seats must equal rows * seats per row"));
    }

    @Test
    void findHallById_ExistingHall_ReturnsHall() {
        // Given
        when(cinemaHallRepository.findById(1L)).thenReturn(Optional.of(testHall));

        // When
        Optional<CinemaHall> result = cinemaService.findHallById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Hall 1", result.get().getName());
    }

    @Test
    void findHallsByCinemaId_ReturnsHalls() {
        // Given
        List<CinemaHall> halls = Arrays.asList(testHall);
        when(cinemaHallRepository.findByCinemaId(1L)).thenReturn(halls);

        // When
        List<CinemaHall> result = cinemaService.findHallsByCinemaId(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals("Hall 1", result.get(0).getName());
    }

    @Test
    void findHallsByType_ReturnsHallsOfType() {
        // Given
        when(cinemaHallRepository.findByHallType(CinemaHall.HallType.STANDARD))
                .thenReturn(Arrays.asList(testHall));

        // When
        List<CinemaHall> result = cinemaService.findHallsByType(CinemaHall.HallType.STANDARD);

        // Then
        assertEquals(1, result.size());
        assertEquals(CinemaHall.HallType.STANDARD, result.get(0).getHallType());
    }

    @Test
    void updateCinemaHall_ExistingHall_Success() {
        // Given
        CinemaHall updates = new CinemaHall();
        updates.setName("Updated Hall");
        updates.setTotalSeats(150);
        updates.setRows(15);
        updates.setSeatsPerRow(10);
        updates.setHallType(CinemaHall.HallType.VIP);

        when(cinemaHallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(cinemaHallRepository.save(any(CinemaHall.class))).thenReturn(testHall);

        // When
        CinemaHall result = cinemaService.updateCinemaHall(1L, updates);

        // Then
        assertNotNull(result);
        assertEquals("Updated Hall", testHall.getName());
        assertEquals(150, testHall.getTotalSeats());
        assertEquals(CinemaHall.HallType.VIP, testHall.getHallType());
        verify(cinemaHallRepository).save(testHall);
    }

    @Test
    void updateCinemaHall_NonExistingHall_ThrowsException() {
        // Given
        when(cinemaHallRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CinemaHallNotFoundException.class,
                () -> cinemaService.updateCinemaHall(99L, new CinemaHall()));
    }

    @Test
    void deleteCinemaHall_ExistingHall_Success() {
        // Given
        when(cinemaHallRepository.existsById(1L)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> cinemaService.deleteCinemaHall(1L));

        // Then
        verify(cinemaHallRepository).deleteById(1L);
    }

    @Test
    void deleteCinemaHall_NonExistingHall_ThrowsException() {
        // Given
        when(cinemaHallRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(CinemaHallNotFoundException.class,
                () -> cinemaService.deleteCinemaHall(99L));
        verify(cinemaHallRepository, never()).deleteById(any());
    }
}