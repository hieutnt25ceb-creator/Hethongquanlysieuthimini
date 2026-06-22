package com.minimart.common.constants;

/**
 * Protocol action constants used in Request/Response DTOs exchanged
 * between Client and Server over the TCP socket.
 *
 * <p>Naming convention: VERB_NOUN or NOUN_ACTION</p>
 */
public final class Actions {

    private Actions() { /* utility class */ }

    // ── Authentication ─────────────────────────────────────
    public static final String LOGIN         = "LOGIN";
    public static final String LOGOUT        = "LOGOUT";
    public static final String REGISTER      = "REGISTER";

    // ── Products ───────────────────────────────────────────
    public static final String GET_PRODUCTS       = "GET_PRODUCTS";
    public static final String GET_PRODUCT_BY_ID  = "GET_PRODUCT_BY_ID";
    public static final String ADD_PRODUCT        = "ADD_PRODUCT";
    public static final String UPDATE_PRODUCT     = "UPDATE_PRODUCT";
    public static final String DELETE_PRODUCT     = "DELETE_PRODUCT";
    public static final String SEARCH_PRODUCTS    = "SEARCH_PRODUCTS";
    public static final String EXPORT_PRODUCTS_XML = "EXPORT_PRODUCTS_XML";
    public static final String IMPORT_PRODUCTS_XML = "IMPORT_PRODUCTS_XML";

    // ── Customers ──────────────────────────────────────────
    public static final String GET_CUSTOMERS      = "GET_CUSTOMERS";
    public static final String GET_CUSTOMER_BY_ID = "GET_CUSTOMER_BY_ID";
    public static final String ADD_CUSTOMER       = "ADD_CUSTOMER";
    public static final String UPDATE_CUSTOMER    = "UPDATE_CUSTOMER";
    public static final String DELETE_CUSTOMER    = "DELETE_CUSTOMER";
    public static final String SEARCH_CUSTOMERS   = "SEARCH_CUSTOMERS";

    // ── Orders ─────────────────────────────────────────────
    public static final String GET_ORDERS         = "GET_ORDERS";
    public static final String GET_ORDER_DETAIL   = "GET_ORDER_DETAIL";
    public static final String CREATE_ORDER       = "CREATE_ORDER";

    // ── Users (admin management) ───────────────────────────
    public static final String GET_USERS          = "GET_USERS";
    public static final String ADD_USER           = "ADD_USER";
    public static final String UPDATE_USER        = "UPDATE_USER";
    public static final String DELETE_USER        = "DELETE_USER";

    // ── Dashboard & Analytics ──────────────────────────────
    public static final String GET_DASHBOARD_DATA = "GET_DASHBOARD_DATA";

    // ── System Logs ────────────────────────────────────────
    public static final String GET_LOGS           = "GET_LOGS";
}
