package com.example.stock_management_system;

import com.example.stock_management_system.models.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SupplierController {

    @FXML private Button btnAddSupplier;
    @FXML private Button btnEditSupplier;
    @FXML private Button btnDeleteSupplier;

    @FXML private TableView<Supplier> tableSuppliers;
    @FXML private TableColumn<Supplier, Integer> colSupplierId;
    @FXML private TableColumn<Supplier, String> colSupplierName;
    @FXML private TableColumn<Supplier, String> colSupplierContact;

    private final ObservableList<Supplier> suppliers =
            FXCollections.observableArrayList(
                    new Supplier(1, "ABC Traders", "017xxxxxxxx"),
                    new Supplier(2, "XYZ Supplies", "018xxxxxxxx")
            );

    private int supplierCounter = 3;

    @FXML
    private void initialize() {
        colSupplierId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        colSupplierName.setCellValueFactory(c -> c.getValue().nameProperty());
        colSupplierContact.setCellValueFactory(c -> c.getValue().contactProperty());

        tableSuppliers.setItems(suppliers);
    }

    @FXML
    private void addSupplier() {
        suppliers.add(new Supplier(
                supplierCounter++,
                "New Supplier",
                "019xxxxxxxx"
        ));
    }

    @FXML
    private void deleteSupplier() {
        Supplier selected = tableSuppliers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            suppliers.remove(selected);
        }
    }
}
