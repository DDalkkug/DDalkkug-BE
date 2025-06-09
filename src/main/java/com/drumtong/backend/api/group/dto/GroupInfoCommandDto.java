package com.drumtong.backend.api.group.dto;

import lombok.Data;

@Data
public class GroupInfoCommandDto {
    private Long leaderId;
    private String name;
    private String description;
    private Integer totalPaid;
}
