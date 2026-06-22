package com.minimart.common.dto;

import com.minimart.common.model.User;

import java.io.Serializable;

/**
 * Payload returned by the server upon successful LOGIN.
 * Contains the session token and the authenticated user's profile (without password).
 */
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** UUID session token; must be sent with every subsequent request */
    private String token;
    /** Authenticated user (password field is null/excluded) */
    private User   user;

    public LoginResponse() {}

    public LoginResponse(String token, User user) {
        this.token = token;
        this.user  = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
