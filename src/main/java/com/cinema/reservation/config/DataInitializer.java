package com.cinema.reservation.config;

import com.cinema.reservation.dto.UserCreateRequest;
import com.cinema.reservation.dto.UserType;
import com.cinema.reservation.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        try {
            // Create default admin user if not exists
            if (userService.findByEmail("admin@cinema.com").isEmpty()) {
                UserCreateRequest adminRequest = UserCreateRequest.builder()
                        .userType(UserType.ADMIN)
                        .email("admin@cinema.com")
                        .password("admin1234!")
                        .firstName("System")
                        .lastName("Administrator")
                        .department("IT")
                        .adminLevel("SUPER_ADMIN")
                        .build();

                userService.createUser(adminRequest);
                log.info("Default admin user created: admin@cinema.com / admin123");
            }

            // Create test regular user if not exists
            if (userService.findByEmail("user@cinema.com").isEmpty()) {
                UserCreateRequest userRequest = UserCreateRequest.builder()
                        .userType(UserType.REGULAR)
                        .email("user@cinema.com")
                        .password("user1234!")
                        .firstName("Test")
                        .lastName("User")
                        .phoneNumber("+48123456789")
                        .build();

                userService.createUser(userRequest);
                log.info("Default regular user created: user@cinema.com / user123");
            }
        } catch (Exception e) {
            log.error("Error initializing data: ", e);
        }
    }
}
