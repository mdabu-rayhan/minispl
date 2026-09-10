package com.bookhaven.controller;

import com.bookhaven.document.BookDocument;
import com.bookhaven.model.Book;
import com.bookhaven.model.User;
import com.bookhaven.service.ReadingTrackerService;
import com.bookhaven.util.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the minimal, distraction-free PDF reader view.
 * Utilizes the Virtual Proxy pattern (BookDocument) for on-demand page rendering and progress tracking.
 */
public class ReaderViewController {

    private static final Logger LOGGER = Logger.getLogger(ReaderViewController.class.getName());

    @FXML private Label bookTitleLabel;
    @FXML private Label bookAuthorLabel;
    @FXML private Label pageIndicatorLabel;
    @FXML private Label zoomLevelLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;

    @FXML private ScrollPane pdfScrollPane;
    @FXML private ImageView pdfImageView;

    private final ReadingTrackerService trackingService = new ReadingTrackerService();
    private Book currentBook;
    private User currentUser;
    private int currentPageNumber = 1;
    private double zoomScale = 1.25; // 125% default reading scale
    private LocalDateTime sessionStartTime;
    private int initialPageNumber = 1;

    private BookCatalogController parentCatalogController;

    @FXML
    public void initialize() {
        this.currentUser = SessionContext.getInstance().getCurrentUser();
        this.sessionStartTime = LocalDateTime.now();

        // Setup keyboard navigation (Arrow keys)
        pdfScrollPane.setFocusTraversable(true);
        pdfScrollPane.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case LEFT -> handlePrevPage(null);
                case RIGHT -> handleNextPage(null);
                case UP -> handleZoomIn(null);
                case DOWN -> handleZoomOut(null);
                default -> {}
            }
        });
    }

    /**
     * Initializes the reader view with the selected book and starting page.
     */
    public void initBook(Book book, int initialPage, BookCatalogController catalogController) {
        this.currentBook = book;
        this.parentCatalogController = catalogController;
        this.currentPageNumber = Math.max(1, Math.min(initialPage, book.getTotalPages()));
        this.initialPageNumber = this.currentPageNumber;
        this.sessionStartTime = LocalDateTime.now();

        bookTitleLabel.setText(book.getTitle());
        bookAuthorLabel.setText("By " + book.getAuthor() + " • " + book.getGenre());

        loadCurrentPage();
    }

    private void loadCurrentPage() {
        if (currentBook == null) return;

        BookDocument document = currentBook.getDocument();
        if (document == null) {
            LOGGER.warning("No BookDocument attached to book: " + currentBook.getTitle());
            return;
        }

        int totalPages = currentBook.getTotalPages();
        pageIndicatorLabel.setText("Page " + currentPageNumber + " of " + totalPages);
        zoomLevelLabel.setText((int) (zoomScale * 100) + "%");

        prevPageButton.setDisable(currentPageNumber <= 1);
        nextPageButton.setDisable(currentPageNumber >= totalPages);

        // Virtual Proxy: Defers PDF loading until renderPageImage is invoked
        Image pageImage = document.renderPageImage(currentPageNumber - 1, zoomScale);
        if (pageImage != null) {
            pdfImageView.setImage(pageImage);
        }

        // Observer event dispatch & progress persistence
        if (currentUser != null) {
            trackingService.updateProgress(currentUser.getId(), currentBook.getId(), currentPageNumber, totalPages);
        }
    }

    private void navigateToPage(int page) {
        int total = currentBook.getTotalPages();
        int validPage = Math.max(1, Math.min(page, total));
        if (validPage != currentPageNumber) {
            this.currentPageNumber = validPage;
            loadCurrentPage();
        }
    }

    @FXML
    private void handlePrevPage(ActionEvent event) {
        if (currentPageNumber > 1) {
            currentPageNumber--;
            loadCurrentPage();
        }
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        if (currentPageNumber < currentBook.getTotalPages()) {
            currentPageNumber++;
            loadCurrentPage();
        }
    }

    @FXML
    private void handleZoomIn(ActionEvent event) {
        if (zoomScale < 2.5) {
            zoomScale += 0.25;
            loadCurrentPage();
        }
    }

    @FXML
    private void handleZoomOut(ActionEvent event) {
        if (zoomScale > 0.5) {
            zoomScale -= 0.25;
            loadCurrentPage();
        }
    }

    @FXML
    private void handleResetZoom(ActionEvent event) {
        zoomScale = 1.25;
        loadCurrentPage();
    }

    @FXML
    private void handleBackToCatalog(ActionEvent event) {
        closeAndSaveSession();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bookhaven/fxml/book_catalog.fxml"));
            Parent root = loader.load();

            BookCatalogController controller = loader.getController();
            controller.refreshDashboardMetrics();
            controller.loadCatalog();

            Stage stage = (Stage) pdfImageView.getScene().getWindow();
            stage.setTitle("BookHaven - Library Catalog [" + currentUser.getUsername() + "]");
            stage.setScene(new Scene(root, 1100, 750));
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to catalog", e);
        }
    }

    private void closeAndSaveSession() {
        if (currentUser != null && currentBook != null) {
            long minutes = Duration.between(sessionStartTime, LocalDateTime.now()).toMinutes();
            int durationMinutes = (int) Math.max(1, minutes);
            int pagesRead = Math.abs(currentPageNumber - initialPageNumber) + 1;

            trackingService.logSession(currentUser.getId(), currentBook.getId(), pagesRead, durationMinutes);

            // Close Virtual Proxy / Real Document resources
            if (currentBook.getDocument() != null) {
                currentBook.getDocument().close();
            }
        }
    }
}
