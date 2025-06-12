package com.drumtong.backend.api.calendar.service;

import com.drumtong.backend.api.calendar.dto.*;
import com.drumtong.backend.api.calendar.entity.CalendarDrink;
import com.drumtong.backend.api.calendar.entity.CalendarEntry;
import com.drumtong.backend.api.calendar.entity.Drink;
import com.drumtong.backend.api.calendar.repository.CalendarDrinkRepository;
import com.drumtong.backend.api.calendar.repository.CalendarEntryRepository;
import com.drumtong.backend.api.calendar.repository.DrinkRepository;
import com.drumtong.backend.api.groupInfo.entity.GroupInfo;
import com.drumtong.backend.api.groupInfo.repository.GroupInfoRepository;
import com.drumtong.backend.api.groupmember.entity.GroupMember;
import com.drumtong.backend.api.groupmember.repository.GroupMemberRepository;
import com.drumtong.backend.api.member.entity.Member;
import com.drumtong.backend.api.member.repository.MemberRepository;
import com.drumtong.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import java.time.DayOfWeek;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
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
    private final MemberRepository memberRepository;
    // 추가된 의존성
    private final GroupInfoRepository groupInfoRepository;
    private final GroupMemberRepository groupMemberRepository;

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

    @Transactional
    public CalendarEntryResponseDto createEntry(CalendarEntryRequestDto dto, String imageUrl) {
        // 그룹 항목인지 확인
        boolean isGroupEntry = dto.getGroupId() != null;
        dto.setPhotoUrl(imageUrl);
        // 저장할 금액 결정 (그룹인 경우 전체 금액 또는 1인당 금액)
        int savePrice = dto.getTotalPrice();
        int memberCount = 1;

        if (isGroupEntry) {
            // 그룹 멤버 수 조회
            List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.getGroupId());
            memberCount = groupMembers.size();
            if (memberCount > 0) {
                // 그룹인 경우 개인 캘린더에는 나눈 금액으로 저장
                savePrice = dto.getTotalPrice() / memberCount;
            }
        }

        // Save the main entry - 그룹인 경우 나눈 금액으로 저장
        CalendarEntry entry = CalendarEntry.builder()
                .userId(dto.getUserId())
                .drinkingDate(dto.getDrinkingDate())
                .memo(dto.getMemo())
                .totalPrice(isGroupEntry ? savePrice : dto.getTotalPrice()) // 그룹이면 나눈 금액, 아니면 전체 금액
                .photoUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .groupId(dto.getGroupId())
                .isGroupShared(isGroupEntry) // 그룹 ID가 있으면 공유 항목
                .build();

        calendarEntryRepository.save(entry);

        // Save associated drinks
        List<CalendarDrink> savedDrinks = new ArrayList<>();
        if (dto.getDrinks() != null && !dto.getDrinks().isEmpty()) {
            for (DrinkDto drinkDto : dto.getDrinks()) {
                Drink drink = drinkService.getDrinkByType(drinkDto.getType());
                if (drink == null) {
                    continue;
                }

                // 음료 수량도 그룹이면 나눈 값으로 저장
                int quantity = isGroupEntry ?
                        Math.max(1, drinkDto.getQuantity() / memberCount) :
                        drinkDto.getQuantity();

                CalendarDrink calendarDrink = CalendarDrink.builder()
                        .calendarEntry(entry)
                        .drink(drink)
                        .quantity(quantity)
                        .build();

                calendarDrinkRepository.save(calendarDrink);
                savedDrinks.add(calendarDrink);
            }
        }

        // 그룹 관련 처리
        if (isGroupEntry && dto.getTotalPrice() != null && dto.getTotalPrice() > 0) {
            processGroupEntry(dto, savedDrinks);
        } else {
            // 개인 항목인 경우 사용자의 totalPaid만 업데이트
            if (dto.getTotalPrice() != null && dto.getTotalPrice() > 0) {
                Member member = memberRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("Member not found"));
                member.addPaid(dto.getTotalPrice());
                memberRepository.save(member);
            }
        }

        // Return complete response with drinks
        return getEntryWithDrinks(entry);
    }

    // 그룹 항목 및 멤버 분담금 처리 메서드
    private void processGroupEntry(CalendarEntryRequestDto dto, List<CalendarDrink> savedDrinks) {
        // 그룹 정보 조회
        GroupInfo groupInfo = groupInfoRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // 그룹 멤버 목록 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.getGroupId());
        if (groupMembers.isEmpty()) {
            throw new NotFoundException("No members in this group");
        }

        int memberCount = groupMembers.size();
        int pricePerMember = dto.getTotalPrice() / memberCount; // 멤버당 금액

        // 그룹의 totalPaid 업데이트 (전체 금액)
        groupInfo.setTotalPaid(groupInfo.getTotalPaid() + dto.getTotalPrice());
        groupInfoRepository.save(groupInfo);

        // 각 멤버별 처리
        for (GroupMember groupMember : groupMembers) {
            // 멤버 정보 조회
            Member member = memberRepository.findById(groupMember.getMemberId())
                    .orElseThrow(() -> new NotFoundException("Member not found: " + groupMember.getMemberId()));

            // 멤버의 totalPaid 업데이트 (나눈 금액)
            member.addPaid(pricePerMember);
            memberRepository.save(member);

            // 원 작성자는 이미 항목이 생성되었으므로 중복 생성 방지
            if (member.getId().equals(dto.getUserId())) {
                continue;
            }

            // 멤버별 캘린더 항목 생성 (각자의 몫)
            createMemberCalendarEntry(member, dto, savedDrinks, memberCount);
        }
    }

    // 멤버별 캘린더 항목 생성 메서드 - 이미지 URL 포함
    private void createMemberCalendarEntry(Member member, CalendarEntryRequestDto dto,
                                           List<CalendarDrink> savedDrinks, int memberCount) {
        // 멤버별 캘린더 항목 생성
        CalendarEntry memberEntry = CalendarEntry.builder()
                .userId(member.getId())
                .groupId(dto.getGroupId())
                .drinkingDate(dto.getDrinkingDate())
                .memo("[그룹] " + dto.getMemo()) // 그룹 캘린더임을 표시
                .totalPrice(dto.getTotalPrice() / memberCount) // 멤버당 가격
                .photoUrl(dto.getPhotoUrl()) // 이미지 URL도 복사해서 추가
                .createdAt(LocalDateTime.now())
                .isGroupShared(true) // 그룹 공유 항목임을 표시
                .build();

        calendarEntryRepository.save(memberEntry);

        // 음료 수량도 분할하여 저장
        for (CalendarDrink originalDrink : savedDrinks) {
            // 정수 나누기 처리 (최소 1)
            int sharedQuantity = Math.max(1, originalDrink.getQuantity() / memberCount);

            CalendarDrink memberDrink = CalendarDrink.builder()
                    .calendarEntry(memberEntry)
                    .drink(originalDrink.getDrink())
                    .quantity(sharedQuantity)
                    .build();

            calendarDrinkRepository.save(memberDrink);
        }
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

    // 그룹별 항목 조회 메소드
    public List<CalendarEntryResponseDto> getEntriesByGroupId(Long groupId) {
        List<CalendarEntry> entries = calendarEntryRepository.findByGroupId(groupId);
        return entries.stream()
                .map(this::getEntryWithDrinks)
                .collect(Collectors.toList());
    }

    // 사용자의 그룹 공유 항목 조회
    public List<CalendarEntryResponseDto> getGroupSharedEntriesByUserId(Long userId) {
        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndIsGroupSharedTrue(userId);
        return entries.stream()
                .map(this::getEntryWithDrinks)
                .collect(Collectors.toList());
    }

    // 최근 5개월 지출 데이터 조회 메소드
    public List<MonthlyExpenseDto> getRecentMonthsExpense(Long userId, int year, int month) {
        List<MonthlyExpenseDto> result = new ArrayList<>();

        // 요청받은 연월로부터 5개월치 데이터 (요청 월 포함 이전 4개월)
        for (int i = 4; i >= 0; i--) {
            // i개월 이전 날짜 계산
            YearMonth targetYearMonth = YearMonth.of(year, month).minusMonths(i);
            int targetYear = targetYearMonth.getYear();
            int targetMonth = targetYearMonth.getMonthValue();

            // 해당 월 지출 금액 조회
            int monthlyTotal = getMonthlyTotalPrice(userId, targetYear, targetMonth);

            MonthlyExpenseDto dto = MonthlyExpenseDto.builder()
                    .year(targetYear)
                    .month(targetMonth)
                    .totalPrice(monthlyTotal)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 이번주 월~금 지출 조회
    public Map<String, Object> getCurrentWeekdaysExpense(Long userId) {
        LocalDate today = LocalDate.now();

        // 이번 주의 월요일 구하기
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 이번 주의 금요일 구하기
        LocalDate friday = monday.plusDays(4);

        // 오늘이 금요일보다 이전이면 이번 주의 금요일로 설정
        if (today.isBefore(friday)) {
            friday = today;
        }

        // 월~금 사이의 지출 조회
        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDrinkingDateBetween(
                userId, monday, friday);

        // 총 금액 계산
        int totalPrice = entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();

        // 일자별 지출 집계
        Map<LocalDate, Integer> dailyExpenses = entries.stream()
                .collect(Collectors.groupingBy(
                        CalendarEntry::getDrinkingDate,
                        Collectors.summingInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                ));

        // 결과 생성
        Map<String, Object> result = new LinkedHashMap<>();  // 순서 보존을 위해 LinkedHashMap 사용
        result.put("startDate", monday.toString());
        result.put("endDate", friday.toString());
        result.put("totalPrice", totalPrice);

        // 각 요일별 지출 추가
        Map<String, Integer> weekdayExpenses = new LinkedHashMap<>();
        for (LocalDate date = monday; !date.isAfter(friday); date = date.plusDays(1)) {
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
            int amount = dailyExpenses.getOrDefault(date, 0);
            weekdayExpenses.put(dayOfWeek, amount);
        }
        result.put("dailyExpenses", weekdayExpenses);

        return result;
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

    // 그룹별 주간 지출 조회 메소드 수정
    public Map<String, Object> getGroupWeeklyTotalPrice(Long groupId, int year, int month, int weekOfMonth) {
        // 그룹 존재 확인
        GroupInfo groupInfo = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

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
        List<CalendarEntry> entries = calendarEntryRepository.findByGroupIdAndDrinkingDateBetween(
                groupId, startDateOfWeek, endDateOfWeek);

        int totalPrice = entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();

        // 결과 생성
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("week", weekOfMonth);
        result.put("weekPrice", totalPrice);
        result.put("groupTotalPaid", groupInfo.getTotalPaid());

        return result;
    }

    // 그룹별 월간 캘린더 요약 조회
    public List<CalendarSummaryDto> getGroupMonthlyCalendarSummary(Long groupId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarEntry> entries = calendarEntryRepository.findByGroupIdAndDrinkingDateBetween(
                groupId, startDate, endDate);

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
                        .totalPrice(totalPrice)
                        .build();

                summaryList.add(summary);
            }
        }

        return summaryList;
    }

    // 그룹별 월간 지출 금액 조회
    public int getGroupMonthlyTotalPrice(Long groupId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarEntry> entries = calendarEntryRepository.findByGroupIdAndDrinkingDateBetween(
                groupId, startDate, endDate);

        return entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();
    }

    // 그룹별 최근 5개월 지출 데이터 조회
    public List<MonthlyExpenseDto> getGroupRecentMonthsExpense(Long groupId, int year, int month) {
        List<MonthlyExpenseDto> result = new ArrayList<>();

        // 요청받은 연월로부터 5개월치 데이터 (요청 월 포함 이전 4개월)
        for (int i = 4; i >= 0; i--) {
            // i개월 이전 날짜 계산
            YearMonth targetYearMonth = YearMonth.of(year, month).minusMonths(i);
            int targetYear = targetYearMonth.getYear();
            int targetMonth = targetYearMonth.getMonthValue();

            // 해당 월 지출 금액 조회
            int monthlyTotal = getGroupMonthlyTotalPrice(groupId, targetYear, targetMonth);

            MonthlyExpenseDto dto = MonthlyExpenseDto.builder()
                    .year(targetYear)
                    .month(targetMonth)
                    .totalPrice(monthlyTotal)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 그룹별 이번주 월~금 지출 조회
    public Map<String, Object> getGroupCurrentWeekdaysExpense(Long groupId) {
        LocalDate today = LocalDate.now();

        // 이번 주의 월요일 구하기
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 이번 주의 금요일 구하기 (월요일 기준으로 +4일)
        LocalDate friday = monday.plusDays(4);

        // 오늘이 금요일보다 이후면 금요일까지만 조회
        if (today.isBefore(friday)) {
            friday = today;
        }

        // 월~금 사이의 지출 조회
        List<CalendarEntry> entries = calendarEntryRepository.findByGroupIdAndDrinkingDateBetween(
                groupId, monday, friday);

        // 총 금액 계산
        int totalPrice = entries.stream()
                .mapToInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                .sum();

        // 일자별 지출 집계
        Map<LocalDate, Integer> dailyExpenses = entries.stream()
                .collect(Collectors.groupingBy(
                        CalendarEntry::getDrinkingDate,
                        Collectors.summingInt(entry -> entry.getTotalPrice() != null ? entry.getTotalPrice() : 0)
                ));

        // 결과 생성
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", monday.toString());
        result.put("endDate", friday.toString());
        result.put("totalPrice", totalPrice);

        // 각 요일별 지출 추가
        Map<String, Integer> weekdayExpenses = new LinkedHashMap<>();
        for (LocalDate date = monday; !date.isAfter(friday); date = date.plusDays(1)) {
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
            int amount = dailyExpenses.getOrDefault(date, 0);
            weekdayExpenses.put(dayOfWeek, amount);
        }
        result.put("dailyExpenses", weekdayExpenses);

        return result;
    }
}