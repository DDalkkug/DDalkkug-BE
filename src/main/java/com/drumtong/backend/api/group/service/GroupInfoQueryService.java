package com.drumtong.backend.api.group.service;

import com.drumtong.backend.api.group.dto.GroupInfoQueryDto;
import com.drumtong.backend.api.group.entity.GroupInfo;
import com.drumtong.backend.api.group.repository.GroupInfoRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional()
public class GroupInfoQueryService {

    private final GroupInfoRepository groupInfoRepository;

    public GroupInfoQueryDto findById(Long id) {
        GroupInfo gm = groupInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("그룹 멤버를 찾을 수 없습니다."));
        return toDto(gm);
    }

    public List<GroupInfoQueryDto> findAll() {
        return groupInfoRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private GroupInfoQueryDto toDto(GroupInfo gi) {
        GroupInfoQueryDto dto = new GroupInfoQueryDto();
        dto.setId(gi.getId());
        dto.setCalendarId(gi.getCalendarId());
        dto.setUserId(gi.getUserId());
        dto.setPaidAmount(gi.getPaidAmount());
        return dto;
    }
}
