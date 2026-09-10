package com.bookhaven.controller;

import com.bookhaven.model.Book;
import com.bookhaven.model.User;
import com.bookhaven.service.AuthService;
import com.bookhaven.service.BookIngestionService;
import com.bookhaven.service.ReadingTrackerService;
import com.bookhaven.util.SessionContext;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Admin Management & System Analytics Dashboard.
 */
public class AdminDashboardController {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());

    private final BookIngestionService ingestionService = new BookIngestionService();
    private final AuthService authService = new AuthService();
    private final ReadingTrackerService trackingService = new ReadingTrackerService();

    @FXML private Label adminGreetingLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalBooksLabel;
    @FXML private Label totalStorageLabel;
    @FXML private Label totalPagesReadLabel;
    @FXML private Label totalReadingHoursLabel;

    // Ingest Tab
    @FXML private Label selectedFileLabel;
    @FXML private TextField ingestTitleField;
    @FXML private TextField ingestAuthorField;
    @FXML private ComboBox<String> ingestGenreComboBox;
    @FXML private TextArea ingestDescriptionArea;
    @FXML private Label ingestMessageLabel;
    @FXML private Button executeIngestButton;

    // Catalog Table Tab
    @FXML private TableView<Book> catalogTableView;
    @FXML private TableColumn<Book, Number> colBookId;
    @FXML private TableColumn<Book, String> colBookTitle;
    @FXML private TableColumn<Book, String> colBookAuthor;
    @FXML private TableColumn<Book, String> colBookGenre;
    @FXML private TableColumn<Book, Number> colBookPages;
    @FXML private TableColumn<Book, String> colBookSize;
    @FXML private TableColumn<Book, String> colBookDate;

    // User Table Tab
    @FXML private TableView<User> usersTableView;
    @FXML private TableColumn<User, Number> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserCreated;

    private File selectedPdfFile;
    private User currentAdmin;

    @FXML
    public void initialize() {
        this.currentAdmin = SessionContext.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            adminGreetingLabel.setText("👤 Administrator: " + currentAdmin.getUsername());
        }

        setupCatalogTable();
        setupUsersTable();
        setupGenres();
        refreshAll();
    }

    private void setupGenres() {
        ingestGenreComboBox.getItems().addAll("Philosophy", "Classics", "Technology", "Science", "Fiction", "History", "Biography", "Business");
    }

    private void setupCatalogTable() {
        colBookId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colBookTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colBookAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthor()));
        colBookGenre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenre()));
        colBookPages.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTotalPages()));
        colBookSize.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedFileSize()));
        colBookDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"
        ));
    }

    private void setupUsersTable() {
        colUserId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colUserRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));
        colUserCreated.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt() != null ? data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"
        ));
    }

    public void refreshAll() {
        refreshAnalytics();
        refreshCatalogTable();
        refreshUsersTable();
    }

    public void refreshAnalytics() {
        Map<String, Object> stats = trackingService.getSystemAnalytics();
        totalUsersLabel.setText(stats.get("totalUsers").toString());
        totalBooksLabel.setText(stats.get("totalBooks").toString());
        totalStorageLabel.setText(stats.get("totalStorageFormatted").toString());
        totalPagesReadLabel.setText(stats.get("totalPagesRead").toString());
        totalReadingHoursLabel.setText(stats.get("totalReadingHours").toString());
    }

    public void refreshCatalogTable() {
        List<Book> books = ingestionService.getAllBooks();
        catalogTableView.setItems(FXCollections.observableArrayList(books));
    }

    public void refreshUsersTable() {
        List<User> users = authService.getAllUsers();
        usersTableView.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    private void handleSelectPdfFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF Document to Ingest");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        File file = chooser.showOpenDialog(adminGreetingLabel.getScene().getWindow());
        if (file != null) {
            this.selectedPdfFile = file;
            selectedFileLabel.setText("Selected: " + file.getName() + " (" + String.format("%.1f KB", file.length() / 1024.0) + ")");
            if (ingestTitleField.getText().isBlank()) {
                String name = file.getName();
                int dot = name.lastIndexOf('.');
                ingestTitleField.setText(dot > 0 ? name.substring(0, dot) : name);
            }
        }
    }

    @FXML
    private void handleExecuteIngest(ActionEvent event) {
        ingestMessageLabel.setText("");
        if (selectedPdfFile == null) {
            showIngestError("Please select a PDF document file first.");
            return;
        }

        String title = ingestTitleField.getText();
        String author = ingestAuthorField.getText();
        String genre = ingestGenreComboBox.getValue();
        String description = ingestDescriptionArea.getText();

        try {
            Book ingestedBook = ingestionService.ingestBook(selectedPdfFile, title, author, genre, description);
            showIngestSuccess("🎉 Book successfully ingested! (ID: " + ingestedBook.getId() + ", Pages: " + ingestedBook.getTotalPages() + ")");

            // Clear inputs
            selectedPdfFile = null;
            selectedFileLabel.setText("No file selected yet");
            ingestTitleField.clear();
            ingestAuthorField.clear();
            ingestDescriptionArea.clear();

            refreshAll();
        } catch (IllegalArgumentException e) {
            showIngestError(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Ingestion failed", e);
            showIngestError("Failed to parse and store PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditSelectedBook(ActionEvent event) {
        Book selected = catalogTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a book from the table to edit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Book Metadata - ID: " + selected.getId());
        dialog.setHeaderText("Update metadata for '" + selected.getTitle() + "'");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(selected.getTitle());
        TextField authorField = new TextField(selected.getAuthor());
        TextField genreField = new TextField(selected.getGenre());
        TextArea descArea = new TextArea(selected.getDescription() != null ? selected.getDescription() : "");
        descArea.setPrefRowCount(3);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Author:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Genre:"), 0, 2);
        grid.add(genreField, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descArea, 1, 3);

        pane.setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selected.setTitle(titleField.getText().trim());
            selected.setAuthor(authorField.getText().trim());
            selected.setGenre(genreField.getText().trim());
            selected.setDescription(descArea.getText().trim());

            boolean updated = ingestionService.updateBookMetadata(selected);
            if (updated) {
                refreshAll();
                showAlert(Alert.AlertType.INFORMATION, "Book metadata updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Failed to update book metadata.");
            }
        }
    }

    @FXML
    private void handleDeleteSelectedBook(ActionEvent event) {
        Book selected = catalogTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a book from the table to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently delete '" + selected.getTitle() + "'?\nAll associated reading progress will be removed.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean deleted = ingestionService.deleteBook(selected.getId());
                if (deleted) {
                    refreshAll();
                    showAlert(Alert.AlertType.INFORMATION, "Book deleted from catalog.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed to delete book.");
                }
            }
        });
    }

    @FXML
    private void handleDeleteSelectedUser(ActionEvent event) {
        User selected = usersTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select a user account to delete.");
            return;
        }

        if (currentAdmin != null && selected.getId() == currentAdmin.getId()) {
            showAlert(Alert.AlertType.ERROR, "You cannot delete your own currently active admin account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete user '" + selected.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean deleted = authService.deleteUser(selected.getId());
                if (deleted) {
                    refreshAll();
                    showAlert(Alert.AlertType.INFORMATION, "User account removed.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed to delete user account.");
                }
            }
        });
    }


    @FXML
    private void handleLogout(ActionEvent event) {
        SessionContext.getInstance().clearSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bookhaven/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminGreetingLabel.getScene().getWindow();
            stage.setTitle("BookHaven - Login");
            stage.setScene(new Scene(root, 900, 650));
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to return to login", e);
        }
    }

    private void showIngestError(String text) {
        ingestMessageLabel.setTextFill(Color.web("#dc2626"));
        ingestMessageLabel.setText(text);
    }

    private void showIngestSuccess(String text) {
        ingestMessageLabel.setTextFill(Color.web("#16a34a"));
        ingestMessageLabel.setText(text);
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }
}
