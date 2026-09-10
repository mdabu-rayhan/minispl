# BookHaven Architectural Design Patterns & Viva Defense Guide

**Project:** BookHaven — Desktop E-Reader & Library Management System  
**Stack:** Java 21 LTS, JavaFX 21, Apache PDFBox 3.0, SQLite JDBC  
**Evaluation Standard:** Gang of Four (GoF) Pattern Taxonomy & Clean Architecture  
**Test Suite Status:** 25 Tests Run, 0 Failures, 100% Green  

---

## 1. Executive Summary & Pattern Matrix

Every design pattern in BookHaven was selected to solve a genuine engineering problem, rather than to satisfy an academic checklist. Each pattern directly prevents a specific risk—such as memory exhaustion, messy constructor calls, or rigid UI-to-database coupling.

### Implemented Patterns Overview

| Pattern | GoF Category | Location in Codebase | Plain-English Purpose | Why It Was Necessary |
| :--- | :--- | :--- | :--- | :--- |
| **Virtual Proxy** | Structural | `com.bookhaven.document` | Delays loading heavy PDF files from SQLite into RAM until the user actually opens the book to read. | Without it, opening a library with 50 books would load hundreds of megabytes of PDF data into memory at once, freezing the application or crashing with `OutOfMemoryError`. |
| **Builder** | Creational | `Book.Builder` in `com.bookhaven.model` | Creates `Book` objects step-by-step with clean, named methods and sensible defaults. | Replaces a clumsy 9-parameter constructor where developers had to guess the order of parameters like file size, byte array, genre, and dates. |
| **Factory Method** | Creational | `UserFactory` in `com.bookhaven.model` | Centralizes creation of `AdminUser` and `ReaderUser` based on role strings or enums. | Prevents duplicating `if (role.equals("ADMIN")) new AdminUser() ...` across login, registration, and database loaders. |
| **Observer** | Behavioral | `com.bookhaven.event` | Notifies listening services whenever a user turns a page or finishes reading. | Decouples the user interface from the database. The reader screen simply announces "page turned" without needing to know how or where progress is saved. |
| **Strategy** | Behavioral | `com.bookhaven.search` | Allows interchangeable sorting algorithms for the book catalog (Title A-Z, Newest, Reading Progress). | Isolates sorting algorithms into individual classes. Crucially, allows sorting by external user reading progress without cluttering the `Book` class. |
| **Data Access Object (DAO)** | Structural / Architectural | `com.bookhaven.dao` | Isolates all raw SQL queries and SQLite JDBC interactions behind clean Java interfaces. | Keeps SQL queries out of controllers and services. If the database engine changes from SQLite to PostgreSQL, only the DAO classes change. |
| **Singleton** | Creational | `com.bookhaven.util` | Ensures only one `DatabaseManager` connection coordinator and one active `SessionContext`. | SQLite locks files if multiple connection pools write at the same time. The singleton ensures orderly access and unified user login state across screens. |

---

## 2. Practical Viva Defense: Questions & Common-Sense Answers

Examiners typically ask high-level, practical questions to verify that you understand *why* you chose a pattern and *what would happen without it*. Below are the most common questions framed naturally in plain English.

---

### Question 1: "Why did you implement the Virtual Proxy pattern? Was it really necessary?"

**How to answer:**
> "Yes, it was absolutely necessary for performance and memory safety.
> 
> In BookHaven, books are stored as full PDF documents directly in our SQLite database as BLOBs. If a user has a library of 50 or 100 books, each book might be 5 to 20 megabytes. 
> 
> If we loaded the real PDF files when the catalog loads, the application would immediately pull hundreds of megabytes into RAM and freeze while parsing PDF structures. 
> 
> With the **Virtual Proxy**, when the catalog opens, we only load lightweight metadata—like the book's title, author, and page count. The heavy PDF bytes and the PDFBox document are only loaded into memory at the exact moment the reader actually opens that specific book."

**Follow-up: "What would happen if you removed the Virtual Proxy?"**
> "The app would become noticeably slow and laggy upon startup or whenever viewing the library catalog, and on systems with limited memory, it would eventually crash with an `OutOfMemoryError`."

---

### Question 2: "Why did you implement the Builder pattern for Book? Why not just use normal constructors or setters?"

**How to answer:**
> "A `Book` in our system has 9 different attributes: ID, title, author, genre, total pages, PDF byte array, file size, description, and creation timestamp.
> 
> If we used a standard constructor, developers had to write:
> `new Book(0, title, author, genre, pages, bytes, size, desc, now);`
> Having 9 positional parameters in a row is error-prone. It's very easy to accidentally swap two strings or pass nulls.
> 
> On the other hand, if we only used setters with an empty constructor, an object could be left half-initialized or in an invalid state.
> 
> The **Builder pattern** gives us the best of both worlds: clear, readable code using method chaining (`Book.builder().title(...).author(...).build()`), and safe default values for optional fields like descriptions or timestamps."

