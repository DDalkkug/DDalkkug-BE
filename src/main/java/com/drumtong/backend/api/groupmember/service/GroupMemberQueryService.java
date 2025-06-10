package com.drumtong.backend.api.groupmember.service;

import com.drumtong.backend.api.groupInfo.dto.GroupInfoDto;
import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.api.groupmember.dto.GroupMemberDto;
import com.drumtong.backend.api.groupmember.entity.GroupMember;
import com.drumtong.backend.api.groupmember.repository.GroupMemberRepository;
import com.drumtong.backend.api.member.dto.MemberSimpleDto;
import com.drumtong.backend.api.member.entity.Member;
import com.drumtong.backend.api.member.repository.MemberRepository;
import com.drumtong.backend.common.exception.NotFoundException;
import com.drumtong.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupMemberQueryService {
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;
    private final GroupInfoRepository groupInfoRepository;

    /**
     * 그룹에 속한 모든 멤버 조회
     */
    public List<MemberSimpleDto> getGroupMembers(Long groupId) {
        // 그룹 존재 확인
        if (!groupInfoRepository.existsById(groupId)) {
            throw new NotFoundException(ErrorStatus.GROUP_NOT_FOUND_EXCEPTION.getMessage());
        }

        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(groupId);

        return groupMembers.stream()
                .map(gm -> memberRepository.findById(gm.getMemberId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toMemberDto)
                .collect(Collectors.toList());
    }

    /**
     * 멤버가 속한 모든 그룹 조회
     */
    public List<GroupInfoDto> getMemberGroups(Long memberId) {
        // 멤버 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(ErrorStatus.USER_NOT_FOUND_EXCEPTION.getMessage());
        }

        List<GroupMember> memberGroups = groupMemberRepository.findByMemberId(memberId);

        return memberGroups.stream()
                .map(gm -> groupInfoRepository.findById(gm.getGroupId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toGroupInfoDto)
                .collect(Collectors.toList());
    }

    private MemberSimpleDto toMemberDto(Member member) {
        return MemberSimpleDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .build();
    }

    private GroupInfoDto toGroupInfoDto(GroupInfo group) {
        GroupInfoDto dto = new GroupInfoDto();
        dto.setId(group.getId());
        dto.setLeaderId(group.getLeaderId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setTotalPaid(group.getTotalPaid());
        return dto;
    }
}