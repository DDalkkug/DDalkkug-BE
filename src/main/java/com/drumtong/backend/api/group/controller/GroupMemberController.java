package com.drumtong.backend.api.group.controller;

import com.drumtong.backend.api.group.dto.GroupMemberCommandDto;
import com.drumtong.backend.api.group.dto.GroupMemberQueryDto;
import com.drumtong.backend.api.group.service.GroupMemberCommandService;
import com.drumtong.backend.api.group.service.GroupMemberQueryService;
import com.drumtong.backend.common.response.ApiResponse;
import com.drumtong.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Group", description = "Group 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/group-members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberCommandService commandService;
    private final GroupMemberQueryService queryService;

    /**
     * 그룹 생성
     * @param dto 그룹 멤버 생성 정보
     * @return 생성된 그룹의 ID
     */
    @PostMapping
    @Operation(summary = "그룹 생성")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody GroupMemberCommandDto dto) {
        Long id = commandService.createGroupMember(dto);
        return ApiResponse.success(SuccessStatus.SEND_GROUP_CREATE_SUCCESS, id);
    }

    /**
     * 그룹 수정
     * @param id 그룹 멤버 ID
     * @param dto 수정할 그룹 멤버 정보
     * @return 성공 여부
     */
    @PutMapping("/{id}")
    @Operation(summary = "그룹 수정")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @RequestBody GroupMemberCommandDto dto) {
        commandService.updateGroupMember(id, dto);
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_UPDATE_SUCCESS);
    }

    /**
     * 그룹 삭제
     * @param id 그룹 멤버 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "그룹 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        commandService.deleteGroupMember(id);
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_DELETE_SUCCESS);
    }

    /**
     * 그룹 단건 조회
     * @param id 조회하고자 하는 그룹 ID
     * @return 조회된 그룹 정보
     */
    @GetMapping("/{id}")
    @Operation(summary = "그룹 단건 조회")
    public ResponseEntity<ApiResponse<GroupMemberQueryDto>> findById(@PathVariable Long id) {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_SUCCESS, queryService.findById(id));
    }

    /**
     * 그룹 전체 조회
     * @return 전체 그룹 멤버 정보 리스트
     */
    @GetMapping
    @Operation(summary = "그룹 전체 조회")
    public ResponseEntity<ApiResponse<List<GroupMemberQueryDto>>> findAll() {
        return ApiResponse.success(SuccessStatus.GET_GROUP_INFO_LIST_SUCCESS, queryService.findAll());
    }
}
