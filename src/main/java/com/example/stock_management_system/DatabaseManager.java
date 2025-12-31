package com.example.stock_management_system;

import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Category;
import com.example.stock_management_system.models.User;
import com.example.stock_management_system.models.Supplier;


import java.sql.*;
import java.time.LocalDate;
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

        String usersTable = """
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        role TEXT DEFAULT 'ADMIN',
        password TEXT NOT NULL
    );
    """;

        String suppliersTable = """
    CREATE TABLE IF NOT EXISTS suppliers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        contact TEXT NOT NULL
    );
    """;




        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute(productsTable);
            stmt.execute(ordersTable);
            stmt.execute(categoriesTable);
            stmt.execute(usersTable);
            stmt.execute(suppliersTable);


            try {
                stmt.execute("ALTER TABLE orders ADD COLUMN order_date TEXT");
            } catch (SQLException ignored) {}

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

    public List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();

        String sql = """
            SELECT * FROM products
            WHERE LOWER(product_name) LIKE LOWER(?)
            ORDER BY product_name
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

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
             ResultSet rs = st.executeQuery("SELECT * FROM categories ORDER BY id")) {

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


    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM orders")) {

            while (rs.next()) {
                LocalDate orderDate = LocalDate.parse(rs.getString("order_date"));

                list.add(new Order(
                        rs.getInt("id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("total_price"),
                        orderDate
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
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

    public double getTotalRevenue() {
        String sql = """
        SELECT SUM(quantity * total_price )
        FROM orders
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getDouble(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0.0;
    }


    public int getOrdersTodayCount() {
        String sql = """
            SELECT COUNT(*)
            FROM orders
            WHERE DATE(order_date) = DATE('now')
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
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


    public boolean validateUser(String username, String password) {

        String sql = """
            SELECT 1 FROM users
            WHERE username = ? AND password = ?
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hashPassword(password));

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public User getUserByUsername(String username) {

        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getString("username"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
    public void updateUserProfile(String username,
                                  String name,
                                  String email,
                                  String hashedPassword) {

        String sql;

        if (hashedPassword == null) {
            sql = """
            UPDATE users
            SET name = ?, email = ?
            WHERE username = ?
            """;
        } else {
            sql = """
            UPDATE users
            SET name = ?, email = ?, password = ?
            WHERE username = ?
            """;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);

            if (hashedPassword == null) {
                ps.setString(3, username);
            } else {
                ps.setString(3, hashedPassword);
                ps.setString(4, username);
            }

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public List<Supplier> getAllSuppliers() {
        List<Supplier> list = new ArrayList<>();

        String sql = "SELECT * FROM suppliers";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Supplier(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("contact")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public void addSupplier(String name, String contact) {
        String sql = "INSERT INTO suppliers (name, contact) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, contact);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateSupplier(int id, String name, String contact) {
        String sql = "UPDATE suppliers SET name=?, contact=? WHERE id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setInt(3, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSupplier(int id) {
        String sql = "DELETE FROM suppliers WHERE id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
