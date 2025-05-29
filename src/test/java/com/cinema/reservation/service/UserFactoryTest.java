package com.cinema.reservation.service;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    private UserFactory userFactory;
    private UserCreateRequest regularUserRequest;
    private UserCreateRequest adminUserRequest;

    @BeforeEach
    void setUp() {
        userFactory = new UserFactory();

        regularUserRequest = new UserCreateRequest();
        regularUserRequest.setUserType(UserType.REGULAR);
        regularUserRequest.setEmail("user@test.com");
        regularUserRequest.setPassword("password123");
        regularUserRequest.setFirstName("John");
        regularUserRequest.setLastName("Doe");
        regularUserRequest.setPhoneNumber("+48123456789");

        adminUserRequest = new UserCreateRequest();
        adminUserRequest.setUserType(UserType.ADMIN);
        adminUserRequest.setEmail("admin@test.com");
        adminUserRequest.setPassword("adminpass123");
        adminUserRequest.setFirstName("Admin");
        adminUserRequest.setLastName("User");
        adminUserRequest.setDepartment("IT");
        adminUserRequest.setAdminLevel("SUPER");
    }

    @Test
    void createUser_RegularUser_ReturnsRegularUser() {
        // When
        User user = userFactory.createUser(regularUserRequest);

        // Then
        assertNotNull(user);
        assertInstanceOf(RegularUser.class, user);
        assertEquals("user@test.com", user.getEmail());
        assertEquals("John", user.getFirstName());
    }

    @Test
    void createUser_AdminUser_ReturnsAdminUser() {
        // When
        User user = userFactory.createUser(adminUserRequest);

        // Then
        assertNotNull(user);
        assertInstanceOf(AdminUser.class, user);
        AdminUser admin = (AdminUser) user;
        assertEquals("admin@test.com", admin.getEmail());
        assertEquals("IT", admin.getDepartment());
    }

    @Test
    void createUserWithValidation_InvalidPhoneNumber_ThrowsException() {
        // Given
        regularUserRequest.setPhoneNumber("invalid");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> userFactory.createUserWithValidation(regularUserRequest));
    }

    @Test
    void createUserWithValidation_AdminWithoutDepartment_ThrowsException() {
        // Given
        adminUserRequest.setDepartment(null);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> userFactory.createUserWithValidation(adminUserRequest));
    }
}