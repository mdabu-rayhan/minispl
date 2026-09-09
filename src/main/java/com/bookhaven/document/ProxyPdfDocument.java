package com.bookhaven.document;

import com.bookhaven.dao.BookDAO;
import javafx.scene.image.Image;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Virtual Proxy in the Virtual Proxy pattern.
 * Holds lightweight book metadata and defers raw PDF byte fetching and PDFBox parsing until needed.
 */
public class ProxyPdfDocument implements BookDocument {

    private static final Logger LOGGER = Logger.getLogger(ProxyPdfDocument.class.getName());

    private final int bookId;
    private final String title;
    private final int totalPages;
    private final long fileSize;
    private final BookDAO bookDAO;

    private RealPdfDocument realPdfDocument;

    public ProxyPdfDocument(int bookId, String title, int totalPages, long fileSize, BookDAO bookDAO) {
        this.bookId = bookId;
        this.title = title;
        this.totalPages = totalPages;
        this.fileSize = fileSize;
        this.bookDAO = bookDAO;
    }

    /**
     * Lazily loads the RealPdfDocument from the database if not already loaded.
     */
    private synchronized void ensureLoaded() {
        if (realPdfDocument == null) {
            LOGGER.info("Virtual Proxy: Loading raw PDF byte stream for book ID " + bookId + " ('" + title + "')...");
            byte[] bytes = new byte[0];
            if (bookDAO != null) {
                Optional<byte[]> optionalBytes = bookDAO.getPdfBytes(bookId);
                if (optionalBytes.isPresent()) {
                    bytes = optionalBytes.get();
                }
            }
            this.realPdfDocument = new RealPdfDocument(bookId, title, fileSize, bytes);
        }
    }

    @Override
    public int getBookId() {
        return bookId;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getPageCount() {
        if (realPdfDocument != null) {
            return realPdfDocument.getPageCount();
        }
        return totalPages > 0 ? totalPages : 1;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public boolean isLoaded() {
        return realPdfDocument != null && realPdfDocument.isLoaded();
    }

    @Override
    public Image renderPageImage(int pageIndex, double scale) {
        ensureLoaded();
        return realPdfDocument.renderPageImage(pageIndex, scale);
    }

    @Override
    public String extractPageText(int pageIndex) {
        ensureLoaded();
        return realPdfDocument.extractPageText(pageIndex);
    }

    @Override
    public synchronized void close() {
        if (realPdfDocument != null) {
            LOGGER.info("Virtual Proxy: Closing RealPdfDocument for book ID " + bookId);
            realPdfDocument.close();
            realPdfDocument = null;
        }
    }

    /**
     * Returns the underlying real document (or null if not yet loaded).
     */
    public RealPdfDocument getRealPdfDocument() {
        return realPdfDocument;
    }
}
