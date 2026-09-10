package com.bookhaven.service;

import com.bookhaven.dao.*;
import com.bookhaven.dao.impl.*;
import com.bookhaven.event.ReadingEvent;
import com.bookhaven.event.ReadingEventListener;
import com.bookhaven.event.ReadingEventPublisher;
import com.bookhaven.event.ReadingEventType;
import com.bookhaven.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Business service managing reading progress, active session logging, and analytics.
 * Connects directly to the ReadingEventPublisher (Observer Pattern).
 */
public class ReadingTrackerService implements ReadingEventListener {

    private static final Logger LOGGER = Logger.getLogger(ReadingTrackerService.class.getName());

    private final ReadingProgressDAO progressDAO;
    private final ReadingSessionDAO sessionDAO;
    private final BookDAO bookDAO;
    private final UserDAO userDAO;
    private final ReadingEventPublisher eventPublisher;

    public ReadingTrackerService() {
        this(
                new SQLiteReadingProgressDAO(),
                new SQLiteReadingSessionDAO(),
                new SQLiteBookDAO(),
                new SQLiteUserDAO(),
                ReadingEventPublisher.getInstance()
        );
    }

    public ReadingTrackerService(ReadingProgressDAO progressDAO,
                                 ReadingSessionDAO sessionDAO,
                                 BookDAO bookDAO,
                                 UserDAO userDAO,
                                 ReadingEventPublisher eventPublisher) {
        this.progressDAO = progressDAO;
        this.sessionDAO = sessionDAO;
        this.bookDAO = bookDAO;
        this.userDAO = userDAO;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Observer pattern handler responding to dispatched reading events.
     */
    @Override
    public void onReadingEvent(ReadingEvent event) {
        if (event == null) return;
        LOGGER.info("Observer received ReadingEvent: " + event.getEventType() + " for user " + event.getUserId() + " on book " + event.getBookId());

        switch (event.getEventType()) {
            case PAGE_CHANGED -> handlePageChanged(event);
            case BOOK_COMPLETED -> handleBookCompleted(event);
            case SESSION_ENDED -> handleSessionEnded(event);
        }
    }

    private void handlePageChanged(ReadingEvent event) {
        ReadingStatus status = (event.getTotalPages() > 0 && event.getCurrentPage() >= event.getTotalPages())
                ? ReadingStatus.COMPLETED
                : ReadingStatus.READING;

        ReadingProgress progress = new ReadingProgress(
                0,
                event.getUserId(),
                event.getBookId(),
                event.getCurrentPage(),
                status,
                event.getTimestamp()
        );
        progressDAO.saveOrUpdate(progress);
    }

    private void handleBookCompleted(ReadingEvent event) {
        ReadingProgress progress = new ReadingProgress(
                0,
                event.getUserId(),
                event.getBookId(),
                event.getTotalPages(),
                ReadingStatus.COMPLETED,
                event.getTimestamp()
        );
        progressDAO.saveOrUpdate(progress);
    }

    private void handleSessionEnded(ReadingEvent event) {
        if (event.getSessionDurationMinutes() > 0) {
            ReadingSession session = new ReadingSession(
                    0,
                    event.getUserId(),
                    event.getBookId(),
                    event.getTimestamp().minusMinutes(event.getSessionDurationMinutes()),
                    event.getTimestamp(),
                    event.getCurrentPage(),
                    event.getSessionDurationMinutes()
            );
            sessionDAO.save(session);
        }
    }

    /**
     * Updates reading progress and dispatches a ReadingEvent through the publisher.
     */
    public void updateProgress(int userId, int bookId, int currentPage, int totalPages) {
        boolean isComplete = (totalPages > 0 && currentPage >= totalPages);
        ReadingEventType type = isComplete ? ReadingEventType.BOOK_COMPLETED : ReadingEventType.PAGE_CHANGED;
        ReadingEvent event = new ReadingEvent(type, userId, bookId, currentPage, totalPages, 0);

        // Update database immediately and publish event
        ReadingStatus status = isComplete ? ReadingStatus.COMPLETED : ReadingStatus.READING;
        progressDAO.saveOrUpdate(new ReadingProgress(0, userId, bookId, currentPage, status, LocalDateTime.now()));
        eventPublisher.publish(event);
    }

    /**
     * Logs a completed reading session and publishes SESSION_ENDED event.
     */
    public void logSession(int userId, int bookId, int pagesRead, int durationMinutes) {
        ReadingSession session = new ReadingSession(0, userId, bookId, LocalDateTime.now().minusMinutes(durationMinutes), LocalDateTime.now(), pagesRead, durationMinutes);
        sessionDAO.save(session);

        ReadingEvent event = new ReadingEvent(ReadingEventType.SESSION_ENDED, userId, bookId, pagesRead, 0, durationMinutes);
        eventPublisher.publish(event);
    }

    /**
     * Retrieves reading progress map for a user (bookId -> ReadingProgress).
     */
    public Map<Integer, ReadingProgress> getProgressMapForUser(int userId) {
        List<ReadingProgress> list = progressDAO.findByUserId(userId);
        Map<Integer, ReadingProgress> map = new HashMap<>();
        for (ReadingProgress p : list) {
            map.put(p.getBookId(), p);
        }
        return map;
    }

    public Optional<ReadingProgress> getProgress(int userId, int bookId) {
        return progressDAO.findByUserAndBook(userId, bookId);
    }

    // Analytics operations
    public Map<String, Object> getUserAnalytics(int userId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalMins = sessionDAO.getTotalReadingMinutesByUser(userId);
        long monthMins = sessionDAO.getTotalReadingMinutesThisMonth(userId);
        long completed = progressDAO.countCompletedByUser(userId);
        long reading = progressDAO.countReadingByUser(userId);

        stats.put("totalMinutes", totalMins);
        stats.put("totalHours", String.format("%.1f hrs", totalMins / 60.0));
        stats.put("monthMinutes", monthMins);
        stats.put("monthHours", String.format("%.1f hrs", monthMins / 60.0));
        stats.put("completedBooks", completed);
        stats.put("activeBooks", reading);
        return stats;
    }

    public Map<String, Object> getSystemAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalUsers = userDAO.count();
        long totalBooks = bookDAO.count();
        long totalStorage = bookDAO.getTotalStorageBytes();
        long totalPagesRead = progressDAO.getTotalPagesReadAcrossAllUsers();
        long totalMinutes = sessionDAO.getTotalReadingMinutesAllUsers();

        stats.put("totalUsers", totalUsers);
        stats.put("totalBooks", totalBooks);
        stats.put("totalStorageFormatted", formatBytes(totalStorage));
        stats.put("totalPagesRead", totalPagesRead);
        stats.put("totalReadingHours", String.format("%.1f hrs", totalMinutes / 60.0));
        return stats;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
