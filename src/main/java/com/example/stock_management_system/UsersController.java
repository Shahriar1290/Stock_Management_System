package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Admin-only screen for adding users, changing roles, resetting passwords and removing accounts. */
public class UsersController {

    @FXML private TextField usernameField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private PasswordField passwordField;
    @FXML private Button roleButton;
    @FXML private Button resetButton;
    @FXML private Button deleteButton;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;

    private final DatabaseManager db = DatabaseManager.getInstance();

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().username()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().email()));
        roleColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().role()));

        roleComboBox.getItems().setAll(User.STAFF, User.ADMIN);
        roleComboBox.setValue(User.STAFF);
        passwordField.setPromptText("At least " + PasswordUtil.MIN_LENGTH + " characters");

        var noSelection = usersTable.getSelectionModel().selectedItemProperty().isNull();
        roleButton.disableProperty().bind(noSelection);
        resetButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, o, user) ->
                roleButton.setText(user != null && user.isAdmin() ? "Make staff" : "Make admin"));

        loadUsers();
    }

    private void loadUsers() {
        usersTable.setItems(FXCollections.observableArrayList(db.getAllUsers()));
    }

    private static boolean isSelf(User user) {
        return user.username().equals(Session.getUser().username());
    }

    @FXML
    private void addUser() {
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || name.isEmpty() || email.isEmpty()) {
            Dialogs.warn("Missing information", "Username, full name and email are required.");
        } else if (!email.contains("@")) {
            Dialogs.warn("Invalid email", "Please enter a valid email address.");
        } else if (password.length() < PasswordUtil.MIN_LENGTH) {
            Dialogs.warn("Password too short", "The password must be at least " + PasswordUtil.MIN_LENGTH + " characters.");
        } else if (!db.createUser(new User(username, name, email, roleComboBox.getValue()), password)) {
            Dialogs.warn("Username taken", "A user called \"" + username + "\" already exists.");
        } else {
            usernameField.clear();
            nameField.clear();
            emailField.clear();
            passwordField.clear();
            roleComboBox.setValue(User.STAFF);
            loadUsers();
        }
    }

    @FXML
    private void toggleRole() {
        User user = usersTable.getSelectionModel().getSelectedItem();
        if (user == null) return;
        if (isSelf(user)) {
            Dialogs.warn("Not allowed", "You cannot change your own role.");
            return;
        }
        String newRole = user.isAdmin() ? User.STAFF : User.ADMIN;
        if (Dialogs.confirm("Change role", "Make " + user.username() + " " + newRole.toLowerCase() + "?")) {
            db.updateUserRole(user.username(), newRole);
            loadUsers();
        }
    }

    @FXML
    private void resetPassword() {
        User user = usersTable.getSelectionModel().getSelectedItem();
        if (user == null) return;
        Dialogs.promptPassword("Reset password", "New password for " + user.username() + ":")
                .ifPresent(password -> {
                    if (password.length() < PasswordUtil.MIN_LENGTH) {
                        Dialogs.warn("Password too short",
                                "The password must be at least " + PasswordUtil.MIN_LENGTH + " characters.");
                    } else if (db.updatePassword(user.username(), password)) {
                        Dialogs.info("Password reset", "The password for " + user.username() + " has been changed.");
                    }
                });
    }

    @FXML
    private void deleteUser() {
        User user = usersTable.getSelectionModel().getSelectedItem();
        if (user == null) return;
        if (isSelf(user)) {
            Dialogs.warn("Not allowed", "You cannot delete your own account while signed in.");
        } else if (user.isAdmin() && db.countAdmins() <= 1) {
            Dialogs.warn("Not allowed", "At least one admin account must remain.");
        } else if (Dialogs.confirm("Delete user", "Delete the account \"" + user.username() + "\"?")) {
            db.deleteUser(user.username());
            loadUsers();
        }
    }
}
