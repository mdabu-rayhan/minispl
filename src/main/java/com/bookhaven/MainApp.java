package com.bookhaven;

import com.bookhaven.util.DatabaseManager;
import com.bookhaven.util.DatabaseSeeder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main JavaFX Application coordinator.
 */
public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("Initializing BookHaven SQLite database schema and seed data...");
        DatabaseManager.getInstance().initializeSchema();
        DatabaseSeeder seeder = new DatabaseSeeder();
        seeder.seed();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bookhaven/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 650);
            primaryStage.setTitle("BookHaven - Authentication");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize and display login window", e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        LOGGER.info("Closing BookHaven database connection pool...");
        DatabaseManager.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
