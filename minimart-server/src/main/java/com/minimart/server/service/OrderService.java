package com.minimart.server.service;

import com.minimart.common.dto.OrderRequest;
import com.minimart.common.model.Order;
import com.minimart.common.model.OrderItem;
import com.minimart.common.model.Product;
import com.minimart.server.config.ConnectionManager;
import com.minimart.server.dao.OrderDAO;
import com.minimart.server.dao.OrderItemDAO;
import com.minimart.server.dao.ProductDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business service for order management.
 *
 * <p>The most critical method is {@link #createOrder(OrderRequest)}, which executes
 * a multi-step database transaction:</p>
 * <ol>
 *   <li>Insert the order header into {@code orders}</li>
 *   <li>Batch-insert all line items into {@code order_items}</li>
 *   <li>Decrement stock for each product in {@code products}</li>
 * </ol>
 * <p>If ANY step fails, the entire transaction is rolled back automatically.</p>
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderDAO     orderDAO     = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final ProductDAO   productDAO   = new ProductDAO();

    /**
     * Returns all orders (with denormalized cashier/customer names).
     */
    public List<Order> findAll() throws SQLException {
        return orderDAO.findAll();
    }

    /**
     * Returns a single order with its line items populated.
     */
    public Optional<Order> findById(int id) throws SQLException {
        Optional<Order> opt = orderDAO.findById(id);
        if (opt.isPresent()) {
            Order order = opt.get();
            order.setItems(orderItemDAO.findByOrderId(id));
        }
        return opt;
    }

    /**
     * Creates a new order atomically using a database transaction.
     *
     * <p>Steps executed within one Connection (auto-commit = false):</p>
     * <ol>
     *   <li>Validate that each product exists and has sufficient stock.</li>
     *   <li>Compute total_amount from current product prices (snapshot).</li>
     *   <li>Insert the order header.</li>
     *   <li>Batch-insert all order_items.</li>
     *   <li>Decrement stock for each product.</li>
     *   <li>COMMIT on success, ROLLBACK on any failure.</li>
     * </ol>
     *
     * @param request the order creation payload from the client
     * @return the newly created Order (with generated ID)
     * @throws SQLException     on any database error (triggers rollback)
     * @throws RuntimeException if validation fails (e.g., out of stock)
     */
    public Order createOrder(OrderRequest request) throws SQLException {
        List<OrderItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        // ── Start transaction ─────────────────────────────────
        Connection conn = ConnectionManager.getConnection();
        try {
            conn.setAutoCommit(false);  // BEGIN TRANSACTION

            // Step 1: Validate stock and fetch current prices (snapshot)
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                Optional<Product> productOpt = productDAO.findById(item.getProductId());
                if (productOpt.isEmpty()) {
                    throw new SQLException("Product not found: id=" + item.getProductId());
                }
                Product product = productOpt.get();

                if (!"ACTIVE".equals(product.getStatus())) {
                    throw new SQLException("Product is inactive: " + product.getName());
                }
                if (product.getStockQuantity() < item.getQuantity()) {
                    throw new SQLException("Insufficient stock for '" + product.getName()
                            + "'. Available: " + product.getStockQuantity()
                            + ", Requested: " + item.getQuantity());
                }

                // Snapshot the current price for historical accuracy
                item.setPriceAtSale(product.getPrice());
                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            // Step 2: Insert the order header
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setCustomerId(request.getCustomerId());
            order.setTotalAmount(totalAmount);
            order.setNote(request.getNote());

            int orderId = orderDAO.insertWithConnection(conn, order);
            order.setId(orderId);

            // Step 3: Batch-insert order items
            orderItemDAO.insertBatch(conn, orderId, items);

            // Step 4: Decrement stock for each product
            for (OrderItem item : items) {
                productDAO.decrementStock(conn, item.getProductId(), item.getQuantity());
            }

            // Step 5: COMMIT — all steps succeeded
            conn.commit();
            order.setItems(items);
            log.info("Order #{} created successfully. Total: {} VND, Items: {}",
                    orderId, totalAmount, items.size());
            return order;

        } catch (Exception e) {
            // ROLLBACK — any failure reverts the entire transaction
            try {
                conn.rollback();
                log.error("Order creation ROLLED BACK due to: {}", e.getMessage());
            } catch (SQLException rollbackEx) {
                log.error("Rollback also failed!", rollbackEx);
            }
            // Re-throw as SQLException so the dispatcher can send an error response
            if (e instanceof SQLException sqlEx) throw sqlEx;
            throw new SQLException("Order creation failed: " + e.getMessage(), e);

        } finally {
            // Always restore auto-commit and return connection to pool
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {}
        }
    }
}
