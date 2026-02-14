package com.example.dispatch.repository;

import com.example.dispatch.model.RegularSchedule;
import com.example.dispatch.model.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RegularScheduleRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public RegularScheduleRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void create(int boardId, List<String> routes) {
        String sql = "INSERT INTO regular_schedules (board_id, week, day_of_week, works) VALUES (?, ?, ?, ?::json)";
        String worksJson = toJson(Work.empty(routes));
        for (int week = 0; week < 5; week++) {
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                jdbc.update(sql, boardId, week, dayOfWeek, worksJson);
            }
        }
    }

    public List<RegularSchedule> findByBoardId(int boardId) {
        return jdbc.query(
                "SELECT * FROM regular_schedules WHERE board_id = ? ORDER BY week, day_of_week",
                this::mapRow,
                boardId
        );
    }

    public List<RegularSchedule> findById(int id) {
        return jdbc.query(
                "SELECT * FROM regular_schedules WHERE id = ?",
                this::mapRow,
                id
        );
    }

    public void update(int id, String worksJson) {
        jdbc.update("UPDATE regular_schedules SET works = ?::json WHERE id = ?", worksJson, id);
    }

    private RegularSchedule mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            List<Work> works = objectMapper.readValue(readJsonString(rs, "works"), new TypeReference<>() {});
            return new RegularSchedule(
                    rs.getInt("id"),
                    rs.getInt("board_id"),
                    rs.getInt("week"),
                    rs.getInt("day_of_week"),
                    works
            );
        } catch (Exception e) {
            throw new SQLException("Failed to parse works JSON", e);
        }
    }

    /**
     * H2 の PostgreSQL 互換モードでは JSON 型カラムを getString() で取得すると
     * 値が JSON 文字列として二重エンコードされる場合がある。
     * その場合はアンラップして生の JSON 文字列を返す。
     */
    private String readJsonString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || !value.startsWith("\"")) {
            return value;
        }
        try {
            return objectMapper.readValue(value, String.class);
        } catch (Exception e) {
            return value;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize error", e);
        }
    }
}
