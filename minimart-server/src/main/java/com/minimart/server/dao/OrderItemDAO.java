package com.minimart.server.dao;

import com.minimart.common.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for the {@code order_items} table.
 *
 * <p>All methods operate on an externally-supplied Connection so they can
 * participate in the order-creation transaction managed by OrderService.</p>
 */
public class OrderItemDAO {

    private static final Logger log = LoggerFactory.getLogger(OrderItemDAO.class);

    private static final String SQL_INSERT =
            "INSERT INTO order_items (order_id, product_id, quantity, price_at_sale) VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ORDER =
            "SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.price_at_sale, oi.subtotal, " +
            "       p.name AS product_name, p.product_code " +
            "FROM order_items oi " +
            "JOIN products p ON oi.product_id = p.id " +
            "WHERE oi.order_id = ?";

    // ── Public DAO methods ──────────────────────────────────

    /**
     * Batch-inserts all order items within an existing transaction connection.
     * Uses a JDBC batch for efficiency. Does NOT commit.
     *
     * @param conn    the active transactional connection
     * @param orderId the ID of the parent order (just inserted)
     * @param items   the list of line items to insert
     */
    public void insertBatch(Connection conn, int orderId, List<OrderItem> items) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getProductId());
                ps.setInt(3, item.getQuantity());
                ps.setBigDecimal(4, item.getPriceAtSale());
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Batch-inserted {} order items for orderId={}", items.size(), orderId);
        }
    }

    /**
     * Retrieves all line items for a given order (joins product name and code).
     * Uses its own connection (read-only query, outside transaction).
     *
     * @param orderId the order ID to query
     * @return list of OrderItem objects
     */
    public List<OrderItem> findByOrderId(int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        // Need a connection — we open one ourselves for this read-only query
        try (java.sql.Connection conn = com.minimart.server.config.ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ORDER)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ── Private helpers ─────────────────────────────────────

    private OrderItem mapRow(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setId(rs.getInt("id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setProductCode(rs.getString("product_code"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPriceAtSale(rs.getBigDecimal("price_at_sale"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        return item;
    }
}
