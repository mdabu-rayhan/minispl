package com.bookhaven.search;

import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;
import com.bookhaven.model.ReadingStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy: Sorts books by reader engagement status:
 * 1. READING (Currently active)
 * 2. UNREAD (Not yet started)
 * 3. COMPLETED (Finished)
 */
public class ReadingStatusSortStrategy implements CatalogSortStrategy {

    @Override
    public String getStrategyName() {
        return "Reading Status";
    }

    @Override
    public List<Book> sort(List<Book> books, Map<Integer, ReadingProgress> progressMap) {
        if (books == null) return new ArrayList<>();
        List<Book> sorted = new ArrayList<>(books);

        sorted.sort(Comparator.comparingInt(book -> {
            if (progressMap == null || !progressMap.containsKey(book.getId())) {
                return 2; // UNREAD
            }
            ReadingProgress progress = progressMap.get(book.getId());
            ReadingStatus status = progress != null ? progress.getStatus() : ReadingStatus.UNREAD;
            return switch (status) {
                case READING -> 1;
                case UNREAD -> 2;
                case COMPLETED -> 3;
            };
        }));

        return sorted;
    }
}
