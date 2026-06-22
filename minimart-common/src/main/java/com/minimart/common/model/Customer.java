package com.minimart.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a customer.
 * The {@code phoneNumber} field holds the AES-encrypted value on the server side.
 * The client receives a masked/decrypted display version depending on context.
 */
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           id;
    private String        customerName;
    /** Raw display phone for client UI (decrypted by server before sending) */
    private String        phoneNumber;
    private String        email;
    private String        address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ────────────────────────────────────────

    public Customer() {}

    public Customer(int id, String customerName, String phoneNumber, String email, String address) {
        this.id           = id;
        this.customerName = customerName;
        this.phoneNumber  = phoneNumber;
        this.email        = email;
        this.address      = address;
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getCustomerName()                 { return customerName; }
    public void setCustomerName(String name)        { this.customerName = name; }

    public String getPhoneNumber()                  { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber)  { this.phoneNumber = phoneNumber; }

    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }

    public String getAddress()                      { return address; }
    public void setAddress(String address)          { this.address = address; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                   { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)     { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + customerName + "'}";
    }
}
