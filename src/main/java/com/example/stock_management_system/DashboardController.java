package com.example.stock_management_system;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class DashboardController {

    public AnchorPane contentPane;
    @FXML private Button dashboardButton;
    @FXML private Button productsButton;
    @FXML private Button categoriesButton;
    @FXML private Button ordersButton;
    @FXML private Button suppliersButton;
    @FXML private Button usersButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;

    @FXML private VBox mainPanel;

    @FXML
    private void initialize() {
        openDashboard();
    }

    @FXML
    private void openDashboard() {
        loadPage("HomePage.fxml");
    }

    @FXML
    private void openProducts() {
        loadPage("Products.fxml");
    }

    @FXML
    private void openCategories() {
        loadPage("Categories.fxml");
    }

    @FXML
    private void openOrders() {
        loadPage("Orders.fxml");
    }

    @FXML
    private void openSuppliers() {
        loadPage("Suppliers.fxml");
    }

    @FXML
    private void openProfile() {
        loadPage("Profile.fxml");
    }

    @FXML
    private void logout() {
        System.out.println("Logout clicked");
    }

    private void loadPage(String fxml) {
        try {
            mainPanel.getChildren().clear();

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Node page = loader.load();

            mainPanel.getChildren().add(page);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
