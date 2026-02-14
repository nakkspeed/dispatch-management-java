package com.example.dispatch.controller;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.RegularSchedule;
import com.example.dispatch.model.Schedule;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.model.Staff;
import com.example.dispatch.service.BoardService;
import com.example.dispatch.service.RegularScheduleService;
import com.example.dispatch.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private static final String[] DAY_OF_WEEK_JP = {"日", "月", "火", "水", "木", "金", "土"};

    private final BoardService boardService;
    private final ScheduleService scheduleService;
    private final RegularScheduleService regularScheduleService;

    public PageController(BoardService boardService, ScheduleService scheduleService,
                          RegularScheduleService regularScheduleService) {
        this.boardService = boardService;
        this.scheduleService = scheduleService;
        this.regularScheduleService = regularScheduleService;
    }

    /** ホームページ */
    @GetMapping("/")
    public String index(Model model) {
        List<Board> boards = boardService.findAll();
        model.addAttribute("boards", boards);

        // 最初のボードの登録済み年月リストを初期表示
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
            Model model) {
        model.addAttribute("boardId", boardId);
        model.addAttribute("targetMonth", targetMonth);
        return "scheduleList";
    }

    /** 定期スケジュール編集画面 */
    @GetMapping("/edit_regular_schedules")
    public String editRegularSchedules(@RequestParam int boardId, Model model) {
        model.addAttribute("boardId", boardId);
        return "regularScheduleList";
    }

    /** 月別スケジュール印刷画面 (サーバーサイドレンダリング) */
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
    public String boardCreate() {
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

    /** 定期スケジュール印刷画面 (サーバーサイドレンダリング) */
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
