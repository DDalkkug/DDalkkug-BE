package com.drumtong.backend.api.groupInfo.controller;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.service.GroupInfoCommandService;
import com.drumtong.backend.api.groupInfo.service.GroupInfoQueryService;
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

@Tag(name = "GroupInfo", description = "GroupInfo 관련 API 입니다.")
@RestController
@RequestMapping("/api/v1/group-info")
@RequiredArgsConstructor
public class GroupInfoController {

    private final GroupInfoCommandService commandService;
    private final GroupInfoQueryService queryService;

    /**
     * 그룹 정보 생성
     * @param dto 그룹 정보 생성 정보
     * @return 생성된 그룹의 ID
     */
    @PostMapping
    @Operation(summary = "그룹 생성")
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody GroupInfoDto dto) {
        Long id = commandService.createGroupInfo(dto);
        return ApiResponse.success(SuccessStatus.SEND_GROUP_CREATE_SUCCESS, id);
    }

    /**
     * 그룹 수정 - groupId
     *
     * @param groupId 그룹 ID
     * @param dto     수정할 그룹 정보
     * @return 성공 여부
     */
    @PutMapping("/id/{groupId}")
    @Operation(summary = "그룹 수정")
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody GroupInfoDto dto, @PathVariable long groupId) {
        queryService.findByGroupId(groupId);
        commandService.updateGroupInfo(groupId, dto);
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_UPDATE_SUCCESS);
    }

    /**
     * 그룹 수정 - 이름
     *
     * @param groupName 그룹 이름
     * @param dto       수정할 그룹 정보
     * @return 성공 여부
     */

    @PutMapping("/name/{groupName}")
    @Operation(summary = "그룹 수정 - 이름")
    public ResponseEntity<ApiResponse<Void>> updateByName(@RequestBody GroupInfoDto dto, @PathVariable String groupName) {
        queryService.findByName(groupName);
        commandService.updateGroupInfo(groupName, dto);
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_UPDATE_SUCCESS);
    }

    /**
     * 그룹 삭제 - 이름
     * @param name 삭제하고자 하는 그룹 이름
     * @return 성공 여부
     */
    @DeleteMapping("/name/{name}")
    @Operation(summary = "그룹 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String name) {
        commandService.deleteGroupInfoByName(name);
        return ApiResponse.success_only(SuccessStatus.SEND_GROUP_DELETE_SUCCESS);
    }
    /**
     * 그룹 삭제 - ID
     * @param groupId 삭제하고자 하는 그룹 ID
     * @return 성공 여부
     */
    
    @DeleteMapping("/id/{groupId}")
    @Operation(summary = "그룹 삭제 - ID")
    public ResponseEntity<ApiResponse<Void>> deleteById(@PathVariable Long groupId) {
        commandService.deleteGroupInfoById(groupId);
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
}
