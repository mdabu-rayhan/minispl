package com.bookhaven.search;

import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;
import com.bookhaven.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test verifying the Strategy pattern for dynamic catalog sorting.
 */
public class SearchStrategyTest {

    private Book bookA;
    private Book bookB;
    private Book bookC;
    private List<Book> testBooks;
    private Map<Integer, ReadingProgress> progressMap;

    @BeforeEach
    public void setUp() {
        bookA = new Book(1, "Zebra Ecology", "Author Z", "Science", 10, new byte[0], 100, "", LocalDateTime.now().minusDays(10));
        bookB = new Book(2, "Alpha Algorithms", "Author A", "Technology", 20, new byte[0], 200, "", LocalDateTime.now().minusDays(1));
        bookC = new Book(3, "Middlemarch", "George Eliot", "Classics", 30, new byte[0], 300, "", LocalDateTime.now().minusDays(5));

        testBooks = List.of(bookA, bookB, bookC);

        progressMap = new HashMap<>();
        // bookA is COMPLETED, bookB is READING, bookC is UNREAD
        progressMap.put(1, new ReadingProgress(1, 10, 1, 10, ReadingStatus.COMPLETED, LocalDateTime.now()));
        progressMap.put(2, new ReadingProgress(2, 10, 2, 5, ReadingStatus.READING, LocalDateTime.now()));
        // bookC is unread (not in map or unread)
    }

    @Test
    public void testTitleSortStrategy() {
        CatalogSortStrategy strategy = new TitleSortStrategy();
        List<Book> sorted = strategy.sort(testBooks, progressMap);

        assertEquals("Alpha Algorithms", sorted.get(0).getTitle());
        assertEquals("Middlemarch", sorted.get(1).getTitle());
        assertEquals("Zebra Ecology", sorted.get(2).getTitle());
    }

    @Test
    public void testRecentSortStrategy() {
        CatalogSortStrategy strategy = new RecentSortStrategy();
        List<Book> sorted = strategy.sort(testBooks, progressMap);

        // Book B (1 day ago) -> Book C (5 days ago) -> Book A (10 days ago)
        assertEquals("Alpha Algorithms", sorted.get(0).getTitle());
        assertEquals("Middlemarch", sorted.get(1).getTitle());
        assertEquals("Zebra Ecology", sorted.get(2).getTitle());
    }

    @Test
    public void testReadingStatusSortStrategy() {
        CatalogSortStrategy strategy = new ReadingStatusSortStrategy();
        List<Book> sorted = strategy.sort(testBooks, progressMap);

        // Reading (Book B) -> Unread (Book C) -> Completed (Book A)
        assertEquals("Alpha Algorithms", sorted.get(0).getTitle(), "READING status should be ordered first");
        assertEquals("Middlemarch", sorted.get(1).getTitle(), "UNREAD status should be ordered second");
        assertEquals("Zebra Ecology", sorted.get(2).getTitle(), "COMPLETED status should be ordered last");
    }
}
