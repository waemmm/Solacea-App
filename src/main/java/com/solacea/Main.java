package com.solacea;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class Main extends Application {
    @Override
    // Function: start - Loads fonts, opens the login screen, sets window options, and shows the app.
    public void start(Stage stage) throws Exception {
        loadBrandFonts();

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/Solacea_logo.png")));
        stage.setTitle("Solacea");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        stage.show();
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
                }
            } catch (IOException ignored) {
                // Keep app startup resilient even if a font file is missing.
            }
        }
    }
}
