package com.minimart.common.dto;

import com.minimart.common.model.OrderItem;

import java.io.Serializable;
import java.util.List;

/**
 * Request payload for creating a new order (CREATE_ORDER action).
 * The server uses this to execute the multi-step transactional insert.
 */
public class OrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID of the cashier processing the sale (populated from session token on server) */
    private int              userId;

    /** Optional — null for walk-in customers */
    private Integer          customerId;

    /** Cart line items to be inserted as order_items */
    private List<OrderItem>  items;

    /** Optional note for the order */
    private String           note;

    public OrderRequest() {}

    public OrderRequest(int userId, Integer customerId, List<OrderItem> items, String note) {
        this.userId     = userId;
        this.customerId = customerId;
        this.items      = items;
        this.note       = note;
    }

    public int getUserId()                           { return userId; }
    public void setUserId(int userId)                { this.userId = userId; }

    public Integer getCustomerId()                   { return customerId; }
    public void setCustomerId(Integer customerId)    { this.customerId = customerId; }

    public List<OrderItem> getItems()                { return items; }
    public void setItems(List<OrderItem> items)      { this.items = items; }

    public String getNote()                          { return note; }
    public void setNote(String note)                 { this.note = note; }
}
