package com.drumtong.backend.api.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MonthlyExpenseDto {
    private int year;
    private int month;
    private int totalPrice;
}