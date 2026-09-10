# Changelog & Development Record

All notable changes, architectural milestones, development flow records, and updates for the **BookHaven** application are documented in this file.

---

## [1.0.0] - Complete Architectural Implementation - 2026-08-27

### 📌 Milestone 1: Requirements Analysis & Design Alignment
- **Scope**:
  - Analyzed user specifications for domain-driven, layered layout and dedicated pattern documentation (`DESIGN_PATTERNS.md`).
  - Completed interactive alignment (`/grill-me`):
    - Platform target: Java 21 LTS, JavaFX 21, Apache PDFBox 3.0.4.
    - Security: BCrypt hashing via `jBCrypt`.
    - Document Engine: PDFBox native `PDFRenderer` with page caching and zooming.
    - Database Strategy: SQLite with automatic schema migration and sample seed initialization.
    - Export Architecture: Template Method pattern with CSV and programmatic PDFBox PDF exporter.
    - Design Aesthetic: Modern Warm Editorial / BookHaven library theme.

### 📌 Milestone 2: Build Descriptor & SQLite Schema
- Configured Maven `pom.xml` with OpenJFX 21, Apache PDFBox 3.0.4, SQLite JDBC 3.45.1.0, jBCrypt 0.4, and JUnit 5.
- Authored normalized SQLite DDL script `schema.sql` (`users`, `books`, `reading_progress`, `reading_sessions`, `bookmarks_notes` with Foreign Keys).
- Authored initial seed data script `seed_data.sql` and programmatic `DatabaseSeeder`.
- Implemented `DatabaseManager` (Singleton) with connection lifecycle management and automatic schema migration.
- Implemented `SessionContext` (Singleton) for active user state tracking.
- Implemented `PasswordHasher` with BCrypt encryption.

### 📌 Milestone 3: Domain Models & Data Access Object (DAO) Pattern
- Implemented domain entity hierarchy in `com.bookhaven.model`:
  - `User`, `AdminUser`, `ReaderUser`, `UserRole` (Polymorphic hierarchy & Factory method).
  - `Book` (Metadata and attached Virtual Proxy document reference).
  - `ReadingProgress` and `ReadingStatus` (`UNREAD`, `READING`, `COMPLETED`).
  - `ReadingSession` (Duration tracking and pages read).
  - `BookmarkNote` (Bookmarks and text annotations).
- Implemented DAO interface contracts and SQLite JDBC implementations in `com.bookhaven.dao`:
  - `UserDAO` & `SQLiteUserDAO`.
  - `BookDAO` & `SQLiteBookDAO` (Lazy BLOB retrieval).
  - `ReadingProgressDAO` & `SQLiteReadingProgressDAO`.
  - `ReadingSessionDAO` & `SQLiteReadingSessionDAO`.
  - `BookmarkNoteDAO` & `SQLiteBookmarkNoteDAO`.

### 📌 Milestone 4: Software Design Pattern Implementations
- **Virtual Proxy Pattern (`com.bookhaven.document`):**
  - `BookDocument` (Subject Interface).
  - `RealPdfDocument` (Real Subject: Apache PDFBox `PDDocument`, `PDFRenderer`, and rendered page image caching).
  - `ProxyPdfDocument` (Virtual Proxy: Defers loading heavy PDF BLOB bytes and PDFBox parser until page rendering is requested).
- **Observer Pattern (`com.bookhaven.event`):**
  - `ReadingEvent` (Immutable event payload: `PAGE_CHANGED`, `BOOK_COMPLETED`, `SESSION_ENDED`, `BOOKMARK_ADDED`).
  - `ReadingEventListener` (Subscriber interface).
  - `ReadingEventPublisher` (Subject: Dispatches events to registered listeners).
- **Strategy Pattern (`com.bookhaven.search`):**
  - `CatalogSortStrategy` (Strategy interface).
  - `TitleSortStrategy` (Alphabetical A-Z).
  - `RecentSortStrategy` (Chronological newest).
  - `ReadingStatusSortStrategy` (Reading -> Unread -> Completed).
- **Template Method Pattern (`com.bookhaven.export`):**
  - `ReportExporter` (Abstract template enforcing invariant export workflow).
  - `CsvReportExporter` (RFC-compliant CSV output).
  - `PdfReportExporter` (Professional formatted PDF reports using Apache PDFBox).
