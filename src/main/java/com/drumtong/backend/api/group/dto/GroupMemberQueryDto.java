package com.drumtong.backend.api.group.dto;

import com.drumtong.backend.api.group.entity.GroupMember;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class GroupMemberQueryDto {
    private Long id;
    private Long calendarId;
    private Long userId;
    private Integer paidAmount;
}
