package com.example.dispatch.controller;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.model.Staff;
import com.example.dispatch.service.BoardService;
import com.example.dispatch.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class BoardController {

    private final BoardService boardService;
    private final ScheduleService scheduleService;

    public BoardController(BoardService boardService, ScheduleService scheduleService) {
        this.boardService = boardService;
        this.scheduleService = scheduleService;
    }

    /** スケジュールボード一覧取得 API */
    @GetMapping("/board/read")
    @ResponseBody
    public List<Board> readAll() {
        return boardService.findAll();
    }

    /** ボード新規作成 (フォーム送信 → PRGパターン) */
    @PostMapping("/board/create")
    public String createBoard(
            @RequestParam String boardName,
            @RequestParam(required = false) List<String> routes,
            Model model) {
        List<String> filteredRoutes = (routes != null) ? routes.stream()
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toList()) : List.of();

        if (boardName.isBlank()) {
            return renderBoardCreateWithError(model, boardName, routes, "ボード名を入力してください");
        }
        if (filteredRoutes.isEmpty()) {
            return renderBoardCreateWithError(model, boardName, routes, "ルートを1件以上入力してください");
        }

        int boardId = boardService.createBoard(boardName, filteredRoutes);
        return "redirect:/edit_regular_schedules?boardId=" + boardId;
    }

    private String renderBoardCreateWithError(Model model, String boardName,
            List<String> routes, String errorMessage) {
        model.addAttribute("routes", (routes != null && !routes.isEmpty()) ? new ArrayList<>(routes) : List.of("内勤"));
        model.addAttribute("boardName", boardName != null ? boardName : "");
        model.addAttribute("errorMessage", errorMessage);
        return "boardCreate";
    }

    /** スタッフ一覧更新 API */
    @PostMapping("/staff/{boardId}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateStaffs(
            @PathVariable int boardId,
            @RequestBody Map<String, List<String>> body) {
        List<String> nicknames = body.getOrDefault("staffs", List.of());
        List<Staff> group0 = nicknames.stream()
                .map(Staff::new)
                .collect(Collectors.toList());
        boardService.updateStaffs(boardId, List.of(group0, List.of()));
        return ResponseEntity.ok(Map.of("boardId", boardId, "count", group0.size()));
    }

    /** スタッフ追加 (フォーム → PRG) */
    @PostMapping("/staff/{boardId}/add")
    public String addStaff(@PathVariable int boardId, @RequestParam String name) {
        List<Staff> staffs = currentStaffs(boardId);
        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
            staffs.add(new Staff(trimmed));
            boardService.updateStaffs(boardId, List.of(staffs, List.of()));
        }
        return "redirect:/staff-maintenance?boardId=" + boardId;
    }

    /** スタッフ削除 (フォーム → PRG) */
    @PostMapping("/staff/{boardId}/{idx}/delete")
    public String deleteStaff(@PathVariable int boardId, @PathVariable int idx) {
        List<Staff> staffs = currentStaffs(boardId);
        if (idx >= 0 && idx < staffs.size()) {
            staffs.remove(idx);
            boardService.updateStaffs(boardId, List.of(staffs, List.of()));
        }
        return "redirect:/staff-maintenance?boardId=" + boardId;
    }


    private List<Staff> currentStaffs(int boardId) {
        return boardService.findAll().stream()
                .filter(b -> b.boardId() == boardId)
                .findFirst()
                .map(b -> {
                    List<List<Staff>> allGroups = b.staffs();
                    return allGroups.isEmpty() ? new ArrayList<Staff>() : new ArrayList<>(allGroups.get(0));
                })
                .orElseGet(ArrayList::new);
    }

    /**
     * HTMX 向け: ボード選択時に年月 <option> リストを返すフラグメント
     * index.html の hx-get で呼び出される
     */
    @GetMapping("/board/{boardId}/scheduleMonths")
    public String scheduleMonthsFragment(@PathVariable int boardId, Model model) {
        List<ScheduleMonth> months = scheduleService.findMonths(boardId);
        model.addAttribute("scheduleMonths", months);
        model.addAttribute("boardId", boardId);
        return "fragments/scheduleMonths :: options";
    }
}
