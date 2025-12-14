package com.example.stock_management_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.example.stock_management_system.models.Category;

public class CategoryStore {

    private static final ObservableList<Category> categories =
            FXCollections.observableArrayList(
                    new Category(1, "Electronic"),
                    new Category(2, "Tech"),
                    new Category(3, "Furniture")
            );

    private CategoryStore() {}

    public static ObservableList<Category> getCategories() {
        return categories;
    }
}
