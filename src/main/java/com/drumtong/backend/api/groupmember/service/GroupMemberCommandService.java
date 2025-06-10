package com.drumtong.backend.api.groupmember.service;

import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.api.groupmember.entity.GroupMember;
import com.drumtong.backend.api.groupmember.repository.GroupMemberRepository;
import com.drumtong.backend.api.member.repository.MemberRepository;
import com.drumtong.backend.common.exception.BadRequestException;
import com.drumtong.backend.common.exception.UnauthorizedException;
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
    private final GroupInfoRepository groupInfoRepository;
    private final MemberRepository memberRepository;

    /**
     * 그룹에 멤버 추가
     */
    public Long addMemberToGroup(Long groupId, Long memberId) {
        // 그룹 존재 여부 확인
        if (!groupInfoRepository.existsById(groupId)) {
            throw new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage());
        }

        // 멤버 존재 여부 확인
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(ErrorStatus.USER_NOT_FOUND_EXCEPTION.getMessage());
        }

        // 이미 그룹에 멤버가 있는지 확인
        if (groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId).isPresent()) {
            throw new BadRequestException("이미 그룹에 참여 중인 멤버입니다.");
        }

        GroupMember groupMember = GroupMember.builder()
                .groupId(groupId)
                .memberId(memberId)
                .build();

        groupMemberRepository.save(groupMember);
        return groupMember.getId();
    }

    /**
     * 그룹에서 멤버 제거
     */
    /**
     * 그룹에서 멤버 제거 - 리더만 가능
     */
    public void removeMemberFromGroup(Long groupId, Long memberId, Long currentUserId) {
        // 그룹 정보 조회
        GroupInfo groupInfo = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage()));

        // 리더 권한 확인
        if (!groupInfo.getLeaderId().equals(currentUserId)) {
            throw new UnauthorizedException("그룹 리더만 멤버를 삭제할 수 있습니다.");
        }

        // 그룹에 멤버가 있는지 확인
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new NotFoundException("멤버가 그룹에 존재하지 않습니다."));

        groupMemberRepository.delete(groupMember);
    }
}