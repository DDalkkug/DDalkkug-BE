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
    private int totalPrice;
    private Map<String, Integer> drinkCounts;
}