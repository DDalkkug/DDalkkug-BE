package com.drumtong.backend.api.calendar.repository;

import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {
    // 기존 메소드들...
    List<CalendarEntry> findByUserId(Long userId);
    List<CalendarEntry> findByUserIdAndDrinkingDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 그룹 관련 메소드 추가
    List<CalendarEntry> findByGroupId(Long groupId);
    List<CalendarEntry> findByUserIdAndIsGroupSharedTrue(Long userId);
    List<CalendarEntry> findByGroupIdAndIsGroupSharedTrue(Long groupId);
    List<CalendarEntry> findByGroupIdAndDrinkingDateBetween(Long groupId, LocalDate startDate, LocalDate endDate);
    List<CalendarEntry> findByGroupIdAndDrinkingDate(Long groupId, LocalDate drinkingDate);
    List<CalendarEntry> findByGroupIdAndGroupEntryId(Long groupId, Long groupEntryId);
    List<CalendarEntry> findByGroupEntryId(Long groupEntryId);
}