package com.drumtong.backend.api.calendar.controller;

import com.drumtong.backend.api.calendar.dto.CalendarEntryRequestDto;
import com.drumtong.backend.api.calendar.dto.CalendarEntryResponseDto;
import com.drumtong.backend.api.calendar.dto.CalendarSummaryDto;
import com.drumtong.backend.api.calendar.service.CalendarEntryService;
import com.drumtong.backend.api.calendar.service.ImageUploadService;
import com.drumtong.backend.common.response.ApiResponse;
import com.drumtong.backend.common.response.SuccessStatus;
import com.drumtong.backend.common.config.security.SecurityMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar-entries")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "캘린더 등록 및 조회")
public class CalendarEntryController {
    private final CalendarEntryService calendarEntryService;
    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "게시글 등록")
    public ResponseEntity<ApiResponse<CalendarEntryResponseDto>> createEntry(
            @AuthenticationPrincipal SecurityMember securityMember,
            @ModelAttribute CalendarEntryRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        // 인증된 사용자의 ID를 설정
        requestDto.setUserId(securityMember.getId());

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = imageUploadService.upload(image);
        }

        CalendarEntryResponseDto response = calendarEntryService.createEntry(requestDto, imageUrl);
        return ApiResponse.success(SuccessStatus.CREATE_RECRUIT_ARTICLE_SUCCESS, response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "캘린더 id로 조회")
    public ResponseEntity<ApiResponse<CalendarEntryResponseDto>> get(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable Long id) {
        // 여기서 서비스 레이어에서 권한 검증을 추가할 수도 있습니다
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS, calendarEntryService.getEntry(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "캘린더 id로 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal SecurityMember securityMember,
            @PathVariable Long id) {
        // 권한 검증이 필요할 수 있습니다
        calendarEntryService.deleteEntry(id);
        return ApiResponse.success_only(SuccessStatus.SEND_HEALTH_SUCCESS);
    }

    @GetMapping
    @Operation(summary = "userid가 일치하는 모든 캘린더 조회")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponseDto>>> getMyEntries(
            @AuthenticationPrincipal SecurityMember securityMember) {
        return ApiResponse.success(SuccessStatus.SEND_HEALTH_SUCCESS,
                calendarEntryService.listEntriesByUser(securityMember.getId()));
    }

    @GetMapping("/month")
    @Operation(summary = "usrid와 일치하는 한달동안의 캘린더 조회")
    public ResponseEntity<ApiResponse<List<CalendarSummaryDto>>> getMyMonthlyCalendar(
            @AuthenticationPrincipal SecurityMember securityMember,
            @RequestParam int year,
            @RequestParam int month) {

        return ApiResponse.success(
                SuccessStatus.SEND_HEALTH_SUCCESS,
                calendarEntryService.getMonthlyCalendarSummary(securityMember.getId(), year, month));
    }

}