package com.example.dispatch.model;

import java.util.List;

public record Work(String name, List<Task> tasks) {

    public static final List<String> NAMES = List.of(
            "内勤", "ルートA", "ルートB", "ルートC", "ルートD",
            "ルートE", "ルートF", "ルートG", "ルートH", "ルートI", "ルートJ"
    );

    public static List<Work> empty() {
        return NAMES.stream()
                .map(n -> new Work(n, List.of(Task.emptyAm(), Task.emptyPm())))
                .toList();
    }
}
