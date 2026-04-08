package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "shifts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Double workingHours;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Pre-defined shift types
    public enum ShiftType {
        GENERAL,      // 10:00 AM - 6:30 PM (Default)
        MORNING,      // 9:00 AM - 5:00 PM
        EVENING,      // 2:00 PM - 10:00 PM
        NIGHT,        // 10:00 PM - 6:00 AM
        HALF_DAY,     // 10:00 AM - 2:30 PM
        CUSTOM        // User defined
    }
}
