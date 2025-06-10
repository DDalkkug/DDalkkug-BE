package com.drumtong.backend.api.groupInfo.service;


import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.common.exception.NotFoundException;
import com.drumtong.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInfoCommandService {

    private final GroupInfoRepository groupInfoRepository;

    public Long createGroupInfo(GroupInfoDto dto) {
        if (groupInfoRepository.findById(dto.getLeaderId()).isPresent()) {
            throw new NotFoundException(ErrorStatus.GROUP_ALREADY_EXISTS_EXCEPTION.getMessage());
        }
        GroupInfo groupInfo = GroupInfo.builder()
                .leaderId(dto.getLeaderId())
                .name(dto.getName())
                .description(dto.getDescription())
                .totalPaid(dto.getTotalPaid() != null ? dto.getTotalPaid() : 0)
                .build();
        groupInfoRepository.save(groupInfo);
        return groupInfo.getId();
    }

    public void updateGroupInfo(Long groupId, GroupInfoDto dto) {
        GroupInfo groupInfo = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        groupInfo.update(dto.getLeaderId(), dto.getName(), dto.getDescription(), dto.getTotalPaid());
        groupInfoRepository.save(groupInfo);
    }

    public void updateGroupInfo(String groupName, GroupInfoDto dto) {
        GroupInfo groupInfo = groupInfoRepository.findByName(groupName)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        groupInfo.update(dto.getLeaderId(), dto.getName(), dto.getDescription(), dto.getTotalPaid());
        groupInfoRepository.save(groupInfo);
    }

    public void deleteGroupInfoById(Long id) {
        GroupInfo groupInfo = groupInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
        groupInfoRepository.delete(groupInfo);
    }

    public void deleteGroupInfoByName(String name) {
        GroupInfo groupInfo = groupInfoRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
        groupInfoRepository.delete(groupInfo);
    }
}
