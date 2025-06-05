package com.drumtong.backend.api.group.service;

import com.drumtong.backend.api.group.dto.GroupMemberQueryDto;
import com.drumtong.backend.api.group.entity.GroupMember;
import com.drumtong.backend.api.group.repository.GroupMemberRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional()
public class GroupMemberQueryService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupMemberQueryDto findById(Long id) {
        GroupMember gm = groupMemberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("그룹 멤버를 찾을 수 없습니다."));
        return toDto(gm);
    }

    public List<GroupMemberQueryDto> findAll() {
        return groupMemberRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private GroupMemberQueryDto toDto(GroupMember gm) {
        GroupMemberQueryDto dto = new GroupMemberQueryDto();
        dto.setId(gm.getId());
        dto.setCalendarId(gm.getCalendarId());
        dto.setUserId(gm.getUserId());
        dto.setPaidAmount(gm.getPaidAmount());
        return dto;
    }
}
