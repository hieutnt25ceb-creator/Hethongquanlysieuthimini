package com.minimart.server.service;

import com.minimart.common.dto.DashboardData;
import com.minimart.common.dto.DashboardData.LowStockItem;
import com.minimart.common.model.Product;
import com.minimart.server.config.ConnectionManager;
import com.minimart.server.dao.ProductDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Provides aggregated analytics data for the Admin Dashboard.
 */
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int REVENUE_DAYS        = 30; // last 30 days

    private final ProductDAO   productDAO   = new ProductDAO();
    private final OrderService orderService = new OrderService();

    private static final String SQL_TOTAL_REVENUE  = "SELECT COALESCE(SUM(total_amount), 0) FROM orders";
    private static final String SQL_TOTAL_ORDERS   = "SELECT COUNT(*) FROM orders";
    private static final String SQL_TOTAL_CUSTOMERS = "SELECT COUNT(*) FROM customers";
    private static final String SQL_TOTAL_PRODUCTS  = "SELECT COUNT(*) FROM products WHERE status = 'ACTIVE'";

    /**
     * Assembles a complete {@link DashboardData} payload for the Admin Dashboard.
     */
    public DashboardData getDashboardData() throws SQLException {
        DashboardData data = new DashboardData();

        try (Connection conn = ConnectionManager.getConnection()) {
            data.setTotalRevenue(querySingleDecimal(conn, SQL_TOTAL_REVENUE));
            data.setTotalOrders(querySingleInt(conn, SQL_TOTAL_ORDERS));
            data.setTotalCustomers(querySingleInt(conn, SQL_TOTAL_CUSTOMERS));
            data.setTotalProducts(querySingleInt(conn, SQL_TOTAL_PRODUCTS));
        }

        // Daily revenue for the bar chart (last 30 days)
        List<Object[]> revenueRows = orderService.findAll().isEmpty()
                ? List.of()
                : new com.minimart.server.dao.OrderDAO().getDailyRevenue(REVENUE_DAYS);

        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        for (Object[] row : revenueRows) {
            dailyRevenue.put((String) row[0], (BigDecimal) row[1]);
        }
        data.setDailyRevenue(dailyRevenue);

        // Low-stock products
        List<Product> lowStock = productDAO.findLowStock(LOW_STOCK_THRESHOLD);
        List<LowStockItem> lowStockItems = new ArrayList<>();
        for (Product p : lowStock) {
            lowStockItems.add(new LowStockItem(p.getProductCode(), p.getName(), p.getStockQuantity(), p.getUnit()));
        }
        data.setLowStockProducts(lowStockItems);

        log.debug("DashboardData assembled: {} low-stock products, {} revenue days", lowStockItems.size(), dailyRevenue.size());
        return data;
    }

    // ── Private query helpers ───────────────────────────────

    private BigDecimal querySingleDecimal(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    private int querySingleInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
