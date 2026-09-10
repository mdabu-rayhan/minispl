package com.bookhaven.service;

import com.bookhaven.dao.*;
import com.bookhaven.event.ReadingEventPublisher;
import com.bookhaven.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying ReadingTrackerService business logic and Observer integration.
 */
public class ReadingTrackerServiceTest {

    private ReadingTrackerService trackerService;
    private List<ReadingProgress> progressList;
    private List<ReadingSession> sessionList;
    private List<Book> bookList;
    private List<User> userList;

    @BeforeEach
    public void setUp() {
        progressList = new ArrayList<>();
        sessionList = new ArrayList<>();
        bookList = new ArrayList<>();
        userList = new ArrayList<>();

        ReadingProgressDAO mockProgressDao = new ReadingProgressDAO() {
            private int idGen = 1;
            @Override public Optional<ReadingProgress> findByUserAndBook(int u, int b) { return progressList.stream().filter(p -> p.getUserId() == u && p.getBookId() == b).findFirst(); }
            @Override public List<ReadingProgress> findByUserId(int u) { return progressList.stream().filter(p -> p.getUserId() == u).toList(); }
            @Override public ReadingProgress saveOrUpdate(ReadingProgress p) {
                progressList.removeIf(existing -> existing.getUserId() == p.getUserId() && existing.getBookId() == p.getBookId());
                if (p.getId() == 0) p.setId(idGen++);
                progressList.add(p);
                return p;
            }
            @Override public boolean delete(int u, int b) { return progressList.removeIf(p -> p.getUserId() == u && p.getBookId() == b); }
            @Override public long countCompletedByUser(int u) { return progressList.stream().filter(p -> p.getUserId() == u && p.getStatus() == ReadingStatus.COMPLETED).count(); }
            @Override public long countReadingByUser(int u) { return progressList.stream().filter(p -> p.getUserId() == u && p.getStatus() == ReadingStatus.READING).count(); }
            @Override public long getTotalPagesReadAcrossAllUsers() { return progressList.stream().mapToLong(ReadingProgress::getCurrentPage).sum(); }
        };

        ReadingSessionDAO mockSessionDao = new ReadingSessionDAO() {
            private int idGen = 1;
            @Override public ReadingSession save(ReadingSession s) {
                if (s.getId() == 0) s.setId(idGen++);
                sessionList.add(s);
                return s;
            }
            @Override public List<ReadingSession> findByUserId(int u) { return sessionList.stream().filter(s -> s.getUserId() == u).toList(); }
            @Override public List<ReadingSession> findByUserIdAndBookId(int u, int b) { return sessionList.stream().filter(s -> s.getUserId() == u && s.getBookId() == b).toList(); }
            @Override public List<ReadingSession> findAll() { return new ArrayList<>(sessionList); }
            @Override public long getTotalReadingMinutesByUser(int u) { return sessionList.stream().filter(s -> s.getUserId() == u).mapToLong(ReadingSession::getDurationMinutes).sum(); }
            @Override public long getTotalReadingMinutesThisMonth(int u) { return getTotalReadingMinutesByUser(u); }
            @Override public long getTotalReadingMinutesAllUsers() { return sessionList.stream().mapToLong(ReadingSession::getDurationMinutes).sum(); }
        };

        BookDAO mockBookDao = new BookDAO() {
            @Override public Optional<Book> findById(int id) { return bookList.stream().filter(b -> b.getId() == id).findFirst(); }
            @Override public Optional<byte[]> getPdfBytes(int bookId) { return Optional.empty(); }
            @Override public List<Book> findAll() { return new ArrayList<>(bookList); }
            @Override public List<Book> search(String q, String g) { return new ArrayList<>(bookList); }
            @Override public List<String> findAllGenres() { return List.of(); }
            @Override public Book save(Book b) { bookList.add(b); return b; }
            @Override public boolean updateMetadata(Book b) { return true; }
            @Override public boolean delete(int id) { return bookList.removeIf(b -> b.getId() == id); }
            @Override public long count() { return bookList.size(); }
            @Override public long getTotalStorageBytes() { return bookList.stream().mapToLong(Book::getFileSize).sum(); }
        };

        UserDAO mockUserDao = new UserDAO() {
            @Override public Optional<User> findById(int id) { return userList.stream().filter(u -> u.getId() == id).findFirst(); }
            @Override public Optional<User> findByUsername(String u) { return userList.stream().filter(x -> x.getUsername().equalsIgnoreCase(u)).findFirst(); }
            @Override public List<User> findAll() { return new ArrayList<>(userList); }
            @Override public User save(User u) { userList.add(u); return u; }
            @Override public boolean update(User u) { return true; }
            @Override public boolean delete(int id) { return userList.removeIf(u -> u.getId() == id); }
            @Override public long count() { return userList.size(); }
        };

        // Populate sample entities
        User reader = new ReaderUser(1, "reader_tester", "pw", LocalDateTime.now());
        userList.add(reader);
        Book book = new Book(10, "Domain-Driven Design", "Eric Evans", "Technology", 10, new byte[0], 5000, "DDD", LocalDateTime.now());
        bookList.add(book);

        trackerService = new ReadingTrackerService(mockProgressDao, mockSessionDao, mockBookDao, mockUserDao, new ReadingEventPublisher());
    }

    @Test
    public void testProgressTransitionsToCompletedOnLastPage() {
        trackerService.updateProgress(1, 10, 5, 10);
        Optional<ReadingProgress> p1 = trackerService.getProgress(1, 10);
        assertTrue(p1.isPresent());
        assertEquals(5, p1.get().getCurrentPage());
        assertEquals(ReadingStatus.READING, p1.get().getStatus());

        // Now advance to final page (10 of 10)
        trackerService.updateProgress(1, 10, 10, 10);
        Optional<ReadingProgress> p2 = trackerService.getProgress(1, 10);
        assertTrue(p2.isPresent());
        assertEquals(10, p2.get().getCurrentPage());
        assertEquals(ReadingStatus.COMPLETED, p2.get().getStatus());
    }

    @Test
    public void testReadingSessionLoggingAndMetrics() {
        trackerService.logSession(1, 10, 5, 45);
        trackerService.logSession(1, 10, 5, 15);

        Map<String, Object> stats = trackerService.getUserAnalytics(1);
        assertEquals(60L, stats.get("totalMinutes"));
        assertEquals("1.0 hrs", stats.get("totalHours"));
    }
}
