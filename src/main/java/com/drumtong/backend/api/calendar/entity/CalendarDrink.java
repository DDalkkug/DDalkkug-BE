package com.drumtong.backend.api.calendar.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "calendar_drinks")
public class CalendarDrink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "calendar_id")
    private CalendarEntry calendarEntry;

    @ManyToOne
    @JoinColumn(name = "drink_id")
    private Drink drink;

    private Integer quantity;
}