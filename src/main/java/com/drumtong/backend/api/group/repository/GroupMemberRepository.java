package com.drumtong.backend.api.group.repository;

import com.drumtong.backend.api.group.entity.GroupMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findUsersByCalendarId(Long calendarId);

    List<GroupMember> findByUserId(Long userId);

    void deleteByCalendarIdAndUserId(Long calendarId, Long userId);
}
