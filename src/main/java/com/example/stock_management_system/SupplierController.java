package com.example.stock_management_system;

import com.example.stock_management_system.models.Supplier;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SupplierController {

    @FXML private TextField txtSupplierId;
    @FXML private TextField txtSupplierName;
    @FXML private TextField txtSupplierContact;

    @FXML private TableView<Supplier> tableSuppliers;
    @FXML private TableColumn<Supplier, Integer> colSupplierId;
    @FXML private TableColumn<Supplier, String> colSupplierName;
    @FXML private TableColumn<Supplier, String> colSupplierContact;

    private DatabaseManager databaseManager;

    @FXML
    public void initialize() {
        databaseManager = new DatabaseManager();

        colSupplierId.setCellValueFactory(
                data -> new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getId()).asObject()
        );
        colSupplierName.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName())
        );
        colSupplierContact.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getContact())
        );

        tableSuppliers.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> populateFields(newVal));

        loadSuppliers();
    }

    private void loadSuppliers() {
        tableSuppliers.setItems(
                FXCollections.observableArrayList(
                        databaseManager.getAllSuppliers()
                )
        );
    }

    private void populateFields(Supplier supplier) {
        if (supplier != null) {
            txtSupplierId.setText(String.valueOf(supplier.getId()));
            txtSupplierName.setText(supplier.getName());
            txtSupplierContact.setText(supplier.getContact());
        }
    }

    @FXML
    private void addSupplier() {

        if (txtSupplierName.getText().isBlank()
                || txtSupplierContact.getText().isBlank()) {
            showAlert("Name and Contact are required");
            return;
        }

        databaseManager.addSupplier(
                txtSupplierName.getText(),
                txtSupplierContact.getText()
        );

        clearFields();
        loadSuppliers();
    }

    @FXML
    private void editSupplier() {

        if (txtSupplierId.getText().isBlank()) {
            showAlert("Select a supplier to edit");
            return;
        }

        databaseManager.updateSupplier(
                Integer.parseInt(txtSupplierId.getText()),
                txtSupplierName.getText(),
                txtSupplierContact.getText()
        );

        clearFields();
        loadSuppliers();
    }

    @FXML
    private void deleteSupplier() {

        if (txtSupplierId.getText().isBlank()) {
            showAlert("Select a supplier to delete");
            return;
        }

        databaseManager.deleteSupplier(
                Integer.parseInt(txtSupplierId.getText())
        );

        clearFields();
        loadSuppliers();
    }

    @FXML
    private void clearFields() {
        txtSupplierId.clear();
        txtSupplierName.clear();
        txtSupplierContact.clear();
        tableSuppliers.getSelectionModel().clearSelection();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
