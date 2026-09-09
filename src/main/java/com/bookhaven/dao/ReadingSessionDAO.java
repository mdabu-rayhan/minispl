package com.bookhaven.dao;

import com.bookhaven.model.ReadingSession;

import java.util.List;

/**
 * Data Access Object contract for ReadingSession logs.
 */
public interface ReadingSessionDAO {
    ReadingSession save(ReadingSession session);
    List<ReadingSession> findByUserId(int userId);
    List<ReadingSession> findByUserIdAndBookId(int userId, int bookId);
    List<ReadingSession> findAll();
    long getTotalReadingMinutesByUser(int userId);
    long getTotalReadingMinutesThisMonth(int userId);
    long getTotalReadingMinutesAllUsers();
}
