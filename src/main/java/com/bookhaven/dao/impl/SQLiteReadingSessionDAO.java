package com.bookhaven.dao.impl;

import com.bookhaven.dao.ReadingSessionDAO;
import com.bookhaven.model.ReadingSession;
import com.bookhaven.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC implementation of ReadingSessionDAO.
 */
public class SQLiteReadingSessionDAO implements ReadingSessionDAO {

    private static final Logger LOGGER = Logger.getLogger(SQLiteReadingSessionDAO.class.getName());
    private final DatabaseManager databaseManager;

    public SQLiteReadingSessionDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public SQLiteReadingSessionDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public ReadingSession save(ReadingSession session) {
        String sql = "INSERT INTO reading_sessions (user_id, book_id, start_time, end_time, pages_read, duration_minutes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, session.getUserId());
            stmt.setInt(2, session.getBookId());
            stmt.setTimestamp(3, Timestamp.valueOf(session.getStartTime() != null ? session.getStartTime() : LocalDateTime.now()));
            stmt.setTimestamp(4, Timestamp.valueOf(session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now()));
            stmt.setInt(5, session.getPagesRead());
            stmt.setInt(6, session.getDurationMinutes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        session.setId(keys.getInt(1));
                    }
                }
            }
            return session;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving reading session", e);
            throw new RuntimeException("Database error saving reading session", e);
        }
    }

    @Override
    public List<ReadingSession> findByUserId(int userId) {
        List<ReadingSession> list = new ArrayList<>();
        String sql = "SELECT id, user_id, book_id, start_time, end_time, pages_read, duration_minutes FROM reading_sessions WHERE user_id = ? ORDER BY start_time DESC";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reading sessions for user " + userId, e);
        }
        return list;
    }

    @Override
    public List<ReadingSession> findByUserIdAndBookId(int userId, int bookId) {
        List<ReadingSession> list = new ArrayList<>();
        String sql = "SELECT id, user_id, book_id, start_time, end_time, pages_read, duration_minutes FROM reading_sessions WHERE user_id = ? AND book_id = ? ORDER BY start_time DESC";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding reading sessions for user and book", e);
        }
        return list;
    }

    @Override
    public List<ReadingSession> findAll() {
        List<ReadingSession> list = new ArrayList<>();
        String sql = "SELECT id, user_id, book_id, start_time, end_time, pages_read, duration_minutes FROM reading_sessions ORDER BY start_time DESC";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToSession(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all reading sessions", e);
        }
        return list;
    }

    @Override
    public long getTotalReadingMinutesByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM reading_sessions WHERE user_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total reading minutes for user " + userId, e);
        }
        return 0;
    }

    @Override
    public long getTotalReadingMinutesThisMonth(int userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM reading_sessions WHERE user_id = ? AND start_time >= ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(startOfMonth));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating monthly reading minutes for user " + userId, e);
        }
        return 0;
    }

    @Override
    public long getTotalReadingMinutesAllUsers() {
        String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM reading_sessions";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total reading minutes across all users", e);
        }
        return 0;
    }

    private ReadingSession mapResultSetToSession(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        int pagesRead = rs.getInt("pages_read");
        int durationMinutes = rs.getInt("duration_minutes");

        LocalDateTime start = startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime end = endTs != null ? endTs.toLocalDateTime() : LocalDateTime.now();

        return new ReadingSession(id, userId, bookId, start, end, pagesRead, durationMinutes);
    }
}
