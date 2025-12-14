package com.example.stock_management_system;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.stock_management_system.models.Category;

public class CategoriesController {

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TextField txtCategoryName;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        categoryTable.setItems(CategoryStore.getCategories());
    }

    @FXML
    private void addCategory() {
        if (txtCategoryName.getText().isEmpty()) return;

        CategoryStore.getCategories().add(
                new Category(
                        CategoryStore.getCategories().size() + 1,
                        txtCategoryName.getText()
                )
        );
        txtCategoryName.clear();
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            CategoryStore.getCategories().remove(selected);
        }
    }
}
