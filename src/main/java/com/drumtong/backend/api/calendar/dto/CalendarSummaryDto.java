package com.drumtong.backend.api.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CalendarSummaryDto {
    private LocalDate date;
    private int totalEntries;
    private int totalPrice; // 추가: 해당 날짜의 총 가격
    private Map<String, Integer> drinkCounts; // Map of drink type to count
}