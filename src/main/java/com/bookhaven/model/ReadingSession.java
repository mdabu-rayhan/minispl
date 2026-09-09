package com.bookhaven.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain entity recording a discreet reading session duration and pages read.
 */
public class ReadingSession {

    private int id;
    private int userId;
    private int bookId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int pagesRead;
    private int durationMinutes;

    public ReadingSession() {
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now();
    }

    public ReadingSession(int id, int userId, int bookId, LocalDateTime startTime, LocalDateTime endTime, int pagesRead, int durationMinutes) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.startTime = startTime != null ? startTime : LocalDateTime.now();
        this.endTime = endTime != null ? endTime : LocalDateTime.now();
        this.pagesRead = pagesRead;
        this.durationMinutes = durationMinutes > 0 ? durationMinutes : calculateDurationMinutes(this.startTime, this.endTime);
    }

    private static int calculateDurationMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        long mins = Duration.between(start, end).toMinutes();
        return (int) Math.max(1, mins);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (this.startTime != null && this.endTime != null) {
            this.durationMinutes = calculateDurationMinutes(this.startTime, this.endTime);
        }
    }

    public int getPagesRead() {
        return pagesRead;
    }

    public void setPagesRead(int pagesRead) {
        this.pagesRead = pagesRead;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    @Override
    public String toString() {
        return "ReadingSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", durationMinutes=" + durationMinutes +
                ", pagesRead=" + pagesRead +
                '}';
    }
}
