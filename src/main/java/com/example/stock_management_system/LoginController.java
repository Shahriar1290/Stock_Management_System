package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Sign-in screen. When the database has no users yet, it switches to a one-time
 * setup mode that creates the first administrator account.
 */
public class LoginController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox setupFields;
    @FXML private VBox confirmFields;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private boolean setupMode;

    @FXML
    public void initialize() {
        setupMode = !db.hasUsers();
        if (setupMode) {
            titleLabel.setText("Create your admin account");
            subtitleLabel.setText("No users exist yet. Set up the first administrator to get started.");
            submitButton.setText("Create account");
            setShown(setupFields, true);
            setShown(confirmFields, true);
            passwordField.setPromptText("At least " + PasswordUtil.MIN_LENGTH + " characters");
        }
    }

    @FXML
    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter a username and password.");
            return;
        }

        if (setupMode && !createAdmin(username, password)) return;

        if (db.validateUser(username, password)) {
            Session.setUser(db.getUserByUsername(username));
            Navigator.showMain();
        } else {
            showError("Invalid username or password.");
            passwordField.clear();
        }
    }

    private boolean createAdmin(String username, String password) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Please enter your name and email.");
        } else if (!email.contains("@")) {
            showError("Please enter a valid email address.");
        } else if (password.length() < PasswordUtil.MIN_LENGTH) {
            showError("Password must be at least " + PasswordUtil.MIN_LENGTH + " characters.");
        } else if (!password.equals(confirmPasswordField.getText())) {
            showError("Passwords do not match.");
        } else if (!db.createUser(new User(username, name, email, "ADMIN"), password)) {
            showError("Could not create the account. Try a different username.");
        } else {
            return true;
        }
        return false;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        setShown(errorLabel, true);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
