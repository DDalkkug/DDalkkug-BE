package com.drumtong.backend.api.calendar.service;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import com.drumtong.backend.api.calendar.repository.CalendarEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEntryService {
    private final CalendarEntryRepository calendarEntryRepository;

    public CalendarEntryResponseDto createEntry(CalendarEntryRequestDto dto) {
        CalendarEntry entry = new CalendarEntry();
        entry.setUserId(dto.getUserId());
        entry.setDrinkingDate(dto.getDrinkingDate());
        entry.setMemo(dto.getMemo());
        entry.setTotalPrice(dto.getTotalPrice());
        entry.setPhotoUrl(dto.getPhotoUrl());

        calendarEntryRepository.save(entry);
        return new CalendarEntryResponseDto(entry);
    }

    public CalendarEntryResponseDto getEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return new CalendarEntryResponseDto(entry);
    }

    public void deleteEntry(Long id) {
        calendarEntryRepository.deleteById(id);
    }

    public List<CalendarEntryResponseDto> listEntriesByUser(Long userId) {
        return calendarEntryRepository.findByUserId(userId).stream()
                .map(CalendarEntryResponseDto::new)
                .collect(Collectors.toList());
    }
}

