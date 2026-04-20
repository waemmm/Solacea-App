package com.solacea;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    // Function: start - Loads fonts, opens the login screen, sets window options, and shows the app.
    public void start(Stage stage) {
        loadBrandFonts();

        URL loginViewUrl = Main.class.getResource("/fxml/login-view.fxml");
        if (loginViewUrl == null) {
            LOGGER.severe("Missing required resource: /fxml/login-view.fxml");
            showStartupError("Unable to start Solacea because required files are missing.");
            Platform.exit();
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(loginViewUrl);
            Scene scene = new Scene(fxmlLoader.load());

            try (InputStream iconStream = getClass().getResourceAsStream("/images/Solacea_logo.png")) {
                if (iconStream != null) {
                    stage.getIcons().add(new Image(iconStream));
                } else {
                    LOGGER.warning("App icon not found at /images/Solacea_logo.png");
                }
            }

            stage.setTitle("Solacea");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(620);
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load login screen.", e);
            showStartupError("Unable to open the login screen. Please restart the app.");
            Platform.exit();
        }
    }

    // Function: main - Starts the JavaFX application lifecycle.
    public static void main(String[] args) {
        launch();
    }

    // Function: loadBrandFonts - Loads custom app fonts from resources and skips missing-font errors.
    private void loadBrandFonts() {
        String[] fontPaths = {
                "/fonts/Poppins-Regular.ttf",
                "/fonts/Poppins-SemiBold.ttf",
                "/fonts/Poppins-Bold.ttf",
                "/fonts/Prata-Regular.ttf"
        };

        for (String path : fontPaths) {
            try (InputStream stream = Main.class.getResourceAsStream(path)) {
                if (stream != null) {
                    Font.loadFont(stream, 12);
                } else {
                    LOGGER.warning("Font resource not found: " + path);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to close font stream for: " + path, e);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to load font: " + path, e);
                // Keep app startup resilient even if a font file is missing.
            }
        }
    }

    // Function: showStartupError - Displays a user-visible startup error when core resources fail to load.
    private void showStartupError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Solacea Startup Error");
        alert.setHeaderText("The application could not start.");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
