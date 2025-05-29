package com.cinema.reservation.service;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.entity.AdminUser;
import com.cinema.reservation.entity.RegularUser;
import com.cinema.reservation.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern - Single Responsibility do tworzenia różnych typów użytkowników
 * Open/Closed Principle - łatwo dodać nowe typy bez modyfikacji istniejącego kodu
 */
@Component
@Slf4j
public class UserFactory {

    /**
     * Factory Method - tworzy odpowiedni typ użytkownika
     * Polimorfizm - zwraca User, ale konkretny typ zależy od UserType
     */
    public User createUser(UserCreateRequest request) {
        log.debug("Creating user of type: {}", request.getUserType());

        return switch (request.getUserType()) {
            case REGULAR -> createRegularUser(request);
            case ADMIN -> createAdminUser(request);
            // Open/Closed - łatwo dodać VIP, PREMIUM, etc.
        };
    }

    /**
     * Factory methods for specific user types
     */
    private RegularUser createRegularUser(UserCreateRequest request) {
        return RegularUser.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // będzie zaszyfrowane w UserService
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    private AdminUser createAdminUser(UserCreateRequest request) {
        return AdminUser.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // będzie zaszyfrowane w UserService
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .department(request.getDepartment())
                .adminLevel(request.getAdminLevel())
                .build();
    }

    /**
     * Factory method z walidacją business rules
     */
    public User createUserWithValidation(UserCreateRequest request) {
        validateUserRequest(request);
        return createUser(request);
    }

    /**
     * Business validation logic
     */
    private void validateUserRequest(UserCreateRequest request) {
        if (request.getUserType() == UserType.ADMIN) {
            if (request.getDepartment() == null || request.getDepartment().trim().isEmpty()) {
                throw new IllegalArgumentException("Department is required for admin users");
            }
        }

        if (request.getUserType() == UserType.REGULAR) {
            if (request.getPhoneNumber() != null && !isValidPhoneNumber(request.getPhoneNumber())) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        // Prosta walidacja numeru telefonu
        return phoneNumber.matches("\\+?[0-9]{9,15}");
    }
}