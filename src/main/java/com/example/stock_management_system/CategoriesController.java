package com.example.stock_management_system;

import com.example.stock_management_system.models.Category;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Locale;

public class CategoriesController {

    @FXML private TextField txtCategoryName;
    @FXML private Button renameButton;
    @FXML private Button deleteButton;
    @FXML private TextField searchField;
    @FXML private Label summaryLabel;

    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, Integer> colProducts;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final FilteredList<Category> filtered = new FilteredList<>(categories);

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        colName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name()));
        colProducts.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productCount()));

        SortedList<Category> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sorted);

        var noSelection = categoryTable.getSelectionModel().selectedItemProperty().isNull();
        renameButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        loadCategories();
    }

    private void loadCategories() {
        categories.setAll(db.getAllCategories());
        applyFilter();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(c -> keyword.isEmpty() || c.name().toLowerCase(Locale.ROOT).contains(keyword));
        summaryLabel.setText(filtered.size() + " of " + categories.size() + " categories");
    }

    @FXML
    private void addCategory() {
        String name = txtCategoryName.getText().trim();
        if (name.isEmpty()) {
            Dialogs.warn("Missing name", "Category name cannot be empty.");
            return;
        }
        if (!db.addCategory(name)) {
            Dialogs.warn("Duplicate category", "A category called \"" + name + "\" already exists.");
            return;
        }
        txtCategoryName.clear();
        loadCategories();
    }

    @FXML
    private void renameCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Dialogs.prompt("Rename category", "New name for \"" + selected.name() + "\":", selected.name())
                .filter(name -> !name.equals(selected.name()))
                .ifPresent(name -> {
                    if (!db.renameCategory(selected, name)) {
                        Dialogs.warn("Rename failed", "A category called \"" + name + "\" may already exist.");
                    }
                    loadCategories();
                });
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (selected.productCount() > 0) {
            Dialogs.warn("Category in use", "\"" + selected.name() + "\" still has " + selected.productCount()
                    + " product(s). Move or delete them before deleting the category.");
            return;
        }
        if (Dialogs.confirm("Delete category", "Delete the category \"" + selected.name() + "\"?")) {
            db.deleteCategory(selected.id());
            loadCategories();
        }
    }
}
