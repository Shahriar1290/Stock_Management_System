package com.example.stock_management_system;

import com.example.stock_management_system.models.User;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** The application shell: sidebar navigation, top bar and the content area that hosts each page. */
public class MainLayoutController {

    @FXML private ToggleButton dashboardNav;
    @FXML private ToggleButton productsNav;
    @FXML private ToggleButton categoriesNav;
    @FXML private ToggleButton ordersNav;
    @FXML private ToggleButton suppliersNav;
    @FXML private ToggleButton profileNav;
    @FXML private Button themeButton;

    @FXML private Label dateLabel;
    @FXML private Label avatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private StackPane contentArea;

    // Held strongly here and weakly by the session, so old shells can be collected after sign-out.
    private final ChangeListener<User> userListener = (obs, oldUser, newUser) -> showUser(newUser);

    @FXML
    public void initialize() {
        ToggleGroup nav = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{
                dashboardNav, productsNav, categoriesNav, ordersNav, suppliersNav, profileNav}) {
            b.setToggleGroup(nav);
        }

        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        showUser(Session.getUser());
        Session.userProperty().addListener(new WeakChangeListener<>(userListener));
        updateThemeButton();
        openDashboard();
    }

    private void showUser(User user) {
        if (user == null) return;
        String name = user.name() == null || user.name().isBlank() ? user.username() : user.name();
        userNameLabel.setText(name);
        userRoleLabel.setText(user.role() == null ? "" : user.role());
        avatarLabel.setText(initials(name));
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    @FXML
    private void openDashboard() {
        show(dashboardNav, "Dashboard.fxml");
    }

    @FXML
    private void openProducts() {
        show(productsNav, "Product.fxml");
    }

    @FXML
    private void openCategories() {
        show(categoriesNav, "Categories.fxml");
    }

    @FXML
    private void openOrders() {
        show(ordersNav, "Orders.fxml");
    }

    @FXML
    private void openSuppliers() {
        show(suppliersNav, "Supplier.fxml");
    }

    @FXML
    private void openProfile() {
        show(profileNav, "Profile.fxml");
    }

    private void show(ToggleButton navButton, String fxml) {
        navButton.setSelected(true);
        contentArea.getChildren().setAll(Navigator.load(fxml));
    }

    @FXML
    private void toggleTheme() {
        Navigator.toggleTheme();
        updateThemeButton();
    }

    private void updateThemeButton() {
        themeButton.setText(Navigator.isDarkTheme() ? "Light mode" : "Dark mode");
    }

    @FXML
    private void handleLogout() {
        if (Dialogs.confirm("Sign out", "Are you sure you want to sign out?")) {
            Navigator.showLogin();
        }
    }
}