---

### Question 3: "Why did you use a Factory Method for User? Why not just do `new AdminUser()` directly?"

**How to answer:**
> "Because when a user logs in, registers, or is loaded from the database, our system only receives the role as text—like the string `'ADMIN'` or `'READER'`.
> 
> Without a factory, every place in our code that loads or creates a user would need an `if-else` or `switch` statement checking the role string and manually calling `new AdminUser()` or `new ReaderUser()`.
> 
> The **UserFactory** centralizes that object-creation decision in one single place. If we ever want to add a third role in the future—such as a 'Librarian' or 'Guest'—we only have to change `UserFactory`. We don't have to search through and edit five different database or authentication files."

---

### Question 4: "Why did you use the Observer pattern for reading events? Wouldn't it be simpler to just save to the database directly when a page turns?"

**How to answer:**
> "Directly calling the database inside the page-turn button handler seems simpler at first, but it quickly creates messy, tightly coupled code.
> 
> When a user turns a page, several different things need to happen:
> 1. We must update the `reading_progress` table in the database.
> 2. If they reached the last page, we must mark the book as completed.
> 3. We record the time spent in a `reading_sessions` log for reading statistics.
> 
> If the `ReaderViewController` did all of that directly, our UI screen would be cluttered with database calls and business logic.
> 
> With the **Observer pattern**, the reader UI simply fires a `ReadingEvent` saying 'Page 5 was read'. It doesn't care who is listening. The progress listener and the session tracking listener receive the event and handle their database updates independently. This keeps the UI code clean and focused solely on displaying pages."

---

### Question 5: "Why did you use the Strategy pattern for sorting? Couldn't you just use a basic `if-else` block?"

**How to answer:**
> "An `if-else` block works for simple cases, but in our catalog, sorting isn't just about alphabetical title or publication date.
> 
> We also sort by the user's **personal reading status**—meaning books you are currently reading appear first, followed by unread books, and completed books at the bottom. A `Book` object does not and should not store an individual user's reading status, because multiple users can read the same book.
> 
> The **Strategy pattern** lets each sorting algorithm live in its own self-contained class. For reading status sorting, the strategy accepts both the list of books and the user's progress map. 
> 
> In the controller, we can simply bind the strategies directly to a dropdown menu. If we want to add another sort option tomorrow (like sorting by file size or genre), we create one new strategy class without touching existing controller code."

---

### Question 6: "Why did you use the DAO (Data Access Object) pattern instead of writing SQL queries directly in your controllers?"

**How to answer:**
> "Writing SQL queries directly inside JavaFX controllers mixes user interface code with database logic. If a table column changes or an SQL query has a bug, you'd have to search through UI files to fix it.
> 
> The **DAO pattern** creates a clear separation of concerns. All SQLite queries, `PreparedStatement` parameters, and `ResultSet` mappings are contained entirely within the DAO classes (`SQLiteBookDAO`, `SQLiteUserDAO`, etc.).
> 
> The rest of the application never touches SQL; it only calls clean methods like `bookDAO.findById(id)` or `userDAO.save(user)`. If we ever migrate from SQLite to PostgreSQL or MySQL, we only have to write new DAO implementations; our UI and service layers won't need to change at all."

---

### Question 7: "Why is DatabaseManager a Singleton?"

**How to answer:**
> "SQLite is an embedded, file-based database. Unlike large client-server databases like PostgreSQL, SQLite locks the database file during write operations. If multiple parts of the application created their own database connection managers, they would frequently clash and throw `SQLiteBusyException: database is locked`.
> 
> Making `DatabaseManager` a **Singleton** ensures that the entire application shares a single, coordinated access point to the database file and guarantees that schema creation and migrations run exactly once when the application starts up."

---

### Question 8: "Why didn't you implement more design patterns, like Abstract Factory, Prototype, or Memento?"

**How to answer:**
> "We intentionally avoided adding patterns where they were not genuinely needed. Adding design patterns without a clear problem is a form of over-engineering that makes software harder to understand and maintain.
> 
> Specifically:
> - **Abstract Factory:** We don't have multiple operating system widget toolkits or cross-platform UI families. JavaFX handles styling through clean CSS, so Abstract Factory would just be useless boilerplate.
> - **Prototype:** In our application, books and user records are retrieved directly from SQLite via SQL queries, not cloned from existing in-memory objects. Cloned objects would have no database identity.
> - **Memento:** Our reader only tracks the current page number—a single integer. Storing a stack of full snapshot objects using Memento would be complete overkill when we can simply store and restore the integer page number.
> 
> Every pattern we chose solves a tangible problem in our desktop reader application."

---

### Question 9: "How do you prove that your design patterns actually work and didn't break anything?"

