package com.bookhaven.dao;

import com.bookhaven.model.ReadingProgress;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for ReadingProgress tracking.
 */
public interface ReadingProgressDAO {
    Optional<ReadingProgress> findByUserAndBook(int userId, int bookId);
    List<ReadingProgress> findByUserId(int userId);
    ReadingProgress saveOrUpdate(ReadingProgress progress);
    boolean delete(int userId, int bookId);
    long countCompletedByUser(int userId);
    long countReadingByUser(int userId);
    long getTotalPagesReadAcrossAllUsers();
}
