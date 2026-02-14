package com.example.dispatch.service;

import com.example.dispatch.model.RegularSchedule;
import com.example.dispatch.repository.RegularScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegularScheduleService {

    private final RegularScheduleRepository repository;
    private final ObjectMapper objectMapper;

    public RegularScheduleService(RegularScheduleRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<RegularSchedule> findByBoardId(int boardId) {
        return repository.findByBoardId(boardId);
    }

    public List<RegularSchedule> findById(int id) {
        return repository.findById(id);
    }

    public void update(int id, JsonNode works) {
        try {
            repository.update(id, objectMapper.writeValueAsString(works));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize works", e);
        }
    }
}
