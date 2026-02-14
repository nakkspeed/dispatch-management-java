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

    private Board mapBoard(ResultSet rs, int rowNum) throws SQLException {
        try {
            List<List<Staff>> staffs = objectMapper.readValue(
                    rs.getString("staffs"),
                    new TypeReference<>() {}
            );
            return new Board(rs.getInt("board_id"), rs.getString("board_name"), staffs);
        } catch (Exception e) {
            throw new SQLException("Failed to parse staffs JSON", e);
        }
    }
}
