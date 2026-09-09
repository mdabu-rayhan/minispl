package com.bookhaven.model;

/**
 * Status of a reader's engagement with a book.
 */
public enum ReadingStatus {
    UNREAD("Unread"),
    READING("Currently Reading"),
    COMPLETED("Completed");

    private final String displayName;

    ReadingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReadingStatus fromString(String status) {
        if (status == null) return UNREAD;
        try {
            return ReadingStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return UNREAD;
        }
    }
}
