package com.example.carservice.dto;

import com.example.carservice.entity.TimeSlot;

/**
 * SlotAvailabilityResponse
 *
 * Provides real-time capacity and availability details for a specific time slot on a given date.
 */
public class SlotAvailabilityResponse {

    private TimeSlot slot;
    private String label;
    private int capacity;
    private int booked;
    private int remaining;
    private boolean available;
    private boolean past;

    public SlotAvailabilityResponse() {}

    public SlotAvailabilityResponse(TimeSlot slot, String label, int capacity, int booked, int remaining, boolean available) {
        this.slot = slot;
        this.label = label;
        this.capacity = capacity;
        this.booked = booked;
        this.remaining = remaining;
        this.available = available;
        this.past = false;
    }

    public SlotAvailabilityResponse(TimeSlot slot, String label, int capacity, int booked, int remaining, boolean available, boolean past) {
        this.slot = slot;
        this.label = label;
        this.capacity = capacity;
        this.booked = booked;
        this.remaining = remaining;
        this.available = available;
        this.past = past;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public void setSlot(TimeSlot slot) {
        this.slot = slot;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBooked() {
        return booked;
    }

    public void setBooked(int booked) {
        this.booked = booked;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isPast() {
        return past;
    }

    public void setPast(boolean past) {
        this.past = past;
    }
}
