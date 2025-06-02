package com.drumtong.backend.api.calendar.repository;

import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {
    List<CalendarEntry> findByUserId(Long userId);
}
