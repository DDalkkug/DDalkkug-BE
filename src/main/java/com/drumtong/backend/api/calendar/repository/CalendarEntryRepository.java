package com.drumtong.backend.api.calendar.repository;

import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {
    // Finding entries by user ID
    List<CalendarEntry> findByUserId(Long userId);

    // Finding entries by user ID and date range
    List<CalendarEntry> findByUserIdAndDrinkingDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}