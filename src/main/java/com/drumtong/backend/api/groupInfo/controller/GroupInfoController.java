package com.drumtong.backend.api.groupInfo.controller;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.service.GroupInfoCommandService;
import com.drumtong.backend.api.groupInfo.service.GroupInfoQueryService;
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

@Tag(name = "GroupInfo", description = "GroupInfo 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/group-info")
@RequiredArgsConstructor
public class GroupInfoController {

    private final GroupInfoCommandService commandService;
    private final GroupInfoQueryService queryService;

    /**
     * 그룹 정보 생성 - 현재 로그인한 사용자 ID 자동 설정
     */
    @PostMapping
    @Operation(summary = "그룹 생성")
    public ResponseEntity<ApiResponse<Long>> create(
            @AuthenticationPrincipal SecurityMember securityMember,
            @RequestBody GroupInfoDto dto) {
        // 현재 로그인한 사용자 ID를 leaderId로 자동 설정
        dto.setLeaderId(securityMember.getId());

        // totalPaid는 서비스에서 null일 경우 0으로 자동 설정됨
        Long id = commandService.createGroupInfo(dto);
        return ApiResponse.success(SuccessStatus.SEND_GROUP_CREATE_SUCCESS, id);
    }

    /**
     * 그룹 수정 - groupId (리더만 가능)
     */
    @PutMapping("/id/{groupId}")
    @Operation(summary = "그룹 수정 (리더만 가능)")
    public ResponseEntity<ApiResponse<Void>> update(
            @AuthenticationPrincipal SecurityMember securityMember,
            @RequestBody GroupInfoDto dto,
        @PathVariable long groupId) {
            queryService.findByGroupId(groupId);
        commandService.updateGroupInfo(groupId, dto, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_UPDATE_SUCCESS);
    }

    /**
     * 그룹 수정 - 이름 (리더만 가능)
     */
    @PutMapping("/name/{groupName}")
    @Operation(summary = "그룹 수정 - 이름 (리더만 가능)")
    public ResponseEntity<ApiResponse<Void>> updateByName(
            @AuthenticationPrincipal SecurityMember securityMember,
            @RequestBody GroupInfoDto dto,
            @PathVariable String groupName) {
        queryService.findByName(groupName);
        commandService.updateGroupInfo(groupName, dto, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_UPDATE_SUCCESS);
    }

    /**
     * 그룹 삭제 - 이름 (리더만 가능)
     */
    @DeleteMapping("/name/{name}")
    @Operation(summary = "그룹 삭제 (리더만 가능)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable String name) {
        commandService.deleteGroupInfoByName(name, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_DELETE_SUCCESS);
    }

    /**
     * 그룹 삭제 - ID (리더만 가능)
     */
    @DeleteMapping("/id/{groupId}")
    @Operation(summary = "그룹 삭제 - ID (리더만 가능)")
    public ResponseEntity<ApiResponse<Void>> deleteById(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable Long groupId) {
        commandService.deleteGroupInfoById(groupId, securityMember.getId());
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_DELETE_SUCCESS);
    }

    /**
     * 그룹 단건 조회 - 이름
     * @param name 조회하고자 하는 그룹 이름
     * @return 조회된 그룹 정보
     */
    @GetMapping("/name/{name}")
    @Operation(summary = "그룹 단건 조회 - 이름")
    public ResponseEntity<ApiResponse<GroupInfoDto>> findByName(@PathVariable String name) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS, queryService.findByName(name));
    }

    /**
     * 그룹 단건 조회 -그룹 id
     * @param id 조회하고자 하는 그룹 ID
     * @return 조회된 그룹 정보
     */
    @GetMapping("/id/{id}")
    @Operation(summary = "그룹 단건 조회")
    public ResponseEntity<ApiResponse<GroupInfoDto>> findById(@PathVariable Long id) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS, queryService.findByGroupId(id));
    }

    /**
     * 그룹 조회 - 리더 ID
     * @param leaderId 조회하고자 하는 그룹 리더 ID
     * @return 조회된 그룹 정보 리스트
     */
    @GetMapping("/id/{leaderId}")
    @Operation(summary = "그룹 단건 조회 - 이름")
    public ResponseEntity<ApiResponse<List<GroupInfoDto>>> findAllByLeaderId(@PathVariable Long leaderId) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS, queryService.findAllByLeaderId(leaderId));
    }

    /**
     * 그룹 전체 조회
     * @return 조회돈 그룹 정보 리스트
     */
    @GetMapping
    @Operation(summary = "그룹 전체 조회")
    public ResponseEntity<ApiResponse<List<GroupInfoDto>>> findAll() {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_LIST_SUCCESS, queryService.findAll());
    }
    @GetMapping("/my-leading-groups")
    @Operation(summary = "내가 리더인 그룹 목록 조회")
    public ResponseEntity<ApiResponse<List<GroupInfoDto>>> getMyLeadingGroups(
            @AuthenticationPrincipal SecurityMember securityMember) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS,
                queryService.findAllByLeaderId(securityMember.getId()));
    }
    @GetMapping("/not-joined-groups")
    @Operation(summary = "내가 아직 가입하지 않은 그룹 목록 조회")
    public ResponseEntity<ApiResponse<List<GroupInfoDto>>> getNotJoinedGroups(
            @AuthenticationPrincipal SecurityMember securityMember) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS,
                queryService.findGroupsNotJoinedByMember(securityMember.getId()));
    }
}
