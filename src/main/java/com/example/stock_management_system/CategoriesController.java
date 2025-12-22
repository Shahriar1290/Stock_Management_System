package com.example.stock_management_system;

import com.example.stock_management_system.models.Category;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CategoriesController {

    @FXML
    private TextField txtCategoryName;

    @FXML
    private TableView<Category> categoryTable;

    @FXML
    private TableColumn<Category, Integer> colId;

    @FXML
    private TableColumn<Category, String> colName;

    private DatabaseManager db;

    @FXML
    public void initialize() {
        db = new DatabaseManager();

        colId.setCellValueFactory(cell ->
                cell.getValue().idProperty().asObject());

        colName.setCellValueFactory(cell ->
                cell.getValue().nameProperty());

        loadCategories();
    }

    private void loadCategories() {
        categoryTable.setItems(
                FXCollections.observableArrayList(
                        db.getAllCategories()
                )
        );
    }

    @FXML
    private void addCategory() {
        String name = txtCategoryName.getText().trim();

        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Validation Error",
                    "Category name cannot be empty");
            return;
        }

        boolean success = db.addCategory(name);

        if (!success) {
            showAlert(Alert.AlertType.ERROR,
                    "Database Error",
                    "Category already exists");
            return;
        }

        txtCategoryName.clear();
        loadCategories();
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable
                .getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING,
                    "No Selection",
                    "Please select a category to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Are you sure you want to delete this category?"
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        db.deleteCategory(selected.getId());
        loadCategories();
    }

    private void showAlert(Alert.AlertType type,
                           String title,
                           String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
