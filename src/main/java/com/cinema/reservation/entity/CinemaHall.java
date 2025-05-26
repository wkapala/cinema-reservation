package com.cinema.reservation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "cinema_halls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaHall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer rows;

    @Column(nullable = false)
    private Integer seatsPerRow;

    @Enumerated(EnumType.STRING)
    private HallType hallType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL)
    private List<Screening> screenings;

    public enum HallType {
        STANDARD, IMAX, VIP, DOLBY_ATMOS
    }
}