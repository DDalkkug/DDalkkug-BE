package com.drumtong.backend.api.group.dto;

import lombok.Data;

@Data
public class GroupInfoQueryDto {
    private Long id;
    private Long calendarId;
    private Long userId;
    private Integer paidAmount;
}
