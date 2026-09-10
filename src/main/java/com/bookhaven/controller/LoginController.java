package com.bookhaven.controller;

import com.bookhaven.model.User;
import com.bookhaven.service.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for user authentication and Reader registration.
 */
public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private final AuthService authService = new AuthService();

    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private VBox confirmPasswordBox;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button primaryActionButton;
    @FXML private Button toggleModeButton;

    private boolean isRegisterMode = false;

    @FXML
    public void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void handlePrimaryAction(ActionEvent event) {
        messageLabel.setText("");

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showError("Please enter both username and password.");
            return;
        }

        if (isRegisterMode) {
            handleRegistration(username, password);
        } else {
            handleLogin(username, password);
        }
    }

    private void handleLogin(String username, String password) {
        Optional<User> userOpt = authService.authenticate(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            LOGGER.info("Logged in successfully as: " + user.getUsername());
            navigateToDashboard(user);
        } else {
            showError("Invalid username or password. Please check your credentials.");
        }
    }

    private void handleRegistration(String username, String password) {
        String confirmPassword = confirmPasswordField.getText();
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match. Please verify.");
            return;
        }

        try {
            User newUser = authService.registerReader(username, password);
            showSuccess("Account created successfully! Redirecting...");
            // Auto login and navigate
            authService.authenticate(username, password);
            navigateToDashboard(newUser);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during registration", e);
            showError("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleMode(ActionEvent event) {
        isRegisterMode = !isRegisterMode;
        messageLabel.setText("");

        if (isRegisterMode) {
            formTitleLabel.setText("Create New Reader Account");
            formSubtitleLabel.setText("Register with a unique username and secure password.");
            confirmPasswordBox.setVisible(true);
            confirmPasswordBox.setManaged(true);
            primaryActionButton.setText("Register & Sign In");
            toggleModeButton.setText("Already have an account? Sign In");
        } else {
            formTitleLabel.setText("Sign In to BookHaven");
            formSubtitleLabel.setText("Enter your account credentials to access your library.");
            confirmPasswordBox.setVisible(false);
            confirmPasswordBox.setManaged(false);
            primaryActionButton.setText("Sign In");
            toggleModeButton.setText("Need an account? Register as Reader");
        }
    }

    @FXML
    private void fillAdminCredentials(ActionEvent event) {
        usernameField.setText("admin");
        passwordField.setText("admin123");
        if (isRegisterMode) handleToggleMode(null);
    }

    @FXML
    private void fillReaderCredentials(ActionEvent event) {
        usernameField.setText("reader1");
        passwordField.setText("reader123");
        if (isRegisterMode) handleToggleMode(null);
    }

    private void navigateToDashboard(User user) {
        try {
            String fxmlPath = user.isAdmin()
                    ? "/com/bookhaven/fxml/admin_dashboard.fxml"
                    : "/com/bookhaven/fxml/book_catalog.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) primaryActionButton.getScene().getWindow();
            Scene scene = new Scene(root, 1100, 750);
            stage.setTitle("BookHaven - " + (user.isAdmin() ? "Admin Portal" : "Library Catalog") + " [" + user.getUsername() + "]");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard scene", e);
            showError("Failed to transition to dashboard: " + e.getMessage());
        }
    }

    private void showError(String text) {
        messageLabel.setTextFill(Color.web("#dc2626"));
        messageLabel.setText(text);
    }

    private void showSuccess(String text) {
        messageLabel.setTextFill(Color.web("#16a34a"));
        messageLabel.setText(text);
    }
}
