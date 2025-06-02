package com.drumtong.backend.api.calendar.controller;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.service.CalendarEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar-entries")
@RequiredArgsConstructor
public class CalendarEntryController {
    private final CalendarEntryService calendarEntryService;

    @PostMapping
    public ResponseEntity<CalendarEntryResponseDto> create(@RequestBody CalendarEntryRequestDto dto) {
        return ResponseEntity.ok(calendarEntryService.createEntry(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalendarEntryResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(calendarEntryService.getEntry(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarEntryService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CalendarEntryResponseDto>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(calendarEntryService.listEntriesByUser(userId));
    }
}

