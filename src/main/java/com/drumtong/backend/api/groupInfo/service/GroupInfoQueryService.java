package com.drumtong.backend.api.groupInfo.service;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.common.exception.BadRequestException;
import com.drumtong.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInfoQueryService {

    private final GroupInfoRepository groupInfoRepository;

    public GroupInfoDto findByGroupId(Long groupId) {
        GroupInfo group = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new BadRequestException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
        return toDto(group);
    }

    public List<GroupInfoDto> findAllByLeaderId(Long leaderId) {
        List<GroupInfo> groups = groupInfoRepository.findAll().stream()
                .filter(group -> group.getLeaderId().equals(leaderId))
                .toList();

        if (groups.isEmpty()) {
            throw new BadRequestException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage());
        }

        return groups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public GroupInfoDto findByName(String GroupName) {
        return groupInfoRepository.findByName(GroupName)
                .map(this::toDto)
                .orElseThrow(() -> new BadRequestException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
    }

    public List<GroupInfoDto> findAll() {
        return groupInfoRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public int getTotalPaidByName(String name) {
        return groupInfoRepository.findAll().stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .map(GroupInfo::getTotalPaid)
                .orElseThrow(() -> new BadRequestException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
    }

    private GroupInfoDto toDto(GroupInfo group) {
        GroupInfoDto dto = new GroupInfoDto();
        dto.setId(group.getId());
        dto.setLeaderId(group.getLeaderId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setTotalPaid(group.getTotalPaid());
        return dto;
    }
}
