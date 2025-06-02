package com.drumtong.backend.api.calendar.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CalendarEntryRequestDto {
    private Long userId;
    private LocalDate drinkingDate;
    private String memo;
    private Integer totalPrice;
    private String photoUrl;
}
