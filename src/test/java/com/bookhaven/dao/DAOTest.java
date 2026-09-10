package com.bookhaven.dao;

import com.bookhaven.dao.impl.*;
import com.bookhaven.model.*;
import com.bookhaven.util.DatabaseManager;
import com.bookhaven.util.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration unit test verifying SQLite DAOs with schema migrations.
 */
public class DAOTest {

    private static final String TEST_DB_FILE = "target/test_bookhaven.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_FILE;

    private UserDAO userDAO;
    private BookDAO bookDAO;
    private ReadingProgressDAO progressDAO;
    private ReadingSessionDAO sessionDAO;

    @BeforeEach
    public void setUp() {
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        dbManager.setDatabaseUrl(TEST_DB_URL);

        userDAO = new SQLiteUserDAO(dbManager);
        bookDAO = new SQLiteBookDAO(dbManager);
        progressDAO = new SQLiteReadingProgressDAO(dbManager);
        sessionDAO = new SQLiteReadingSessionDAO(dbManager);
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.getInstance().closeConnection();
    }

    @Test
    public void testUserDaoOperations() {
        User user = new ReaderUser(0, "testreader", PasswordHasher.hashPassword("secret123"), LocalDateTime.now());
        User saved = userDAO.save(user);

        assertTrue(saved.getId() > 0);
        Optional<User> found = userDAO.findByUsername("testreader");
        assertTrue(found.isPresent());
        assertEquals("testreader", found.get().getUsername());
        assertFalse(found.get().isAdmin());
        assertTrue(PasswordHasher.checkPassword("secret123", found.get().getPasswordHash()));
        assertEquals(1, userDAO.count());
    }

    @Test
    public void testBookDaoAndProgressOperations() {
        byte[] dummyPdf = new byte[]{1, 2, 3, 4, 5};
        Book book = new Book(0, "Test Architecture Book", "Martin Fowler", "Technology", 12, dummyPdf, dummyPdf.length, "Architecture", LocalDateTime.now());
        Book savedBook = bookDAO.save(book);

        assertTrue(savedBook.getId() > 0);
        Optional<byte[]> blobBytes = bookDAO.getPdfBytes(savedBook.getId());
        assertTrue(blobBytes.isPresent());
        assertEquals(5, blobBytes.get().length);

        // Test search
        List<Book> searchResults = bookDAO.search("Architecture", "Technology");
        assertEquals(1, searchResults.size());

        // Test Progress
        User user = userDAO.save(new ReaderUser(0, "readerX", PasswordHasher.hashPassword("pw"), LocalDateTime.now()));
        ReadingProgress progress = new ReadingProgress(0, user.getId(), savedBook.getId(), 6, ReadingStatus.READING, LocalDateTime.now());
        progressDAO.saveOrUpdate(progress);

        Optional<ReadingProgress> fetchedProgress = progressDAO.findByUserAndBook(user.getId(), savedBook.getId());
        assertTrue(fetchedProgress.isPresent());
        assertEquals(6, fetchedProgress.get().getCurrentPage());
        assertEquals(ReadingStatus.READING, fetchedProgress.get().getStatus());
        assertEquals(50.0, fetchedProgress.get().getProgressPercentage(12));

        // Test Sessions
        ReadingSession session = new ReadingSession(0, user.getId(), savedBook.getId(), LocalDateTime.now().minusMinutes(30), LocalDateTime.now(), 6, 30);
        sessionDAO.save(session);
        assertEquals(30, sessionDAO.getTotalReadingMinutesByUser(user.getId()));
    }
}
