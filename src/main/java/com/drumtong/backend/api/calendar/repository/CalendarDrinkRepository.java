package com.drumtong.backend.api.calendar.repository;

import com.drumtong.backend.api.calendar.entity.CalendarDrink;
import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarDrinkRepository extends JpaRepository<CalendarDrink, Long> {
    List<CalendarDrink> findByCalendarEntry(CalendarEntry entry);
    void deleteByCalendarEntry(CalendarEntry entry);
}