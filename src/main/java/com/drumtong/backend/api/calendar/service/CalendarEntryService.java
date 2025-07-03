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

        // 그룹 정보 및 멤버 수 계산
        int memberCount = 1;
        if (isGroupEntry) {
            List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.getGroupId());
            memberCount = Math.max(1, groupMembers.size());
        }

        // 그룹 항목일 경우 원본 가격 저장 (그룹의 totalPaid 계산용)
        int originalTotalPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : 0;

        // 저장할 금액 결정 (그룹인 경우 나눈 금액으로)
        int savePrice = isGroupEntry ? (originalTotalPrice / memberCount) : originalTotalPrice;

        // Save the main entry - 원 작성자의 항목
        CalendarEntry entry = CalendarEntry.builder()
                .userId(dto.getUserId())
                .drinkingDate(dto.getDrinkingDate())
                .memo(dto.getMemo())
                .totalPrice(savePrice)
                .photoUrl(imageUrl)
                .createdAt(LocalDateTime.now())
                .groupId(dto.getGroupId())
                .isGroupShared(false) // 원본 항목은 false로 설정
                .build();

        CalendarEntry savedEntry = calendarEntryRepository.save(entry);

        // 그룹 항목인 경우 groupEntryId 설정 (자기 자신의 ID로)
        if (isGroupEntry) {
            savedEntry.setGroupEntryId(savedEntry.getId());
            calendarEntryRepository.save(savedEntry);
        }

        System.out.println("Created main entry: " + savedEntry.getId() + " with price: " + savePrice);

        // Save associated drinks - 원 작성자의 음료 정보
        List<CalendarDrink> savedDrinks = new ArrayList<>();
        if (dto.getDrinks() != null && !dto.getDrinks().isEmpty()) {
            for (DrinkDto drinkDto : dto.getDrinks()) {
                Drink drink = drinkService.getDrinkByType(drinkDto.getType());
                if (drink == null) {
                    continue;
                }

                // 그룹인 경우 음료 수량도 나눈 값으로 저장
                int quantity = isGroupEntry ?
                        Math.max(1, drinkDto.getQuantity() / memberCount) :
                        drinkDto.getQuantity();

                CalendarDrink calendarDrink = CalendarDrink.builder()
                        .calendarEntry(savedEntry)
                        .drink(drink)
                        .quantity(quantity)
                        .build();

                calendarDrinkRepository.save(calendarDrink);
                savedDrinks.add(calendarDrink);
            }
        }

        // 그룹 관련 처리
        if (isGroupEntry && originalTotalPrice > 0) {
            // 원 작성자의 totalPaid 업데이트 (나눈 금액)
            Member member = memberRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            member.addPaid(savePrice);
            memberRepository.save(member);

            // 그룹 처리 시 원본 가격 전달 (총액 계산용)
            dto.setTotalPrice(originalTotalPrice);
            processGroupEntry(dto, savedDrinks, savedEntry.getId()); // groupEntryId 전달
        } else if (!isGroupEntry && originalTotalPrice > 0) {
            // 개인 항목인 경우 사용자의 totalPaid만 업데이트
            Member member = memberRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Member not found"));
            member.addPaid(originalTotalPrice);
            memberRepository.save(member);
        }

        // Return complete response with drinks
        return getEntryWithDrinks(savedEntry);
    }

    private void processGroupEntry(CalendarEntryRequestDto dto, List<CalendarDrink> savedDrinks, Long groupEntryId) {
        // 그룹 정보 조회
        GroupInfo groupInfo = groupInfoRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // 그룹 멤버 목록 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.getGroupId());
        if (groupMembers.isEmpty()) {
            throw new NotFoundException("No members in this group");
        }

        int memberCount = groupMembers.size();
        int totalPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : 0;
        int pricePerMember = totalPrice / memberCount;

        // 그룹의 totalPaid 업데이트 (전체 금액)
        groupInfo.setTotalPaid(groupInfo.getTotalPaid() + totalPrice);
        groupInfoRepository.save(groupInfo);
        System.out.println("Updated group totalPaid: " + groupInfo.getTotalPaid());

        // 각 멤버별 처리
        for (GroupMember groupMember : groupMembers) {
            // 작성자 본인은 이미 처리했으므로 건너뜀
            if (groupMember.getMemberId().equals(dto.getUserId())) {
                continue;
            }

            // 멤버 정보 조회
            Member member = memberRepository.findById(groupMember.getMemberId())
                    .orElseThrow(() -> new NotFoundException("Member not found: " + groupMember.getMemberId()));

            // 멤버의 totalPaid 업데이트 (나눈 금액)
            member.addPaid(pricePerMember);
            memberRepository.save(member);
            System.out.println("Updated member totalPaid: " + member.getId() + " with: " + pricePerMember);

            // 멤버별 캘린더 항목 생성 (각자의 몫)
            createMemberCalendarEntry(member, dto, savedDrinks, memberCount, groupEntryId);
        }
    }

    private void createMemberCalendarEntry(Member member, CalendarEntryRequestDto dto,
                                           List<CalendarDrink> savedDrinks, int memberCount, Long groupEntryId) {
        // 멤버별 캘린더 항목 생성
        CalendarEntry memberEntry = CalendarEntry.builder()
                .userId(member.getId())
                .groupId(dto.getGroupId())
                .groupEntryId(groupEntryId)  // 원본 항목 ID로 설정
                .drinkingDate(dto.getDrinkingDate())
                .memo(dto.getMemo())
                .totalPrice(dto.getTotalPrice() / memberCount)
                .photoUrl(dto.getPhotoUrl())
                .createdAt(LocalDateTime.now())
                .isGroupShared(true)
                .build();

        CalendarEntry savedMemberEntry = calendarEntryRepository.save(memberEntry);
        System.out.println("Created member entry: " + savedMemberEntry.getId() +
                " for member: " + member.getId() +
                " with price: " + (dto.getTotalPrice() / memberCount) +
                " and groupEntryId: " + groupEntryId);

        // 음료 수량도 분할하여 저장
        for (CalendarDrink originalDrink : savedDrinks) {
            // 정수 나누기 처리 (최소 1)
            int sharedQuantity = originalDrink.getQuantity();

            CalendarDrink memberDrink = CalendarDrink.builder()
                    .calendarEntry(memberEntry)
                    .drink(originalDrink.getDrink())
                    .quantity(sharedQuantity)
                    .build();

            calendarDrinkRepository.save(memberDrink);
        }
    }

    @Transactional
    public void deleteEntry(Long id) {
        CalendarEntry entry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));

        // 삭제 전 가격 정보 저장
        Integer totalPrice = entry.getTotalPrice() != null ? entry.getTotalPrice() : 0;
        Long userId = entry.getUserId();
        Long groupId = entry.getGroupId();
        Long groupEntryId = entry.getGroupEntryId();
        boolean isGroupEntry = groupId != null;

        // 삭제할 항목이 그룹 항목인 경우, 관련된 모든 항목 찾기
        List<CalendarEntry> relatedEntries = new ArrayList<>();
        if (isGroupEntry && groupEntryId != null) {
            // 동일한 groupEntryId를 가진 항목들 찾기
            relatedEntries = calendarEntryRepository.findByGroupEntryId(groupEntryId);

            // 그룹의 totalPaid 업데이트
            GroupInfo groupInfo = groupInfoRepository.findById(groupId)
                    .orElseThrow(() -> new NotFoundException("Group not found"));

            // 전체 금액 계산 (모든 멤버의 금액 합산)
            int totalGroupPrice = 0;
            for (CalendarEntry e : relatedEntries) {
                if (e.getTotalPrice() != null) {
                    totalGroupPrice += e.getTotalPrice();
                }
            }

            int updatedGroupTotal = Math.max(0, groupInfo.getTotalPaid() - totalGroupPrice);
            groupInfo.setTotalPaid(updatedGroupTotal);
            groupInfoRepository.save(groupInfo);
            System.out.println("Updated group totalPaid on delete: " + updatedGroupTotal);
        }

        // 사용자의 totalPaid 업데이트
        updateMemberTotalPaidOnDelete(userId, totalPrice);

        // Delete associated drinks first
        calendarDrinkRepository.deleteByCalendarEntry(entry);

        // Delete image if exists
        if (entry.getPhotoUrl() != null && !entry.getPhotoUrl().isEmpty()) {
            imageUploadService.delete(entry.getPhotoUrl());
        }

        // Delete the entry
        calendarEntryRepository.delete(entry);
        System.out.println("Deleted entry: " + id);

        // 그룹 항목인 경우 관련된 다른 항목들도 삭제
        if (isGroupEntry && !relatedEntries.isEmpty() && !entry.getIsGroupShared()) {
            for (CalendarEntry relatedEntry : relatedEntries) {
                if (!relatedEntry.getId().equals(id)) {
                    // 멤버의 totalPaid 업데이트
                    updateMemberTotalPaidOnDelete(relatedEntry.getUserId(),
                            relatedEntry.getTotalPrice() != null ? relatedEntry.getTotalPrice() : 0);

                    // 관련 음료 정보 삭제
                    calendarDrinkRepository.deleteByCalendarEntry(relatedEntry);

                    // 항목 삭제
                    calendarEntryRepository.delete(relatedEntry);
                    System.out.println("Deleted related entry: " + relatedEntry.getId());
                }
            }
        }
    }

    /**
     * 개인 항목 삭제 시 해당 멤버의 totalPaid 업데이트
     */
    private void updateMemberTotalPaidOnDelete(Long userId, int totalPrice) {
        Member member = memberRepository.findById(userId)
                .orElse(null);

        if (member != null) {
            // Member 클래스는 setTotalPaid 대신 addPaid를 사용
            member.addPaid(-totalPrice); // 음수값으로 전달하여 차감
            memberRepository.save(member);
        }
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

    @Transactional
    public CalendarEntryResponseDto updateEntry(Long id, CalendarEntryRequestDto dto, String imageUrl) {
        // 1. 기존 항목 조회
        CalendarEntry existingEntry = calendarEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));

        // 2. 원본 정보 저장
        Long originalGroupId = existingEntry.getGroupId();
        Integer originalTotalPrice = existingEntry.getTotalPrice() != null ? existingEntry.getTotalPrice() : 0;
        String originalMemo = existingEntry.getMemo();
        LocalDate originalDate = existingEntry.getDrinkingDate();
        Long groupEntryId = existingEntry.getGroupEntryId();

        // groupEntryId가 없는 경우 (이전 데이터) 자신의 ID로 설정
        if (existingEntry.getGroupId() != null && groupEntryId == null) {
            groupEntryId = existingEntry.getId();
            existingEntry.setGroupEntryId(groupEntryId);
            calendarEntryRepository.save(existingEntry);
        }

        // 3. 이미지 처리
        String updatedImageUrl = imageUrl;
        if (updatedImageUrl == null) {
            updatedImageUrl = existingEntry.getPhotoUrl(); // 새 이미지가 없으면 기존 이미지 유지
        } else if (existingEntry.getPhotoUrl() != null && !existingEntry.getPhotoUrl().equals(updatedImageUrl)) {
            // 기존 이미지가 있고 새 이미지와 다르면 기존 이미지 삭제
            imageUploadService.delete(existingEntry.getPhotoUrl());
        }

        // 4. 그룹 변경 여부 확인
        boolean hadGroupBefore = originalGroupId != null;
        boolean hasGroupNow = dto.getGroupId() != null;
        boolean groupChanged = (hadGroupBefore && !hasGroupNow) ||
                (!hadGroupBefore && hasGroupNow) ||
                (hadGroupBefore && hasGroupNow && !originalGroupId.equals(dto.getGroupId()));

        // 5. 새 총액 확인
        int newTotalPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : 0;

        // 6. 케이스별 처리 시작
        if (groupChanged) {
            // 그룹 변경이 있는 경우
            System.out.println("Group changed from " + originalGroupId + " to " + dto.getGroupId());
            handleGroupChangeComplete(existingEntry, originalGroupId, dto, updatedImageUrl, originalTotalPrice);
        } else if (hadGroupBefore && hasGroupNow) {
            // 그룹 유지하며 내용만 수정하는 경우
            System.out.println("Group remained same: " + originalGroupId + ", updating content");
            handleGroupContentUpdateComplete(existingEntry, dto, updatedImageUrl, originalTotalPrice, newTotalPrice);
        } else {
            // 개인 항목 수정 (그룹과 무관)
            System.out.println("Personal entry update");
            handlePersonalEntryUpdateComplete(existingEntry, dto, updatedImageUrl, originalTotalPrice, newTotalPrice);
        }

        // 7. 저장 확인
        CalendarEntry savedEntry = calendarEntryRepository.save(existingEntry);
        System.out.println("Final save completed for entry: " + savedEntry.getId());

        return getEntryWithDrinks(savedEntry);
    }

    /**
     * 그룹 변경 처리 (그룹→개인, 개인→그룹, 그룹→다른 그룹)
     * 완전히 새로 작성된 메소드
     */
    private void handleGroupChangeComplete(CalendarEntry existingEntry, Long originalGroupId,
                                           CalendarEntryRequestDto dto, String imageUrl, int originalTotalPrice) {
        // 1. 기존 그룹 처리 (원본 항목과 관련 항목 모두 처리)
        if (originalGroupId != null) {
            List<CalendarEntry> relatedEntries;

            // groupEntryId로 관련 항목 찾기 (우선순위)
            if (existingEntry.getGroupEntryId() != null) {
                relatedEntries = calendarEntryRepository.findByGroupEntryId(existingEntry.getGroupEntryId());
            } else {
                // 기존 방식으로 fallback
                relatedEntries = calendarEntryRepository.findByGroupIdAndDrinkingDate(
                        originalGroupId, existingEntry.getDrinkingDate());
            }

            // 전체 그룹 금액 계산
            int totalGroupPrice = 0;
            for (CalendarEntry entry : relatedEntries) {
                if (entry.getTotalPrice() != null) {
                    totalGroupPrice += entry.getTotalPrice();
                }
            }

            // 그룹 totalPaid 업데이트 - 전체 금액 차감
            GroupInfo originalGroup = groupInfoRepository.findById(originalGroupId)
                    .orElseThrow(() -> new NotFoundException("Original group not found"));
            originalGroup.setTotalPaid(Math.max(0, originalGroup.getTotalPaid() - totalGroupPrice));
            groupInfoRepository.save(originalGroup);
            System.out.println("Updated original group totalPaid: " + originalGroup.getTotalPaid());

            // 멤버들의 관련 항목 모두 삭제
            for (CalendarEntry relatedEntry : relatedEntries) {
                if (!relatedEntry.getId().equals(existingEntry.getId())) {
                    // 멤버의 totalPaid 차감
                    Member member = memberRepository.findById(relatedEntry.getUserId())
                            .orElse(null);
                    if (member != null && relatedEntry.getTotalPrice() != null) {
                        member.addPaid(-relatedEntry.getTotalPrice());
                        memberRepository.save(member);
                        System.out.println("Updated member totalPaid for user: " + member.getId());
                    }

                    // 관련 음료 정보 삭제
                    calendarDrinkRepository.deleteByCalendarEntry(relatedEntry);

                    // 항목 삭제
                    calendarEntryRepository.delete(relatedEntry);
                    System.out.println("Deleted related entry: " + relatedEntry.getId());
                }
            }
        }

        // 2. 원 작성자 처리
        Member authorMember = memberRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Author member not found"));

        // 기존 금액 차감
        authorMember.addPaid(-originalTotalPrice);
        memberRepository.save(authorMember);

        // 3. 새 그룹으로 변경하는 경우
        if (dto.getGroupId() != null) {
            // 새 그룹 정보 및 멤버 조회
            GroupInfo newGroup = groupInfoRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new NotFoundException("New group not found"));
            List<GroupMember> newGroupMembers = groupMemberRepository.findByGroupId(dto.getGroupId());
            int memberCount = Math.max(1, newGroupMembers.size());

            // 개인별 금액 계산
            int newTotalPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : 0;
            int pricePerMember = newTotalPrice / memberCount;

            // 그룹 totalPaid 업데이트 (전체 금액 추가)
            newGroup.setTotalPaid(newGroup.getTotalPaid() + newTotalPrice);
            groupInfoRepository.save(newGroup);
            System.out.println("Updated new group totalPaid: " + newGroup.getTotalPaid());

            // 본인 항목 업데이트
            existingEntry.setGroupId(dto.getGroupId());
            existingEntry.setDrinkingDate(dto.getDrinkingDate());
            existingEntry.setMemo(dto.getMemo());
            existingEntry.setTotalPrice(pricePerMember);
            existingEntry.setPhotoUrl(imageUrl);
            existingEntry.setIsGroupShared(false); // 원본 항목

            // groupEntryId 설정 또는 유지
            if (existingEntry.getGroupEntryId() == null) {
                existingEntry.setGroupEntryId(existingEntry.getId());
            }

            calendarEntryRepository.save(existingEntry);

            // 본인의 totalPaid 업데이트 (나눈 금액 추가)
            authorMember.addPaid(pricePerMember);
            memberRepository.save(authorMember);
            System.out.println("Updated author totalPaid: " + authorMember.getTotalPaid());

            // 음료 정보 업데이트
            updateDrinksComplete(existingEntry, dto.getDrinks(), memberCount);

            // 다른 멤버들에게 항목 생성
            for (GroupMember groupMember : newGroupMembers) {
                // 작성자는 제외
                if (groupMember.getMemberId().equals(dto.getUserId())) {
                    continue;
                }

                Member member = memberRepository.findById(groupMember.getMemberId())
                        .orElseThrow(() -> new NotFoundException("Member not found"));

                // 멤버의 totalPaid 업데이트
                member.addPaid(pricePerMember);
                memberRepository.save(member);

                // 멤버별 캘린더 항목 생성
                CalendarEntry memberEntry = CalendarEntry.builder()
                        .userId(member.getId())
                        .groupId(dto.getGroupId())
                        .groupEntryId(existingEntry.getId()) // 중요: 원본 항목의 ID로 설정
                        .drinkingDate(dto.getDrinkingDate())
                        .memo(dto.getMemo())
                        .totalPrice(pricePerMember)
                        .photoUrl(imageUrl)
                        .createdAt(LocalDateTime.now())
                        .isGroupShared(true) // 공유 항목
                        .build();

                CalendarEntry savedMemberEntry = calendarEntryRepository.save(memberEntry);
                System.out.println("Created new member entry: " + savedMemberEntry.getId());

                // 멤버의 음료 정보 추가
                addMemberDrinks(savedMemberEntry, dto.getDrinks(), memberCount);
            }
        } else {
            // 4. 개인 항목으로 변경하는 경우
            int newTotalPrice = dto.getTotalPrice() != null ? dto.getTotalPrice() : 0;

            // 기본 필드 업데이트
            existingEntry.setGroupId(null);
            existingEntry.setGroupEntryId(null);
            existingEntry.setDrinkingDate(dto.getDrinkingDate());
            existingEntry.setMemo(dto.getMemo());
            existingEntry.setTotalPrice(newTotalPrice);
            existingEntry.setPhotoUrl(imageUrl);
            existingEntry.setIsGroupShared(false);
            calendarEntryRepository.save(existingEntry);

            // 작성자의 totalPaid 업데이트 (전체 금액 추가)
            authorMember.addPaid(newTotalPrice);
            memberRepository.save(authorMember);
            System.out.println("Updated author totalPaid for personal entry: " + authorMember.getTotalPaid());

            // 음료 정보 업데이트
            updateDrinksComplete(existingEntry, dto.getDrinks(), 1);
        }
    }

    /**
     * 그룹 내용만 수정하는 경우 처리
     * 완전히 새로 작성된 메소드
     */
    private void handleGroupContentUpdateComplete(CalendarEntry existingEntry, CalendarEntryRequestDto dto,
                                                  String imageUrl, int originalTotalPrice, int newTotalPrice) {
        Long groupId = existingEntry.getGroupId();
        Long groupEntryId = existingEntry.getGroupEntryId();

        // 1. 관련 항목 찾기 (같은 groupEntryId를 가진 항목들)
        List<CalendarEntry> relatedEntries;

        if (groupEntryId != null) {
            relatedEntries = calendarEntryRepository.findByGroupEntryId(groupEntryId);
        } else {
            // fallback: 기존 방식으로 찾기 (이전 데이터 호환성)
            relatedEntries = calendarEntryRepository.findByGroupIdAndDrinkingDate(
                    groupId, existingEntry.getDrinkingDate());

            // groupEntryId 설정 (이후 사용을 위해)
            for (CalendarEntry entry : relatedEntries) {
                if (entry.getGroupEntryId() == null) {
                    entry.setGroupEntryId(existingEntry.getId());
                    calendarEntryRepository.save(entry);
                }
            }
        }

        int memberCount = relatedEntries.size();

        // 2. 기존 그룹 총액 계산
        int totalGroupOriginalPrice = 0;
        for (CalendarEntry entry : relatedEntries) {
            if (entry.getTotalPrice() != null) {
                totalGroupOriginalPrice += entry.getTotalPrice();
            }
        }

        // 3. 그룹 총액 업데이트 (기존 금액 전체 차감 후 새 금액 전체 추가)
        GroupInfo group = groupInfoRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        group.setTotalPaid(group.getTotalPaid() - totalGroupOriginalPrice + newTotalPrice);
        groupInfoRepository.save(group);
        System.out.println("Updated group totalPaid: " + group.getTotalPaid());

        // 4. 멤버당 새 가격 계산
        int pricePerMemberNew = newTotalPrice / memberCount;

        // 5. 모든 관련 항목 및 멤버 업데이트
        for (CalendarEntry entry : relatedEntries) {
            // 멤버 정보 조회
            Member member = memberRepository.findById(entry.getUserId())
                    .orElse(null);

            if (member != null) {
                // 멤버의 totalPaid 업데이트 (기존 금액 차감 후 새 금액 추가)
                int currentPrice = entry.getTotalPrice() != null ? entry.getTotalPrice() : 0;
                member.addPaid(-currentPrice); // 기존 금액 차감
                member.addPaid(pricePerMemberNew); // 새 금액 추가
                memberRepository.save(member);
                System.out.println("Updated member totalPaid for " + member.getId());
            }

            // 항목 업데이트
            entry.setDrinkingDate(dto.getDrinkingDate());
            entry.setMemo(dto.getMemo());
            entry.setTotalPrice(pricePerMemberNew);
            entry.setPhotoUrl(imageUrl);
            calendarEntryRepository.save(entry);
            System.out.println("Updated entry: " + entry.getId());

            // 음료 정보 업데이트 (본인 항목이면 새 정보로, 아니면 기존 정보 유지)
            if (entry.getId().equals(existingEntry.getId())) {
                updateDrinksComplete(entry, dto.getDrinks(), memberCount);
            } else {
                // 다른 멤버 항목의 음료 정보 업데이트
                updateMemberDrinks(entry, dto.getDrinks(), memberCount);
            }
        }
    }

    /**
     * 개인 항목 수정 처리
     * 완전히 새로 작성된 메소드
     */
    private void handlePersonalEntryUpdateComplete(CalendarEntry existingEntry, CalendarEntryRequestDto dto,
                                                   String imageUrl, int originalTotalPrice, int newTotalPrice) {
        // 작성자의 totalPaid 업데이트 (기존 금액 차감 후 새 금액 추가)
        Member member = memberRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("Member not found"));

        member.addPaid(-originalTotalPrice); // 기존 금액 차감
        member.addPaid(newTotalPrice); // 새 금액 추가
        memberRepository.save(member);
        System.out.println("Updated personal entry member totalPaid: " + member.getTotalPaid());

        // 항목 기본 정보 업데이트
        existingEntry.setDrinkingDate(dto.getDrinkingDate());
        existingEntry.setMemo(dto.getMemo());
        existingEntry.setTotalPrice(newTotalPrice);
        existingEntry.setPhotoUrl(imageUrl);
        calendarEntryRepository.save(existingEntry);
        System.out.println("Updated personal entry: " + existingEntry.getId());

        // 음료 정보 업데이트
        updateDrinksComplete(existingEntry, dto.getDrinks(), 1);
    }

    /**
     * 음료 정보 완전 업데이트 (기존 삭제 후 새로 추가)
     */
    private void updateDrinksComplete(CalendarEntry entry, List<DrinkDto> drinks, int memberCount) {
        // 기존 음료 정보 삭제
        calendarDrinkRepository.deleteByCalendarEntry(entry);
        System.out.println("Deleted old drinks for entry: " + entry.getId());

        // 새 음료 정보가 없으면 종료
        if (drinks == null || drinks.isEmpty()) {
            return;
        }

        // 새 음료 정보 추가
        for (DrinkDto drinkDto : drinks) {
            Drink drink = drinkService.getDrinkByType(drinkDto.getType());
            if (drink != null) {
                // 그룹 항목인 경우(그룹 ID가 있으면) 항상 수량을 나눔
                int quantity;
                if (entry.getGroupId() != null) {
                    // 그룹 항목은 항상 나눈 수량 사용
                    quantity = Math.max(1, drinkDto.getQuantity() / memberCount);
                } else {
                    // 개인 항목은 원래 수량 사용
                    quantity = drinkDto.getQuantity();
                }

                CalendarDrink calendarDrink = CalendarDrink.builder()
                        .calendarEntry(entry)
                        .drink(drink)
                        .quantity(quantity)
                        .build();

                calendarDrinkRepository.save(calendarDrink);
            }
        }
        System.out.println("Added new drinks for entry: " + entry.getId());
    }

    /**
     * 멤버 항목의 음료 정보 업데이트 (기존 삭제 후 새로 추가)
     */
    private void updateMemberDrinks(CalendarEntry memberEntry, List<DrinkDto> drinks, int memberCount) {
        // 기존 음료 정보 삭제
        calendarDrinkRepository.deleteByCalendarEntry(memberEntry);

        // 새 음료 정보가 없으면 종료
        if (drinks == null || drinks.isEmpty()) {
            return;
        }

        // 새 음료 정보 추가 (멤버 항목은 항상 나눈 수량)
        for (DrinkDto drinkDto : drinks) {
            Drink drink = drinkService.getDrinkByType(drinkDto.getType());
            if (drink != null) {
                int sharedQuantity = Math.max(1, drinkDto.getQuantity() / memberCount);

                CalendarDrink memberDrink = CalendarDrink.builder()
                        .calendarEntry(memberEntry)
                        .drink(drink)
                        .quantity(sharedQuantity)
                        .build();

                calendarDrinkRepository.save(memberDrink);
            }
        }
    }

    /**
     * 특정 날짜의 캘린더 항목 조회
     */
    public List<CalendarEntryResponseDto> getDailyEntries(Long userId, LocalDate date) {
        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDrinkingDate(userId, date);

        return entries.stream()
                .map(this::getEntryWithDrinks)
                .collect(Collectors.toList());
    }

    /**
     * 새 멤버에게 음료 정보 추가
     */
    private void addMemberDrinks(CalendarEntry memberEntry, List<DrinkDto> drinks, int memberCount) {
        if (drinks == null || drinks.isEmpty()) {
            return;
        }

        for (DrinkDto drinkDto : drinks) {
            Drink drink = drinkService.getDrinkByType(drinkDto.getType());
            if (drink != null) {
                int sharedQuantity = Math.max(1, drinkDto.getQuantity() / memberCount);

                CalendarDrink memberDrink = CalendarDrink.builder()
                        .calendarEntry(memberEntry)
                        .drink(drink)
                        .quantity(sharedQuantity)
                        .build();

                calendarDrinkRepository.save(memberDrink);
            }
        }
    }
}