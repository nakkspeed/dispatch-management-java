package com.example.dispatch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record Schedule(
        int id,
        @JsonProperty("board_id") int boardId,
        int week,
        @JsonProperty("day_of_week") int dayOfWeek,
        @JsonProperty("schedule_date") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate scheduleDate,
        List<Work> works,
        List<List<Staff>> staffs
) {}
