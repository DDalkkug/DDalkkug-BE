package com.drumtong.backend.api.calendar.dto;

import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CalendarEntryResponseDto {
    private Long id;
    private Long userId;
    private LocalDate drinkingDate;
    private String memo;
    private Integer totalPrice;
    private String photoUrl;
    private LocalDateTime createdAt;
    private List<DrinkDto> drinks; // Added drinks list

    public CalendarEntryResponseDto(CalendarEntry entry) {
        this.id = entry.getId();
        this.userId = entry.getUserId();
        this.drinkingDate = entry.getDrinkingDate();
        this.memo = entry.getMemo();
        this.totalPrice = entry.getTotalPrice();
        this.photoUrl = entry.getPhotoUrl();
        this.createdAt = entry.getCreatedAt();
    }

}