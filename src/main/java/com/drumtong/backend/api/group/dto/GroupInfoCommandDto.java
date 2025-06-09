package com.drumtong.backend.api.group.dto;

import lombok.Data;

@Data
public class GroupInfoCommandDto {
    private Long calendarId;
    private Long userId;
    private Integer paidAmount;
}
