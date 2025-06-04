package com.drumtong.backend.api.calendar.controller;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.service.CalendarEntryService;
import com.drumtong.backend.api.calendar.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar-entries")
@RequiredArgsConstructor
public class CalendarEntryController {
    private final CalendarEntryService calendarEntryService;
    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<String> createEntry(
            @ModelAttribute CalendarEntryRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageUploadService.upload(image);
        }

        calendarEntryService.createEntry(requestDto, imageUrl);
        return ResponseEntity.ok("등록 완료");
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

