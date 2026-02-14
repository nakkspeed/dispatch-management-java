package com.example.dispatch.service;

import com.example.dispatch.model.Schedule;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final ObjectMapper objectMapper;

    public ScheduleService(ScheduleRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<Schedule> findByMonth(int boardId, int year, int month) {
        return repository.findByMonth(boardId, year, month);
    }

    public int countByMonth(int boardId, int year, int month) {
        return repository.countByMonth(boardId, year, month);
    }

    public List<ScheduleMonth> findMonths(int boardId) {
        return repository.findMonths(boardId);
    }

    public List<Schedule> findById(int id) {
        return repository.findById(id);
    }

    /** 定期スケジュールから月別スケジュールを生成する (既存データは削除して再作成) */
    @Transactional
    public void createByMonth(int boardId, int year, int month) {
        repository.delete(boardId, year, month);
        repository.create(boardId, year, month);
    }

    public void update(int id, JsonNode works, JsonNode staffs) {
        try {
            repository.update(id,
                    objectMapper.writeValueAsString(works),
                    objectMapper.writeValueAsString(staffs));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize schedule", e);
        }
    }
}
