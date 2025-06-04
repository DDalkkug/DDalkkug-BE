package com.drumtong.backend.api.calendar.service;

import com.drumtong.backend.api.calendar.dto.*;
import com.drumtong.backend.api.calendar.entity.CalendarDrink;
import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import com.drumtong.backend.api.calendar.entity.Drink;
import com.drumtong.backend.api.calendar.repository.CalendarDrinkRepository;
import com.drumtong.backend.api.calendar.repository.CalendarEntryRepository;
import com.drumtong.backend.api.calendar.repository.DrinkRepository;
import com.drumtong.backend.api.member.entity.Member;
import com.drumtong.backend.api.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import java.time.DayOfWeek;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarEntryService {
    private final CalendarEntryRepository calendarEntryRepository;
    private final CalendarDrinkRepository calendarDrinkRepository;
    private final DrinkRepository drinkRepository;
    private final ImageUploadService imageUploadService;
    private final DrinkService drinkService;
    private final MemberRepository memberRepository; // 추가

    // 캘린더 항목과 관련 음료 정보를 함께 조회하는 헬퍼 메소드
    private CalendarEntryResponseDto getEntryWithDrinks(CalendarEntry entry) {
        CalendarEntryResponseDto responseDto = new CalendarEntryResponseDto(entry);

        // 관련 음료 정보 조회
        List<CalendarDrink> calendarDrinks = calendarDrinkRepository.findByCalendarEntry(entry);
        List<DrinkDto> drinkDtos = calendarDrinks.stream()
                .map(cd -> {
                    DrinkDto dto = new DrinkDto();
                    dto.setId(cd.getDrink().getId());
                    dto.setName(cd.getDrink().getName());
                    dto.setType(cd.getDrink().getType());
                    dto.setQuantity(cd.getQuantity());
                    return dto;
                })
                .collect(Collectors.toList());

        responseDto.setDrinks(drinkDtos);
        return responseDto;
    }
    // Create entry with drinks
    @Transactional
    public CalendarEntryResponseDto createEntry(CalendarEntryRequestDto dto, String imageUrl) {
        // Save the main entry
        CalendarEntry entry = CalendarEntry.builder()
                .userId(dto.getUserId())
                .drinkingDate(dto.getDrinkingDate())
                .memo(dto.getMemo())
                .totalPrice(dto.getTotalPrice())
                .photoUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .build();

        calendarEntryRepository.save(entry);

        // Save associated drinks
        if (dto.getDrinks() != null && !dto.getDrinks().isEmpty()) {
            for (DrinkDto drinkDto : dto.getDrinks()) {
                Drink drink = drinkService.getDrinkByType(drinkDto.getType());
                if (drink == null) {
                    continue; // Skip if drink type not found
                }

                CalendarDrink calendarDrink = CalendarDrink.builder()
                        .calendarEntry(entry)
                        .drink(drink)
                        .quantity(drinkDto.getQuantity())
                        .build();

                calendarDrinkRepository.save(calendarDrink);
            }
        }

        // Member의 totalPaid 업데이트
        if (dto.getTotalPrice() != null && dto.getTotalPrice() > 0) {
            Member member = memberRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            member.addPaid(dto.getTotalPrice());
            memberRepository.save(member);
        }

        // Return complete response with drinks
        return getEntryWithDrinks(entry);
    }

    // Delete entry and associated drinks
    @Transactional
    public void deleteEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));

        // Delete associated drinks first
        calendarDrinkRepository.deleteByCalendarEntry(entry);

        // Delete image if exists
        if (entry.getPhotoUrl() != null && !entry.getPhotoUrl().isEmpty()) {
            imageUploadService.delete(entry.getPhotoUrl());
        }

        // Delete the entry
        calendarEntryRepository.delete(entry);
    }
    public CalendarEntryResponseDto getEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found"));

        return getEntryWithDrinks(entry);
    }

    // Get entries by user
    public List<CalendarEntryResponseDto> listEntriesByUser(Long userId) {
        List<CalendarEntry> entries = calendarEntryRepository.findByUserId(userId);
        return entries.stream()
                .map(this::getEntryWithDrinks)
                .collect(Collectors.toList());
    }

    // getMonthlyCalendarSummary 메소드 내부의 코드 수정
    public List<CalendarSummaryDto> getMonthlyCalendarSummary(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDrinkingDateBetween(
                userId, startDate, endDate);

        // Group entries by date
        Map<LocalDate, List<CalendarEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(CalendarEntry::getDrinkingDate));

        List<CalendarSummaryDto> summaryList = new ArrayList<>();

        // Generate summary for each day in the month
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<CalendarEntry> dateEntries = entriesByDate.getOrDefault(date, Collections.emptyList());

            if (!dateEntries.isEmpty()) {
                // Count drinks for this date
                Map<String, Integer> drinkCounts = new HashMap<>();

                // 같은 날짜의 totalPrice 합산
                int totalPrice = dateEntries.stream()
                        .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                        .sum();

                for (CalendarEntry entry : dateEntries) {
                    List<CalendarDrink> calendarDrinks = calendarDrinkRepository.findByCalendarEntry(entry);

                    for (CalendarDrink cd : calendarDrinks) {
                        String drinkType = cd.getDrink().getType();
                        int quantity = cd.getQuantity();

                        drinkCounts.put(drinkType,
                                drinkCounts.getOrDefault(drinkType, 0) + quantity);
                    }
                }

                CalendarSummaryDto summary = CalendarSummaryDto.builder()
                        .date(date)
                        .totalEntries(dateEntries.size())
                        .drinkCounts(drinkCounts)
                        .totalPrice(totalPrice) // 합산된 총 가격 설정
                        .build();

                summaryList.add(summary);
            }
        }

        return summaryList;
    }
    // 한 달 동안 사용한 금액 조회
    public int getMonthlyTotalPrice(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDrinkingDateBetween(
                userId, startDate, endDate);

        return entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();
    }


    // 특정 주(week)의 사용 금액 조회
    public int getWeeklyTotalPrice(Long userId, int year, int month, int weekOfMonth) {
        YearMonth yearMonth = YearMonth.of(year, month);

        // 해당 월의 첫 번째 날짜 구하기
        LocalDate firstDayOfMonth = yearMonth.atDay(1);

        // 해당 월의 첫 번째 날짜가 속한 주의 시작일 (월요일)
        LocalDate firstDayOfFirstWeek = firstDayOfMonth.with(DayOfWeek.MONDAY);

        // 첫 번째 날짜가 이전 달에 속하면 해당 월의 첫 날로 조정
        if (firstDayOfFirstWeek.getMonth() != firstDayOfMonth.getMonth()) {
            firstDayOfFirstWeek = firstDayOfMonth;
        }

        // 요청한 주의 시작일과 종료일 계산
        LocalDate startDateOfWeek = firstDayOfFirstWeek.plusWeeks(weekOfMonth - 1);
        LocalDate endDateOfWeek = startDateOfWeek.plusDays(6);

        // 해당 월을 벗어나는 경우 마지막 날로 조정
        if (endDateOfWeek.getMonthValue() != month) {
            endDateOfWeek = yearMonth.atEndOfMonth();
        }

        // 해당 주의 데이터 조회
        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDrinkingDateBetween(
                userId, startDateOfWeek, endDateOfWeek);

        return entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();
    }
}