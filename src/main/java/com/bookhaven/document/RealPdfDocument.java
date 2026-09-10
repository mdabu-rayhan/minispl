package com.bookhaven.document;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real Subject in the Virtual Proxy pattern.
 * Manages the heavy Apache PDFBox PDDocument instance, byte streams, and page image rendering cache.
 */
public class RealPdfDocument implements BookDocument {

    private static final Logger LOGGER = Logger.getLogger(RealPdfDocument.class.getName());
    private static final int MAX_CACHE_PAGES = 10;

    private final int bookId;
    private final String title;
    private final long fileSize;
    private final byte[] pdfBytes;

    private PDDocument pdDocument;
    private PDFRenderer pdfRenderer;
    private int pageCount;

    // Simple LRU cache for rendered JavaFX page images: "pageIndex_scale" -> Image
    private final Map<String, Image> pageImageCache = new LinkedHashMap<>(MAX_CACHE_PAGES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > MAX_CACHE_PAGES;
        }
    };

    public RealPdfDocument(int bookId, String title, long fileSize, byte[] pdfBytes) {
        this.bookId = bookId;
        this.title = title;
        this.fileSize = fileSize;
        this.pdfBytes = pdfBytes;
        initializeDocument();
    }

    private void initializeDocument() {
        try {
            if (pdfBytes == null || pdfBytes.length == 0) {
                LOGGER.warning("Provided PDF byte array is empty for book: " + title);
                this.pdDocument = new PDDocument();
                this.pageCount = 0;
                return;
            }
            this.pdDocument = Loader.loadPDF(pdfBytes);
            this.pageCount = pdDocument.getNumberOfPages();
            this.pdfRenderer = new PDFRenderer(pdDocument);
            LOGGER.info("Successfully parsed RealPdfDocument for '" + title + "' (" + pageCount + " pages, " + pdfBytes.length + " bytes)");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load and parse PDF byte array for: " + title, e);
            this.pageCount = 0;
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
        return pageCount;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public boolean isLoaded() {
        return pdDocument != null;
    }

    @Override
    public synchronized Image renderPageImage(int pageIndex, double scale) {
        if (pdDocument == null || pageIndex < 0 || pageIndex >= pageCount) {
            return null;
        }

        String cacheKey = pageIndex + "_" + String.format("%.2f", scale);
        if (pageImageCache.containsKey(cacheKey)) {
            return pageImageCache.get(cacheKey);
        }

        try {
            float dpiScale = (float) Math.max(0.5, Math.min(3.0, scale));
            BufferedImage bufferedImage = pdfRenderer.renderImage(pageIndex, dpiScale);
            Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            pageImageCache.put(cacheKey, fxImage);
            return fxImage;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering page " + pageIndex + " at scale " + scale, e);
            return null;
        }
    }

    @Override
    public synchronized String extractPageText(int pageIndex) {
        if (pdDocument == null || pageIndex < 0 || pageIndex >= pageCount) {
            return "";
        }
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);
            return stripper.getText(pdDocument);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error extracting text from page " + pageIndex, e);
            return "";
        }
    }

    @Override
    public synchronized void close() {
        if (pdDocument != null) {
            try {
                pdDocument.close();
                LOGGER.info("Closed RealPdfDocument for: " + title);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing PDDocument for " + title, e);
            } finally {
                pdDocument = null;
                pdfRenderer = null;
                pageImageCache.clear();
            }
        }
    }
}
