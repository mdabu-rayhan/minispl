package com.bookhaven.event;

import java.time.LocalDateTime;

/**
 * Immutable event data object published when reading interactions occur.
 */
public final class ReadingEvent {

    private final ReadingEventType eventType;
    private final int userId;
    private final int bookId;
    private final int currentPage;
    private final int totalPages;
    private final int sessionDurationMinutes;
    private final LocalDateTime timestamp;

    public ReadingEvent(ReadingEventType eventType, int userId, int bookId, int currentPage, int totalPages, int sessionDurationMinutes) {
        this.eventType = eventType;
        this.userId = userId;
        this.bookId = bookId;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.timestamp = LocalDateTime.now();
    }

    public ReadingEventType getEventType() {
        return eventType;
    }

    public int getUserId() {
        return userId;
    }

    public int getBookId() {
        return bookId;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isCompleted() {
        return eventType == ReadingEventType.BOOK_COMPLETED || (totalPages > 0 && currentPage >= totalPages);
    }

    @Override
    public String toString() {
        return "ReadingEvent{" +
                "eventType=" + eventType +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", currentPage=" + currentPage +
                ", totalPages=" + totalPages +
                ", sessionDurationMinutes=" + sessionDurationMinutes +
                ", timestamp=" + timestamp +
                '}';
    }
}
