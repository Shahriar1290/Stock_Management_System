package com.example.stock_management_system.models;

public record Product(int id,
                      String productName,
                      String category,
                      double price,
                      int quantity,
                      int reorderLevel) {

    public enum Status {
        IN_STOCK("In stock"), LOW_STOCK("Low stock"), OUT_OF_STOCK("Out of stock");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public Product(String productName, String category, double price, int quantity, int reorderLevel) {
        this(0, productName, category, price, quantity, reorderLevel);
    }

    public double stockValue() {
        return price * quantity;
    }

    public Status status() {
        if (quantity == 0) return Status.OUT_OF_STOCK;
        if (quantity <= reorderLevel) return Status.LOW_STOCK;
        return Status.IN_STOCK;
    }
}
