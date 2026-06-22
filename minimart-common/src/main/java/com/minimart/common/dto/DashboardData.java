package com.minimart.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payload returned by GET_DASHBOARD_DATA.
 * Contains revenue trend data for the BarChart and low-stock alerts.
 */
public class DashboardData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Total revenue across all time */
    private BigDecimal totalRevenue;

    /** Total number of orders */
    private int totalOrders;

    /** Total number of products */
    private int totalProducts;

    /** Total number of customers */
    private int totalCustomers;

    /**
     * Revenue per day for the last N days.
     * Key: date string "yyyy-MM-dd", Value: revenue amount
     */
    private Map<String, BigDecimal> dailyRevenue;

    /**
     * Low-stock product summaries: productCode → stockQuantity
     */
    private List<LowStockItem> lowStockProducts;

    public DashboardData() {}

    // ── Getters & Setters ───────────────────────────────────

    public BigDecimal getTotalRevenue()                      { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue)     { this.totalRevenue = totalRevenue; }

    public int getTotalOrders()                              { return totalOrders; }
    public void setTotalOrders(int totalOrders)              { this.totalOrders = totalOrders; }

    public int getTotalProducts()                            { return totalProducts; }
    public void setTotalProducts(int totalProducts)          { this.totalProducts = totalProducts; }

    public int getTotalCustomers()                           { return totalCustomers; }
    public void setTotalCustomers(int totalCustomers)        { this.totalCustomers = totalCustomers; }

    public Map<String, BigDecimal> getDailyRevenue()               { return dailyRevenue; }
    public void setDailyRevenue(Map<String, BigDecimal> daily)     { this.dailyRevenue = daily; }

    public List<LowStockItem> getLowStockProducts()                { return lowStockProducts; }
    public void setLowStockProducts(List<LowStockItem> items)      { this.lowStockProducts = items; }

    // ── Inner DTO ───────────────────────────────────────────

    /**
     * A compact summary of a low-stock product for the dashboard alert table.
     */
    public static class LowStockItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String productCode;
        private String name;
        private int    stockQuantity;
        private String unit;

        public LowStockItem() {}

        public LowStockItem(String productCode, String name, int stockQuantity, String unit) {
            this.productCode   = productCode;
            this.name          = name;
            this.stockQuantity = stockQuantity;
            this.unit          = unit;
        }

        public String getProductCode()                      { return productCode; }
        public void setProductCode(String productCode)      { this.productCode = productCode; }

        public String getName()                             { return name; }
        public void setName(String name)                    { this.name = name; }

        public int getStockQuantity()                       { return stockQuantity; }
        public void setStockQuantity(int stockQuantity)     { this.stockQuantity = stockQuantity; }

        public String getUnit()                             { return unit; }
        public void setUnit(String unit)                    { this.unit = unit; }
    }
}
