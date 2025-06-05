package com.drumtong.backend.api.group.controller;

import com.drumtong.backend.api.group.dto.GroupMemberCommandDto;
import com.drumtong.backend.api.group.dto.GroupMemberQueryDto;
import com.drumtong.backend.api.group.service.GroupMemberCommandService;
import com.drumtong.backend.api.group.service.GroupMemberQueryService;
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

@RestController
@RequestMapping("/api/group-members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberCommandService commandService;
    private final GroupMemberQueryService queryService;

    // 생성
    @PostMapping
    public ResponseEntity<Long> create(@RequestBody GroupMemberCommandDto dto) {
        Long id = commandService.createGroupMember(dto);
        return ResponseEntity.ok(id);
    }

    // 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody GroupMemberCommandDto dto) {
        commandService.updateGroupMember(id, dto);
        return ResponseEntity.ok().build();
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commandService.deleteGroupMember(id);
        return ResponseEntity.ok().build();
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<GroupMemberQueryDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.findById(id));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<GroupMemberQueryDto>> findAll() {
        return ResponseEntity.ok(queryService.findAll());
    }
}
