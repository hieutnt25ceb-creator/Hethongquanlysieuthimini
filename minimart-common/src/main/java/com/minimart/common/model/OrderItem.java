package com.minimart.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a single line item within an Order.
 * {@code priceAtSale} is a snapshot of the product price at purchase time,
 * ensuring historical accuracy even if the product price changes later.
 */
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int        id;
    private int        orderId;
    private int        productId;
    private String     productName;    // denormalized for display
    private String     productCode;    // denormalized for display
    private int        quantity;
    private BigDecimal priceAtSale;
    private BigDecimal subtotal;       // quantity * priceAtSale (computed)

    // ── Constructors ────────────────────────────────────────

    public OrderItem() {}

    public OrderItem(int productId, String productName, int quantity, BigDecimal priceAtSale) {
        this.productId   = productId;
        this.productName = productName;
        this.quantity    = quantity;
        this.priceAtSale = priceAtSale;
        this.subtotal    = priceAtSale.multiply(BigDecimal.valueOf(quantity));
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getOrderId()                     { return orderId; }
    public void setOrderId(int orderId)         { this.orderId = orderId; }

    public int getProductId()                   { return productId; }
    public void setProductId(int productId)     { this.productId = productId; }

    public String getProductName()              { return productName; }
    public void setProductName(String name)     { this.productName = name; }

    public String getProductCode()              { return productCode; }
    public void setProductCode(String code)     { this.productCode = code; }

    public int getQuantity()                    { return quantity; }
    public void setQuantity(int quantity)       { this.quantity = quantity; }

    public BigDecimal getPriceAtSale()          { return priceAtSale; }
    public void setPriceAtSale(BigDecimal p)    { this.priceAtSale = p; }

    public BigDecimal getSubtotal() {
        if (subtotal == null && priceAtSale != null) {
            subtotal = priceAtSale.multiply(BigDecimal.valueOf(quantity));
        }
        return subtotal;
    }
    public void setSubtotal(BigDecimal subtotal){ this.subtotal = subtotal; }

    @Override
    public String toString() {
        return "OrderItem{product=" + productId + ", qty=" + quantity + ", price=" + priceAtSale + "}";
    }
}
