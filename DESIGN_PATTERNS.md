# Design Patterns & Architectural Justification

This document details the architectural decisions and software design patterns applied throughout the **BookHaven** codebase. Rather than isolating patterns into artificial "pattern" folders, each design pattern is integrated natively into its natural domain package and architectural layer.

---

## 1. Design Pattern Mapping Matrix

| Pattern | Category | Source Location | Problem Solved & Justification | Future Benefit / Extensibility |
| :--- | :--- | :--- | :--- | :--- |
| **Virtual Proxy** | Structural | `com.bookhaven.document` | Prevents loading large PDF BLOB byte arrays and heavy PDFBox document trees into RAM when querying book lists, catalog cards, or search results. | Enables swapping the underlying PDF rendering engine (e.g., PDFBox vs. Apache PDF.js vs. JPedal) or adding on-demand cloud PDF streaming without touching UI controllers. |
| **Builder** | Creational | `Book.Builder` in `com.bookhaven.model` | Eliminates the error-prone 9-parameter constructor for books; provides clean, fluent object assembly with default values. | Allows adding future book metadata fields (e.g., ISBN, publisher, language) without breaking existing construction code or needing new constructor overloads. |
| **Factory Method** | Creational | `UserFactory` in `com.bookhaven.model` | Encapsulates polymorphic user instantiation (`AdminUser`, `ReaderUser`) and consistent entity creation from database role strings. | Allows introducing new user tiers (e.g., `GuestUser`, `LibrarianUser`) with custom permissions without modifying database DAOs or auth services. |
| **Observer** | Behavioral | `com.bookhaven.event` | Decouples the reader view (`ReaderViewController`) from progress persistence, session tracking, and dashboard metric calculations. | Easily attach new progress observers (e.g., Reading Badges, Gamification, Daily Reading Goal alerts, Remote Sync) without modifying reader code. |
| **Strategy** | Behavioral | `com.bookhaven.search` | Eliminates complex, branching `if-else` / `switch` statements for catalog sorting; cleanly handles contextual sorting by user reading status. | Add new sorting or recommendation strategies (e.g., *Genre-Weighted Affinity*, *AI Recommendations*, *Popularity Ranking*) by simply creating a new class implementing `CatalogSortStrategy`. |
| **Data Access Object (DAO)** | Architectural / Structural | `com.bookhaven.dao` | Isolates all raw SQLite DDL/DML SQL queries, connection management, and ResultSet mappings from domain business services. | Enables switching the underlying database engine from SQLite to PostgreSQL, MySQL, or an external REST API by writing new DAO implementations without modifying business rules. |
| **Singleton** | Creational | `com.bookhaven.util` | Guarantees a single, centralized database connection pool (`DatabaseManager`) and a single active user session (`SessionContext`) across JavaFX scenes. | Prevents concurrent SQLite file-lock conflicts (`database is locked`) and ensures unified session authentication state across multiple windows and scene switches. |

---

## 2. In-Depth Architectural Analysis

### 2.1 Virtual Proxy Pattern (`com.bookhaven.document`)
- **Key Classes:** `BookDocument` (Subject Interface), `RealPdfDocument` (Real Subject), `ProxyPdfDocument` (Proxy).
- **Motivation:** Storing PDF files as database BLOBs allows self-contained portability, but loading dozens of raw PDF byte streams into heap memory during catalog discovery causes excessive memory pressure and potential `OutOfMemoryError`.
- **Implementation:** `ProxyPdfDocument` holds lightweight book metadata (ID, title, total pages, file size). It defers loading the full byte array from `BookDAO` and instantiating Apache PDFBox's `PDDocument` until the user explicitly calls `renderPageImage(pageIndex, scale)` or opens the reading view.

```
       «interface»
      BookDocument
     +getPageCount(): int
     +renderPageImage(page: int, scale: double): Image
     +close(): void
          ▲
          │
    ┌─────┴────────────────┐
    │                      │
RealPdfDocument       ProxyPdfDocument
(PDFBox parser &      (Lazy-loading proxy;
 page renderer)        instantiates RealPdfDocument on demand)
```

---

