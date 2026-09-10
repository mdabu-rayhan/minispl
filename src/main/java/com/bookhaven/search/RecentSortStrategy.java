package com.bookhaven.search;

import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy: Sorts books by creation date descending (Recently Added first).
 */
public class RecentSortStrategy implements CatalogSortStrategy {

    @Override
    public String getStrategyName() {
        return "Recently Added";
    }

    @Override
    public List<Book> sort(List<Book> books, Map<Integer, ReadingProgress> progressMap) {
        if (books == null) return new ArrayList<>();
        List<Book> sorted = new ArrayList<>(books);
        sorted.sort(Comparator.comparing(
                (Book book) -> book.getCreatedAt() != null ? book.getCreatedAt() : LocalDateTime.MIN
        ).reversed());
        return sorted;
    }
}
