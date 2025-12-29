package com.example.stock_management_system;

import com.example.stock_management_system.models.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

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

    @FXML private ListView<String> outOfStockList;
    @FXML private ListView<String> lowStockList;
    @FXML private Label highestSaleLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label totalStockLabel;
    @FXML private Label revenueLabel;
    @FXML private Label ordersTodayLabel;

    private DatabaseManager databaseManager;
    private static final int LOW_STOCK_THRESHOLD = 3;

    private List<Node> dashboardNodes;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();
        dashboardNodes = new ArrayList<>(mainPanel.getChildren());
        dashboardButton.setDisable(true);
        loadDashboardData();
    }

    @FXML
    private void openDashboard() {
        dashboardButton.setDisable(true);
        mainPanel.getChildren().setAll(dashboardNodes);
        loadDashboardData();
    }

    private void loadDashboardData() {

        List<Product> products = databaseManager.getAllProducts();

        totalProductsLabel.setText(String.valueOf(products.size()));

        int totalStock = products.stream()
                .mapToInt(Product::getQuantity)
                .sum();
        totalStockLabel.setText(String.valueOf(totalStock));

        ordersTodayLabel.setText(
                String.valueOf(databaseManager.getOrdersTodayCount())
        );

        outOfStockList.setItems(
                FXCollections.observableArrayList(
                        products.stream()
                                .filter(p -> p.getQuantity() == 0)
                                .map(Product::getProductName)
                                .toList()
                )
        );

        lowStockList.setItems(
                FXCollections.observableArrayList(
                        products.stream()
                                .filter(p -> p.getQuantity() > 0 &&
                                        p.getQuantity() < LOW_STOCK_THRESHOLD)
                                .map(p -> p.getProductName()
                                        + " (Only " + p.getQuantity() + ")")
                                .toList()
                )
        );

        highestSaleLabel.setText(
                databaseManager.getHighestSaleProduct()
        );

        revenueLabel.setText(
                String.format("%.2f", databaseManager.getTotalRevenue())
        );
    }


    @FXML
    private void openProducts() {
        dashboardButton.setDisable(false);
        loadPage("Product.fxml");
    }

    @FXML
    private void openCategories() {
        dashboardButton.setDisable(false);
        loadPage("Categories.fxml");
    }

    @FXML
    private void openOrders() {
        dashboardButton.setDisable(false);
        loadPage("Orders.fxml");
    }

    @FXML
    private void openSuppliers() {
        dashboardButton.setDisable(false);
        loadPage("Supplier.fxml");
    }

    @FXML
    private void openProfile() {
        System.out.println("Opening Profile Page...");

        dashboardButton.setDisable(false);
        loadPage("Profile.fxml");
    }




    private void loadPage(String fxml) {
        try {
            mainPanel.getChildren().clear();
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(
                            "/com/example/stock_management_system/" + fxml));
            Node page = loader.load();
            mainPanel.getChildren().add(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(
                            "/com/example/stock_management_system/LoginPage.fxml"
                    ));

            Scene loginScene = new Scene(loader.load());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Stock Management System - Login");
            stage.setScene(loginScene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
