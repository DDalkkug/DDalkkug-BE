package com.drumtong.backend.api.calendar.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrinkDto {
    private Long id;
    private String name;
    private String type;
    private Integer quantity;
}