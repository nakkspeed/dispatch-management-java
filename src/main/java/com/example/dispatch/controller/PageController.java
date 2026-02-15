package com.example.dispatch.controller;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.RegularSchedule;
import com.example.dispatch.model.Schedule;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.model.Staff;
import com.example.dispatch.service.BoardService;
import com.example.dispatch.service.RegularScheduleService;
import com.example.dispatch.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private static final String[] DAY_OF_WEEK_JP = {"日", "月", "火", "水", "木", "金", "土"};

    private final BoardService boardService;
    private final ScheduleService scheduleService;
    private final RegularScheduleService regularScheduleService;
    private final ObjectMapper objectMapper;

    public PageController(BoardService boardService, ScheduleService scheduleService,
                          RegularScheduleService regularScheduleService, ObjectMapper objectMapper) {
        this.boardService = boardService;
        this.scheduleService = scheduleService;
        this.regularScheduleService = regularScheduleService;
        this.objectMapper = objectMapper;
    }

    /** ホームページ */
    @GetMapping("/")
    public String index(Model model) {
        List<Board> boards = boardService.findAll();
        model.addAttribute("boards", boards);

        if (!boards.isEmpty()) {
            int defaultBoardId = boards.get(0).boardId();
            List<ScheduleMonth> months = scheduleService.findMonths(defaultBoardId);
            model.addAttribute("defaultBoardId", defaultBoardId);
            model.addAttribute("scheduleMonths", months);
        } else {
            model.addAttribute("defaultBoardId", null);
            model.addAttribute("scheduleMonths", List.of());
        }
        return "index";
    }

    /** 月別スケジュール編集画面 */
    @GetMapping("/schedule")
    public String schedule(
            @RequestParam int boardId,
            @RequestParam String targetMonth,
            @RequestParam(required = false, defaultValue = "false") boolean filterActive,
            @RequestParam(required = false, defaultValue = "false") boolean showPastDay,
            @RequestParam(required = false, defaultValue = "false") boolean showDetail,
            @RequestParam(required = false, defaultValue = "false") boolean showAm,
            @RequestParam(required = false, defaultValue = "false") boolean showPm,
            @RequestParam(required = false) List<Integer> dow,
            @RequestParam(required = false) List<Integer> week,
            Model model) {
        String[] parts = targetMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        Set<Integer> effectiveDow;
        Set<Integer> effectiveWeek;
        boolean effectiveShowAm;
        boolean effectiveShowPm;
        boolean effectiveShowPastDay;

        if (!filterActive) {
            effectiveDow = Set.of(0, 1, 2, 3, 4, 5, 6);
            effectiveWeek = Set.of(0, 1, 2, 3, 4);
            effectiveShowAm = true;
            effectiveShowPm = true;
            effectiveShowPastDay = false;
        } else {
            effectiveDow = dow != null ? new HashSet<>(dow) : Set.of();
            effectiveWeek = week != null ? new HashSet<>(week) : Set.of();
            effectiveShowAm = showAm;
            effectiveShowPm = showPm;
            effectiveShowPastDay = showPastDay;
        }

        LocalDate today = LocalDate.now();
        List<Schedule> filtered = scheduleService.findByMonth(boardId, year, month).stream()
                .filter(s -> effectiveShowPastDay || !s.scheduleDate().isBefore(today))
                .filter(s -> effectiveDow.contains(s.dayOfWeek()))
                .filter(s -> effectiveWeek.contains(s.week()))
                .collect(Collectors.toList());

        try {
            model.addAttribute("schedulesJson", objectMapper.writeValueAsString(filtered));
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }

        model.addAttribute("schedules", filtered);
        model.addAttribute("showAm", effectiveShowAm);
        model.addAttribute("showPm", effectiveShowPm);
        model.addAttribute("showPastDay", effectiveShowPastDay);
        model.addAttribute("showDetail", showDetail);
        model.addAttribute("selectedDow", effectiveDow);
        model.addAttribute("selectedWeek", effectiveWeek);
        model.addAttribute("filterActive", filterActive);
        model.addAttribute("daysJp", DAY_OF_WEEK_JP);
        model.addAttribute("boardId", boardId);
        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        return "scheduleList";
    }

    /** 定期スケジュール編集画面 */
    @GetMapping("/edit_regular_schedules")
    public String editRegularSchedules(
            @RequestParam int boardId,
            @RequestParam(required = false, defaultValue = "false") boolean filterActive,
            @RequestParam(required = false, defaultValue = "false") boolean showDetail,
            @RequestParam(required = false, defaultValue = "false") boolean showAm,
            @RequestParam(required = false, defaultValue = "false") boolean showPm,
            @RequestParam(required = false) List<Integer> dow,
            @RequestParam(required = false) List<Integer> week,
            Model model) {
        Set<Integer> effectiveDow;
        Set<Integer> effectiveWeek;
        boolean effectiveShowAm;
        boolean effectiveShowPm;

        if (!filterActive) {
            effectiveDow = Set.of(0, 1, 2, 3, 4, 5, 6);
            effectiveWeek = Set.of(0, 1, 2, 3, 4);
            effectiveShowAm = true;
            effectiveShowPm = true;
        } else {
            effectiveDow = dow != null ? new HashSet<>(dow) : Set.of();
            effectiveWeek = week != null ? new HashSet<>(week) : Set.of();
            effectiveShowAm = showAm;
            effectiveShowPm = showPm;
        }

        List<RegularSchedule> filtered = regularScheduleService.findByBoardId(boardId).stream()
                .filter(s -> effectiveDow.contains(s.dayOfWeek()))
                .filter(s -> effectiveWeek.contains(s.week()))
                .collect(Collectors.toList());

        try {
            model.addAttribute("schedulesJson", objectMapper.writeValueAsString(filtered));
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }

        model.addAttribute("schedules", filtered);
        model.addAttribute("showAm", effectiveShowAm);
        model.addAttribute("showPm", effectiveShowPm);
        model.addAttribute("showDetail", showDetail);
        model.addAttribute("selectedDow", effectiveDow);
        model.addAttribute("selectedWeek", effectiveWeek);
        model.addAttribute("filterActive", filterActive);
        model.addAttribute("daysJp", DAY_OF_WEEK_JP);
        model.addAttribute("boardId", boardId);
        return "regularScheduleList";
    }

    /** 月別スケジュール印刷画面 */
    @GetMapping("/dailyReport")
    public String dailyReport(@RequestParam int scheduleId, Model model) {
        List<Schedule> schedules = scheduleService.findById(scheduleId);
        if (schedules.isEmpty()) {
            return "redirect:/";
        }
        Schedule schedule = schedules.get(0);
        model.addAttribute("schedule", schedule);
        model.addAttribute("dayOfWeekJp", DAY_OF_WEEK_JP[schedule.dayOfWeek()]);
        return "print/dailyReport";
    }

    /** ボード追加画面 */
    @GetMapping("/board-create")
    public String boardCreate(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer deleteIdx,
            @RequestParam(required = false) Integer boardId,
            @RequestParam(required = false, defaultValue = "") String boardName,
            @RequestParam(required = false) List<String> routes,
            Model model) {
        List<String> mutableRoutes = (routes != null && !routes.isEmpty())
                ? new ArrayList<>(routes)
                : new ArrayList<>(List.of("内勤", "ルートA", "ルートB", "ルートC"));

        if ("add".equals(action)) {
            mutableRoutes.add("");
        } else if (deleteIdx != null && deleteIdx > 0 && deleteIdx < mutableRoutes.size()) {
            mutableRoutes.remove(deleteIdx.intValue());
        }

        model.addAttribute("routes", mutableRoutes);
        model.addAttribute("boardId", boardId);
        model.addAttribute("boardName", boardName);
        return "boardCreate";
    }

    /** スタッフメンテナンス画面 */
    @GetMapping("/staff-maintenance")
    public String staffMaintenance(@RequestParam(required = false) Integer boardId, Model model) {
        List<Board> boards = boardService.findAll();
        model.addAttribute("boards", boards);

        Board currentBoard = null;
        if (boardId != null) {
            currentBoard = boards.stream()
                    .filter(b -> b.boardId() == boardId)
                    .findFirst()
                    .orElse(null);
        }
        if (currentBoard == null && !boards.isEmpty()) {
            currentBoard = boards.get(0);
        }
        model.addAttribute("currentBoard", currentBoard);

        List<String> staffNicknames = List.of();
        if (currentBoard != null && !currentBoard.staffs().isEmpty()) {
            staffNicknames = currentBoard.staffs().get(0).stream()
                    .map(Staff::nickname)
                    .collect(Collectors.toList());
        }
        model.addAttribute("staffNicknames", staffNicknames);
        return "staffMaintenance";
    }

    /** 月別スケジュール生成 (フォーム送信 → 確認 or 即時生成 → PRG) */
    @PostMapping("/schedule/{boardId}/createByMonthForm")
    public String createByMonthForm(
            @PathVariable int boardId,
            @RequestParam String fromDate,
            @RequestParam(required = false, defaultValue = "false") boolean confirmed,
            Model model,
            RedirectAttributes redirectAttributes) {
        // スタッフ未定義チェック
        Board board = boardService.findAll().stream()
                .filter(b -> b.boardId() == boardId)
                .findFirst()
                .orElse(null);
        boolean hasStaffs = board != null && !board.staffs().isEmpty()
                && !board.staffs().get(0).isEmpty();
        if (!hasStaffs) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "スタッフが登録されていません。スタッフ管理画面で登録してください。");
            return "redirect:/edit_regular_schedules?boardId=" + boardId;
        }

        String[] parts = fromDate.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        if (!confirmed) {
            int count = scheduleService.countByMonth(boardId, year, month);
            if (count > 0) {
                model.addAttribute("boardId", boardId);
                model.addAttribute("fromDate", fromDate);
                model.addAttribute("count", count);
                return "scheduleCreateConfirm";
            }
        }

        scheduleService.createByMonth(boardId, year, month);
        return "redirect:/schedule?boardId=" + boardId + "&targetMonth=" + fromDate;
    }

    /** 定期スケジュール印刷画面 */
    @GetMapping("/dailyReportSample")
    public String dailyReportSample(@RequestParam int scheduleId, Model model) {
        List<RegularSchedule> schedules = regularScheduleService.findById(scheduleId);
        if (schedules.isEmpty()) {
            return "redirect:/";
        }
        RegularSchedule schedule = schedules.get(0);
        model.addAttribute("schedule", schedule);
        model.addAttribute("weekLabel", "第" + (schedule.week() + 1) + DAY_OF_WEEK_JP[schedule.dayOfWeek()] + "曜");
        return "print/dailyReportSample";
    }
}