### 2.2 Builder Pattern (`com.bookhaven.model.Book`)
- **Key Classes:** `Book` (Target Class), `Book.Builder` (Static Nested Builder).
- **Motivation:** A `Book` entity has 9 distinct fields: `id`, `title`, `author`, `genre`, `totalPages`, `fileData`, `fileSize`, `description`, and `createdAt`. A telescoping constructor with 9 positional arguments is hard to read and highly error-prone (e.g., mixing up string fields or passing dummy nulls).
- **Implementation:** Provides a fluent API with method chaining and defaults:
```java
Book book = Book.builder()
        .title("Pride and Prejudice")
        .author("Jane Austen")
        .genre("Classics")
        .totalPages(4)
        .fileData(pdfBytes)
        .fileSize(pdfBytes.length)
        .description("Romantic novel of manners.")
        .build();
```

---

### 2.3 Factory Method Pattern (`com.bookhaven.model.UserFactory`)
- **Key Classes:** `User` (Abstract Product), `AdminUser`, `ReaderUser` (Concrete Products), `UserFactory` (Creator).
- **Motivation:** Roles are loaded as strings from the database or authentication forms. Scattering `if (role.equals("ADMIN"))` checks across DAOs and services violates the Open/Closed Principle.
- **Implementation:** `UserFactory` encapsulates role evaluation and instantiation:
```java
User user = UserFactory.createUser(id, username, passwordHash, roleString, createdAt);
User newReader = UserFactory.createReader(username, hashedPassword);
User newAdmin  = UserFactory.createAdmin(username, hashedPassword);
```

---

### 2.4 Observer Pattern (`com.bookhaven.event`)
- **Key Classes:** `ReadingEvent` (Event Object), `ReadingEventType` (Event Types), `ReadingEventListener` (Subscriber Interface), `ReadingEventPublisher` (Subject / Event Broker).
- **Motivation:** When a user turns a page, reaches the last page, or closes the book, multiple side-effects must occur:
  1. Update `reading_progress` in the database.
  2. If the user reached the final page, transition status from `READING` to `COMPLETED`.
  3. Calculate elapsed session duration and record a `reading_session` log entry.
  4. Invalidate and refresh dashboard reading metrics.
- **Implementation:** The `ReaderViewController` publishes a `ReadingEvent` to `ReadingEventPublisher`. Registered listeners handle persistence and session logging independently without cluttering the UI.

```
ReaderViewController ──(publishes)──► ReadingEventPublisher
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
             ProgressUpdateListener                     SessionTrackingListener
             (Updates progress DB)                      (Logs reading session)
```

---

### 2.5 Strategy Pattern (`com.bookhaven.search`)
- **Key Classes:** `CatalogSortStrategy` (Strategy Interface), `TitleSortStrategy`, `RecentSortStrategy`, `ReadingStatusSortStrategy` (Concrete Strategies).
- **Motivation:** Different users prioritize different catalog perspectives: alphabetical browsing, newly added books, or active reading backlog. Hardcoding sort logic in the controller violates the Open/Closed Principle.
- **Implementation:** The controller delegates sorting to a `CatalogSortStrategy` reference that can be swapped dynamically at runtime when the user selects a different sort option in the UI dropdown.

```
BookCatalogController ──(delegates sort)──► «interface» CatalogSortStrategy
                                                   ▲
                     ┌─────────────────────────────┼─────────────────────────────┐
                     │                             │                             │
             TitleSortStrategy             RecentSortStrategy            ReadingStatusSortStrategy
             (Alphabetical A-Z)            (Chronological Newest)        (Reading > Unread > Done)
```

---

### 2.6 Data Access Object (DAO) Pattern (`com.bookhaven.dao`)
- **Key Interfaces:** `UserDAO`, `BookDAO`, `ReadingProgressDAO`, `ReadingSessionDAO`.
- **Implementations:** `SQLiteUserDAO`, `SQLiteBookDAO`, `SQLiteReadingProgressDAO`, `SQLiteReadingSessionDAO`.
- **Motivation:** Direct JDBC queries embedded inside controllers or services tightly couple the application to SQLite SQL syntax.
- **Implementation:** The DAO layer encapsulates all database connection retrieval, parameterized SQL execution, transaction management, and `ResultSet` mapping, exposing clean domain entity collections to the service layer.

---

### 2.7 Singleton Pattern (`com.bookhaven.util`)
- **Key Classes:** `DatabaseManager`, `SessionContext`.
- **Motivation:**
  - SQLite databases do not support unbounded concurrent write connections from separate connection pools without encountering lock contention.
  - User session state (user ID, role, permissions) must be consistent and accessible throughout the JavaFX application lifecycle across scene changes.
- **Implementation:** Thread-safe Double-Checked Locking to guarantee a single global instance for database coordination and user session tracking.
