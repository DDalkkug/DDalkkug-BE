package com.drumtong.backend.api.group.service;


import com.drumtong.backend.api.group.dto.GroupInfoCommandDto;
import com.drumtong.backend.api.group.entity.GroupInfo;
import com.drumtong.backend.api.group.repository.GroupInfoRepository;
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

    public Long createGroupMember(GroupInfoCommandDto dto) {
        GroupInfo groupInfo = GroupInfo.builder()
                .calendarId(dto.getCalendarId())
                .userId(dto.getUserId())
                .paidAmount(dto.getPaidAmount())
                .build();
        groupInfoRepository.save(groupInfo);
        return groupInfo.getId();
    }

    public void updateGroupMember(Long id, GroupInfoCommandDto dto) {
        GroupInfo groupInfo = groupInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
        groupInfo.update(dto.getCalendarId(), dto.getUserId(), dto.getPaidAmount());
    }

    public void deleteGroupMember(Long id) {
        groupInfoRepository.deleteById(id);
    }
}
