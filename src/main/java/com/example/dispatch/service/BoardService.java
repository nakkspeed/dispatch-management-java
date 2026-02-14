package com.example.dispatch.service;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.Staff;
import com.example.dispatch.repository.BoardRepository;
import com.example.dispatch.repository.RegularScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final RegularScheduleRepository regularScheduleRepository;

    public BoardService(BoardRepository boardRepository, RegularScheduleRepository regularScheduleRepository) {
        this.boardRepository = boardRepository;
        this.regularScheduleRepository = regularScheduleRepository;
    }

    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    /** 指定ボードの定期スケジュール初期データ (5週×7日) を生成する */
    public void initialize(int boardId) {
        regularScheduleRepository.create(boardId);
    }

    /** 指定ボードのスタッフ一覧を更新する */
    public void updateStaffs(int boardId, List<List<Staff>> staffs) {
        boardRepository.updateStaffs(boardId, staffs);
    }
}
