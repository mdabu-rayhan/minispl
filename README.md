# 📚 BookHaven

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?logo=openjdk)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg?logo=java)](https://openjfx.io/)
[![PDFBox](https://img.shields.io/badge/Apache%20PDFBox-3.0.1-red.svg)](https://pdfbox.apache.org/)
[![SQLite](https://img.shields.io/badge/Database-SQLite%203-lightgrey.svg?logo=sqlite)](https://www.sqlite.org/)
[![Tests](https://img.shields.io/badge/Tests-25%20Passing%20(100%25)-brightgreen.svg)]()
[![Build](https://img.shields.io/badge/Build-Maven%203.9+-C71A36.svg?logo=apachemaven)](https://maven.apache.org/)

> A lightweight, distraction-free desktop e-reader and library management system built with pure JavaFX and clean object-oriented architecture.

---

## 📑 Table of Contents

- [About the Project](#-about-the-project)
  - [What is BookHaven? (In Plain English)](#what-is-bookhaven-in-plain-english)
  - [Built With](#built-with)
- [Key Features](#-key-features)
- [User Roles & Default Accounts](#-user-roles--default-accounts)
- [Software Architecture](#-software-architecture)
  - [Layered Architecture](#layered-architecture)
  - [Design Patterns Matrix](#design-patterns-matrix)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Launch](#installation--launch)
  - [Running Automated Tests](#running-automated-tests)
- [Project Documentation](#-project-documentation)
- [License](#-license)

---

## 📖 About the Project

### What is BookHaven? (In Plain English)

**BookHaven** is a personal digital library and PDF reader for your desktop computer. Think of it like a personal, offline version of **Kindle** or **Calibre**, designed to be simple, responsive, and easy to use.

If you have a collection of PDF books, documents, or lecture notes, BookHaven organizes them in one local place:
- **Read effortlessly:** Open any PDF book with smooth page navigation, direct page jump, and zoom controls.
- **Never lose your spot:** It automatically remembers the exact page you were on, so you can close the app and pick up right where you left off.
- **Track your reading journey:** The app tracks which books you are currently reading, which ones you have completed, and how much time you've spent reading.
- **Search & discover:** Filter your books by genre, search by title or author, or sort by your personal reading progress.
- **Add new books:** Administrators can upload any PDF file from their computer; BookHaven automatically reads the page count, extracts metadata, and saves it into the library.
- **100% Offline & Private:** All books, reading records, and user accounts are stored in a single local database file (`bookhaven.db`). No cloud subscriptions, no tracking, and no internet connection required.

### Built With

- **[Java 21 LTS](https://openjdk.org/)** — Core platform and language runtime.
- **[JavaFX 21](https://openjfx.io/)** — Desktop UI presentation layer using standard Modena styling.
- **[Apache PDFBox 3.0.1](https://pdfbox.apache.org/)** — PDF document parsing and page bitmap rendering engine.
- **[SQLite JDBC](https://github.com/xerial/sqlite-jdbc)** — Lightweight embedded file-based persistence.
- **[jBCrypt](https://www.mindrot.org/projects/jBCrypt/)** — Salted password hashing for authentication security.
- **[JUnit 5 (Jupiter)](https://junit.org/junit5/)** — Automated unit and integration testing framework.
- **[Apache Maven](https://maven.apache.org/)** — Dependency management and build automation.

---

## ✨ Key Features

- **Distraction-Free Reading:** Minimal reader view with page turning (`Prev` / `Next`), quick jump-to-page input, and zoom presets (`50%` to `200%`).
- **Zero Memory Lag (Virtual Proxy):** Heavy PDF files are only loaded from the database into RAM when you open a book to read, keeping library browsing fast and responsive.
- **Automatic Progress Tracking (Observer):** Progress and session reading duration are updated automatically in the background as you read.
- **Dynamic Catalog Sorting (Strategy):** Sort books alphabetically (A-Z), chronologically (newest additions), or by personal reading status (*Currently Reading* first, then *Unread*, then *Completed*).
- **Admin Ingestion Pipeline:** Upload PDF files with automatic validation, page count extraction, and secure binary storage.
- **Role-Based Access Control (RBAC):** Distinct dashboards and capabilities for Readers and Administrators with secure password hashing.

---

## 👥 User Roles & Default Accounts

BookHaven automatically creates default accounts and sample books in `bookhaven.db` on first run:

| Role | Username | Password | Default Landing Screen |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Admin Dashboard (`admin_dashboard.fxml`) |
| **Reader** | `reader1` | `reader123` | Book Catalog (`book_catalog.fxml`) |
| **Reader** | `jane_doe` | `reader123` | Book Catalog (`book_catalog.fxml`) |

> 💡 *New reader accounts can also be created anytime from the **Register** button on the login screen.*

---

## 🏛️ Software Architecture

### Layered Architecture

BookHaven follows a strict four-layer architecture ensuring high cohesion and loose coupling:

```text
┌─────────────────────────────────────────────────────────────┐
│                 Presentation Layer (JavaFX)                 │
│  FXML Views + Controllers (Login, Catalog, Reader, Admin)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Business Service Layer                   │
│   AuthService, BookIngestionService, ReadingTrackerService  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                   Data Access Layer (DAO)                   │
│   BookDAO, UserDAO, ReadingProgressDAO, ReadingSessionDAO    │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                   Persistence Layer (SQLite)                │
│             Embedded Database File (bookhaven.db)           │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns Matrix

The system implements **7 Gang of Four (GoF) design patterns** addressing specific engineering needs:

| Pattern | Category | Package | Concrete Classes | Engineering Problem Solved |
| :--- | :--- | :--- | :--- | :--- |
| **Virtual Proxy** | Structural | `com.bookhaven.document` | `BookDocument`, `ProxyPdfDocument`, `RealPdfDocument` | Prevents Out-Of-Memory errors by deferring PDF byte loading until a book is opened. |
| **Builder** | Creational | `com.bookhaven.model` | `Book`, `Book.Builder` | Eliminates telescoping 9-parameter constructors; provides safe fluent construction with defaults. |
| **Factory Method** | Creational | `com.bookhaven.model` | `UserFactory`, `User`, `AdminUser`, `ReaderUser` | Centralizes creation of user roles from database strings without scattered `if-else` blocks. |
| **Observer** | Behavioral | `com.bookhaven.event` | `ReadingEvent`, `ReadingEventListener`, `ReadingEventPublisher` | Decouples the reader screen from database persistence and reading duration logging. |
| **Strategy** | Behavioral | `com.bookhaven.search` | `CatalogSortStrategy`, `TitleSortStrategy`, `ReadingStatusSortStrategy` | Encapsulates interchangeable sorting algorithms, including contextual sorting by user reading status. |
| **Data Access Object (DAO)** | Structural / Arch | `com.bookhaven.dao` | `BookDAO`, `UserDAO`, `ReadingProgressDAO`, `ReadingSessionDAO` | Isolates all raw SQL queries and JDBC operations behind clean Java interfaces. |
| **Singleton** | Creational | `com.bookhaven.util` | `DatabaseManager`, `SessionContext` | Prevents SQLite file lock contention by coordinating access through a single connection coordinator. |

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK):** Version 21 LTS or newer installed.
- **Apache Maven:** Version 3.9+ installed and configured on your system path.

Verify your environment:
```bash
java -version
mvn -version
```

### Installation & Launch

1. **Navigate to the project directory:**
   ```bash
   cd "BookHaven Trial"
   ```

2. **Compile the source code:**
   ```bash
   mvn clean compile
   ```

3. **Launch the desktop application:**
   ```bash
   mvn javafx:run
   ```
   *(Alternative launch command:)*
   ```bash
   mvn exec:java
   ```

### Running Automated Tests

Run the complete JUnit 5 test suite covering all services, DAOs, and design patterns:
```bash
mvn clean test
```

**Expected output:**
```text
[INFO] Results:
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📚 Project Documentation

Detailed architecture specifications and oral defense guides are included in the repository:

- **[DESIGN_PATTERNS.md](DESIGN_PATTERNS.md):** Complete architectural design pattern reference with UML class diagrams and code snippets.
- **[docs/DESIGN_PATTERNS_DEFENSE.md](docs/DESIGN_PATTERNS_DEFENSE.md):** Practical viva voce examination guide with conversational questions, answers, and justifications.
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md):** Database Entity-Relationship (ER) diagram and Mermaid class specifications.
- **[CHANGELOG.md](CHANGELOG.md):** Detailed record of architectural refactorings and features.

---

## 📄 License

This project is developed for educational and academic demonstration purposes under the MIT License.
