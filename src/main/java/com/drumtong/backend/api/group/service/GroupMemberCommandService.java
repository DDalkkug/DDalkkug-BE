package com.drumtong.backend.api.group.service;


import com.drumtong.backend.api.group.dto.GroupMemberCommandDto;
import com.drumtong.backend.api.group.entity.GroupMember;
import com.drumtong.backend.api.group.repository.GroupMemberRepository;
import com.drumtong.backend.common.exception.NotFoundException;
import com.drumtong.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupMemberCommandService {

    private final GroupMemberRepository groupMemberRepository;

    public Long createGroupMember(GroupMemberCommandDto dto) {
        GroupMember groupMember = GroupMember.builder()
                .calendarId(dto.getCalendarId())
                .userId(dto.getUserId())
                .paidAmount(dto.getPaidAmount())
                .build();
        groupMemberRepository.save(groupMember);
        return groupMember.getId();
    }

    public void updateGroupMember(Long id, GroupMemberCommandDto dto) {
        GroupMember groupMember = groupMemberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));
        groupMember.update(dto.getCalendarId(), dto.getUserId(), dto.getPaidAmount());
    }

    public void deleteGroupMember(Long id) {
        groupMemberRepository.deleteById(id);
    }
}
