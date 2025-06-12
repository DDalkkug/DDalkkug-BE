package com.drumtong.backend.api.calendar.dto;

import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class CalendarEntryResponseDto {
    private Long id;
    private Long userId;
    private Long groupId;  // 그룹 ID 추가
    private LocalDate drinkingDate;
    private String memo;
    private Integer totalPrice;
    private String photoUrl;
    private LocalDateTime createdAt;
    private Boolean isGroupShared; // 그룹 공유 여부
    private List<DrinkDto> drinks;

    public CalendarEntryResponseDto(CalendarEntry entry) {
        this.id = entry.getId();
        this.userId = entry.getUserId();
        this.groupId = entry.getGroupId();
        this.drinkingDate = entry.getDrinkingDate();
        this.memo = entry.getMemo();
        this.totalPrice = entry.getTotalPrice();
        this.photoUrl = entry.getPhotoUrl();
        this.createdAt = entry.getCreatedAt();
        this.isGroupShared = entry.getIsGroupShared();
    }
}