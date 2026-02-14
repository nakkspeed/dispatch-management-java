package com.example.dispatch.repository;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.Staff;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class BoardRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BoardRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<Board> findAll() {
        return jdbc.query(
                "SELECT board_id, board_name, staffs FROM boards ORDER BY board_id",
                this::mapBoard
        );
    }

    public void updateStaffs(int boardId, List<List<Staff>> staffs) {
        try {
            String staffsJson = objectMapper.writeValueAsString(staffs);
            jdbc.update(
                    "UPDATE boards SET staffs = ?::json WHERE board_id = ?",
                    staffsJson, boardId
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize staffs JSON", e);
        }
    }

    private Board mapBoard(ResultSet rs, int rowNum) throws SQLException {
        try {
            List<List<Staff>> staffs = objectMapper.readValue(
                    readJsonString(rs, "staffs"),
                    new TypeReference<>() {}
            );
            return new Board(rs.getInt("board_id"), rs.getString("board_name"), staffs);
        } catch (Exception e) {
            throw new SQLException("Failed to parse staffs JSON", e);
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
}
