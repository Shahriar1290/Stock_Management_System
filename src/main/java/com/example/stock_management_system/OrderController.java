package com.example.stock_management_system;

import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.Locale;

public class OrderController {

    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField quantityField;
    @FXML private Label totalPriceLabel;
    @FXML private Label stockHintLabel;
    @FXML private Button addOrderButton;

    @FXML private TextField searchField;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label summaryLabel;
    @FXML private Button cancelOrderButton;

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> productColumn;
    @FXML private TableColumn<Order, Integer> quantityColumn;
    @FXML private TableColumn<Order, Double> totalPriceColumn;
    @FXML private TableColumn<Order, LocalDate> dateColumn;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final FilteredList<Order> filtered = new FilteredList<>(orders);

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        productColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productName()));
        quantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        totalPriceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().totalPrice()));
        totalPriceColumn.setCellFactory(Formats.cells(Formats::money));
        dateColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().orderDate()));
        dateColumn.setCellFactory(Formats.cells(Formats::date));

        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : p.productName() + "  (" + p.quantity() + " in stock)";
            }

            @Override
            public Product fromString(String s) {
                return null;
            }
        });
        productComboBox.valueProperty().addListener((obs, o, n) -> calculateTotal());
        quantityField.textProperty().addListener((obs, o, n) -> calculateTotal());

        SortedList<Order> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sorted);
        cancelOrderButton.disableProperty().bind(orderTable.getSelectionModel().selectedItemProperty().isNull());

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        fromDate.valueProperty().addListener((obs, o, n) -> applyFilter());
        toDate.valueProperty().addListener((obs, o, n) -> applyFilter());

        reload();
    }

    private void reload() {
        productComboBox.setItems(FXCollections.observableArrayList(
                db.getAllProducts().stream().filter(p -> p.quantity() > 0).toList()));
        productComboBox.setValue(null);
        quantityField.clear();
        orders.setAll(db.getAllOrders());
        applyFilter();
        calculateTotal();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        filtered.setPredicate(o ->
                (keyword.isEmpty() || o.productName().toLowerCase(Locale.ROOT).contains(keyword))
                        && (from == null || (o.orderDate() != null && !o.orderDate().isBefore(from)))
                        && (to == null || (o.orderDate() != null && !o.orderDate().isAfter(to))));

        double revenue = filtered.stream().mapToDouble(Order::totalPrice).sum();
        summaryLabel.setText(filtered.size() + " orders  •  revenue " + Formats.money(revenue));
    }

    private void calculateTotal() {
        Product product = productComboBox.getValue();
        int qty = Formats.parseCount(quantityField.getText() == null ? "" : quantityField.getText());
        totalPriceLabel.getStyleClass().remove("error");
        stockHintLabel.setText(product == null ? "Only products with stock are listed."
                : product.quantity() + " units available at " + Formats.money(product.price()) + " each.");

        if (product == null || qty <= 0) {
            totalPriceLabel.setText("0.00");
            addOrderButton.setDisable(true);
        } else if (qty > product.quantity()) {
            totalPriceLabel.setText("Only " + product.quantity() + " in stock");
            totalPriceLabel.getStyleClass().add("error");
            addOrderButton.setDisable(true);
        } else {
            totalPriceLabel.setText(Formats.money(qty * product.price()));
            addOrderButton.setDisable(false);
        }
    }

    @FXML
    private void addOrder() {
        Product product = productComboBox.getValue();
        int qty = Formats.parseCount(quantityField.getText());
        if (product == null || qty <= 0) return;

        if (!db.placeOrder(product.productName(), qty)) {
            Dialogs.warn("Stock error", "Not enough stock available for this order.");
        }
        reload();
    }

    @FXML
    private void cancelOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!Dialogs.confirm("Cancel order",
                "Cancel order #" + selected.id() + " and return " + selected.quantity()
                        + " units of \"" + selected.productName() + "\" to stock?")) {
            return;
        }
        if (!db.cancelOrder(selected)) {
            Dialogs.error("Error", "The order could not be cancelled.");
        }
        reload();
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    @FXML
    private void exportCsv() {
        CsvExporter.export(orderTable, "orders.csv");
    }
}
