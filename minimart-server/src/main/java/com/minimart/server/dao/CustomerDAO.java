package com.minimart.server.dao;

import com.minimart.common.model.Customer;
import com.minimart.server.config.ConnectionManager;
import com.minimart.server.security.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code customers} table.
 *
 * <p>Phone numbers are AES-encrypted before storage and decrypted transparently
 * when reading, so callers always deal with plain-text phone numbers.</p>
 */
public class CustomerDAO {

    private static final Logger log = LoggerFactory.getLogger(CustomerDAO.class);

    private static final String SQL_FIND_ALL =
            "SELECT id, customer_name, phone_number, email, address, created_at, updated_at " +
            "FROM customers ORDER BY customer_name";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, customer_name, phone_number, email, address, created_at, updated_at " +
            "FROM customers WHERE id = ?";

    private static final String SQL_SEARCH =
            "SELECT id, customer_name, phone_number, email, address, created_at, updated_at " +
            "FROM customers WHERE customer_name LIKE ? ORDER BY customer_name";

    private static final String SQL_INSERT =
            "INSERT INTO customers (customer_name, phone_number, email, address) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE customers SET customer_name=?, phone_number=?, email=?, address=? WHERE id=?";

    private static final String SQL_DELETE =
            "DELETE FROM customers WHERE id = ?";

    // ── Public DAO methods ──────────────────────────────────

    /**
     * Returns all customers with decrypted phone numbers.
     */
    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Finds a customer by ID with decrypted phone number.
     */
    public Optional<Customer> findById(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Searches by customer name (LIKE %keyword%).
     */
    public List<Customer> search(String keyword) throws SQLException {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Inserts a new customer. The phone number is AES-encrypted before storage.
     */
    public int insert(Customer customer) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getCustomerName());
            ps.setString(2, AESUtil.encrypt(customer.getPhoneNumber())); // Encrypt phone
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    log.info("Customer inserted: id={}, name={}", newId, customer.getCustomerName());
                    return newId;
                }
            }
        }
        throw new SQLException("Insert customer failed — no generated key");
    }

    /**
     * Updates a customer. Phone number is re-encrypted before storage.
     */
    public boolean update(Customer customer) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, customer.getCustomerName());
            ps.setString(2, AESUtil.encrypt(customer.getPhoneNumber())); // Re-encrypt
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getAddress());
            ps.setInt(5, customer.getId());
            int rows = ps.executeUpdate();
            log.info("Customer updated: id={}", customer.getId());
            return rows > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            log.info("Customer deleted: id={}", id);
            return rows > 0;
        }
    }

    // ── Private helpers ─────────────────────────────────────

    /** Maps a ResultSet row to a Customer, decrypting the phone number. */
    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setCustomerName(rs.getString("customer_name"));
        // Decrypt phone number transparently
        String encryptedPhone = rs.getString("phone_number");
        c.setPhoneNumber(AESUtil.decrypt(encryptedPhone));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());
        if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());
        return c;
    }
}
