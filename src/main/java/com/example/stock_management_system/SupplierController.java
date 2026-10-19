package com.example.stock_management_system;

import com.example.stock_management_system.models.Supplier;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Locale;

public class SupplierController {

    @FXML private Label formTitle;
    @FXML private TextField txtSupplierName;
    @FXML private TextField txtSupplierContact;
    @FXML private Button addButton;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private TextField searchField;
    @FXML private Label summaryLabel;

    @FXML private TableView<Supplier> tableSuppliers;
    @FXML private TableColumn<Supplier, Integer> colSupplierId;
    @FXML private TableColumn<Supplier, String> colSupplierName;
    @FXML private TableColumn<Supplier, String> colSupplierContact;

    private final DatabaseManager db = DatabaseManager.getInstance();
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final FilteredList<Supplier> filtered = new FilteredList<>(suppliers);

    @FXML
    public void initialize() {
        colSupplierId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        colSupplierName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().name()));
        colSupplierContact.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().contact()));

        SortedList<Supplier> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableSuppliers.comparatorProperty());
        tableSuppliers.setItems(sorted);
        tableSuppliers.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showSelection(selected));

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        loadSuppliers();
        showSelection(null);
    }

    private void loadSuppliers() {
        suppliers.setAll(db.getAllSuppliers());
        applyFilter();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(s -> keyword.isEmpty()
                || s.name().toLowerCase(Locale.ROOT).contains(keyword)
                || s.contact().toLowerCase(Locale.ROOT).contains(keyword));
        summaryLabel.setText(filtered.size() + " of " + suppliers.size() + " suppliers");
    }

    private void showSelection(Supplier supplier) {
        boolean hasSelection = supplier != null;
        addButton.setDisable(hasSelection);
        saveButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        formTitle.setText(hasSelection ? "Edit supplier" : "New supplier");
        if (hasSelection) {
            txtSupplierName.setText(supplier.name());
            txtSupplierContact.setText(supplier.contact());
        }
    }

    private boolean formIsValid() {
        if (txtSupplierName.getText().isBlank() || txtSupplierContact.getText().isBlank()) {
            Dialogs.warn("Missing information", "Name and contact are required.");
            return false;
        }
        return true;
    }

    @FXML
    private void addSupplier() {
        if (!formIsValid()) return;
        db.addSupplier(txtSupplierName.getText().trim(), txtSupplierContact.getText().trim());
        clearFields();
        loadSuppliers();
    }

    @FXML
    private void editSupplier() {
        Supplier selected = tableSuppliers.getSelectionModel().getSelectedItem();
        if (selected == null || !formIsValid()) return;
        db.updateSupplier(selected.id(), txtSupplierName.getText().trim(), txtSupplierContact.getText().trim());
        clearFields();
        loadSuppliers();
    }

    @FXML
    private void deleteSupplier() {
        Supplier selected = tableSuppliers.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (Dialogs.confirm("Delete supplier", "Delete the supplier \"" + selected.name() + "\"?")) {
            db.deleteSupplier(selected.id());
            clearFields();
            loadSuppliers();
        }
    }

    @FXML
    private void clearFields() {
        tableSuppliers.getSelectionModel().clearSelection();
        txtSupplierName.clear();
        txtSupplierContact.clear();
    }

    @FXML
    private void exportCsv() {
        CsvExporter.export(tableSuppliers, "suppliers.csv");
    }
}
