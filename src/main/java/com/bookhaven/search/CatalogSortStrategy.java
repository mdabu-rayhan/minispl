package com.bookhaven.search;

import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for sorting and organizing book catalog collections.
 */
public interface CatalogSortStrategy {

    /**
     * Returns human-readable label for UI dropdowns.
     */
    String getStrategyName();

    /**
     * Sorts the given list of books according to the specific strategy algorithm.
     *
     * @param books       the list of books to sort
     * @param progressMap optional map of bookId -> ReadingProgress for user-specific ordering
     * @return sorted list of books
     */
    List<Book> sort(List<Book> books, Map<Integer, ReadingProgress> progressMap);
}
