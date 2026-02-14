package com.example.dispatch.controller;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.model.Staff;
import com.example.dispatch.service.BoardService;
import com.example.dispatch.service.ScheduleService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

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

    /** ボード新規作成 API (ボード登録 + 定期スケジュール初期化を一括実行) */
    @PostMapping("/board/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBoard(@RequestBody Map<String, Object> body) {
        int boardId = ((Number) body.get("boardId")).intValue();
        String boardName = (String) body.get("boardName");
        @SuppressWarnings("unchecked")
        List<String> routes = (List<String>) body.get("routes");
        try {
            boardService.createBoard(boardId, boardName, routes);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "ボードID " + boardId + " は既に使用されています。別のIDを指定してください。"));
        }
        return ResponseEntity.ok(Map.of("boardId", boardId));
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
