package com.bookhaven.document;

import javafx.scene.image.Image;

/**
 * Subject interface for the Virtual Proxy pattern representing a readable book document.
 */
public interface BookDocument extends AutoCloseable {

    int getBookId();

    String getTitle();

    int getPageCount();

    long getFileSize();

    boolean isLoaded();

    /**
     * Renders a page of the document at the specified zoom scale.
     *
     * @param pageIndex 0-based page index
     * @param scale     zoom scale factor (e.g. 1.0 = standard 72/96 DPI, 1.5 = 150%)
     * @return JavaFX Image of the rendered page
     */
    Image renderPageImage(int pageIndex, double scale);

    /**
     * Extracts plain text content from the specified page.
     *
     * @param pageIndex 0-based page index
     * @return page text
     */
    String extractPageText(int pageIndex);

    @Override
    void close();
}
