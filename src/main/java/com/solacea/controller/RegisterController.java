package com.solacea.controller;

import com.solacea.util.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {
    private static final Color STATUS_SUCCESS = Color.web("#7A9E7E");
    private static final Color STATUS_ERROR = Color.web("#D57F7E");

    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 16;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmTextField;
    @FXML private Button eyeButton;
    @FXML private Button confirmEyeButton;
    @FXML private Label statusLabel;

    @FXML
    // Function: initialize - Sets up password/confirm field syncing and initial toggle state.
    public void initialize() {
        passwordField.setManaged(true);
        passwordTextField.setManaged(false);
        confirmPasswordField.setManaged(true);
        confirmTextField.setManaged(false);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordTextField.getText().equals(newVal)) passwordTextField.setText(newVal);
            updatePasswordToggles();
        });
        passwordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.getText().equals(newVal)) passwordField.setText(newVal);
            updatePasswordToggles();
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmTextField.getText().equals(newVal)) confirmTextField.setText(newVal);
            updatePasswordToggles();
        });
        confirmTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmPasswordField.getText().equals(newVal)) confirmPasswordField.setText(newVal);
            updatePasswordToggles();
        });

        updatePasswordToggles();
    }

    @FXML
    // Function: togglePassword - Switches main password field between hidden and visible text.
    private void togglePassword(ActionEvent event) {
        if (passwordField.isVisible()) {
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
        }
        updatePasswordToggles();
    }

    @FXML
    // Function: toggleConfirmPassword - Switches confirm-password field between hidden and visible text.
    private void toggleConfirmPassword(ActionEvent event) {
        if (confirmPasswordField.isVisible()) {
            confirmTextField.setVisible(true);
            confirmTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
        } else {
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            confirmTextField.setVisible(false);
            confirmTextField.setManaged(false);
        }
        updatePasswordToggles();
    }

    @FXML
    // Function: handleRegister - Validates form data, creates account, and returns to login when successful.
    protected void handleRegister(ActionEvent event) {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
        String confirmPass = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmTextField.getText();

        if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            showError("Please fill all fields.");
            return;
        }

        if (user.length() < USERNAME_MIN_LENGTH) {
            showError("Username must be at least 3 characters.");
            return;
        }

        if (user.length() > USERNAME_MAX_LENGTH) {
            showError("Username must be at most 16 characters.");
            return;
        }

        if (pass.length() < PASSWORD_MIN_LENGTH) {
            showError("Password must be at least 8 characters.");
            return;
        }

        if (pass.length() > PASSWORD_MAX_LENGTH) {
            showError("Password must be at most 72 characters.");
            return;
        }

        if (!pass.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            if (DatabaseManager.registerUser(user, pass)) {
                showSuccess("Account created! Please log in.");
                try {
                    handleBackToLogin(event);
                } catch (IOException e) {
                    showError("Account created, but failed to open login page.");
                    e.printStackTrace();
                }
            } else {
                showError("Username already exists.");
            }
        } catch (SQLException e) {
            showError("Database Error. Try again.");
            e.printStackTrace();
        }
    }

    // Function: showError - Shows an error status message in the register form.
    private void showError(String message) {
        showStatus(message, STATUS_ERROR);
    }

    // Function: showSuccess - Shows a success status message in the register form.
    private void showSuccess(String message) {
        showStatus(message, STATUS_SUCCESS);
    }

    // Function: showStatus - Writes status text and color to the register status label.
    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }

    @FXML
    // Function: handleBackToLogin - Navigates from register screen back to login screen.
    protected void handleBackToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
    }

    // Function: updatePasswordToggles - Controls eye-button visibility and keeps password fields in a valid state.
    private void updatePasswordToggles() {
        if (eyeButton != null) {
            String passText = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();
            boolean hasText = passText != null && !passText.isEmpty();

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

        if (confirmEyeButton != null) {
            String confirmText = confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmTextField.getText();
            boolean hasText = confirmText != null && !confirmText.isEmpty();

            if (!hasText && !confirmPasswordField.isVisible()) {
                confirmPasswordField.setVisible(true);
                confirmPasswordField.setManaged(true);
                confirmTextField.setVisible(false);
                confirmTextField.setManaged(false);
            }

            confirmEyeButton.setVisible(hasText);
            confirmEyeButton.setManaged(hasText);
            confirmEyeButton.setText(confirmPasswordField.isVisible() ? "Show" : "Hide");
        }
    }
}
