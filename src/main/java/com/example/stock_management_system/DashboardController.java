package com.example.stock_management_system;

import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DashboardController {

    private static final int RECENT_ORDER_COUNT = 8;
    private static final int TOP_PRODUCT_COUNT = 5;
    private static final int REVENUE_DAYS = 7;

    @FXML private Label greetingLabel;
    @FXML private HBox statsRow;
    @FXML private AreaChart<String, Number> revenueChart;
    @FXML private BarChart<Number, String> topProductsChart;
    @FXML private Label alertCountLabel;
    @FXML private ListView<Product> stockAlertList;
    @FXML private TableView<Order> recentOrdersTable;
    @FXML private TableColumn<Order, String> recentProductColumn;
    @FXML private TableColumn<Order, Integer> recentQuantityColumn;
    @FXML private TableColumn<Order, Double> recentTotalColumn;
    @FXML private TableColumn<Order, LocalDate> recentDateColumn;

    private final DatabaseManager db = DatabaseManager.getInstance();

    private Label productsValue;
    private Label productsHint;
    private Label unitsValue;
    private Label unitsHint;
    private Label stockValue;
    private Label ordersTodayValue;
    private Label revenueValue;

    @FXML
    public void initialize() {
        greetingLabel.setText(greeting());
        buildStatCards();

        recentProductColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productName()));
        recentQuantityColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantity()));
        recentTotalColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().totalPrice()));
        recentTotalColumn.setCellFactory(Formats.cells(Formats::money));
        recentDateColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().orderDate()));
        recentDateColumn.setCellFactory(Formats.cells(Formats::date));

        stockAlertList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                getStyleClass().removeAll("alert-item", "low-stock", "out-of-stock");
                if (empty || p == null) {
                    setText(null);
                    return;
                }
                boolean out = p.status() == Product.Status.OUT_OF_STOCK;
                setText(p.productName() + (out
                        ? "  -  out of stock"
                        : "  -  " + p.quantity() + " left (reorder at " + p.reorderLevel() + ")"));
                getStyleClass().addAll("alert-item", out ? "out-of-stock" : "low-stock");
            }
        });

        refresh();
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        User user = Session.getUser();
        if (user == null || user.name() == null || user.name().isBlank()) return part;
        return part + ", " + user.name().trim().split("\\s+")[0];
    }

    private void buildStatCards() {
        productsValue = new Label();
        productsHint = new Label();
        unitsValue = new Label();
        unitsHint = new Label();
        stockValue = new Label();
        ordersTodayValue = new Label();
        revenueValue = new Label();

        statsRow.getChildren().setAll(
                statCard("Products", "tone-indigo", "icon-box", productsValue, productsHint),
                statCard("Units in stock", "tone-green", "icon-layers", unitsValue, unitsHint),
                statCard("Inventory value", "tone-sky", "icon-money", stockValue, new Label("At current prices")),
                statCard("Orders today", "tone-amber", "icon-cart", ordersTodayValue, new Label("Placed since midnight")),
                statCard("Total revenue", "tone-violet", "icon-money", revenueValue, new Label("All time")));
    }

    private static VBox statCard(String title, String tone, String icon, Label value, Label hint) {
        Region glyph = new Region();
        glyph.getStyleClass().addAll("icon", icon);
        StackPane badge = new StackPane(glyph);
        badge.getStyleClass().addAll("stat-icon", tone);

        Label label = new Label(title);
        label.getStyleClass().add("stat-label");
        HBox header = new HBox(badge, label);
        header.getStyleClass().add("stat-header");

        value.getStyleClass().add("stat-value");
        hint.getStyleClass().add("stat-hint");

        VBox card = new VBox(header, value, hint);
        card.getStyleClass().add("stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    @FXML
    private void refresh() {
        List<Product> products = db.getAllProducts();
        List<Order> orders = db.getAllOrders();

        List<Product> alerts = products.stream()
                .filter(p -> p.status() != Product.Status.IN_STOCK)
                .sorted(Comparator.comparingInt(Product::quantity))
                .toList();
        long outOfStock = alerts.stream().filter(p -> p.status() == Product.Status.OUT_OF_STOCK).count();

        productsValue.setText(Formats.number(products.size()));
        productsHint.setText(db.getAllCategories().size() + " categories");
        unitsValue.setText(Formats.number(products.stream().mapToLong(Product::quantity).sum()));
        unitsHint.setText(outOfStock + " out of stock, " + (alerts.size() - outOfStock) + " running low");
        stockValue.setText(Formats.money(products.stream().mapToDouble(Product::stockValue).sum()));
        ordersTodayValue.setText(Formats.number(db.getOrderCountOn(LocalDate.now())));
        revenueValue.setText(Formats.money(db.getTotalRevenue()));

        stockAlertList.setItems(FXCollections.observableArrayList(alerts));
        alertCountLabel.setText(alerts.isEmpty() ? "" : alerts.size() + " need attention");
        recentOrdersTable.setItems(FXCollections.observableArrayList(
                orders.subList(0, Math.min(RECENT_ORDER_COUNT, orders.size()))));

        loadRevenueChart();
        loadTopProductsChart();
    }

    private void loadRevenueChart() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEE d");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        db.getDailyRevenue(today.minusDays(REVENUE_DAYS - 1), today).forEach((day, revenue) ->
                series.getData().add(new XYChart.Data<>(day.format(dayFormat), revenue)));
        revenueChart.getData().setAll(List.of(series));
    }

    private void loadTopProductsChart() {
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        // Horizontal bars are drawn bottom-up, so add the best seller last to show it on top.
        for (Map.Entry<String, Integer> e : db.getTopSellingProducts(TOP_PRODUCT_COUNT).entrySet()) {
            series.getData().addFirst(new XYChart.Data<>(e.getValue(), e.getKey()));
        }
        topProductsChart.getData().setAll(List.of(series));
    }
}
