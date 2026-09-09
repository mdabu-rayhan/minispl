package com.bookhaven.model;

import com.bookhaven.document.BookDocument;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Book in the library catalog.
 */
public class Book {

    private int id;
    private String title;
    private String author;
    private String genre;
    private int totalPages;
    private byte[] fileData;
    private long fileSize;
    private String description;
    private LocalDateTime createdAt;
    private BookDocument document;

    public Book() {
    }

    public Book(int id, String title, String author, String genre, int totalPages, byte[] fileData, long fileSize, String description, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalPages = totalPages;
        this.fileData = fileData;
        this.fileSize = fileSize;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Entry point for the Builder pattern.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing Book instances with optional and mandatory attributes.
     */
    public static class Builder {
        private int id = 0;
        private String title;
        private String author = "Unknown Author";
        private String genre = "General";
        private int totalPages = 1;
        private byte[] fileData = new byte[0];
        private long fileSize = 0;
        private String description = "";
        private LocalDateTime createdAt;
        private BookDocument document;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder fileData(byte[] fileData) {
            this.fileData = fileData != null ? fileData : new byte[0];
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder document(BookDocument document) {
            this.document = document;
            return this;
        }

        public Book build() {
            Book book = new Book(id, title, author, genre, totalPages, fileData, fileSize, description, createdAt);
            book.setDocument(document);
            return book;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BookDocument getDocument() {
        return document;
    }

    public void setDocument(BookDocument document) {
        this.document = document;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", genre='" + genre + '\'' +
                ", totalPages=" + totalPages +
                '}';
    }
}
