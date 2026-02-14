package com.example.dispatch.repository;

import com.example.dispatch.model.Schedule;
import com.example.dispatch.model.ScheduleMonth;
import com.example.dispatch.model.Staff;
import com.example.dispatch.model.Work;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ScheduleRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ScheduleRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 定期スケジュールから指定年月の月別スケジュールを生成する。
     * delete → insert の順に実行し、重複を防ぐ。
     */
    public void create(int boardId, int year, int month) {
        String sql = """
                INSERT INTO schedules (board_id, week, day_of_week, schedule_date, works, staffs)
                SELECT rs.board_id, rs.week, rs.day_of_week, ? AS schedule_date,
                       rs.works, bd.staffs
                  FROM regular_schedules rs
                  JOIN boards bd ON rs.board_id = bd.board_id
                 WHERE rs.board_id = ? AND rs.week = ? AND rs.day_of_week = ?
                """;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        int startDayOfWeek = firstDay.getDayOfWeek().getValue() % 7; // 0=Sun, 6=Sat

        int dayOfWeek = startDayOfWeek;
        int week = 0;
        List<Object[]> params = new ArrayList<>();

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            params.add(new Object[]{Date.valueOf(LocalDate.of(year, month, day)), boardId, week, dayOfWeek});
            dayOfWeek++;
            if (dayOfWeek == 7) dayOfWeek = 0;
            if (dayOfWeek == startDayOfWeek) week++;
        }
        jdbc.batchUpdate(sql, params);
    }

    public void delete(int boardId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        jdbc.update(
                "DELETE FROM schedules WHERE board_id = ? AND schedule_date BETWEEN ? AND ?",
                boardId,
                Date.valueOf(ym.atDay(1)),
                Date.valueOf(ym.atEndOfMonth())
        );
    }

    public List<Schedule> findByMonth(int boardId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return jdbc.query(
                "SELECT * FROM schedules WHERE board_id = ? AND schedule_date >= ? AND schedule_date <= ? ORDER BY schedule_date",
                this::mapRow,
                boardId,
                Date.valueOf(ym.atDay(1)),
                Date.valueOf(ym.atEndOfMonth())
        );
    }

    public int countByMonth(int boardId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM schedules WHERE board_id = ? AND schedule_date >= ? AND schedule_date <= ?",
                Integer.class,
                boardId,
                Date.valueOf(ym.atDay(1)),
                Date.valueOf(ym.atEndOfMonth())
        );
        return count != null ? count : 0;
    }

    public List<ScheduleMonth> findMonths(int boardId) {
        return jdbc.query(
                "SELECT DISTINCT TO_CHAR(DATE_TRUNC('month', schedule_date), 'YYYY-MM') AS schedule_month FROM schedules WHERE board_id = ? ORDER BY schedule_month ASC",
                (rs, rowNum) -> new ScheduleMonth(rs.getString("schedule_month")),
                boardId
        );
    }

    public List<Schedule> findById(int id) {
        return jdbc.query(
                "SELECT * FROM schedules WHERE id = ?",
                this::mapRow,
                id
        );
    }

    public void update(int id, String worksJson, String staffsJson) {
        jdbc.update(
                "UPDATE schedules SET works = ?::json, staffs = ?::json WHERE id = ?",
                worksJson, staffsJson, id
        );
    }

    private Schedule mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            List<Work> works = objectMapper.readValue(readJsonString(rs, "works"), new TypeReference<>() {});
            List<List<Staff>> staffs = objectMapper.readValue(readJsonString(rs, "staffs"), new TypeReference<>() {});
            return new Schedule(
                    rs.getInt("id"),
                    rs.getInt("board_id"),
                    rs.getInt("week"),
                    rs.getInt("day_of_week"),
                    rs.getDate("schedule_date").toLocalDate(),
                    works,
                    staffs
            );
        } catch (Exception e) {
            throw new SQLException("Failed to parse schedule JSON", e);
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
