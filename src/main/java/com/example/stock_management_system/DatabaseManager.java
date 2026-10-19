package com.example.stock_management_system;

import com.example.stock_management_system.models.Category;
import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.Supplier;
import com.example.stock_management_system.models.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Single access point to the SQLite database. The schema is created and migrated once,
 * the first time {@link #getInstance()} is called.
 */
public final class DatabaseManager {

    public static final int DEFAULT_REORDER_LEVEL = 5;

    private static final System.Logger LOG = System.getLogger(DatabaseManager.class.getName());

    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty(
            "stock.db.path", System.getProperty("user.home") + "/products.db");

    private static final class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    private interface TransactionWork {
        boolean run(Connection conn) throws SQLException;
    }

    private DatabaseManager() {
        initializeDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        reorder_level INTEGER NOT NULL DEFAULT 5
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        total_price REAL NOT NULL,
                        order_date TEXT
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT UNIQUE NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        role TEXT DEFAULT 'ADMIN',
                        password TEXT NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        contact TEXT NOT NULL
                    )""");

            // Migrations for databases created by older versions of the app
            addColumnIfMissing(conn, "orders", "order_date", "TEXT");
            addColumnIfMissing(conn, "products", "reorder_level",
                    "INTEGER NOT NULL DEFAULT " + DEFAULT_REORDER_LEVEL);
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.ERROR, "Could not initialise database", e);
        }
    }

    private void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) return;
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.ERROR, "Query failed: " + sql, e);
        }
        return list;
    }

    private <T> T queryOne(String sql, RowMapper<T> mapper, T fallback, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() || rows.getFirst() == null ? fallback : rows.getFirst();
    }

    /** Runs an INSERT/UPDATE/DELETE and returns whether it succeeded. */
    private boolean update(String sql, Object... params) {
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.WARNING, "Update failed: " + sql, e);
            return false;
        }
    }

    /** Runs work in a transaction; it is committed only when the work returns true. */
    private boolean inTransaction(TransactionWork work) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                if (work.run(conn)) {
                    conn.commit();
                    return true;
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.ERROR, "Transaction failed", e);
            return false;
        }
    }

    private static int executeUpdate(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("quantity"),
                rs.getInt("reorder_level"));
    }

    private static Order mapOrder(ResultSet rs) throws SQLException {
        String date = rs.getString("order_date");
        return new Order(
                rs.getInt("id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                rs.getDouble("total_price"),
                date == null ? null : LocalDate.parse(date));
    }

    // --------------------------------------------------------------- products

    public List<Product> getAllProducts() {
        return query("SELECT * FROM products ORDER BY product_name COLLATE NOCASE",
                DatabaseManager::mapProduct);
    }

    public boolean addProduct(Product p) {
        return update("""
                        INSERT INTO products (product_name, category, price, quantity, reorder_level)
                        VALUES (?, ?, ?, ?, ?)""",
                p.productName(), p.category(), p.price(), p.quantity(), p.reorderLevel());
    }

    public boolean updateProduct(Product p) {
        return update("""
                        UPDATE products
                        SET product_name = ?, category = ?, price = ?, quantity = ?, reorder_level = ?
                        WHERE id = ?""",
                p.productName(), p.category(), p.price(), p.quantity(), p.reorderLevel(), p.id());
    }

    public boolean restockProduct(int id, int amount) {
        return update("UPDATE products SET quantity = quantity + ? WHERE id = ?", amount, id);
    }

    public boolean deleteProduct(int id) {
        return update("DELETE FROM products WHERE id = ?", id);
    }

    // ------------------------------------------------------------- categories

    public List<Category> getAllCategories() {
        return query("""
                        SELECT c.id, c.name, COUNT(p.id) AS product_count
                        FROM categories c
                        LEFT JOIN products p ON p.category = c.name
                        GROUP BY c.id, c.name
                        ORDER BY c.name COLLATE NOCASE""",
                rs -> new Category(rs.getInt("id"), rs.getString("name"), rs.getInt("product_count")));
    }

    /** Returns false when a category with this name already exists. */
    public boolean addCategory(String name) {
        return update("INSERT INTO categories (name) VALUES (?)", name);
    }

    /** Renames a category and moves every product in it to the new name. */
    public boolean renameCategory(Category category, String newName) {
        return inTransaction(conn -> {
            executeUpdate(conn, "UPDATE categories SET name = ? WHERE id = ?", newName, category.id());
            executeUpdate(conn, "UPDATE products SET category = ? WHERE category = ?", newName, category.name());
            return true;
        });
    }

    public boolean deleteCategory(int id) {
        return update("DELETE FROM categories WHERE id = ?", id);
    }

    // ----------------------------------------------------------------- orders

    public List<Order> getAllOrders() {
        return query("SELECT * FROM orders ORDER BY id DESC", DatabaseManager::mapOrder);
    }

    /** Records an order and deducts its quantity from stock; fails if stock is insufficient. */
    public boolean placeOrder(String productName, int orderQty) {
        return inTransaction(conn -> {
            double price;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT quantity, price FROM products WHERE product_name = ?")) {
                ps.setString(1, productName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("quantity") < orderQty) return false;
                    price = rs.getDouble("price");
                }
            }
            executeUpdate(conn, "UPDATE products SET quantity = quantity - ? WHERE product_name = ?",
                    orderQty, productName);
            executeUpdate(conn, """
                            INSERT INTO orders (product_name, quantity, total_price, order_date)
                            VALUES (?, ?, ?, ?)""",
                    productName, orderQty, price * orderQty, LocalDate.now().toString());
            return true;
        });
    }

    /** Deletes an order and returns its quantity to stock (if the product still exists). */
    public boolean cancelOrder(Order order) {
        return inTransaction(conn -> {
            executeUpdate(conn, "UPDATE products SET quantity = quantity + ? WHERE product_name = ?",
                    order.quantity(), order.productName());
            return executeUpdate(conn, "DELETE FROM orders WHERE id = ?", order.id()) == 1;
        });
    }

    public double getTotalRevenue() {
        return queryOne("SELECT COALESCE(SUM(total_price), 0) FROM orders", rs -> rs.getDouble(1), 0.0);
    }

    public int getOrderCountOn(LocalDate date) {
        return queryOne("SELECT COUNT(*) FROM orders WHERE order_date = ?",
                rs -> rs.getInt(1), 0, date.toString());
    }

    /** Product name to units sold, best sellers first. */
    public Map<String, Integer> getTopSellingProducts(int limit) {
        Map<String, Integer> result = new LinkedHashMap<>();
        query("""
                        SELECT product_name, SUM(quantity) AS sold
                        FROM orders
                        GROUP BY product_name
                        ORDER BY sold DESC
                        LIMIT ?""",
                rs -> result.put(rs.getString("product_name"), rs.getInt("sold")), limit);
        return result;
    }

    /** Revenue per day between the two dates (inclusive); days without orders map to 0. */
    public Map<LocalDate, Double> getDailyRevenue(LocalDate from, LocalDate to) {
        Map<LocalDate, Double> result = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.put(d, 0.0);
        }
        query("""
                        SELECT order_date, SUM(total_price) AS revenue
                        FROM orders
                        WHERE order_date BETWEEN ? AND ?
                        GROUP BY order_date""",
                rs -> result.put(LocalDate.parse(rs.getString("order_date")), rs.getDouble("revenue")),
                from.toString(), to.toString());
        return result;
    }

    // ------------------------------------------------------------------ users

    public boolean hasUsers() {
        return queryOne("SELECT COUNT(*) FROM users", rs -> rs.getInt(1), 0) > 0;
    }

    public boolean createUser(User user, String password) {
        return update("INSERT INTO users (username, name, email, role, password) VALUES (?, ?, ?, ?, ?)",
                user.username(), user.name(), user.email(), user.role(), PasswordUtil.hashPassword(password));
    }

    public boolean validateUser(String username, String password) {
        return queryOne("SELECT 1 FROM users WHERE username = ? AND password = ?",
                rs -> true, false, username, PasswordUtil.hashPassword(password));
    }

    public User getUserByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?",
                rs -> new User(rs.getString("username"), rs.getString("name"),
                        rs.getString("email"), rs.getString("role")),
                null, username);
    }

    public boolean updateUserProfile(String username, String name, String email) {
        return update("UPDATE users SET name = ?, email = ? WHERE username = ?", name, email, username);
    }

    public boolean updatePassword(String username, String newPassword) {
        return update("UPDATE users SET password = ? WHERE username = ?",
                PasswordUtil.hashPassword(newPassword), username);
    }

    // -------------------------------------------------------------- suppliers

    public List<Supplier> getAllSuppliers() {
        return query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE",
                rs -> new Supplier(rs.getInt("id"), rs.getString("name"), rs.getString("contact")));
    }

    public boolean addSupplier(String name, String contact) {
        return update("INSERT INTO suppliers (name, contact) VALUES (?, ?)", name, contact);
    }

    public boolean updateSupplier(int id, String name, String contact) {
        return update("UPDATE suppliers SET name = ?, contact = ? WHERE id = ?", name, contact, id);
    }

    public boolean deleteSupplier(int id) {
        return update("DELETE FROM suppliers WHERE id = ?", id);
    }
}
