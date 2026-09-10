package com.bookhaven.util;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.dao.ReadingProgressDAO;
import com.bookhaven.dao.ReadingSessionDAO;
import com.bookhaven.dao.UserDAO;
import com.bookhaven.dao.impl.SQLiteBookDAO;
import com.bookhaven.dao.impl.SQLiteReadingProgressDAO;
import com.bookhaven.dao.impl.SQLiteReadingSessionDAO;
import com.bookhaven.dao.impl.SQLiteUserDAO;
import com.bookhaven.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to seed initial user accounts and sample PDF books into the database on first run.
 */
public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    private final UserDAO userDAO;
    private final BookDAO bookDAO;
    private final ReadingProgressDAO progressDAO;
    private final ReadingSessionDAO sessionDAO;

    public DatabaseSeeder() {
        this.userDAO = new SQLiteUserDAO();
        this.bookDAO = new SQLiteBookDAO();
        this.progressDAO = new SQLiteReadingProgressDAO();
        this.sessionDAO = new SQLiteReadingSessionDAO();
    }

    public DatabaseSeeder(UserDAO userDAO, BookDAO bookDAO, ReadingProgressDAO progressDAO, ReadingSessionDAO sessionDAO) {
        this.userDAO = userDAO;
        this.bookDAO = bookDAO;
        this.progressDAO = progressDAO;
        this.sessionDAO = sessionDAO;
    }

    /**
     * Seeds initial data if the database is empty or missing required accounts.
     */
    public void seed() {
        try {
            seedUsers();
            seedBooksAndProgress();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed during database seeding", e);
        }
    }

    private void seedUsers() {
        // Admin user: admin / admin123
        if (userDAO.findByUsername("admin").isEmpty()) {
            User admin = UserFactory.createAdmin("admin", PasswordHasher.hashPassword("admin123"));
            userDAO.save(admin);
            LOGGER.info("Seeded default admin account: admin / admin123");
        }

        // Reader user: reader1 / reader123
        if (userDAO.findByUsername("reader1").isEmpty()) {
            User reader1 = UserFactory.createReader("reader1", PasswordHasher.hashPassword("reader123"));
            userDAO.save(reader1);
            LOGGER.info("Seeded default reader account: reader1 / reader123");
        }

        // Reader user: jane_doe / reader123
        if (userDAO.findByUsername("jane_doe").isEmpty()) {
            User jane = UserFactory.createReader("jane_doe", PasswordHasher.hashPassword("reader123"));
            userDAO.save(jane);
            LOGGER.info("Seeded default reader account: jane_doe / reader123");
        }
    }

    private void seedBooksAndProgress() {
        if (bookDAO.count() > 0) {
            return;
        }

        LOGGER.info("Seeding initial public-domain books and generated PDF files...");

        // Book 1: The Art of War (5 pages)
        byte[] pdf1 = createSamplePdf(
                "The Art of War",
                "Sun Tzu",
                "Philosophy & Strategy",
                5,
                new String[]{
                        "Chapter I: Laying Plans. Sun Tzu said: The art of war is of vital importance to the State.",
                        "Chapter II: Waging War. In the operations of war, where there are in the field a thousand swift chariots...",
                        "Chapter III: Attack by Stratagem. In the practical art of war, the best thing of all is to take the enemy's country whole and intact.",
                        "Chapter IV: Tactical Dispositions. The good fighters of old first put themselves beyond the possibility of defeat.",
                        "Chapter V: Energy. The control of a large force is the same principle as the control of a few men: it is merely a question of dividing up their numbers."
                }
        );
        Book book1 = Book.builder()
                .title("The Art of War")
                .author("Sun Tzu")
                .genre("Philosophy")
                .totalPages(5)
                .fileData(pdf1)
                .fileSize(pdf1.length)
                .description("An ancient military treatise dating from the Late Spring and Autumn Period.")
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
        Book savedBook1 = bookDAO.save(book1);

        // Book 2: Pride and Prejudice (4 pages)
        byte[] pdf2 = createSamplePdf(
                "Pride and Prejudice",
                "Jane Austen",
                "Classic Literature",
                4,
                new String[]{
                        "Chapter 1. It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
                        "Chapter 2. Mr. Bennet was among the earliest of those who waited on Mr. Bingley. He had always intended to visit him...",
                        "Chapter 3. Not all that Mrs. Bennet, however, with the assistance of her five daughters, could ask on the subject, was sufficient to draw from her husband...",
                        "Chapter 4. When Jane and Elizabeth were alone, the former, who had been cautious in her praise of Mr. Bingley before, expressed to her sister how very much she admired him."
                }
        );
        Book book2 = Book.builder()
                .title("Pride and Prejudice")
                .author("Jane Austen")
                .genre("Classics")
                .totalPages(4)
                .fileData(pdf2)
                .fileSize(pdf2.length)
                .description("A romantic novel of manners written by Jane Austen in 1813.")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
        Book savedBook2 = bookDAO.save(book2);

        // Book 3: Software Design Patterns Handbook (6 pages)
        byte[] pdf3 = createSamplePdf(
                "Software Design Patterns Handbook",
                "Robert C. Martin",
                "Computer Science",
                6,
                new String[]{
                        "Section 1: The Virtual Proxy Pattern. Delaying resource-intensive object instantiation until actual point of use.",
                        "Section 2: The Observer Pattern. Defining a one-to-many dependency between objects for reactive state notification.",
                        "Section 3: The Strategy Pattern. Defining a family of algorithms, encapsulating each one, and making them interchangeable.",
                        "Section 4: Data Access Object (DAO) Pattern. Abstracting and encapsulating all access to the data source.",
                        "Section 5: The Singleton Pattern. Ensuring a class has only one instance, and providing a global point of access to it.",
                        "Section 6: The Template Method Pattern. Defining the skeleton of an algorithm in an operation, deferring some steps to subclasses."
                }
        );
        Book book3 = Book.builder()
                .title("Software Design Patterns Handbook")
                .author("Robert C. Martin")
                .genre("Technology")
                .totalPages(6)
                .fileData(pdf3)
                .fileSize(pdf3.length)
                .description("A comprehensive guide to idiomatic architectural patterns and clean code principles.")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
        Book savedBook3 = bookDAO.save(book3);

        // Seed initial progress for reader1 (id = 2)
        userDAO.findByUsername("reader1").ifPresent(reader -> {
            // Book 1: Reading, page 3
            ReadingProgress prog1 = new ReadingProgress(0, reader.getId(), savedBook1.getId(), 3, ReadingStatus.READING, LocalDateTime.now().minusHours(2));
            progressDAO.saveOrUpdate(prog1);

            // Book 2: Completed, page 4
            ReadingProgress prog2 = new ReadingProgress(0, reader.getId(), savedBook2.getId(), 4, ReadingStatus.COMPLETED, LocalDateTime.now().minusDays(1));
            progressDAO.saveOrUpdate(prog2);

            // Add sample reading sessions
            ReadingSession session1 = new ReadingSession(0, reader.getId(), savedBook1.getId(), LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2), 3, 45);
            ReadingSession session2 = new ReadingSession(0, reader.getId(), savedBook2.getId(), LocalDateTime.now().minusDays(1).minusHours(1), LocalDateTime.now().minusDays(1), 4, 60);
            sessionDAO.save(session1);
            sessionDAO.save(session2);
        });

        LOGGER.info("Successfully seeded 3 sample books with progress and sessions!");
    }

    /**
     * Helper to generate a multi-page PDF document in memory using Apache PDFBox.
     */
    public static byte[] createSamplePdf(String title, String author, String subtitle, int pageCount, String[] pageContents) {
        try (PDDocument document = new PDDocument()) {
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // Header Banner
                    contentStream.beginText();
                    contentStream.setFont(titleFont, 18);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText(title);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(italicFont, 12);
                    contentStream.newLineAtOffset(50, 730);
                    contentStream.showText("By " + author + " | " + subtitle);
                    contentStream.endText();

                    // Content text
                    String pageText = (i < pageContents.length) ? pageContents[i] : "Content for page " + (i + 1) + " in " + title + ".";
                    contentStream.beginText();
                    contentStream.setFont(bodyFont, 12);
                    contentStream.newLineAtOffset(50, 680);
                    contentStream.showText(pageText);
                    contentStream.endText();

                    // Additional paragraph lines
                    contentStream.beginText();
                    contentStream.setFont(bodyFont, 10);
                    contentStream.newLineAtOffset(50, 640);
                    contentStream.showText("BookHaven Digital Library Edition • High Fidelity Reading Engine.");
                    contentStream.newLineAtOffset(0, -18);
                    contentStream.showText("Demonstrating Virtual Proxy lazy loading and page rendering architecture.");
                    contentStream.endText();

                    // Page Footer
                    contentStream.beginText();
                    contentStream.setFont(italicFont, 10);
                    contentStream.newLineAtOffset(250, 50);
                    contentStream.showText("Page " + (i + 1) + " of " + pageCount);
                    contentStream.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create sample PDF", e);
            return new byte[0];
        }
    }
}
