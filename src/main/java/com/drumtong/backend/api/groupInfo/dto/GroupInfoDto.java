package com.drumtong.backend.api.groupInfo.dto;

import lombok.Data;

@Data
public class GroupInfoDto {
    private Long id;
    private Long leaderId;
    private String name;
    private String description;
    private Integer totalPaid;
}
