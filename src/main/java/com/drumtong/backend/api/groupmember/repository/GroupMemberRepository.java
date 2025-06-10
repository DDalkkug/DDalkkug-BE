package com.drumtong.backend.api.groupmember.repository;

import com.drumtong.backend.api.groupmember.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByMemberId(Long memberId);
    Optional<GroupMember> findByGroupIdAndMemberId(Long groupId, Long memberId);
    void deleteByGroupIdAndMemberId(Long groupId, Long memberId);
}