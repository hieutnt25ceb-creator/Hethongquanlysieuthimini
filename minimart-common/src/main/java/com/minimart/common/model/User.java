package com.minimart.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a system user (Admin or Employee).
 * Password field is never sent to the client; it is server-side only.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           id;
    private String        username;
    private String        password;   // BCrypt hash — server-side only, never serialized to client
    private String        fullName;
    private String        role;       // "ADMIN" or "EMPLOYEE"
    private boolean       active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructors ────────────────────────────────────────

    public User() {}

    public User(int id, String username, String fullName, String role, boolean active) {
        this.id       = id;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
        this.active   = active;
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getUsername()               { return username; }
    public void setUsername(String username)  { this.username = username; }

    public String getPassword()               { return password; }
    public void setPassword(String password)  { this.password = password; }

    public String getFullName()               { return fullName; }
    public void setFullName(String fullName)  { this.fullName = fullName; }

    public String getRole()                   { return role; }
    public void setRole(String role)          { this.role = role; }

    public boolean isActive()                 { return active; }
    public void setActive(boolean active)     { this.active = active; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                   { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)     { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
