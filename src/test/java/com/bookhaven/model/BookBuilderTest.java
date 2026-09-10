package com.bookhaven.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BookBuilderTest {

    @Test
    @DisplayName("Builder constructs a complete Book object with all attributes")
    void testCompleteBookBuilder() {
        LocalDateTime now = LocalDateTime.now();
        byte[] bytes = new byte[]{1, 2, 3};

        Book book = Book.builder()
                .id(42)
                .title("Design Patterns: Elements of Reusable Object-Oriented Software")
                .author("Gang of Four")
                .genre("Computer Science")
                .totalPages(395)
                .fileData(bytes)
                .fileSize(bytes.length)
                .description("Seminal work on software engineering design patterns.")
                .createdAt(now)
                .build();

        assertEquals(42, book.getId());
        assertEquals("Design Patterns: Elements of Reusable Object-Oriented Software", book.getTitle());
        assertEquals("Gang of Four", book.getAuthor());
        assertEquals("Computer Science", book.getGenre());
        assertEquals(395, book.getTotalPages());
        assertArrayEquals(bytes, book.getFileData());
        assertEquals(bytes.length, book.getFileSize());
        assertEquals("Seminal work on software engineering design patterns.", book.getDescription());
        assertEquals(now, book.getCreatedAt());
    }

    @Test
    @DisplayName("Builder provides sensible defaults for optional fields")
    void testDefaultBookBuilder() {
        Book book = Book.builder()
                .title("Minimal Book")
                .build();

        assertEquals(0, book.getId());
        assertEquals("Minimal Book", book.getTitle());
        assertEquals("Unknown Author", book.getAuthor());
        assertEquals("General", book.getGenre());
        assertEquals(1, book.getTotalPages());
        assertNotNull(book.getFileData());
        assertEquals(0, book.getFileSize());
        assertEquals("", book.getDescription());
        assertNotNull(book.getCreatedAt());
    }
}
