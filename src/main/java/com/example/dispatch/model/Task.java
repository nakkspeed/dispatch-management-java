package com.example.dispatch.model;

import java.util.List;

public record Task(String name, boolean editable, String taskContents, List<Staff> staffs) {

    public static Task emptyAm() {
        return new Task("AM", false, "", List.of());
    }

    public static Task emptyPm() {
        return new Task("PM", false, "", List.of());
    }
}
