package com.minimart.common.dto;

import java.io.Serializable;

/**
 * Generic response DTO sent from Server → Client over TCP socket.
 *
 * <p>The {@code data} field carries action-specific result payload as a JSON string.
 * On failure, {@code data} is null and {@code message} contains the error description.</p>
 *
 * <pre>
 * Success example:
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": "{\"token\":\"abc123\",\"user\":{...}}"
 * }
 *
 * Failure example:
 * {
 *   "success": false,
 *   "message": "Invalid username or password",
 *   "data": null
 * }
 * </pre>
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String  message;
    /**
     * Action-specific result payload as a JSON string.
     * The client deserializes this into the appropriate model/DTO.
     */
    private String  data;

    // ── Static factory methods ──────────────────────────────

    public static Response ok(String message, String data) {
        Response r = new Response();
        r.success = true;
        r.message = message;
        r.data    = data;
        return r;
    }

    public static Response ok(String data) {
        return ok("OK", data);
    }

    public static Response error(String message) {
        Response r = new Response();
        r.success = false;
        r.message = message;
        r.data    = null;
        return r;
    }

    // ── Constructors ────────────────────────────────────────

    public Response() {}

    // ── Getters & Setters ───────────────────────────────────

    public boolean isSuccess()                 { return success; }
    public void setSuccess(boolean success)    { this.success = success; }

    public String getMessage()                 { return message; }
    public void setMessage(String message)     { this.message = message; }

    public String getData()                    { return data; }
    public void setData(String data)           { this.data = data; }

    @Override
    public String toString() {
        return "Response{success=" + success + ", message='" + message + "'}";
    }
}
