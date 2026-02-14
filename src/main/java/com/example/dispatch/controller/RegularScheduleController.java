package com.example.dispatch.controller;

import com.example.dispatch.model.RegularSchedule;
import com.example.dispatch.service.RegularScheduleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class RegularScheduleController {

    private final RegularScheduleService service;

    public RegularScheduleController(RegularScheduleService service) {
        this.service = service;
    }

    /** 定期スケジュール一覧取得 API */
    @GetMapping("/regular_schedules/list/{boardId}")
    public List<RegularSchedule> list(@PathVariable int boardId) {
        return service.findByBoardId(boardId);
    }

    /** 定期スケジュール取得 API (ID 指定) */
    @GetMapping("/regular_schedule/{scheduleId}/get")
    public List<RegularSchedule> getById(@PathVariable int scheduleId) {
        return service.findById(scheduleId);
    }

    /** 定期スケジュール更新 API */
    @PostMapping("/regular_schedules/update")
    public ResponseEntity<Void> update(@RequestBody Map<String, JsonNode> body) {
        int id = body.get("id").asInt();
        service.update(id, body.get("works"));
        return ResponseEntity.ok().build();
    }
}
