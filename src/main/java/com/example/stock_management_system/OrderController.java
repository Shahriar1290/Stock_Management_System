package com.example.stock_management_system;

import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class OrderController {

    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField quantityField;
    @FXML private Label totalPriceLabel;

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> productColumn;
    @FXML private TableColumn<Order, Integer> quantityColumn;
    @FXML private TableColumn<Order, Double> totalPriceColumn;

    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();
        productComboBox.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllProducts()
                )
        );

        productComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getProductName());
            }
        });
        productComboBox.setButtonCell(productComboBox.getCellFactory().call(null));

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
        Product product = productComboBox.getValue();
        if (product == null || quantityField.getText().isEmpty()) {
            totalPriceLabel.setText("0.0");
            return;
        }

        try {
            int qty = Integer.parseInt(quantityField.getText());
            double total = qty * product.getPrice();
            totalPriceLabel.setText(String.valueOf(total));
        } catch (NumberFormatException e) {
            totalPriceLabel.setText("0.0");
        }
    }

    @FXML
    private void addOrder() {
        Product product = productComboBox.getValue();
        if (product == null || quantityField.getText().isEmpty()) {
            showAlert("Validation Error", "Select product and enter quantity");
            return;
        }

        try {
            int qty = Integer.parseInt(quantityField.getText());
            double total = qty * product.getPrice();

            Order order = new Order(
                    product.getProductName(),
                    qty,
                    total
            );

            databaseManager.addOrder(order);
            loadOrders();

            quantityField.clear();
            totalPriceLabel.setText("0.0");

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Quantity must be a number");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
