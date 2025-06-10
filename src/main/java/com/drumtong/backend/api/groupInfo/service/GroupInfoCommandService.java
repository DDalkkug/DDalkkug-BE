package com.drumtong.backend.api.groupInfo.service;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.common.exception.UnauthorizedException;
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
    private final com.drumtong.backend.api.groupmember.service.GroupMemberCommandService groupMemberCommandService;

    public Long createGroupInfo(GroupInfoDto dto) {
        GroupInfo groupInfo = GroupInfo.builder()
                .leaderId(dto.getLeaderId())
                .name(dto.getName())
                .description(dto.getDescription())
                .totalPaid(dto.getTotalPaid() != null ? dto.getTotalPaid() : 0) // null이면 0으로 설정
                .build();

        groupInfoRepository.save(groupInfo);

        // 그룹장을 그룹 멤버로 자동 추가
        groupMemberCommandService.addMemberToGroup(groupInfo.getId(), dto.getLeaderId());

        return groupInfo.getId();
    }

    public void updateGroupInfo(Long groupId, GroupInfoDto dto, Long currentUserId) {
        GroupInfo groupInfo = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        // 리더 권한 확인
        if (!groupInfo.getLeaderId().equals(currentUserId)) {
            throw new UnauthorizedException("그룹 리더만 그룹 정보를 수정할 수 있습니다.");
        }

        groupInfo.update(dto.getLeaderId(), dto.getName(), dto.getDescription(), dto.getTotalPaid());
        groupInfoRepository.save(groupInfo);
    }

    public void updateGroupInfo(String groupName, GroupInfoDto dto, Long currentUserId) {
        GroupInfo groupInfo = groupInfoRepository.findByName(groupName)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        // 리더 권한 확인
        if (!groupInfo.getLeaderId().equals(currentUserId)) {
            throw new UnauthorizedException("그룹 리더만 그룹 정보를 수정할 수 있습니다.");
        }

        groupInfo.update(dto.getLeaderId(), dto.getName(), dto.getDescription(), dto.getTotalPaid());
        groupInfoRepository.save(groupInfo);
    }

    public void deleteGroupInfoById(Long id, Long currentUserId) {
        GroupInfo groupInfo = groupInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        // 리더 권한 확인
        if (!groupInfo.getLeaderId().equals(currentUserId)) {
            throw new UnauthorizedException("그룹 리더만 그룹을 삭제할 수 있습니다.");
        }

        groupInfoRepository.delete(groupInfo);
    }

    public void deleteGroupInfoByName(String name, Long currentUserId) {
        GroupInfo groupInfo = groupInfoRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        // 리더 권한 확인
        if (!groupInfo.getLeaderId().equals(currentUserId)) {
            throw new UnauthorizedException("그룹 리더만 그룹을 삭제할 수 있습니다.");
        }

        groupInfoRepository.delete(groupInfo);
    }
}