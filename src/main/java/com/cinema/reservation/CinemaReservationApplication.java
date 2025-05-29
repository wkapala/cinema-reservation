package com.cinema.reservation;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(
        name   = "basicAuth",
        type   = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class CinemaReservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaReservationApplication.class, args);
    }
}
