package com.bookhaven.dao.impl;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.document.ProxyPdfDocument;
import com.bookhaven.model.Book;
import com.bookhaven.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC implementation of BookDAO.
 * Optimized to load metadata and lazily supply PDF BLOBs through ProxyPdfDocument.
 */
public class SQLiteBookDAO implements BookDAO {

    private static final Logger LOGGER = Logger.getLogger(SQLiteBookDAO.class.getName());
    private final DatabaseManager databaseManager;

    public SQLiteBookDAO() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public SQLiteBookDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Optional<Book> findById(int id) {
        String sql = "SELECT id, title, author, genre, total_pages, file_size, description, created_at FROM books WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs, false));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding book by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<byte[]> getPdfBytes(int bookId) {
        String sql = "SELECT file_data FROM books WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("file_data");
                    return Optional.ofNullable(bytes);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reading PDF binary BLOB for book: " + bookId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, author, genre, total_pages, file_size, description, created_at FROM books ORDER BY id DESC";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs, false));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all books", e);
        }
        return books;
    }

    @Override
    public List<Book> search(String query, String genre) {
        List<Book> books = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, title, author, genre, total_pages, file_size, description, created_at FROM books WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ?) ");
            String wild = "%" + query.trim().toLowerCase() + "%";
            params.add(wild);
            params.add(wild);
        }

        if (genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("All Genres")) {
            sql.append("AND LOWER(genre) = ? ");
            params.add(genre.trim().toLowerCase());
        }

        sql.append("ORDER BY id DESC");

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs, false));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching books", e);
        }
        return books;
    }

    @Override
    public List<String> findAllGenres() {
        List<String> genres = new ArrayList<>();
        String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND TRIM(genre) != '' ORDER BY genre ASC";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                genres.add(rs.getString("genre"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching unique genres", e);
        }
        return genres;
    }

    @Override
    public Book save(Book book) {
        String sql = "INSERT INTO books (title, author, genre, total_pages, file_data, file_size, description, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getGenre());
            stmt.setInt(4, book.getTotalPages());
            stmt.setBytes(5, book.getFileData() != null ? book.getFileData() : new byte[0]);
            stmt.setLong(6, book.getFileSize());
            stmt.setString(7, book.getDescription());
            stmt.setTimestamp(8, Timestamp.valueOf(book.getCreatedAt() != null ? book.getCreatedAt() : LocalDateTime.now()));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        book.setId(keys.getInt(1));
                    }
                }
            }
            // Set proxy document
            book.setDocument(new ProxyPdfDocument(book.getId(), book.getTitle(), book.getTotalPages(), book.getFileSize(), this));
            return book;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving book: " + book.getTitle(), e);
            throw new RuntimeException("Database error saving book", e);
        }
    }

    @Override
    public boolean updateMetadata(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, genre = ?, description = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getGenre());
            stmt.setString(4, book.getDescription());
            stmt.setInt(5, book.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating book metadata: " + book.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting book: " + id, e);
            return false;
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting books", e);
        }
        return 0;
    }

    @Override
    public long getTotalStorageBytes() {
        String sql = "SELECT COALESCE(SUM(file_size), 0) FROM books";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total storage bytes", e);
        }
        return 0;
    }

    private Book mapResultSetToBook(ResultSet rs, boolean loadBlob) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String author = rs.getString("author");
        String genre = rs.getString("genre");
        int totalPages = rs.getInt("total_pages");
        long fileSize = rs.getLong("file_size");
        String description = rs.getString("description");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        byte[] blob = loadBlob ? rs.getBytes("file_data") : null;

        return Book.builder()
                .id(id)
                .title(title)
                .author(author)
                .genre(genre)
                .totalPages(totalPages)
                .fileData(blob)
                .fileSize(fileSize)
                .description(description)
                .createdAt(createdAt)
                .document(new ProxyPdfDocument(id, title, totalPages, fileSize, this))
                .build();
    }
}
