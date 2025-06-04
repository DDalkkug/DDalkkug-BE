package com.drumtong.backend.api.calendar.controller;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.dto.CalendarSummaryDto;
import com.drumtong.backend.api.calendar.service.CalendarEntryService;
import com.drumtong.backend.api.calendar.service.ImageUploadService;
import com.drumtong.backend.common.response.ApiResponse;
import com.drumtong.backend.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar-entries")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Calendar API for tracking drinking records")
public class CalendarEntryController {
    private final CalendarEntryService calendarEntryService;
    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Create a new calendar entry with drinks")
    public ResponseEntity<ApiResponse<CalendarEntryResponseDto>> createEntry(
            @ModelAttribute CalendarEntryRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageUploadService.upload(image);
        }

        CalendarEntryResponseDto response = calendarEntryService.createEntry(requestDto, imageUrl);
        return ApiResponse.success(SuccessStatus.CREATE_RECRUIT_ARTICLE_SUCCESS, response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a calendar entry by ID")
    public ResponseEntity<ApiResponse<CalendarEntryResponseDto>> get(@PathVariable Long id) {
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, calendarEntryService.getEntry(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a calendar entry")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        calendarEntryService.deleteEntry(id);
        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all calendar entries for a user")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponseDto>>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, calendarEntryService.listEntriesByUser(userId));
    }

    @GetMapping("/user/{userId}/month")
    @Operation(summary = "Get monthly calendar summary for a user")
    public ResponseEntity<ApiResponse<List<CalendarSummaryDto>>> getMonthlyCalendar(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month) {

        return ApiResponse.success(
                SuccessStatus.SEND_HEALTH_SUCCESS,
                calendarEntryService.getMonthlyCalendarSummary(userId, year, month));
    }
}