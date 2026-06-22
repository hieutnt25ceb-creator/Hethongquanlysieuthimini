package com.minimart.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sales order header.
 * {@code items} is populated for order-detail views only.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private int            id;
    private LocalDateTime  orderDate;
    private BigDecimal     totalAmount;
    private String         note;
    private int            userId;
    private String         cashierName;   // denormalized for display
    private Integer        customerId;    // nullable (walk-in customers)
    private String         customerName;  // denormalized for display
    private List<OrderItem> items = new ArrayList<>();
    private LocalDateTime  createdAt;

    // ── Constructors ────────────────────────────────────────

    public Order() {}

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public LocalDateTime getOrderDate()                   { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate)     { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount()              { return totalAmount; }
    public void setTotalAmount(BigDecimal amount)   { this.totalAmount = amount; }

    public String getNote()                         { return note; }
    public void setNote(String note)                { this.note = note; }

    public int getUserId()                          { return userId; }
    public void setUserId(int userId)               { this.userId = userId; }

    public String getCashierName()                  { return cashierName; }
    public void setCashierName(String cashierName)  { this.cashierName = cashierName; }

    public Integer getCustomerId()                  { return customerId; }
    public void setCustomerId(Integer customerId)   { this.customerId = customerId; }

    public String getCustomerName()                 { return customerName; }
    public void setCustomerName(String customerName){ this.customerName = customerName; }

    public List<OrderItem> getItems()               { return items; }
    public void setItems(List<OrderItem> items)     { this.items = items; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", total=" + totalAmount + ", date=" + orderDate + "}";
    }
}
