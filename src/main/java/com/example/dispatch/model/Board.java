package com.example.dispatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Board(
        @JsonProperty("board_id") int boardId,
        @JsonProperty("board_name") String boardName,
        List<List<Staff>> staffs
) {}
