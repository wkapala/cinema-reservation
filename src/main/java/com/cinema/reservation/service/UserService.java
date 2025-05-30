package com.cinema.reservation.service;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserStatistics;
import com.cinema.reservation.dto.UserUpdateRequest;
import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.entity.User;
import com.cinema.reservation.exception.InvalidPasswordException;
import com.cinema.reservation.exception.UserAlreadyExistsException;
import com.cinema.reservation.exception.UserDeactivationException;
import com.cinema.reservation.exception.UserNotFoundException;
import com.cinema.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    // Dependency Inversion - zależy od interfejsu, nie implementacji
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserFactory userFactory; // Factory Pattern

    // Single Responsibility - tylko zarządzanie użytkownikami

    /**
     * Factory Pattern + Template Method - tworzenie różnych typów użytkowników
     */
    public User createUser(UserCreateRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Business rule validation
        validateUserCreation(request);

        // Factory Pattern - delegacja tworzenia do factory
        User user = userFactory.createUser(request);

        // Template Method - wspólny proces, różne implementacje
        user = processUserCreation(user);

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Template Method Pattern - wspólny proces dla wszystkich typów użytkowników
     */
    private User processUserCreation(User user) {
        // Enkodowanie hasła
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Polimorfizm - różne zachowanie dla różnych typów
        if (user instanceof AdminUser admin) {
            log.info("Processing admin user creation for department: {}", admin.getDepartment());
            // Dodatkowa logika dla adminów
        } else if (user instanceof RegularUser regular) {
            log.info("Processing regular user creation");
            // Dodatkowa logika dla zwykłych użytkowników
        }

        return user;
    }

    /**
     * Business logic validation - Open/Closed Principle
     */
    private void validateUserCreation(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Walidacja siły hasła
        validatePasswordStrength(request.getPassword());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long");
        }
        // Dodatkowe reguły walidacji hasła
    }

    /**
     * Polimorfizm w akcji - metoda działa z każdym typem User
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Sprawdza czy użytkownik jest właścicielem danych o podanym ID
     * Używane w Spring Security @PreAuthorize
     */
    public boolean isOwner(Long userId, String email) {
        return findById(userId)
                .map(user -> user.getEmail().equals(email))
                .orElse(false);
    }

    /**
     * Business logic - autoryzacja oparta na polimorfizmie
     */
    public boolean canUserPerformAction(Long userId, String action) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Polimorfizm - różne zachowanie dla różnych typów
        return switch (action) {
            case "MAKE_RESERVATION" -> user.canMakeReservation();
            case "MANAGE_MOVIES" -> user.canManageMovies();
            case "VIEW_ALL_RESERVATIONS" -> user.canViewAllReservations();
            default -> false;
        };
    }

    /**
     * Analytics - używa repository do business intelligence
     */
    public UserStatistics getUserStatistics() {
        long regularUsers = userRepository.countRegularUsers();
        long adminUsers = userRepository.countAdminUsers();
        List<User> recentUsers = userRepository.findRecentUsers(LocalDateTime.now().minusDays(30));

        return UserStatistics.builder()
                .regularUsersCount(regularUsers)
                .adminUsersCount(adminUsers)
                .recentUsersCount((long) recentUsers.size())
                .totalUsers(regularUsers + adminUsers)
                .build();
    }

    /**
     * SOLID - metoda do aktualizacji profilu
     */
    public User updateUserProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Aktualizacja wspólnych pól
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Polimorfizm - różne aktualizacje dla różnych typów
        if (user instanceof RegularUser regular && request.getPhoneNumber() != null) {
            regular.setPhoneNumber(request.getPhoneNumber());
        }

        if (user instanceof AdminUser admin && request.getDepartment() != null) {
            admin.setDepartment(request.getDepartment());
        }

        return userRepository.save(user);
    }

    /**
     * Soft delete pattern - nie usuwamy, tylko dezaktywujemy
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Business logic - sprawdzenie czy można dezaktywować
        if (hasActiveReservations(user)) {
            throw new UserDeactivationException("Cannot deactivate user with active reservations");
        }

        // W przyszłości można dodać pole 'active' do User entity
        log.info("User {} deactivated", userId);
    }

    /**
     * Helper method - sprawdzenie aktywnych rezerwacji
     */
    private boolean hasActiveReservations(User user) {
        // Tu będzie integracja z ReservationService
        return false; // placeholder
    }
}