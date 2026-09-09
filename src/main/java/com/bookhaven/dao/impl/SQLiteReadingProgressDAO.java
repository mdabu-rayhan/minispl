package com.bookhaven.dao.impl;

import com.bookhaven.dao.ReadingProgressDAO;
import com.bookhaven.model.ReadingProgress;
import com.bookhaven.model.ReadingStatus;
import com.bookhaven.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC implementation of ReadingProgressDAO.
 */
public class SQLiteReadingProgressDAO implements ReadingProgressDAO {

    private static final Logger LOGGER = Logger.getLogger(SQLiteReadingProgressDAO.class.getName());
    private final DatabaseManager databaseManager;

    public SQLiteReadingProgressDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public SQLiteReadingProgressDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Optional<ReadingProgress> findByUserAndBook(int userId, int bookId) {
        String sql = "SELECT id, user_id, book_id, current_page, status, last_read_at FROM reading_progress WHERE user_id = ? AND book_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProgress(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reading progress for user " + userId + " and book " + bookId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<ReadingProgress> findByUserId(int userId) {
        List<ReadingProgress> list = new ArrayList<>();
        String sql = "SELECT id, user_id, book_id, current_page, status, last_read_at FROM reading_progress WHERE user_id = ? ORDER BY last_read_at DESC";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProgress(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reading progress for user " + userId, e);
        }
        return list;
    }

    @Override
    public ReadingProgress saveOrUpdate(ReadingProgress progress) {
        String sql = "INSERT INTO reading_progress (user_id, book_id, current_page, status, last_read_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(user_id, book_id) DO UPDATE SET " +
                "current_page = excluded.current_page, " +
                "status = excluded.status, " +
                "last_read_at = excluded.last_read_at";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, progress.getUserId());
            stmt.setInt(2, progress.getBookId());
            stmt.setInt(3, progress.getCurrentPage());
            stmt.setString(4, progress.getStatus().name());
            stmt.setTimestamp(5, Timestamp.valueOf(progress.getLastReadAt() != null ? progress.getLastReadAt() : LocalDateTime.now()));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        progress.setId(keys.getInt(1));
                    }
                }
            }
            return progress;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving/updating reading progress", e);
            throw new RuntimeException("Database error saving reading progress", e);
        }
    }

    @Override
    public boolean delete(int userId, int bookId) {
        String sql = "DELETE FROM reading_progress WHERE user_id = ? AND book_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting reading progress", e);
            return false;
        }
    }

    @Override
    public long countCompletedByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM reading_progress WHERE user_id = ? AND status = 'COMPLETED'";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting completed books for user " + userId, e);
        }
        return 0;
    }

    @Override
    public long countReadingByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM reading_progress WHERE user_id = ? AND status = 'READING'";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting active books for user " + userId, e);
        }
        return 0;
    }

    @Override
    public long getTotalPagesReadAcrossAllUsers() {
        String sql = "SELECT COALESCE(SUM(current_page), 0) FROM reading_progress";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total pages read across all users", e);
        }
        return 0;
    }

    private ReadingProgress mapResultSetToProgress(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        int currentPage = rs.getInt("current_page");
        ReadingStatus status = ReadingStatus.fromString(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("last_read_at");
        LocalDateTime lastReadAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new ReadingProgress(id, userId, bookId, currentPage, status, lastReadAt);
    }
}
