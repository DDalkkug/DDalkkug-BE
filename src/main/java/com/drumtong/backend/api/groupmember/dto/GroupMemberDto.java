package com.drumtong.backend.api.groupmember.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDto {
    private Long id;
    private Long groupId;
    private Long memberId;
    private String memberNickname; // 멤버 정보 표시용 (선택사항)
}