# BookHaven Architecture & UML / ER Specifications

This document contains visual diagrams and architecture models for the **BookHaven** application.

---

## 1. Database Entity-Relationship (ER) Diagram

```mermaid
erDiagram
    users ||--o{ reading_progress : tracks
    users ||--o{ reading_sessions : logs
    books ||--o{ reading_progress : referenced_by
    books ||--o{ reading_sessions : measured_in

    users {
        int id PK
        string username UK
        string password_hash
        string role
        timestamp created_at
    }

    books {
        int id PK
        string title
        string author
        string genre
        int total_pages
        blob file_data
        long file_size
        string description
        timestamp created_at
    }

    reading_progress {
        int id PK
        int user_id FK
        int book_id FK
        int current_page
        string status
        timestamp last_read_at
    }

    reading_sessions {
        int id PK
        int user_id FK
        int book_id FK
        timestamp start_time
        timestamp end_time
        int pages_read
        int duration_minutes
    }
```

---

## 2. Core Class & Design Pattern Diagram

```mermaid
classDiagram
    class BookDocument {
        <<interface>>
        +getBookId() int
        +getTitle() String
        +getPageCount() int
        +getFileSize() long
        +isLoaded() boolean
        +renderPageImage(page: int, scale: double) Image
        +extractPageText(page: int) String
        +close() void
    }

    class ProxyPdfDocument {
        -int bookId
        -String title
        -int totalPages
        -long fileSize
        -BookDAO bookDAO
        -RealPdfDocument realPdfDocument
        +ensureLoaded() void
        +renderPageImage(page: int, scale: double) Image
        +close() void
    }

    class RealPdfDocument {
        -PDDocument pdDocument
        -PDFRenderer pdfRenderer
        -Map pageImageCache
        +renderPageImage(page: int, scale: double) Image
        +extractPageText(page: int) String
        +close() void
    }

    BookDocument <|.. ProxyPdfDocument : implements
    BookDocument <|.. RealPdfDocument : implements
    ProxyPdfDocument o--> RealPdfDocument : instantiates on demand

    class ReadingEventPublisher {
        -List~ReadingEventListener~ listeners
        +subscribe(listener: ReadingEventListener)
        +unsubscribe(listener: ReadingEventListener)
        +publish(event: ReadingEvent)
    }

    class ReadingEventListener {
        <<interface>>
        +onReadingEvent(event: ReadingEvent) void
    }

    class ReadingTrackerService {
        +onReadingEvent(event: ReadingEvent) void
        +updateProgress(userId, bookId, page, total)
        +logSession(userId, bookId, pages, duration)
    }

    ReadingEventListener <|.. ReadingTrackerService : implements
    ReadingEventPublisher o--> ReadingEventListener : notifies

    class CatalogSortStrategy {
        <<interface>>
        +getStrategyName() String
        +sort(books: List, progress: Map) List
    }

    class TitleSortStrategy {
        +sort(books, progress) List
    }

    class RecentSortStrategy {
        +sort(books, progress) List
    }

    class ReadingStatusSortStrategy {
        +sort(books, progress) List
    }

    CatalogSortStrategy <|.. TitleSortStrategy : implements
    CatalogSortStrategy <|.. RecentSortStrategy : implements
    CatalogSortStrategy <|.. ReadingStatusSortStrategy : implements
```
