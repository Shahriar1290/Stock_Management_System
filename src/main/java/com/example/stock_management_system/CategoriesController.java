package com.example.stock_management_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.stock_management_system.models.Category;

public class CategoriesController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TextField txtCategoryName;

    private final ObservableList<Category> categories =
            FXCollections.observableArrayList(
                    new Category(1, "Electronic"),
                    new Category(2, "Tech"),
                    new Category(3, "Furniture")
            );

    private int idCounter = 4;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        categoryTable.setItems(categories);
    }

    @FXML
    private void addCategory() {
        if (txtCategoryName.getText().isEmpty()) return;

        categories.add(new Category(idCounter++, txtCategoryName.getText()));
        txtCategoryName.clear();
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            categories.remove(selected);
        }
    }
}
