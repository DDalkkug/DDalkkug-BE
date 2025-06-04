package com.drumtong.backend.api.calendar.service;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import com.drumtong.backend.api.calendar.repository.CalendarEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEntryService {

    private final CalendarEntryRepository calendarEntryRepository;
    private final ImageUploadService imageUploadService;

    // 글 등록
    public CalendarEntryResponseDto createEntry(CalendarEntryRequestDto dto, String imageUrl) {
        CalendarEntry entry = CalendarEntry.builder()
                .userId(dto.getUserId())
                .drinkingDate(dto.getDrinkingDate())
                .memo(dto.getMemo())
                .totalPrice(dto.getTotalPrice())
                .photoUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();

        calendarEntryRepository.save(entry);
        return new CalendarEntryResponseDto(entry);
    }

    // 단일 조회
    public CalendarEntryResponseDto getEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return new CalendarEntryResponseDto(entry);
    }

    // 삭제
    @Transactional
    public void deleteEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("엔트리를 찾을 수 없습니다."));
        // 이미지 URL이 있으면 S3에서 삭제
        if (entry.getPhotoUrl() != null && !entry.getPhotoUrl().isEmpty()) {
            imageUploadService.delete(entry.getPhotoUrl());
        }
        calendarEntryRepository.delete(entry);
    }
    // 사용자별 목록 조회
    public List<CalendarEntryResponseDto> listEntriesByUser(Long userId) {
        return calendarEntryRepository.findByUserId(userId).stream()
                .map(CalendarEntryResponseDto::new)
                .collect(Collectors.toList());
    }
}
