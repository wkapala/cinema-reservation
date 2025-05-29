package com.cinema.reservation.service;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserStatistics;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.dto.UserUpdateRequest;
import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.entity.User;
import com.cinema.reservation.exception.InvalidPasswordException;
import com.cinema.reservation.exception.UserAlreadyExistsException;
import com.cinema.reservation.exception.UserDeactivationException;
import com.cinema.reservation.exception.UserNotFoundException;
import com.cinema.reservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserFactory userFactory;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest regularUserRequest;
    private UserCreateRequest adminUserRequest;
    private RegularUser regularUser;
    private AdminUser adminUser;

    @BeforeEach
    void setUp() {
        // Setup Regular User Request
        regularUserRequest = new UserCreateRequest();
        regularUserRequest.setUserType(UserType.REGULAR);
        regularUserRequest.setEmail("user@test.com");
        regularUserRequest.setPassword("password123");
        regularUserRequest.setFirstName("John");
        regularUserRequest.setLastName("Doe");
        regularUserRequest.setPhoneNumber("+48123456789");

        // Setup Admin User Request
        adminUserRequest = new UserCreateRequest();
        adminUserRequest.setUserType(UserType.ADMIN);
        adminUserRequest.setEmail("admin@test.com");
        adminUserRequest.setPassword("adminpass123");
        adminUserRequest.setFirstName("Admin");
        adminUserRequest.setLastName("User");
        adminUserRequest.setDepartment("IT");

        // Setup Regular User Entity
        regularUser = new RegularUser();
        regularUser.setId(1L);
        regularUser.setEmail("user@test.com");
        regularUser.setPassword("password123");
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setPhoneNumber("+48123456789");

        // Setup Admin User Entity
        adminUser = new AdminUser();
        adminUser.setId(2L);
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("adminpass123");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setDepartment("IT");
    }

    @Test
    void createUser_RegularUser_Success() {
        // Given
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(userFactory.createUser(regularUserRequest)).thenReturn(regularUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // When
        User result = userService.createUser(regularUserRequest);

        // Then
        assertNotNull(result);
        assertEquals("user@test.com", result.getEmail());
        verify(userRepository).existsByEmail("user@test.com");
        verify(userFactory).createUser(regularUserRequest);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_AdminUser_Success() {
        // Given
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(userFactory.createUser(adminUserRequest)).thenReturn(adminUser);
        when(passwordEncoder.encode("adminpass123")).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // When
        User result = userService.createUser(adminUserRequest);

        // Then
        assertNotNull(result);
        assertEquals("admin@test.com", result.getEmail());
        assertInstanceOf(AdminUser.class, result);
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class,
                () -> userService.createUser(regularUserRequest));

        verify(userRepository).existsByEmail("user@test.com");
        verify(userFactory, never()).createUser(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WeakPassword_ThrowsException() {
        // Given
        regularUserRequest.setPassword("weak");
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        // When & Then
        assertThrows(InvalidPasswordException.class,
                () -> userService.createUser(regularUserRequest));
    }

    @Test
    void createUser_NullPassword_ThrowsException() {
        // Given
        regularUserRequest.setPassword(null);
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        // When & Then
        assertThrows(InvalidPasswordException.class,
                () -> userService.createUser(regularUserRequest));
    }

    @Test
    void findById_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // When
        Optional<User> result = userService.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("user@test.com", result.get().getEmail());
    }

    @Test
    void findById_NonExistingUser_ReturnsEmpty() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(99L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        // When
        Optional<User> result = userService.findByEmail("user@test.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("user@test.com", result.get().getEmail());
    }

    @Test
    void canUserPerformAction_RegularUser_MakeReservation() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // When
        boolean result = userService.canUserPerformAction(1L, "MAKE_RESERVATION");

        // Then
        assertTrue(result);
    }

    @Test
    void canUserPerformAction_RegularUser_ManageMovies() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // When
        boolean result = userService.canUserPerformAction(1L, "MANAGE_MOVIES");

        // Then
        assertFalse(result);
    }

    @Test
    void canUserPerformAction_AdminUser_AllActions() {
        // Given
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        // When
        boolean canMakeReservation = userService.canUserPerformAction(2L, "MAKE_RESERVATION");
        boolean canManageMovies = userService.canUserPerformAction(2L, "MANAGE_MOVIES");
        boolean canViewAllReservations = userService.canUserPerformAction(2L, "VIEW_ALL_RESERVATIONS");

        // Then
        assertTrue(canMakeReservation);
        assertTrue(canManageMovies);
        assertTrue(canViewAllReservations);
    }

    @Test
    void canUserPerformAction_UserNotFound_ReturnsFalse() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        boolean result = userService.canUserPerformAction(99L, "MAKE_RESERVATION");

        // Then
        assertFalse(result);
    }

    @Test
    void canUserPerformAction_UnknownAction_ReturnsFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));

        // When
        boolean result = userService.canUserPerformAction(1L, "UNKNOWN_ACTION");

        // Then
        assertFalse(result);
    }

    @Test
    void getUserStatistics_ReturnsCorrectStats() {
        // Given
        List<User> recentUsers = Arrays.asList(regularUser, adminUser);
        when(userRepository.countRegularUsers()).thenReturn(10L);
        when(userRepository.countAdminUsers()).thenReturn(2L);
        when(userRepository.findRecentUsers(any(LocalDateTime.class))).thenReturn(recentUsers);

        // When
        UserStatistics result = userService.getUserStatistics();

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getRegularUsersCount());
        assertEquals(2L, result.getAdminUsersCount());
        assertEquals(2L, result.getRecentUsersCount());
        assertEquals(12L, result.getTotalUsers());
    }

    @Test
    void updateUserProfile_RegularUser_Success() {
        // Given
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setPhoneNumber("+48987654321");

        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // When
        User result = userService.updateUserProfile(1L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Jane", regularUser.getFirstName());
        assertEquals("Smith", regularUser.getLastName());
        assertEquals("+48987654321", regularUser.getPhoneNumber());
        verify(userRepository).save(regularUser);
    }

    @Test
    void updateUserProfile_AdminUser_Success() {
        // Given
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Super");
        updateRequest.setLastName("Admin");
        updateRequest.setDepartment("HR");

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        // When
        User result = userService.updateUserProfile(2L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Super", adminUser.getFirstName());
        assertEquals("Admin", adminUser.getLastName());
        assertEquals("HR", adminUser.getDepartment());
    }

    @Test
    void updateUserProfile_UserNotFound_ThrowsException() {
        // Given
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> userService.updateUserProfile(99L, updateRequest));
    }

    @Test
    void deactivateUser_NoActiveReservations_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(regularUser));
        // hasActiveReservations returns false by default

        // When
        assertDoesNotThrow(() -> userService.deactivateUser(1L));

        // Then
        verify(userRepository).findById(1L);
    }

    @Test
    void deactivateUser_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> userService.deactivateUser(99L));
    }
}