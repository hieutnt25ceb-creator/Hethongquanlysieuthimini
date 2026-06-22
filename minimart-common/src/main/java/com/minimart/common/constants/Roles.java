package com.minimart.common.constants;

/**
 * Role constants for role-based access control (RBAC).
 */
public final class Roles {

    private Roles() { /* utility class */ }

    /** Full access: user/product/order management, dashboard, logs */
    public static final String ADMIN    = "ADMIN";

    /** Limited access: POS checkout, view products/customers only */
    public static final String EMPLOYEE = "EMPLOYEE";
}
