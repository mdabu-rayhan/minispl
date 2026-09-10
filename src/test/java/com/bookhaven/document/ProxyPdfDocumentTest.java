package com.bookhaven.document;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.util.DatabaseSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying the Virtual Proxy design pattern implementation.
 */
public class ProxyPdfDocumentTest {

    private byte[] samplePdfBytes;

    @BeforeEach
    public void setUp() {
        samplePdfBytes = DatabaseSeeder.createSamplePdf(
                "Proxy Test Book",
                "Architect",
                "Virtual Proxy Testing",
                3,
                new String[]{"Page 1 Content", "Page 2 Content", "Page 3 Content"}
        );
    }

    @Test
    public void testVirtualProxyDefersDocumentLoading() {
        // Stub BookDAO that returns our sample PDF bytes
        BookDAO stubDao = new BookDAO() {
            @Override public Optional<byte[]> getPdfBytes(int bookId) { return Optional.of(samplePdfBytes); }
            @Override public Optional<com.bookhaven.model.Book> findById(int id) { return Optional.empty(); }
            @Override public java.util.List<com.bookhaven.model.Book> findAll() { return java.util.List.of(); }
            @Override public java.util.List<com.bookhaven.model.Book> search(String q, String g) { return java.util.List.of(); }
            @Override public java.util.List<String> findAllGenres() { return java.util.List.of(); }
            @Override public com.bookhaven.model.Book save(com.bookhaven.model.Book b) { return b; }
            @Override public boolean updateMetadata(com.bookhaven.model.Book b) { return true; }
            @Override public boolean delete(int id) { return true; }
            @Override public long count() { return 1; }
            @Override public long getTotalStorageBytes() { return samplePdfBytes.length; }
        };

        ProxyPdfDocument proxy = new ProxyPdfDocument(101, "Proxy Test Book", 3, samplePdfBytes.length, stubDao);

        // 1. Initial State: Lightweight metadata is accessible without loading the heavy PDFBox document
        assertEquals(101, proxy.getBookId());
        assertEquals("Proxy Test Book", proxy.getTitle());
        assertEquals(3, proxy.getPageCount());
        assertEquals(samplePdfBytes.length, proxy.getFileSize());
        assertFalse(proxy.isLoaded(), "Real document must NOT be loaded upon proxy instantiation");
        assertNull(proxy.getRealPdfDocument(), "Underlying RealPdfDocument reference should be null initially");

        // 2. Invoking text extraction triggers on-demand lazy instantiation
        String text = proxy.extractPageText(0);
        assertNotNull(text);
        assertTrue(text.contains("Proxy Test Book") || text.contains("Page 1"), "Extracted text should match PDF content");
        assertTrue(proxy.isLoaded(), "Real document must now be loaded in memory");
        assertNotNull(proxy.getRealPdfDocument(), "RealPdfDocument should now be instantiated");

        // 3. Closing the proxy unloads the document and frees memory
        proxy.close();
        assertFalse(proxy.isLoaded(), "Document must be unloaded after closing");
        assertNull(proxy.getRealPdfDocument(), "RealPdfDocument reference should be cleared after close");
    }
}
