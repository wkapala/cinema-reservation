package com.cinema.reservation.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonBackReference
    private Cinema cinema;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Screening> screenings;

    public enum HallType {
        STANDARD, IMAX, VIP, DOLBY_ATMOS
    }

    /**
     * Alias getter for backward compatibility of tests expecting 'capacity'
     */
    @Transient
    public Integer getCapacity() {
        return totalSeats;
    }

    /**
     * Alias setter for backward compatibility of tests expecting 'capacity'
     */
    public void setCapacity(Integer capacity) {
        this.totalSeats = capacity;
    }
}
