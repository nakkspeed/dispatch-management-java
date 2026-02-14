package com.example.dispatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RegularSchedule(
        int id,
        @JsonProperty("board_id") int boardId,
        int week,
        @JsonProperty("day_of_week") int dayOfWeek,
        List<Work> works
) {}
