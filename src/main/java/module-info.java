module com.example.stock_management_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;

    opens com.example.stock_management_system to javafx.fxml;
    exports com.example.stock_management_system;
    exports com.example.stock_management_system.models;
}
