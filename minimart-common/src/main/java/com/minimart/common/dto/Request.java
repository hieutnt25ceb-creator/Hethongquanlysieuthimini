package com.minimart.common.dto;

import java.io.Serializable;

/**
 * Generic request DTO sent from Client → Server over TCP socket.
 *
 * <p>Serialized as a single JSON line (newline-delimited).
 * The {@code payload} field holds action-specific data serialized as a JSON string.</p>
 *
 * <pre>
 * Example:
 * {
 *   "action": "LOGIN",
 *   "payload": "{\"username\":\"admin\",\"password\":\"Admin@123\"}",
 *   "token": null
 * }
 * </pre>
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Protocol action name (see {@link com.minimart.common.constants.Actions}) */
    private String action;

    /**
     * Action-specific payload, serialized as a JSON string.
     * The server deserializes this into the appropriate DTO.
     */
    private String payload;

    /**
     * Session token issued upon successful login.
     * Required for all authenticated actions.
     */
    private String token;

    // ── Constructors ────────────────────────────────────────

    public Request() {}

    public Request(String action, String payload) {
        this.action  = action;
        this.payload = payload;
    }

    public Request(String action, String payload, String token) {
        this.action  = action;
        this.payload = payload;
        this.token   = token;
    }

    // ── Getters & Setters ───────────────────────────────────

    public String getAction()                { return action; }
    public void setAction(String action)     { this.action = action; }

    public String getPayload()               { return payload; }
    public void setPayload(String payload)   { this.payload = payload; }

    public String getToken()                 { return token; }
    public void setToken(String token)       { this.token = token; }

    @Override
    public String toString() {
        return "Request{action='" + action + "', token='" + (token != null ? token.substring(0, 8) + "..." : "null") + "'}";
    }
}
