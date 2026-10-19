package com.example.stock_management_system;

import com.example.stock_management_system.models.Category;
import com.example.stock_management_system.models.Order;
import com.example.stock_management_system.models.Product;
import com.example.stock_management_system.models.StockMovement;
import com.example.stock_management_system.models.Supplier;
import com.example.stock_management_system.models.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Single access point to the SQLite database. The schema is created and migrated once,
 * the first time {@link #getInstance()} is called (and again after a restore).
 * Every stock change is written to {@code stock_movements} in the same transaction.
 */
public final class DatabaseManager {

    public static final int DEFAULT_REORDER_LEVEL = 5;

    private static final System.Logger LOG = System.getLogger(DatabaseManager.class.getName());

    private static final Path DB_PATH = Path.of(System.getProperty(
            "stock.db.path", System.getProperty("user.home") + "/products.db"));
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static final String PRODUCT_SELECT = """
            SELECT p.*, s.name AS supplier_name
            FROM products p
            LEFT JOIN suppliers s ON s.id = p.supplier_id""";

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

    private static Connection connect() throws SQLException {
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
                        reorder_level INTEGER NOT NULL DEFAULT 5,
                        supplier_id INTEGER
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
            st.execute("""
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        product_id INTEGER,
                        product_name TEXT NOT NULL,
                        change INTEGER NOT NULL,
                        quantity_after INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        note TEXT,
                        username TEXT,
                        created_at TEXT NOT NULL
                    )""");

            // Migrations for databases created by older versions of the app
            addColumnIfMissing(conn, "orders", "order_date", "TEXT");
            addColumnIfMissing(conn, "products", "reorder_level",
                    "INTEGER NOT NULL DEFAULT " + DEFAULT_REORDER_LEVEL);
            addColumnIfMissing(conn, "products", "supplier_id", "INTEGER");
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.ERROR, "Could not initialise database", e);
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String definition)
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

    private static int lastInsertId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Looks up a product's id and quantity inside a transaction; returns null if it does not exist. */
    private static int[] findStock(Connection conn, String column, Object value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, quantity FROM products WHERE " + column + " = ?")) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new int[]{rs.getInt("id"), rs.getInt("quantity")} : null;
            }
        }
    }

    private static void recordMovement(Connection conn, int productId, String productName, int change,
                                       int quantityAfter, StockMovement.Reason reason, String note)
            throws SQLException {
        User user = Session.getUser();
        executeUpdate(conn, """
                        INSERT INTO stock_movements
                            (product_id, product_name, change, quantity_after, reason, note, username, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                productId, productName, change, quantityAfter, reason.name(), note,
                user == null ? null : user.username(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }

    private static Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("quantity"),
                rs.getInt("reorder_level"),
                rs.getInt("supplier_id"),
                rs.getString("supplier_name"));
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

    /** Stores 0 (no supplier) as NULL. */
    private static Integer supplierOrNull(int supplierId) {
        return supplierId > 0 ? supplierId : null;
    }

    // --------------------------------------------------------------- products

    public List<Product> getAllProducts() {
        return query(PRODUCT_SELECT + " ORDER BY p.product_name COLLATE NOCASE", DatabaseManager::mapProduct);
    }

    public boolean addProduct(Product p) {
        return inTransaction(conn -> {
            executeUpdate(conn, """
                            INSERT INTO products (product_name, category, price, quantity, reorder_level, supplier_id)
                            VALUES (?, ?, ?, ?, ?, ?)""",
                    p.productName(), p.category(), p.price(), p.quantity(), p.reorderLevel(),
                    supplierOrNull(p.supplierId()));
            recordMovement(conn, lastInsertId(conn), p.productName(), p.quantity(), p.quantity(),
                    StockMovement.Reason.ADDED, null);
            return true;
        });
    }

    /** Saves product details; a changed quantity is logged as a manual adjustment. */
    public boolean updateProduct(Product p) {
        return inTransaction(conn -> {
            int[] before = findStock(conn, "id", p.id());
            if (before == null) return false;
            executeUpdate(conn, """
                            UPDATE products
                            SET product_name = ?, category = ?, price = ?, quantity = ?, reorder_level = ?,
                                supplier_id = ?
                            WHERE id = ?""",
                    p.productName(), p.category(), p.price(), p.quantity(), p.reorderLevel(),
                    supplierOrNull(p.supplierId()), p.id());
            if (before[1] != p.quantity()) {
                recordMovement(conn, p.id(), p.productName(), p.quantity() - before[1], p.quantity(),
                        StockMovement.Reason.ADJUSTMENT, "Edited on the products page");
            }
            return true;
        });
    }

    public boolean restockProduct(int id, int amount, String note) {
        return inTransaction(conn -> {
            int[] before = findStock(conn, "id", id);
            if (before == null) return false;
            executeUpdate(conn, "UPDATE products SET quantity = quantity + ? WHERE id = ?", amount, id);
            String name = queryName(conn, id);
            recordMovement(conn, id, name, amount, before[1] + amount, StockMovement.Reason.RESTOCK, note);
            return true;
        });
    }

    public boolean deleteProduct(Product p) {
        return inTransaction(conn -> {
            if (executeUpdate(conn, "DELETE FROM products WHERE id = ?", p.id()) != 1) return false;
            recordMovement(conn, p.id(), p.productName(), -p.quantity(), 0, StockMovement.Reason.DELETED, null);
            return true;
        });
    }

    private static String queryName(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT product_name FROM products WHERE id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
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
            int productId;
            int stock;
            double price;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, quantity, price FROM products WHERE product_name = ?")) {
                ps.setString(1, productName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("quantity") < orderQty) return false;
                    productId = rs.getInt("id");
                    stock = rs.getInt("quantity");
                    price = rs.getDouble("price");
                }
            }
            executeUpdate(conn, "UPDATE products SET quantity = quantity - ? WHERE id = ?", orderQty, productId);
            executeUpdate(conn, """
                            INSERT INTO orders (product_name, quantity, total_price, order_date)
                            VALUES (?, ?, ?, ?)""",
                    productName, orderQty, price * orderQty, LocalDate.now().toString());
            recordMovement(conn, productId, productName, -orderQty, stock - orderQty,
                    StockMovement.Reason.SALE, "Order #" + lastInsertId(conn));
            return true;
        });
    }

    /** Deletes an order and returns its quantity to stock (if the product still exists). */
    public boolean cancelOrder(Order order) {
        return inTransaction(conn -> {
            if (executeUpdate(conn, "DELETE FROM orders WHERE id = ?", order.id()) != 1) return false;
            int[] before = findStock(conn, "product_name", order.productName());
            if (before != null) {
                executeUpdate(conn, "UPDATE products SET quantity = quantity + ? WHERE id = ?",
                        order.quantity(), before[0]);
                recordMovement(conn, before[0], order.productName(), order.quantity(),
                        before[1] + order.quantity(), StockMovement.Reason.CANCELLED, "Order #" + order.id());
            }
            return true;
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

    // -------------------------------------------------------- stock movements

    public List<StockMovement> getStockMovements() {
        return query("SELECT * FROM stock_movements ORDER BY id DESC",
                rs -> new StockMovement(
                        rs.getInt("id"),
                        LocalDateTime.parse(rs.getString("created_at")),
                        rs.getString("product_name"),
                        rs.getInt("change"),
                        rs.getInt("quantity_after"),
                        StockMovement.Reason.valueOf(rs.getString("reason")),
                        rs.getString("note"),
                        rs.getString("username")));
    }

    // ------------------------------------------------------------------ users

    public boolean hasUsers() {
        return queryOne("SELECT COUNT(*) FROM users", rs -> rs.getInt(1), 0) > 0;
    }

    public List<User> getAllUsers() {
        return query("SELECT * FROM users ORDER BY username COLLATE NOCASE", DatabaseManager::mapUser);
    }

    public int countAdmins() {
        return queryOne("SELECT COUNT(*) FROM users WHERE UPPER(role) = ?", rs -> rs.getInt(1), 0, User.ADMIN);
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
        return queryOne("SELECT * FROM users WHERE username = ?", DatabaseManager::mapUser, null, username);
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getString("username"), rs.getString("name"), rs.getString("email"), rs.getString("role"));
    }

    public boolean updateUserProfile(String username, String name, String email) {
        return update("UPDATE users SET name = ?, email = ? WHERE username = ?", name, email, username);
    }

    public boolean updateUserRole(String username, String role) {
        return update("UPDATE users SET role = ? WHERE username = ?", role, username);
    }

    public boolean updatePassword(String username, String newPassword) {
        return update("UPDATE users SET password = ? WHERE username = ?",
                PasswordUtil.hashPassword(newPassword), username);
    }

    public boolean deleteUser(String username) {
        return update("DELETE FROM users WHERE username = ?", username);
    }

    // -------------------------------------------------------------- suppliers

    public List<Supplier> getAllSuppliers() {
        return query("""
                        SELECT s.id, s.name, s.contact, COUNT(p.id) AS product_count
                        FROM suppliers s
                        LEFT JOIN products p ON p.supplier_id = s.id
                        GROUP BY s.id, s.name, s.contact
                        ORDER BY s.name COLLATE NOCASE""",
                rs -> new Supplier(rs.getInt("id"), rs.getString("name"), rs.getString("contact"),
                        rs.getInt("product_count")));
    }

    public boolean addSupplier(String name, String contact) {
        return update("INSERT INTO suppliers (name, contact) VALUES (?, ?)", name, contact);
    }

    public boolean updateSupplier(int id, String name, String contact) {
        return update("UPDATE suppliers SET name = ?, contact = ? WHERE id = ?", name, contact, id);
    }

    /** Deletes a supplier and unlinks it from its products. */
    public boolean deleteSupplier(int id) {
        return inTransaction(conn -> {
            executeUpdate(conn, "UPDATE products SET supplier_id = NULL WHERE supplier_id = ?", id);
            executeUpdate(conn, "DELETE FROM suppliers WHERE id = ?", id);
            return true;
        });
    }

    // -------------------------------------------------------- backup / restore

    public Path getDatabasePath() {
        return DB_PATH.toAbsolutePath();
    }

    /** Writes a consistent copy of the database to the target file (replacing it). */
    public void backupTo(Path target) throws SQLException, IOException {
        Files.deleteIfExists(target);
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement("VACUUM INTO ?")) {
            ps.setString(1, target.toAbsolutePath().toString());
            ps.execute();
        }
    }

    /**
     * Replaces the current database with a backup, after checking that the backup is a
     * database from this app. Older backups are migrated to the current schema.
     */
    public void restoreFrom(Path source) throws SQLException, IOException {
        // Checked first: opening a missing path with SQLite would create an empty database there.
        if (!Files.isRegularFile(source)) {
            throw new IOException("Backup file not found: " + source);
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + source.toAbsolutePath());
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT COUNT(*) FROM products").close();
            st.executeQuery("SELECT COUNT(*) FROM users").close();
        } catch (SQLException e) {
            throw new SQLException("This file is not a Stock Management System backup.", e);
        }
        Files.copy(source, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
        initializeDatabase();
    }
}
