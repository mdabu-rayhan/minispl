package com.bookhaven.service;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.model.Book;
import com.bookhaven.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying the Book Ingestion Pipeline service.
 */
public class BookIngestionServiceTest {

    private BookIngestionService ingestionService;
    private List<Book> memoryDb;

    @BeforeEach
    public void setUp() {
        memoryDb = new ArrayList<>();
        BookDAO inMemoryDao = new BookDAO() {
            private int idSeq = 1;
            @Override public Optional<Book> findById(int id) { return memoryDb.stream().filter(b -> b.getId() == id).findFirst(); }
            @Override public Optional<byte[]> getPdfBytes(int bookId) {
                return findById(bookId).map(Book::getFileData);
            }
            @Override public List<Book> findAll() { return new ArrayList<>(memoryDb); }
            @Override public List<Book> search(String query, String genre) { return new ArrayList<>(memoryDb); }
            @Override public List<String> findAllGenres() { return List.of("Philosophy"); }
            @Override public Book save(Book book) {
                book.setId(idSeq++);
                memoryDb.add(book);
                return book;
            }
            @Override public boolean updateMetadata(Book book) { return true; }
            @Override public boolean delete(int id) { return memoryDb.removeIf(b -> b.getId() == id); }
            @Override public long count() { return memoryDb.size(); }
            @Override public long getTotalStorageBytes() { return memoryDb.stream().mapToLong(Book::getFileSize).sum(); }
        };

        ingestionService = new BookIngestionService(inMemoryDao);
    }

    @Test
    public void testIngestValidPdfBytes() throws IOException {
        byte[] pdfBytes = DatabaseSeeder.createSamplePdf(
                "Ingestion Pipeline Test",
                "Pipeline Engineer",
                "Automated Ingestion",
                4,
                new String[]{"P1", "P2", "P3", "P4"}
        );

        Book ingested = ingestionService.ingestBookFromBytes(
                pdfBytes,
                "Ingestion Pipeline Test",
                "Pipeline Engineer",
                "Engineering",
                "Testing pipeline"
        );

        assertNotNull(ingested);
        assertEquals(1, ingested.getId());
        assertEquals("Ingestion Pipeline Test", ingested.getTitle());
        assertEquals("Pipeline Engineer", ingested.getAuthor());
        assertEquals("Engineering", ingested.getGenre());
        assertEquals(4, ingested.getTotalPages(), "Page count must be extracted automatically by PDFBox");
        assertEquals(pdfBytes.length, ingested.getFileSize());
        assertEquals(1, memoryDb.size());
    }

    @Test
    public void testIngestEmptyPdfFails() {
        assertThrows(IllegalArgumentException.class, () -> {
            ingestionService.ingestBookFromBytes(new byte[0], "Empty", "Author", "Genre", "");
        });
    }
}
