package com.bookhaven.service;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.dao.impl.SQLiteBookDAO;
import com.bookhaven.model.Book;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multi-step Admin Ingestion Pipeline service for validating, parsing, and storing PDF books into SQLite.
 */
public class BookIngestionService {

    private static final Logger LOGGER = Logger.getLogger(BookIngestionService.class.getName());
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

    private final BookDAO bookDAO;

    public BookIngestionService() {
        this.bookDAO = new SQLiteBookDAO();
    }

    public BookIngestionService(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    /**
     * Executes the multi-step ingestion pipeline:
     * 1. Validates file existence and PDF format/size.
     * 2. Parses PDF with Apache PDFBox to extract page count and document information.
     * 3. Loads binary bytes into memory.
     * 4. Persists the Book record into SQLite.
     *
     * @param pdfFile     the source PDF file on disk
     * @param customTitle optional title (falls back to PDF metadata or filename if empty)
     * @param author      the book author
     * @param genre       the genre classification
     * @param description short synopsis
     * @return the saved Book entity with attached Virtual Proxy
     * @throws IllegalArgumentException if validation fails
     * @throws IOException              if file reading or PDF parsing fails
     */
    public Book ingestBook(File pdfFile, String customTitle, String author, String genre, String description) throws IOException {
        // Step 1: Validation
        validatePdfFile(pdfFile);

        // Step 2: PDF Parsing and Metadata Extraction via Apache PDFBox
        int pageCount;
        String extractedTitle = null;
        String extractedAuthor = null;

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            pageCount = document.getNumberOfPages();
            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                extractedTitle = info.getTitle();
                extractedAuthor = info.getAuthor();
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse PDF document structure: " + e.getMessage(), e);
        }

        if (pageCount <= 0) {
            throw new IllegalArgumentException("PDF file contains no readable pages");
        }

        // Determine final metadata fields
        String title = (customTitle != null && !customTitle.isBlank()) ? customTitle.trim() :
                (extractedTitle != null && !extractedTitle.isBlank()) ? extractedTitle.trim() :
                        stripExtension(pdfFile.getName());

        String finalAuthor = (author != null && !author.isBlank()) ? author.trim() :
                (extractedAuthor != null && !extractedAuthor.isBlank()) ? extractedAuthor.trim() : "Unknown Author";

        String finalGenre = (genre != null && !genre.isBlank()) ? genre.trim() : "General";
        String finalDesc = (description != null) ? description.trim() : "";

        // Step 3: Read raw binary bytes
        byte[] pdfBytes = readFileToByteArray(pdfFile);
        long fileSize = pdfFile.length();

        // Step 4: Construct and save Book entity using Builder Pattern
        Book book = Book.builder()
                .title(title)
                .author(finalAuthor)
                .genre(finalGenre)
                .totalPages(pageCount)
                .fileData(pdfBytes)
                .fileSize(fileSize)
                .description(finalDesc)
                .createdAt(LocalDateTime.now())
                .build();

        Book savedBook = bookDAO.save(book);
        LOGGER.info("Successfully ingested PDF book: '" + savedBook.getTitle() + "' (ID: " + savedBook.getId() + ", Pages: " + pageCount + ")");
        return savedBook;
    }

    /**
     * Ingests book from in-memory byte array (useful for testing or generated PDFs).
     */
    public Book ingestBookFromBytes(byte[] pdfBytes, String title, String author, String genre, String description) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF byte array cannot be empty");
        }

        int pageCount;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            pageCount = document.getNumberOfPages();
        } catch (Exception e) {
            throw new IOException("Failed to parse in-memory PDF bytes: " + e.getMessage(), e);
        }

        Book book = Book.builder()
                .title(title != null ? title.trim() : "Untitled")
                .author(author != null ? author.trim() : "Unknown Author")
                .genre(genre != null ? genre.trim() : "General")
                .totalPages(pageCount)
                .fileData(pdfBytes)
                .fileSize(pdfBytes.length)
                .description(description != null ? description.trim() : "")
                .createdAt(LocalDateTime.now())
                .build();

        return bookDAO.save(book);
    }

    /**
     * Validates input PDF file properties.
     */
    public void validatePdfFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Selected file does not exist or is not a regular file");
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Selected file must be a PDF document (.pdf extension)");
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("Selected file is empty (0 bytes)");
        }
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 50 MB");
        }
    }

    public List<Book> getAllBooks() {
        return bookDAO.findAll();
    }

    public Optional<Book> getBookById(int id) {
        return bookDAO.findById(id);
    }

    public boolean updateBookMetadata(Book book) {
        return bookDAO.updateMetadata(book);
    }

    public boolean deleteBook(int id) {
        return bookDAO.delete(id);
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int bytesRead = 0;
            while (bytesRead < data.length) {
                int read = fis.read(data, bytesRead, data.length - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            return data;
        }
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Untitled";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
