package com.minimart.server.dao;

import com.minimart.common.model.Product;
import com.minimart.server.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code products} table.
 * Supports full CRUD + search + stock decrement (used in order transactions).
 */
public class ProductDAO {

    private static final Logger log = LoggerFactory.getLogger(ProductDAO.class);

    private static final String SQL_FIND_ALL =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products ORDER BY product_code";

    private static final String SQL_FIND_ACTIVE =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products WHERE status = 'ACTIVE' ORDER BY product_code";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products WHERE id = ?";

    private static final String SQL_FIND_BY_CODE =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products WHERE product_code = ?";

    private static final String SQL_SEARCH =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products WHERE (product_code LIKE ? OR name LIKE ?) ORDER BY product_code";

    private static final String SQL_INSERT =
            "INSERT INTO products (product_code, name, category, description, unit, price, stock_quantity, status, image_path) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE products SET product_code=?, name=?, category=?, description=?, unit=?, price=?, stock_quantity=?, status=?, image_path=? " +
            "WHERE id=?";

    private static final String SQL_DELETE =
            "DELETE FROM products WHERE id = ?";

    /**
     * Decrements stock by {@code quantity}. Used inside a transaction.
     * Fails if stock would go negative.
     */
    private static final String SQL_DECREMENT_STOCK =
            "UPDATE products SET stock_quantity = stock_quantity - ? " +
            "WHERE id = ? AND stock_quantity >= ?";

    private static final String SQL_LOW_STOCK =
            "SELECT id, product_code, name, category, description, unit, price, stock_quantity, status, image_path, created_at, updated_at " +
            "FROM products WHERE stock_quantity <= ? AND status = 'ACTIVE' ORDER BY stock_quantity";

    // ── Public DAO methods ──────────────────────────────────

    public List<Product> findAll() throws SQLException {
        return queryList(SQL_FIND_ALL);
    }

    public List<Product> findAllActive() throws SQLException {
        return queryList(SQL_FIND_ACTIVE);
    }

    public Optional<Product> findById(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findByCode(String code) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CODE)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Full-text search by product_code or name (case-insensitive LIKE).
     */
    public List<Product> search(String keyword) throws SQLException {
        String pattern = "%" + keyword + "%";
        List<Product> results = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    /**
     * Returns products whose stock is at or below the given threshold.
     */
    public List<Product> findLowStock(int threshold) throws SQLException {
        List<Product> results = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOW_STOCK)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    public int insert(Product product) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            setProductParams(ps, product);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    log.info("Product inserted: id={}, code={}", newId, product.getProductCode());
                    return newId;
                }
            }
        }
        throw new SQLException("Insert product failed — no generated key");
    }

    public boolean update(Product product) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            setProductParams(ps, product);
            ps.setInt(10, product.getId());
            int rows = ps.executeUpdate();
            log.info("Product updated: id={}", product.getId());
            return rows > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            log.info("Product deleted: id={}", id);
            return rows > 0;
        }
    }

    /**
     * Decrements stock within an existing transaction connection.
     * <b>Does NOT commit.</b> The caller (OrderService) manages the transaction.
     *
     * @param conn     the transactional connection
     * @param productId the product to update
     * @param quantity  the amount to deduct
     * @throws SQLException if stock is insufficient or the product does not exist
     */
    public void decrementStock(Connection conn, int productId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_DECREMENT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.setInt(3, quantity); // ensures stock - quantity >= 0
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insufficient stock for product id=" + productId
                        + " (requested: " + quantity + ")");
            }
        }
    }

    // ── Private helpers ─────────────────────────────────────

    private List<Product> queryList(String sql) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private void setProductParams(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getProductCode());
        ps.setString(2, p.getName());
        ps.setString(3, p.getCategory());
        ps.setString(4, p.getDescription());
        ps.setString(5, p.getUnit());
        ps.setBigDecimal(6, p.getPrice());
        ps.setInt(7, p.getStockQuantity());
        ps.setString(8, p.getStatus());
        ps.setString(9, p.getImagePath());
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setProductCode(rs.getString("product_code"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setDescription(rs.getString("description"));
        p.setUnit(rs.getString("unit"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setStatus(rs.getString("status"));
        p.setImagePath(rs.getString("image_path"));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
        return p;
    }
}
