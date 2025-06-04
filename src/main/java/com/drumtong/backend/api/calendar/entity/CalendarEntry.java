package com.drumtong.backend.api.calendar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "calendar_entries")
public class CalendarEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate drinkingDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    private Integer totalPrice;
    private String photoUrl;

    private LocalDateTime createdAt = LocalDateTime.now();


}
