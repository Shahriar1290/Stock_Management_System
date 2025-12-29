package com.example.stock_management_system;

import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.Category;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

public class ProductsController {

    @FXML private TextField productNameField;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private ComboBox<Category> categoryComboBox;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TextField searchField;


    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();

        categoryComboBox.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllCategories()
                )
        );

        categoryComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        categoryComboBox.setButtonCell(
                categoryComboBox.getCellFactory().call(null)
        );

        productNameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getProductName()));

        categoryColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCategory()));

        priceColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getPrice()));

        quantityColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getQuantity()));

        loadProducts();
    }
    @FXML
    private void searchProduct() {

        String keyword = searchField.getText();

        if (keyword == null || keyword.isBlank()) {
            productTable.setItems(
                    FXCollections.observableArrayList(
                            databaseManager.getAllProducts()
                    )
            );
            return;
        }

        productTable.setItems(
                FXCollections.observableArrayList(
                        databaseManager.searchProducts(keyword)
                )
        );
    }

    private void loadProducts() {
        productTable.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllProducts()
                )
        );
    }

    @FXML
    private void handleAddProduct() {

        if (productNameField.getText().isEmpty()
                || priceField.getText().isEmpty()
                || quantityField.getText().isEmpty()
                || categoryComboBox.getValue() == null) {

            showAlert("Validation Error", "Please fill all fields");
            return;
        }

        try {
            Product product = new Product(
                    productNameField.getText(),
                    categoryComboBox.getValue().getName(),
                    Double.parseDouble(priceField.getText()),
                    Integer.parseInt(quantityField.getText())
            );

            databaseManager.addProduct(product);
            loadProducts();
            clearFields();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Price and Quantity must be numbers");
        }
    }

    @FXML
    private void handleUpdateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a product to update");
            return;
        }

        try {
            selected.setProductName(productNameField.getText());
            selected.setCategory(categoryComboBox.getValue().getName());
            selected.setPrice(Double.parseDouble(priceField.getText()));
            selected.setQuantity(Integer.parseInt(quantityField.getText()));

            databaseManager.updateProduct(selected);
            loadProducts();
            clearFields();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Price and Quantity must be numbers");
        }
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Select a product to delete");
            return;
        }

        databaseManager.deleteProduct(selected.getId());
        loadProducts();
        clearFields();
    }

    @FXML
    private void handleSelectProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        productNameField.setText(selected.getProductName());
        priceField.setText(String.valueOf(selected.getPrice()));
        quantityField.setText(String.valueOf(selected.getQuantity()));

        categoryComboBox.getItems().stream()
                .filter(c -> c.getName().equals(selected.getCategory()))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);
    }

    private void clearFields() {
        productNameField.clear();
        priceField.clear();
        quantityField.clear();
        categoryComboBox.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
