package com.minimart.server.dao;

import com.minimart.common.model.Order;
import com.minimart.server.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code orders} table.
 *
 * <p><b>Important:</b> {@link #insertWithConnection(Connection, Order)} accepts an external
 * Connection so that OrderService can manage the full multi-step transaction
 * (insert order → insert items → decrement stock) as a single atomic unit.</p>
 */
public class OrderDAO {

    private static final Logger log = LoggerFactory.getLogger(OrderDAO.class);

    private static final String SQL_FIND_ALL =
            "SELECT o.id, o.order_date, o.total_amount, o.note, o.user_id, o.customer_id, " +
            "       u.full_name AS cashier_name, c.customer_name, o.created_at " +
            "FROM orders o " +
            "JOIN users u ON o.user_id = u.id " +
            "LEFT JOIN customers c ON o.customer_id = c.id " +
            "ORDER BY o.order_date DESC";

    private static final String SQL_FIND_BY_ID =
            "SELECT o.id, o.order_date, o.total_amount, o.note, o.user_id, o.customer_id, " +
            "       u.full_name AS cashier_name, c.customer_name, o.created_at " +
            "FROM orders o " +
            "JOIN users u ON o.user_id = u.id " +
            "LEFT JOIN customers c ON o.customer_id = c.id " +
            "WHERE o.id = ?";

    /** Used within a transaction — does NOT take its own connection */
    private static final String SQL_INSERT =
            "INSERT INTO orders (order_date, total_amount, note, user_id, customer_id) " +
            "VALUES (NOW(), ?, ?, ?, ?)";

    private static final String SQL_DAILY_REVENUE =
            "SELECT DATE(order_date) AS sale_date, SUM(total_amount) AS total_revenue " +
            "FROM orders " +
            "WHERE order_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
            "GROUP BY DATE(order_date) " +
            "ORDER BY sale_date";

    // ── Public DAO methods ──────────────────────────────────

    public List<Order> findAll() throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Optional<Order> findById(int id) throws SQLException {
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
     * Inserts an order within an EXISTING transaction connection.
     * The caller is responsible for commit/rollback.
     *
     * @param conn  the active transactional connection (auto-commit must be OFF)
     * @param order the order to insert
     * @return the auto-generated order ID
     */
    public int insertWithConnection(Connection conn, Order order) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, order.getTotalAmount());
            ps.setString(2, order.getNote());
            ps.setInt(3, order.getUserId());
            if (order.getCustomerId() != null) {
                ps.setInt(4, order.getCustomerId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert order failed — no generated key");
    }

    /**
     * Returns daily revenue data for the last {@code days} days.
     * Used by the Dashboard BarChart.
     *
     * @return list of [sale_date, total_revenue] rows
     */
    public List<Object[]> getDailyRevenue(int days) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DAILY_REVENUE)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Object[]{
                        rs.getString("sale_date"),
                        rs.getBigDecimal("total_revenue")
                    });
                }
            }
        }
        return results;
    }

    // ── Private helpers ─────────────────────────────────────

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        Timestamp orderDate = rs.getTimestamp("order_date");
        if (orderDate != null) o.setOrderDate(orderDate.toLocalDateTime());
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setNote(rs.getString("note"));
        o.setUserId(rs.getInt("user_id"));
        o.setCashierName(rs.getString("cashier_name"));
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) o.setCustomerId(custId);
        o.setCustomerName(rs.getString("customer_name"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) o.setCreatedAt(created.toLocalDateTime());
        return o;
    }
}
