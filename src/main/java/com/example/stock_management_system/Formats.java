package com.example.stock_management_system;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Number/date formatting and table cell factories shared by the screens. */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private Formats() {
    }

    public static String money(double value) {
        return String.format("%,.2f", value);
    }

    public static String number(long value) {
        return String.format("%,d", value);
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    /** A cell factory that renders values with the given formatter. */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> cells(Function<T, String> formatter) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        };
    }

    /** Parses a non-negative integer, or returns -1 if the text is not one. */
    public static int parseCount(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value < 0 ? -1 : value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Parses a non-negative decimal, or returns -1 if the text is not one. */
    public static double parseAmount(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return value < 0 || Double.isNaN(value) || Double.isInfinite(value) ? -1 : value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
