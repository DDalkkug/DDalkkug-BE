package com.drumtong.backend.api.groupmember.controller;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupmember.service.GroupMemberCommandService;
import com.drumtong.backend.api.groupmember.service.GroupMemberQueryService;
import com.drumtong.backend.api.member.dto.MemberSimpleDto;
import com.drumtong.backend.common.config.security.SecurityMember;
import com.drumtong.backend.common.response.ApiResponse;
import com.drumtong.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "GroupMember", description = "그룹 멤버 관리 API 입니다.")
@RestController
@RequestMapping("/api/v1/group-member")
@RequiredArgsConstructor
public class GroupMemberController {
    private final GroupMemberCommandService commandService;
    private final GroupMemberQueryService queryService;

    /**
     * 그룹에 멤버 추가
     */
    @PostMapping("/{groupId}/members")
    @Operation(summary = "그룹에 멤버 추가")
    public ResponseEntity<ApiResponse<Long>> addMemberToGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal SecurityMember securityMember
    ) {
        Long id = commandService.addMemberToGroup(groupId, securityMember.getId());
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, id);
    }

    /**
     * 그룹에서 멤버 삭제 - 그룹 리더만 가능
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "그룹에서 멤버 삭제 (리더만 가능)")
    public ResponseEntity<ApiResponse<Void>> removeMemberFromGroup(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable Long groupId,
            @PathVariable Long memberId) {
        // 현재 로그인한 사용자 ID를 서비스로 전달하여 권한 확인
        commandService.removeMemberFromGroup(groupId, memberId, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }

    /**
     * 그룹 탈퇴
     */
    @DeleteMapping("quit/{groupId}/")
    @Operation(summary = "그룹 탈퇴")
    public ResponseEntity<ApiResponse<Void>> quitMemberFromGroup(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable Long groupId) {
        // 현재 로그인한 사용자 ID를 서비스로 전달하여 권한 확인
        commandService.quitMemberFromGroup(groupId, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }


    /**
     * 그룹 멤버 조회
     */
    @GetMapping("/{groupId}/members")
    @Operation(summary = "그룹 멤버 목록 조회")
    public ResponseEntity<ApiResponse<List<MemberSimpleDto>>> getGroupMembers(
            @PathVariable Long groupId) {
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, queryService.getGroupMembers(groupId));
    }

    /**
     * 현재 로그인한 사용자가 속한 그룹 목록 조회
     */
    @GetMapping("/my-groups")
    @Operation(summary = "내가 속한 그룹 목록 조회")
    public ResponseEntity<ApiResponse<List<GroupInfoDto>>> getMyGroups(
            @AuthenticationPrincipal SecurityMember securityMember) {
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS,
                queryService.getMemberGroups(securityMember.getId()));
    }
}