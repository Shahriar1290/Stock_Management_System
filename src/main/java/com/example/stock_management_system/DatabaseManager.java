package com.example.stock_management_system;

import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL =
            "jdbc:sqlite:" + System.getProperty("user.home") + "/products.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {

        String productsTable = """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                quantity INTEGER NOT NULL
            );
            """;

        String ordersTable = """
            CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                total_price REAL NOT NULL,
                order_date TEXT
            );
            """;

        String categoriesTable = """
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(productsTable);
            stmt.execute(ordersTable);
            stmt.execute(categoriesTable);

            try {
                stmt.execute("ALTER TABLE orders ADD COLUMN order_date TEXT");
            } catch (SQLException ignored) {
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addProduct(Product product) {
        String sql = """
            INSERT INTO products (product_name, category, price, quantity)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getQuantity());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM products")) {

            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateProduct(Product product) {
        String sql = """
            UPDATE products
            SET product_name=?, category=?, price=?, quantity=?
            WHERE id=?
            """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getQuantity());
            ps.setInt(5, product.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteProduct(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps =
                     conn.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public boolean addCategory(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps =
                     conn.prepareStatement("INSERT INTO categories (name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs =
                     st.executeQuery("SELECT * FROM categories ORDER BY id")) {

            while (rs.next()) {
                list.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void deleteCategory(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps =
                     conn.prepareStatement("DELETE FROM categories WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addOrder(Order order) {
        String sql = """
        INSERT INTO orders (product_name, quantity, total_price, order_date)
        VALUES (?, ?, ?, date('now'))
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, order.getProductName());
            ps.setInt(2, order.getQuantity());
            ps.setDouble(3, order.getTotalPrice());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM orders")) {

            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_price")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public double getTotalRevenue() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs =
                     st.executeQuery("SELECT SUM(total_price) FROM orders")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getOrdersTodayCount() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs =
                     st.executeQuery(
                             "SELECT COUNT(*) FROM orders WHERE order_date = date('now')")) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean placeOrderAndReduceStock(String productName, int orderQty) {

        String selectProduct =
                "SELECT quantity, price FROM products WHERE product_name=?";
        String updateProduct =
                "UPDATE products SET quantity=? WHERE product_name=?";
        String insertOrder =
                "INSERT INTO orders (product_name, quantity, total_price, order_date) " +
                        "VALUES (?, ?, ?, date('now'))";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            conn.setAutoCommit(false);

            int currentQty;
            double price;

            try (PreparedStatement ps = conn.prepareStatement(selectProduct)) {
                ps.setString(1, productName);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                currentQty = rs.getInt("quantity");
                price = rs.getDouble("price");
            }

            if (currentQty < orderQty) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(updateProduct)) {
                ps.setInt(1, currentQty - orderQty);
                ps.setString(2, productName);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertOrder)) {
                ps.setString(1, productName);
                ps.setInt(2, orderQty);
                ps.setDouble(3, price * orderQty);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getHighestSaleProduct() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs =
                     st.executeQuery("""
                         SELECT product_name, SUM(quantity)
                         FROM orders
                         GROUP BY product_name
                         ORDER BY SUM(quantity) DESC
                         LIMIT 1
                     """)) {

            if (rs.next()) {
                return rs.getString(1) + " (" + rs.getInt(2) + " sold)";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No sales yet";
    }
}