**How to answer:**
> "We wrote a comprehensive automated unit test suite using **JUnit 5**.
> 
> We have dedicated tests covering:
> - `BookBuilderTest`: Verifying that `Book.builder()` constructs objects with full parameters and applies sensible defaults.
> - `UserFactoryTest`: Verifying that `UserFactory` produces correct `AdminUser` and `ReaderUser` instances with proper roles and permissions.
> - `ProxyPdfDocumentTest`: Verifying that `ProxyPdfDocument` defers loading until `renderPageImage()` or `ensureLoaded()` is triggered.
> - `ReadingEventPublisherTest`: Verifying that multiple observers receive events synchronously and independently.
> - `SearchStrategyTest`: Verifying sorting behavior across title, date, and reading status strategies.
> - `DAOTest`: Verifying CRUD operations and database mappings.
> 
> All **25 automated tests pass with 100% success** on every build."

---

## 3. Detailed Code Architecture & Mapping

### 3.1 Creational Patterns

#### 1. Builder Pattern (`Book.java`)
- **Location:** `src/main/java/com/bookhaven/model/Book.java`
- **Callers:** `BookIngestionService.java`, `SQLiteBookDAO.java`, `DatabaseSeeder.java`
- **Usage Example:**
```java
Book book = Book.builder()
        .title("The Art of War")
        .author("Sun Tzu")
        .genre("Philosophy")
        .totalPages(5)
        .fileData(pdfBytes)
        .fileSize(pdfBytes.length)
        .description("Ancient military treatise.")
        .build();
```

#### 2. Factory Method Pattern (`UserFactory.java`)
- **Location:** `src/main/java/com/bookhaven/model/UserFactory.java`
- **Callers:** `SQLiteUserDAO.java`, `AuthService.java`, `DatabaseSeeder.java`
- **Usage Example:**
```java
// Centralized instantiation from database role string
User user = UserFactory.createUser(id, username, passwordHash, roleString, createdAt);

// Convenience methods for domain operations
User newReader = UserFactory.createReader(username, hashedPassword);
User newAdmin  = UserFactory.createAdmin(username, hashedPassword);
```

#### 3. Singleton Pattern (`DatabaseManager.java`, `SessionContext.java`)
- **Location:** `src/main/java/com/bookhaven/util/DatabaseManager.java`, `SessionContext.java`
- **Mechanism:** Thread-safe Double-Checked Locking.

---

### 3.2 Structural Patterns

#### 4. Virtual Proxy Pattern (`com.bookhaven.document`)
- **Subject Interface:** `BookDocument.java`
- **Real Subject:** `RealPdfDocument.java` (loads PDFBox `PDDocument`, renders page bitmap via PDFRenderer)
- **Proxy:** `ProxyPdfDocument.java` (stores lightweight metadata; instantiates `RealPdfDocument` upon first access)

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
(PDFBox parser &      (Holds metadata; fetches bytes
 page renderer)        from BookDAO only when opened)
```

#### 5. Data Access Object (DAO) Pattern (`com.bookhaven.dao`)
- **Interfaces:** `BookDAO`, `UserDAO`, `ReadingProgressDAO`, `ReadingSessionDAO`
- **Implementations:** `SQLiteBookDAO`, `SQLiteUserDAO`, `SQLiteReadingProgressDAO`, `SQLiteReadingSessionDAO`

---

### 3.3 Behavioral Patterns

#### 6. Observer Pattern (`com.bookhaven.event`)
- **Subject / Broker:** `ReadingEventPublisher.java`
- **Event Object:** `ReadingEvent.java`
- **Subscriber Interface:** `ReadingEventListener.java`
- **Subscribers:**
  - `ProgressUpdateListener`: Updates `reading_progress` table.
  - `SessionTrackingListener`: Calculates session duration and writes to `reading_sessions`.

#### 7. Strategy Pattern (`com.bookhaven.search`)
- **Strategy Interface:** `CatalogSortStrategy.java`
- **Concrete Strategies:**
  - `TitleSortStrategy`: Alphabetical sorting by title (A-Z).
  - `RecentSortStrategy`: Chronological sorting by addition date (newest first).
  - `ReadingStatusSortStrategy`: Prioritizes books in active reading queue over unread/completed.

---

## 4. Summary Checklist for Viva Defense

When entering your defense, keep these 3 core points in mind:

1. **Balance & Legitimacy:** You have 7 clean, standard design patterns covering all three GoF families (**Creational**, **Structural**, and **Behavioral**).
2. **Real Practical Value:** Each pattern directly prevents an engineering problem (Virtual Proxy prevents out-of-memory errors; Builder prevents parameter mix-ups; Observer keeps UI code clean; DAO isolates SQL; Factory Method centralizes role logic).
3. **No Over-Engineering:** You deliberately rejected patterns like Abstract Factory or Memento because they add needless complexity to a desktop reader app.
