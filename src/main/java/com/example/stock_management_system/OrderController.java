package com.example.stock_management_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.stock_management_system.models.Order;

import java.time.LocalDate;

public class OrderController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> colOrderId;
    @FXML private TableColumn<Order, String> colProduct;
    @FXML private TableColumn<Order, Integer> colQuantity;
    @FXML private TableColumn<Order, String> colDate;

    private final ObservableList<Order> orders =
            FXCollections.observableArrayList(
                    new Order(1, "Monitor", 1, "2025-03-01"),
                    new Order(2, "PC", 1, "2025-03-02")
            );

    private int orderCounter = 3;

    @FXML
    public void initialize() {
        colOrderId.setCellValueFactory(c -> c.getValue().orderIdProperty().asObject());
        colProduct.setCellValueFactory(c -> c.getValue().productProperty());
        colQuantity.setCellValueFactory(c -> c.getValue().quantityProperty().asObject());
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());
        orderTable.setItems(orders);
    }

    @FXML
    private void placeOrder() {
        orders.add(new Order(orderCounter++, "Keyboard", 1, LocalDate.now().toString()));
    }
}
