package com.solacea.controller;

import com.solacea.util.DatabaseManager;
import com.solacea.util.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.prefs.Preferences;

public class LoginController {
    private static final Color STATUS_SUCCESS = Color.web("#7A9E7E");
    private static final Color STATUS_ERROR = Color.web("#D57F7E");

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button eyeButton;
    @FXML private Label statusLabel;

    @FXML private CheckBox rememberMeCheckbox;

    private Preferences prefs = Preferences.userRoot().node("com.solacea");

    @FXML
    // Function: initialize - Sets up password field syncing, restore-login behavior, and optional auto-login flow.
    public void initialize() {
        passwordField.setManaged(true);
        passwordTextField.setManaged(false);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordTextField.getText().equals(newVal)) passwordTextField.setText(newVal);
            updatePasswordToggle();
        });
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.getText().equals(newVal)) passwordField.setText(newVal);
            updatePasswordToggle();
        });
        updatePasswordToggle();

        String savedUser = prefs.get("savedUsername", null);
        if (savedUser != null) {
            usernameField.setText(savedUser);
            if (rememberMeCheckbox != null) {
                rememberMeCheckbox.setSelected(true);
            }

            if (DatabaseManager.isDatabaseAvailable()) {
                UserSession.setUser(savedUser);
                Platform.runLater(() -> {
                    try {
                        Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard-view.fxml"));
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        stage.getScene().setRoot(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                UserSession.cleanUserSession();
                showError("Database is offline.");
            }
        }
    }

    @FXML
    // Function: togglePassword - Switches between hidden and visible password input fields.
    private void togglePassword(ActionEvent event) {
        if (passwordField.isVisible()) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }
        updatePasswordToggle();
    }

    @FXML
    // Function: handleLogin - Validates inputs, signs the user in, stores remember-me choice, and opens dashboard.
    protected void handleLogin(ActionEvent event) throws IOException {
        String user = usernameField.getText();
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please enter credentials.");
            return;
        }

        try {
            if (DatabaseManager.validateLogin(user, pass)) {
                UserSession.setUser(user);

                if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                    prefs.put("savedUsername", user);
                } else {
                    prefs.remove("savedUsername");
                }

                switchToScene(event, "/fxml/dashboard-view.fxml");
            } else {
                showError("Login failed. Check your credentials.");
            }
        } catch (SQLException e) {
            showError("Database Connection Error.");
            e.printStackTrace();
        }
    }

    @FXML
    // Function: handleGoToRegister - Navigates from login screen to register screen.
    protected void handleGoToRegister(ActionEvent event) throws IOException {
        switchToScene(event, "/fxml/register-view.fxml");
    }

    // Function: switchToScene - Loads an FXML view and swaps it into the current stage scene.
    private void switchToScene(ActionEvent event, String path) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(path));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    // Function: updatePasswordToggle - Updates eye-button visibility/text based on current password field state.
    private void updatePasswordToggle() {
        if (eyeButton == null || passwordField == null || passwordTextField == null) return;

        String text = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        boolean hasText = text != null && !text.isEmpty();

        if (!hasText && !passwordField.isVisible()) {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }

        eyeButton.setVisible(hasText);
        eyeButton.setManaged(hasText);
        eyeButton.setText(passwordField.isVisible() ? "Show" : "Hide");
    }

    // Function: showError - Shows an error status message in the login form.
    private void showError(String message) {
        showStatus(message, STATUS_ERROR);
    }

    // Function: showSuccess - Shows a success status message in the login form.
    private void showSuccess(String message) {
        showStatus(message, STATUS_SUCCESS);
    }

    // Function: showStatus - Writes status text and color to the login status label.
    private void showStatus(String message, Color color) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
}
