package com.bookhaven.dao;

import com.bookhaven.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for Book entities and PDF binary storage.
 */
public interface BookDAO {
    Optional<Book> findById(int id);
    Optional<byte[]> getPdfBytes(int bookId);
    List<Book> findAll();
    List<Book> search(String query, String genre);
    List<String> findAllGenres();
    Book save(Book book);
    boolean updateMetadata(Book book);
    boolean delete(int id);
    long count();
    long getTotalStorageBytes();
}
