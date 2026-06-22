package com.minimart.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a structured audit log entry written by the server.
 */
public class SystemLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private int           id;
    private LocalDateTime logTime;
    private String        level;     // "INFO" | "WARN" | "ERROR"
    private String        action;
    private String        username;
    private String        message;
    private String        ipAddress;

    // ── Constructors ────────────────────────────────────────

    public SystemLog() {}

    public SystemLog(String level, String action, String username, String message, String ipAddress) {
        this.level     = level;
        this.action    = action;
        this.username  = username;
        this.message   = message;
        this.ipAddress = ipAddress;
        this.logTime   = LocalDateTime.now();
    }

    // ── Getters & Setters ───────────────────────────────────

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public LocalDateTime getLogTime()                 { return logTime; }
    public void setLogTime(LocalDateTime logTime)     { this.logTime = logTime; }

    public String getLevel()                    { return level; }
    public void setLevel(String level)          { this.level = level; }

    public String getAction()                   { return action; }
    public void setAction(String action)        { this.action = action; }

    public String getUsername()                 { return username; }
    public void setUsername(String username)    { this.username = username; }

    public String getMessage()                  { return message; }
    public void setMessage(String message)      { this.message = message; }

    public String getIpAddress()                { return ipAddress; }
    public void setIpAddress(String ip)         { this.ipAddress = ip; }

    @Override
    public String toString() {
        return "[" + level + "] " + logTime + " | " + action + " | " + username + " | " + message;
    }
}
