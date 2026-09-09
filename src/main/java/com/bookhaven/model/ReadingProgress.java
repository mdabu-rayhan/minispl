package com.bookhaven.model;

import java.time.LocalDateTime;

/**
 * Domain entity tracking a user's reading state and position for a specific book.
 */
public class ReadingProgress {

    private int id;
    private int userId;
    private int bookId;
    private int currentPage;
    private ReadingStatus status;
    private LocalDateTime lastReadAt;

    public ReadingProgress() {
        this.currentPage = 1;
        this.status = ReadingStatus.UNREAD;
        this.lastReadAt = LocalDateTime.now();
    }

    public ReadingProgress(int id, int userId, int bookId, int currentPage, ReadingStatus status, LocalDateTime lastReadAt) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.currentPage = Math.max(1, currentPage);
        this.status = status != null ? status : ReadingStatus.UNREAD;
        this.lastReadAt = lastReadAt != null ? lastReadAt : LocalDateTime.now();
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

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(1, currentPage);
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    /**
     * Calculates the percentage completed given the total book pages.
     */
    public double getProgressPercentage(int totalPages) {
        if (totalPages <= 0) return 0.0;
        if (status == ReadingStatus.COMPLETED) return 100.0;
        double pct = ((double) currentPage / totalPages) * 100.0;
        return Math.min(100.0, Math.max(0.0, pct));
    }

    @Override
    public String toString() {
        return "ReadingProgress{" +
                "id=" + id +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", currentPage=" + currentPage +
                ", status=" + status +
                '}';
    }
}
