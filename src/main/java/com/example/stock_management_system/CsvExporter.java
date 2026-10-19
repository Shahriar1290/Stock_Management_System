package com.example.stock_management_system;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Exports the rows currently shown in a table (visible columns only) to a CSV file. */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static void export(TableView<?> table, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to CSV");
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        List<? extends TableColumn<?, ?>> columns = table.getVisibleLeafColumns();
        List<String> lines = new ArrayList<>();
        lines.add(columns.stream().map(c -> escape(c.getText())).collect(Collectors.joining(",")));
        for (int row = 0; row < table.getItems().size(); row++) {
            final int r = row;
            lines.add(columns.stream()
                    .map(c -> {
                        Object value = c.getCellData(r);
                        return escape(value == null ? "" : value.toString());
                    })
                    .collect(Collectors.joining(",")));
        }

        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            Dialogs.info("Export complete",
                    "Exported " + table.getItems().size() + " rows to\n" + file.getAbsolutePath());
        } catch (IOException e) {
            Dialogs.error("Export failed", e.getMessage());
        }
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
