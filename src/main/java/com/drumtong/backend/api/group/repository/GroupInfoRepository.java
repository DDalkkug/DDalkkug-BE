package com.drumtong.backend.api.group.repository;

import com.drumtong.backend.api.group.entity.GroupInfo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInfoRepository extends JpaRepository<GroupInfo, Long> {
    List<GroupInfo> findUsersByCalendarId(Long calendarId);

    List<GroupInfo> findByUserId(Long userId);

    void deleteByCalendarIdAndUserId(Long calendarId, Long userId);
}
