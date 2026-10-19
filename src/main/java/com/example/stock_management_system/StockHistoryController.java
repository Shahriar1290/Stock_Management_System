package com.example.stock_management_system;

import com.example.stock_management_system.models.StockMovement;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

public class StockHistoryController {

    @FXML private TextField searchField;
    @FXML private ComboBox<StockMovement.Reason> reasonFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label summaryLabel;

    @FXML private TableView<StockMovement> historyTable;
    @FXML private TableColumn<StockMovement, LocalDateTime> timeColumn;
    @FXML private TableColumn<StockMovement, String> productColumn;
    @FXML private TableColumn<StockMovement, Integer> changeColumn;
    @FXML private TableColumn<StockMovement, Integer> afterColumn;
    @FXML private TableColumn<StockMovement, StockMovement.Reason> reasonColumn;
    @FXML private TableColumn<StockMovement, String> userColumn;
    @FXML private TableColumn<StockMovement, String> noteColumn;

    private final ObservableList<StockMovement> movements = FXCollections.observableArrayList();
    private final FilteredList<StockMovement> filtered = new FilteredList<>(movements);

    @FXML
    public void initialize() {
        timeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().time()));
        timeColumn.setCellFactory(Formats.cells(Formats::dateTime));
        productColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().productName()));
        changeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().change()));
        changeColumn.setCellFactory(column -> new ChangeCell());
        afterColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().quantityAfter()));
        reasonColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().reason()));
        userColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(orDash(c.getValue().username())));
        noteColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(orDash(c.getValue().note())));

        // A null value stands for "all activity".
        reasonFilter.getItems().add(null);
        reasonFilter.getItems().addAll(StockMovement.Reason.values());
        reasonFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StockMovement.Reason reason) {
                return reason == null ? "All activity" : reason.toString();
            }

            @Override
            public StockMovement.Reason fromString(String s) {
                return null;
            }
        });
        reasonFilter.setValue(null);

        SortedList<StockMovement> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(historyTable.comparatorProperty());
        historyTable.setItems(sorted);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        reasonFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        fromDate.valueProperty().addListener((obs, o, n) -> applyFilter());
        toDate.valueProperty().addListener((obs, o, n) -> applyFilter());

        movements.setAll(DatabaseManager.getInstance().getStockMovements());
        applyFilter();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        StockMovement.Reason reason = reasonFilter.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        filtered.setPredicate(m -> {
            LocalDate day = m.time().toLocalDate();
            return (keyword.isEmpty() || contains(m.productName(), keyword)
                    || contains(m.username(), keyword) || contains(m.note(), keyword))
                    && (reason == null || m.reason() == reason)
                    && (from == null || !day.isBefore(from))
                    && (to == null || !day.isAfter(to));
        });

        int in = filtered.stream().mapToInt(StockMovement::change).filter(c -> c > 0).sum();
        int out = -filtered.stream().mapToInt(StockMovement::change).filter(c -> c < 0).sum();
        summaryLabel.setText(filtered.size() + " entries  •  +" + Formats.number(in) + " in, -"
                + Formats.number(out) + " out");
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @FXML
    private void clearFilters() {
        searchField.clear();
        reasonFilter.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
    }

    @FXML
    private void exportCsv() {
        CsvExporter.export(historyTable, "stock-history.csv");
    }

    /** Shows stock changes with a sign, green for stock in and red for stock out. */
    private static final class ChangeCell extends TableCell<StockMovement, Integer> {
        @Override
        protected void updateItem(Integer change, boolean empty) {
            super.updateItem(change, empty);
            getStyleClass().removeAll("change-in", "change-out");
            if (empty || change == null) {
                setText(null);
                return;
            }
            setText(change > 0 ? "+" + change : String.valueOf(change));
            if (change != 0) getStyleClass().add(change > 0 ? "change-in" : "change-out");
        }
    }
}
