package com.example.dispatch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScheduleMonth(
        @JsonProperty("schedule_month") String scheduleMonth
) {}
