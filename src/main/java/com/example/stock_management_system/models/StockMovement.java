package com.example.stock_management_system.models;

import java.time.LocalDateTime;

/** One change to a product's stock level, kept as an audit trail. */
public record StockMovement(int id,
                            LocalDateTime time,
                            String productName,
                            int change,
                            int quantityAfter,
                            Reason reason,
                            String note,
                            String username) {

    public enum Reason {
        ADDED("Product added"),
        RESTOCK("Restock"),
        SALE("Sale"),
        CANCELLED("Order cancelled"),
        ADJUSTMENT("Manual adjustment"),
        DELETED("Product deleted");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
