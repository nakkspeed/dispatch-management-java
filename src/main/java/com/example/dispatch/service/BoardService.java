package com.example.dispatch.service;

import com.example.dispatch.model.Board;
import com.example.dispatch.model.Staff;
import com.example.dispatch.repository.BoardRepository;
import com.example.dispatch.repository.RegularScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** ボードと定期スケジュール (5週×7日) を一括作成する */
    @Transactional
    public void createBoard(int boardId, String boardName, List<String> routes) {
        boardRepository.create(boardId, boardName);
        regularScheduleRepository.create(boardId, routes);
    }

    /** 指定ボードのスタッフ一覧を更新する */
    public void updateStaffs(int boardId, List<List<Staff>> staffs) {
        boardRepository.updateStaffs(boardId, staffs);
    }
}
