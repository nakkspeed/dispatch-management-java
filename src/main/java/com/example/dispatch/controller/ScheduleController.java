package com.example.dispatch.controller;

import com.example.dispatch.model.Schedule;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.service.ScheduleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    /** 月別スケジュール一覧取得 API */
    @GetMapping("/schedule/{boardId}/{targetMonth}/readByMonth")
    public List<Schedule> readByMonth(@PathVariable int boardId, @PathVariable String targetMonth) {
        String[] parts = targetMonth.split("-");
        return service.findByMonth(boardId, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /** 月別スケジュール件数取得 API */
    @GetMapping("/schedule/{boardId}/{targetMonth}/countByMonth")
    public Map<String, Integer> countByMonth(@PathVariable int boardId, @PathVariable String targetMonth) {
        String[] parts = targetMonth.split("-");
        int count = service.countByMonth(boardId, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        return Map.of("count", count);
    }

    /** 月別スケジュール生成 API (定期スケジュールから) */
    @GetMapping("/schedule/{boardId}/{targetMonth}/createByMonth")
    public Map<String, String> createByMonth(@PathVariable int boardId, @PathVariable String targetMonth) {
        String[] parts = targetMonth.split("-");
        service.createByMonth(boardId, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        return Map.of("status", "end");
    }

    /** 登録済み年月リスト取得 API */
    @GetMapping("/schedule/{boardId}/readScheduleMonths")
    public List<ScheduleMonth> readScheduleMonths(@PathVariable int boardId) {
        return service.findMonths(boardId);
    }

    /** 月別スケジュール取得 API (ID 指定) */
    @GetMapping("/schedule/{scheduleId}/get")
    public List<Schedule> getById(@PathVariable int scheduleId) {
        return service.findById(scheduleId);
    }

    /** 月別スケジュール更新 API */
    @PostMapping("/schedule/update")
    public ResponseEntity<Void> update(@RequestBody Map<String, JsonNode> body) {
        int id = body.get("id").asInt();
        service.update(id, body.get("works"), body.get("staffs"));
        return ResponseEntity.ok().build();
    }
}
