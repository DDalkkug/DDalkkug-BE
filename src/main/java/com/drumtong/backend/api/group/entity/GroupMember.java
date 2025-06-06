package com.drumtong.backend.api.group.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long calendarId;
    private Long userId;
    private int paidAmount;


    public void update(Long calendarId, Long userId, Integer paidAmount) {
        this.calendarId = calendarId;
        this.userId = userId;
        this.paidAmount = paidAmount;
    }
}
