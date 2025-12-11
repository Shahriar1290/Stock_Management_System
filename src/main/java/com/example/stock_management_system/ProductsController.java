package com.example.stock_management_system;

import com.example.stock_management_system.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProductsController {

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Integer> colStock;

    private final ObservableList<Product> products =
            FXCollections.observableArrayList(
                    new Product(1, "Monitor", "Electronics", 3),
                    new Product(2, "PC", "Tech", 3),
                    new Product(3, "RAM", "Tech", 2)
            );

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colCategory.setCellValueFactory(c -> c.getValue().categoryProperty());
        colStock.setCellValueFactory(c -> c.getValue().stockProperty().asObject());

        productTable.setItems(products);
    }

    @FXML
    private void addProduct() {
        products.add(new Product(4, "Keyboard", "Tech", 5));
    }
}
