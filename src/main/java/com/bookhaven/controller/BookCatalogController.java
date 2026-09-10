package com.bookhaven.controller;

import com.bookhaven.dao.BookDAO;
import com.bookhaven.dao.impl.SQLiteBookDAO;
import com.bookhaven.model.Book;
import com.bookhaven.model.ReadingProgress;
import com.bookhaven.model.ReadingStatus;
import com.bookhaven.model.User;
import com.bookhaven.search.*;
import com.bookhaven.service.ReadingTrackerService;
import com.bookhaven.util.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Reader Book Catalog, Discovery, and Personal Analytics Dashboard.
 * Integrates Strategy Pattern for sorting and Template Method for report exports.
 */
public class BookCatalogController {

    private static final Logger LOGGER = Logger.getLogger(BookCatalogController.class.getName());

    private final BookDAO bookDAO = new SQLiteBookDAO();
    private final ReadingTrackerService trackingService = new ReadingTrackerService();

    @FXML private Label userGreetingLabel;
    @FXML private Label totalHoursLabel;
    @FXML private Label monthHoursLabel;
    @FXML private Label completedBooksLabel;
    @FXML private Label activeBooksLabel;

    @FXML private Button allBooksButton;
    @FXML private Button currentlyReadingButton;
    @FXML private Button completedButton;
    @FXML private Label catalogSectionTitleLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private ComboBox<CatalogSortStrategy> sortStrategyComboBox;
    @FXML private Label resultsCountLabel;
    @FXML private FlowPane booksFlowPane;

    public enum CatalogFilterMode {
        ALL("My Library"),
        CURRENTLY_READING("Currently Reading"),
        COMPLETED("Completed Books");

        private final String title;
        CatalogFilterMode(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    private CatalogFilterMode currentFilterMode = CatalogFilterMode.ALL;
    private User currentUser;
    private Map<Integer, ReadingProgress> userProgressMap = new HashMap<>();

    @FXML
    public void initialize() {
        this.currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser != null) {
            userGreetingLabel.setText(currentUser.getUsername());
        }

        setupSortStrategies();
        setupFilters();
        refreshDashboardMetrics();
        loadCatalog();
    }

    private void setupSortStrategies() {
        sortStrategyComboBox.getItems().addAll(
                new TitleSortStrategy(),
                new RecentSortStrategy(),
                new ReadingStatusSortStrategy()
        );
        sortStrategyComboBox.getSelectionModel().selectFirst();
        sortStrategyComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(CatalogSortStrategy strategy) {
                return strategy != null ? strategy.getStrategyName() : "";
            }

            @Override
            public CatalogSortStrategy fromString(String string) {
                return null;
            }
        });