- **Singleton Pattern (`com.bookhaven.util`):**
  - `DatabaseManager` and `SessionContext`.

### 📌 Milestone 5: Multi-Step Business Services Layer
- `AuthService`: Authentication, validation, salted BCrypt hashing, and role-based session initialization.
- `BookIngestionService`: Admin pipeline (File validation $\rightarrow$ PDFBox page/metadata extraction $\rightarrow$ SQLite BLOB insert).
- `ReadingTrackerService`: Reading progress calculation, session logging, bookmark/note persistence, personal and system analytics computation.

### 📌 Milestone 6: JavaFX UI, FXML Views & Styling
- Created BookHaven Modern Warm Editorial stylesheet `styles.css`.
- Created `login.fxml` & `LoginController` (Authentication and Reader registration modal with role routing).
- Created `book_catalog.fxml` & `BookCatalogController` (Live search, genre filter, sort strategy selector, dynamic progress cards, and reader analytics).
- Created `reader_view.fxml` & `ReaderViewController` (Lazy-loaded PDF canvas, page navigation, zoom controls, in-page notes drawer).
- Created `admin_dashboard.fxml` & `AdminDashboardController` (PDF Ingestion wizard, catalog CRUD table, user management, system metrics).
- Created `MainApp.java` and `AppLauncher.java` entrypoints.

### 📌 Milestone 7: Automated Unit & Integration Testing
- Added comprehensive JUnit 5 test suites (23 total tests):
  - `ProxyPdfDocumentTest`: Verifies lazy loading and resource release.
  - `ReadingEventPublisherTest`: Verifies event publishing and listener subscription.
  - `BookIngestionServiceTest`: Verifies PDF ingestion pipeline and page extraction.
  - `SearchStrategyTest`: Verifies sorting strategies.
  - `ReportExporterTest`: Verifies CSV and PDF report generation.
  - `DAOTest`: Verifies SQLite DAOs and persistence.
  - `PasswordHasherTest`: Verifies BCrypt hashing and validation.
  - `AuthServiceTest`: Verifies user registration and login workflows.
  - `ReadingTrackerServiceTest`: Verifies progress tracking and session metrics.
- **Verification Status**: 23/23 tests passing with 0 failures and 0 errors (`mvn clean test` BUILD SUCCESS).

### 📌 Milestone 8: UI/UX & Design Overhaul (High-Fidelity Editorial Theme)
- Redesigned visual theme in `styles.css` with dark slate header bars (`linear-gradient(to right, #0f172a, #1e293b)`), refined pearl backgrounds (`#f8fafc`), sapphire/indigo accents (`#4f46e5`), and subtle multi-pass box shadows.
- Fixed layout clipping and text truncation across all screens.

### 📌 Milestone 9: Stitch MCP Integration & Warm Editorial / Digital Vellum Overhaul
- Created Stitch Project **"BookHaven - Literary Desktop Reader"** (`projects/16617631417198514927`) and generated Desktop Screen (`fae3743685534fe7b5c777c3373448ea`).
- Extracted Stitch design tokens:
  - Paper base canvas (`#FAF7F2`), deep charcoal ink typography (`#1C1917`), forest green primary accent (`#2D5A3F`), warm amber highlights (`#C26B38`), and hairline structural borders (`#E6E1D6`).
- Implemented Stitch Desktop Architecture in JavaFX:
  - **Left Navigation Sidebar (`sidebar-pane`):** Added persistent navigation menu (`📚 All Books`, `📖 Currently Reading`, `✅ Completed`), live reading metrics card (`⏱️ Total Time`, `📅 This Month`, `🎯 Completed/Active`), and user profile card with direct export & logout actions.
  - **Main Catalog Grid:** Added top search & genre/sort filter card, dynamic section header, and updated book cards with warm forest green headers, wrapped typography, and distinct action buttons.
  - **BookCatalogController:** Added interactive sidebar navigation filtering (`CatalogFilterMode`: `ALL`, `CURRENTLY_READING`, `COMPLETED`), maintaining full integration with SQLite DAOs and Strategy sorting.
  - **Harmonized All Screens:** Aligned `styles.css`, `reader_view.fxml`, `admin_dashboard.fxml`, and `login.fxml` with the Warm Editorial color tokens.
- **Verification Status**: 23/23 tests passing with 0 failures (`mvn clean test` BUILD SUCCESS).
