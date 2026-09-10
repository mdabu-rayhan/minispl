package com.bookhaven.search;

import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy: Sorts books alphabetically by Title (A-Z).
 */
public class TitleSortStrategy implements CatalogSortStrategy {

    @Override
    public String getStrategyName() {
        return "Alphabetical (A - Z)";
    }

    @Override
    public List<Book> sort(List<Book> books, Map<Integer, ReadingProgress> progressMap) {
        if (books == null) return new ArrayList<>();
        List<Book> sorted = new ArrayList<>(books);
        sorted.sort(Comparator.comparing(
                book -> book.getTitle() != null ? book.getTitle().toLowerCase().trim() : "",
                String.CASE_INSENSITIVE_ORDER
        ));
        return sorted;
    }
}
