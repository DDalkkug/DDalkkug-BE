package com.drumtong.backend.api.groupInfo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leaderId;

    private String name;

    private String description;

    @Column(columnDefinition = "integer default 0")
    private Integer totalPaid;

    public void update(Long leaderId, String name, String description, Integer totalPaid) {
        this.leaderId = leaderId;
        this.name = name;
        this.description = description;
        this.totalPaid = totalPaid;
    }
}
