package com.example.stock_management_system;

import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class OrderController {

    @FXML
    private ComboBox<String> productComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private Label totalPriceLabel;

    @FXML
    private Button addOrderButton;

    @FXML
    private TableView<Order> orderTable;

    @FXML
    private TableColumn<Order, String> productColumn;

    @FXML
    private TableColumn<Order, Integer> quantityColumn;

    @FXML
    private TableColumn<Order, Double> totalPriceColumn;

    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();
        refreshProductsComboBox();
        addOrderButton.setDisable(true);
        productComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                addOrderButton.setDisable(newVal == null)
        );

        productColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProductName()));

        quantityColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getQuantity()));

        totalPriceColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getTotalPrice()));

        loadOrders();
    }

    private void loadOrders() {
        orderTable.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllOrders()
                )
        );
    }

    @FXML
    private void calculateTotal() {

        if (productComboBox.getValue() == null ||
                quantityField.getText().isEmpty()) {
            totalPriceLabel.setText("0.0");
            addOrderButton.setDisable(true);
            return;
        }

        try {
            int qty = Integer.parseInt(quantityField.getText());

            Product product = databaseManager.getAllProducts()
                    .stream()
                    .filter(p -> p.getProductName()
                            .equals(productComboBox.getValue()))
                    .findFirst()
                    .orElse(null);

            if (product == null || qty <= 0) {
                totalPriceLabel.setText("0.0");
                addOrderButton.setDisable(true);
                return;
            }

            if (qty > product.getQuantity()) {
                totalPriceLabel.setText("Insufficient Stock");
                addOrderButton.setDisable(true);
                return;
            }

            double total = qty * product.getPrice();
            totalPriceLabel.setText(String.valueOf(total));
            addOrderButton.setDisable(false);

        } catch (NumberFormatException e) {
            totalPriceLabel.setText("0.0");
            addOrderButton.setDisable(true);
        }
    }

    @FXML
    private void addOrder() {

        try {
            String productName = productComboBox.getValue();
            int qty = Integer.parseInt(quantityField.getText());

            boolean success =
                    databaseManager.placeOrderAndReduceStock(
                            productName, qty);

            if (!success) {
                showAlert("Stock Error",
                        "Not enough stock available");
                return;
            }

            loadOrders();
            refreshProductsComboBox();
            quantityField.clear();
            totalPriceLabel.setText("0.0");
            addOrderButton.setDisable(true);

        } catch (NumberFormatException e) {
            showAlert("Invalid Input",
                    "Quantity must be a number");
        }
    }

    private void refreshProductsComboBox() {
        productComboBox.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllProducts()
                                .stream()
                                .filter(p -> p.getQuantity() > 0)
                                .map(Product::getProductName)
                                .toList()
                )
        );
        productComboBox.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
