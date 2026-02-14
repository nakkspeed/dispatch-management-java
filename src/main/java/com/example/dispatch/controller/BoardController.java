package com.example.dispatch.controller;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.service.BoardService;
import com.example.dispatch.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

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

    /** スケジュールボード初期セットアップ API */
    @GetMapping("/initialize/{boardId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initialize(@PathVariable int boardId) {
        boardService.initialize(boardId);
        return ResponseEntity.ok(Map.of("boardId", boardId));
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
