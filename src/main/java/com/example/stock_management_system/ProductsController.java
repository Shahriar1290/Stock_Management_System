package com.example.stock_management_system;

import com.example.stock_management_system.models.Category;
import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.Supplier;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Locale;

public class ProductsController {

    private static final String ALL_CATEGORIES = "All categories";
    private static final Supplier NO_SUPPLIER = new Supplier(0, "No supplier", "", 0);

    @FXML private Label formTitle;
    @FXML private TextField productNameField;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private ComboBox<Supplier> supplierComboBox;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private TextField reorderField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button restockButton;
    @FXML private Button deleteButton;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockOnly;
    @FXML private Label summaryLabel;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> supplierColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Integer> reorderColumn;
    @FXML private TableColumn<Product, Double> valueColumn;
    @FXML private TableColumn<Product, Product.Status> statusColumn;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(products);

    @FXML
    public void initialize() {
        productNameColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productName()));
        categoryColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().category()));
        supplierColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().supplierName() == null ? "-" : c.getValue().supplierName()));
        priceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().price()));
        priceColumn.setCellFactory(Formats.cells(Formats::money));
        quantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        reorderColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().reorderLevel()));
        valueColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().stockValue()));
        valueColumn.setCellFactory(Formats.cells(Formats::money));
        statusColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().status()));
        statusColumn.setCellFactory(column -> new StatusCell());

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sorted);
        productTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showSelection(selected));

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        lowStockOnly.selectedProperty().addListener((obs, o, n) -> applyFilter());

        // Staff can edit and restock, but only admins can delete products.
        deleteButton.setVisible(Session.isAdmin());
        deleteButton.setManaged(Session.isAdmin());

        reload();
        showSelection(null);
    }

    private void reload() {
        List<Category> categories = db.getAllCategories();
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        ObservableList<Supplier> suppliers = FXCollections.observableArrayList(NO_SUPPLIER);
        suppliers.addAll(db.getAllSuppliers());
        supplierComboBox.setItems(suppliers);
        if (supplierComboBox.getValue() == null) supplierComboBox.setValue(NO_SUPPLIER);

        String currentFilter = categoryFilter.getValue();
        ObservableList<String> filterItems = FXCollections.observableArrayList(ALL_CATEGORIES);
        categories.forEach(c -> filterItems.add(c.name()));
        categoryFilter.setItems(filterItems);
        categoryFilter.setValue(filterItems.contains(currentFilter) ? currentFilter : ALL_CATEGORIES);

        products.setAll(db.getAllProducts());
        applyFilter();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        boolean lowOnly = lowStockOnly.isSelected();

        filtered.setPredicate(p ->
                (keyword.isEmpty() || p.productName().toLowerCase(Locale.ROOT).contains(keyword))
                        && (category == null || ALL_CATEGORIES.equals(category) || category.equals(p.category()))
                        && (!lowOnly || p.status() != Product.Status.IN_STOCK));

        double value = filtered.stream().mapToDouble(Product::stockValue).sum();
        summaryLabel.setText(filtered.size() + " of " + products.size() + " products  •  value " + Formats.money(value));
    }

    private void showSelection(Product selected) {
        boolean hasSelection = selected != null;
        updateButton.setDisable(!hasSelection);
        restockButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        addButton.setDisable(hasSelection);
        formTitle.setText(hasSelection ? "Edit product" : "New product");
        if (!hasSelection) return;

        productNameField.setText(selected.productName());
        priceField.setText(String.valueOf(selected.price()));
        quantityField.setText(String.valueOf(selected.quantity()));
        reorderField.setText(String.valueOf(selected.reorderLevel()));
        categoryComboBox.getItems().stream()
                .filter(c -> c.name().equals(selected.category()))
                .findFirst()
                .ifPresentOrElse(categoryComboBox::setValue, () -> categoryComboBox.setValue(null));
        supplierComboBox.setValue(supplierComboBox.getItems().stream()
                .filter(s -> s.id() == selected.supplierId())
                .findFirst()
                .orElse(NO_SUPPLIER));
    }

    /** Validates the form; returns the product it describes (with the given id) or null. */
    private Product readForm(int id) {
        String name = productNameField.getText().trim();
        Category category = categoryComboBox.getValue();
        if (name.isEmpty() || category == null
                || priceField.getText().isBlank() || quantityField.getText().isBlank()) {
            Dialogs.warn("Missing information", "Please enter a name, category, price and quantity.");
            return null;
        }

        double price = Formats.parseAmount(priceField.getText());
        int quantity = Formats.parseCount(quantityField.getText());
        int reorder = reorderField.getText().isBlank()
                ? DatabaseManager.DEFAULT_REORDER_LEVEL
                : Formats.parseCount(reorderField.getText());
        if (price < 0 || quantity < 0 || reorder < 0) {
            Dialogs.warn("Invalid number",
                    "Price must be a positive number, and quantity and reorder level whole numbers of 0 or more.");
            return null;
        }

        boolean duplicate = products.stream()
                .anyMatch(p -> p.id() != id && p.productName().equalsIgnoreCase(name));
        if (duplicate) {
            Dialogs.warn("Duplicate product", "A product called \"" + name + "\" already exists.");
            return null;
        }
        Supplier supplier = supplierComboBox.getValue();
        return new Product(id, name, category.name(), price, quantity, reorder,
                supplier == null ? 0 : supplier.id(), null);
    }

    @FXML
    private void handleAddProduct() {
        Product product = readForm(0);
        if (product == null) return;
        if (!db.addProduct(product)) {
            Dialogs.error("Error", "The product could not be saved.");
            return;
        }
        clearForm();
        reload();
    }

    @FXML
    private void handleUpdateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Product product = readForm(selected.id());
        if (product == null) return;
        if (!db.updateProduct(product)) {
            Dialogs.error("Error", "The product could not be updated.");
            return;
        }
        reload();
        selectById(product.id());
    }

    @FXML
    private void handleRestock() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Dialogs.prompt("Restock " + selected.productName(),
                        "Units received (currently " + selected.quantity() + " in stock):", "")
                .ifPresent(text -> {
                    int amount = Formats.parseCount(text);
                    if (amount <= 0) {
                        Dialogs.warn("Invalid quantity", "Enter a whole number greater than 0.");
                        return;
                    }
                    db.restockProduct(selected.id(), amount, null);
                    reload();
                    selectById(selected.id());
                });
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!Dialogs.confirm("Delete product",
                "Delete \"" + selected.productName() + "\"? Its order and stock history are kept.")) {
            return;
        }
        db.deleteProduct(selected);
        clearForm();
        reload();
    }

    @FXML
    private void clearForm() {
        productTable.getSelectionModel().clearSelection();
        productNameField.clear();
        priceField.clear();
        quantityField.clear();
        reorderField.clear();
        categoryComboBox.setValue(null);
        supplierComboBox.setValue(NO_SUPPLIER);
    }

    @FXML
    private void exportCsv() {
        CsvExporter.export(productTable, "products.csv");
    }

    private void selectById(int id) {
        productTable.getItems().stream()
                .filter(p -> p.id() == id)
                .findFirst()
                .ifPresent(p -> productTable.getSelectionModel().select(p));
    }

    /** Renders a product's stock status as a coloured pill. */
    private static final class StatusCell extends TableCell<Product, Product.Status> {
        private final Label pill = new Label();

        StatusCell() {
            getStyleClass().add("status-cell");
            pill.getStyleClass().add("pill");
        }

        @Override
        protected void updateItem(Product.Status status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                return;
            }
            pill.setText(status.toString());
            pill.getStyleClass().removeAll("in-stock", "low-stock", "out-of-stock");
            pill.getStyleClass().add(status.name().toLowerCase(Locale.ROOT).replace('_', '-'));
            setGraphic(pill);
        }
    }
}