        sortStrategyComboBox.setOnAction(e -> loadCatalog());
    }

    private void setupFilters() {
        genreComboBox.getItems().clear();
        genreComboBox.getItems().add("All Genres");
        List<String> genres = bookDAO.findAllGenres();
        genreComboBox.getItems().addAll(genres);
        genreComboBox.getSelectionModel().selectFirst();

        genreComboBox.setOnAction(e -> loadCatalog());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadCatalog());
    }

    public void refreshDashboardMetrics() {
        if (currentUser == null) return;
        Map<String, Object> stats = trackingService.getUserAnalytics(currentUser.getId());
        totalHoursLabel.setText(stats.get("totalHours").toString());
        monthHoursLabel.setText(stats.get("monthHours").toString());
        completedBooksLabel.setText(stats.get("completedBooks").toString());
        activeBooksLabel.setText(stats.get("activeBooks").toString());
        this.userProgressMap = trackingService.getProgressMapForUser(currentUser.getId());
    }

    @FXML
    private void handleFilterAll(ActionEvent event) {
        currentFilterMode = CatalogFilterMode.ALL;
        updateSidebarActiveState();
        loadCatalog();
    }

    @FXML
    private void handleFilterReading(ActionEvent event) {
        currentFilterMode = CatalogFilterMode.CURRENTLY_READING;
        updateSidebarActiveState();
        loadCatalog();
    }

    @FXML
    private void handleFilterCompleted(ActionEvent event) {
        currentFilterMode = CatalogFilterMode.COMPLETED;
        updateSidebarActiveState();
        loadCatalog();
    }

    private void updateSidebarActiveState() {
        if (allBooksButton != null) allBooksButton.setStyle("");
        if (currentlyReadingButton != null) currentlyReadingButton.setStyle("");
        if (completedButton != null) completedButton.setStyle("");

        String activeStyle = "-fx-font-weight: bold;";
        switch (currentFilterMode) {
            case ALL -> {
                if (allBooksButton != null) allBooksButton.setStyle(activeStyle);
            }
            case CURRENTLY_READING -> {
                if (currentlyReadingButton != null) currentlyReadingButton.setStyle(activeStyle);
            }
            case COMPLETED -> {
                if (completedButton != null) completedButton.setStyle(activeStyle);
            }
        }

        if (catalogSectionTitleLabel != null) {
            catalogSectionTitleLabel.setText(currentFilterMode.getTitle());
        }
    }

    public void loadCatalog() {
        String query = searchField.getText();
        String selectedGenre = genreComboBox.getValue();

        List<Book> books = bookDAO.search(query, selectedGenre);

        // Filter by sidebar category (All vs Currently Reading vs Completed)
        if (currentFilterMode == CatalogFilterMode.CURRENTLY_READING) {
            books = books.stream().filter(b -> {
                ReadingProgress prog = userProgressMap.get(b.getId());
                return prog != null && prog.getStatus() == ReadingStatus.READING;
            }).toList();
        } else if (currentFilterMode == CatalogFilterMode.COMPLETED) {
            books = books.stream().filter(b -> {
                ReadingProgress prog = userProgressMap.get(b.getId());
                return prog != null && prog.getStatus() == ReadingStatus.COMPLETED;
            }).toList();
        }

        // Apply Strategy Pattern for sorting
        CatalogSortStrategy activeStrategy = sortStrategyComboBox.getValue();
        if (activeStrategy != null) {
            books = activeStrategy.sort(books, userProgressMap);
        }

        resultsCountLabel.setText("Showing " + books.size() + " book" + (books.size() == 1 ? "" : "s"));
        renderBookCards(books);
    }

    private void renderBookCards(List<Book> books) {
        booksFlowPane.getChildren().clear();

        if (books.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50, 40, 50, 40));
            Label emptyLabel = new Label("📚 No books found in this view.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #78716c; -fx-font-weight: bold;");
            Label hintLabel = new Label("Try changing your search term, switching genres, or choosing 'All Books'.");
            hintLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #a8a29e;");
            emptyBox.getChildren().addAll(emptyLabel, hintLabel);
            booksFlowPane.getChildren().add(emptyBox);
            return;
        }

        for (Book book : books) {
            booksFlowPane.getChildren().add(createBookCard(book));
        }
    }

    private VBox createBookCard(Book book) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d1d5db; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-padding: 14px;");
        card.setPrefWidth(280);
        card.setMinWidth(260);

        // Top Row: Genre + Status
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label genreLabel = new Label(book.getGenre());
        genreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4b5563; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ReadingProgress progress = userProgressMap.get(book.getId());
        ReadingStatus status = progress != null ? progress.getStatus() : ReadingStatus.UNREAD;

        Label statusLabel = new Label(status.getDisplayName());
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(genreLabel, spacer, statusLabel);

        // Book Header
        VBox coverHeader = new VBox(4);
        coverHeader.setAlignment(Pos.CENTER);
        coverHeader.setPadding(new Insets(12, 8, 12, 8));
        coverHeader.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-width: 1px; -fx-border-radius: 4px;");

        Label bookIcon = new Label("📖");
        bookIcon.setStyle("-fx-font-size: 24px;");

        Label coverTitle = new Label(book.getTitle());
        coverTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        coverTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        coverTitle.setWrapText(true);
        coverTitle.setMaxWidth(230);

        coverHeader.getChildren().addAll(bookIcon, coverTitle);

        // Title and Author Details
        VBox metaDetails = new VBox(4);
        Label titleLabel = new Label(book.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(36);
        titleLabel.setMaxWidth(250);

        Label authorLabel = new Label("By " + book.getAuthor());
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label metaLabel = new Label("Pages: " + book.getTotalPages() + " | Size: " + book.getFormattedFileSize());
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        metaDetails.getChildren().addAll(titleLabel, authorLabel, metaLabel);

        // Reading Progress Indicator
        VBox progressBox = new VBox(4);
        int currentPage = progress != null ? progress.getCurrentPage() : 1;
        double pct = progress != null ? progress.getProgressPercentage(book.getTotalPages()) : 0.0;

        ProgressBar progressBar = new ProgressBar(pct / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        HBox pctRow = new HBox();
        Label pctLabel = new Label(String.format("%.0f%%", pct));
        pctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151; -fx-font-weight: bold;");

        Region pctSpacer = new Region();
        HBox.setHgrow(pctSpacer, Priority.ALWAYS);

        Label pageLabel = new Label("Page " + currentPage + " of " + book.getTotalPages());
        pageLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        pctRow.getChildren().addAll(pctLabel, pctSpacer, pageLabel);
        progressBox.getChildren().addAll(progressBar, pctRow);

        // Action Button
        Button readButton;
        if (status == ReadingStatus.READING) {
            readButton = new Button("Resume (Page " + currentPage + ")");
        } else if (status == ReadingStatus.COMPLETED) {
            readButton = new Button("Re-read Book");
        } else {
            readButton = new Button("Start Reading");
        }
        readButton.setMaxWidth(Double.MAX_VALUE);
        readButton.setOnAction(e -> openReaderView(book, currentPage));

        card.getChildren().addAll(topRow, coverHeader, metaDetails, progressBox, readButton);
        return card;
    }

    private void openReaderView(Book book, int initialPage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bookhaven/fxml/reader_view.fxml"));
            Parent root = loader.load();

            ReaderViewController controller = loader.getController();
            controller.initBook(book, initialPage, this);

            Stage stage = (Stage) booksFlowPane.getScene().getWindow();
            Scene scene = new Scene(root, 1150, 800);
            stage.setTitle("BookHaven Reader - " + book.getTitle());
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open reader view for: " + book.getTitle(), e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to open reader: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleResetFilters(ActionEvent event) {
        searchField.clear();
        genreComboBox.getSelectionModel().selectFirst();
        sortStrategyComboBox.getSelectionModel().selectFirst();
        loadCatalog();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionContext.getInstance().clearSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bookhaven/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) booksFlowPane.getScene().getWindow();
            stage.setTitle("BookHaven - Login");
            stage.setScene(new Scene(root, 900, 650));
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to return to login", e);
        }
    }
}
