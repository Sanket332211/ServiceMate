package com.example.carservice.entity;

import java.time.LocalTime;

/**
 * TimeSlot
 *
 * Defines the 4 fixed daily intake time slots for vehicle servicing at ServiceMate.
 * Capacity per slot is strictly limited to 2 vehicles.
 */
public enum TimeSlot {
    MORNING_SLOT_1("09:00 AM - 11:00 AM", LocalTime.of(9, 0), LocalTime.of(11, 0)),
    MORNING_SLOT_2("11:00 AM - 01:00 PM", LocalTime.of(11, 0), LocalTime.of(13, 0)),
    AFTERNOON_SLOT_1("02:00 PM - 04:00 PM", LocalTime.of(14, 0), LocalTime.of(16, 0)),
    AFTERNOON_SLOT_2("04:00 PM - 06:00 PM", LocalTime.of(16, 0), LocalTime.of(18, 0));

    private final String label;
    private final LocalTime startTime;
    private final LocalTime endTime;

    TimeSlot(String label, LocalTime startTime, LocalTime endTime) {
        this.label = label;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getLabel() {
        return label;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
