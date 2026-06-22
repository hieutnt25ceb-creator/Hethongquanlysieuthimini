package com.minimart.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minimart.common.constants.Actions;
import com.minimart.common.dto.*;
import com.minimart.common.model.*;
import com.minimart.server.dao.LogDAO;
import com.minimart.server.log.StructuredLogger;
import com.minimart.server.security.TokenManager;
import com.minimart.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;

import com.google.gson.reflect.TypeToken;

/**
 * Routes an incoming {@link Request} to the appropriate service method
 * and returns a {@link Response}.
 *
 * <p>This is the central dispatcher for all protocol actions. Each action
 * maps to a service method call. Unauthorized actions are rejected if the
 * request token is invalid or the role is insufficient.</p>
 */
public class RequestDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RequestDispatcher.class);

    // ── Shared Gson instance (thread-safe) ──────────────────
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                            (src, type, ctx) -> ctx.serialize(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>)
                            (json, type, ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    // ── Services (stateless, shared across threads) ─────────
    private final AuthService      authService      = new AuthService();
    private final ProductService   productService   = new ProductService();
    private final CustomerService  customerService  = new CustomerService();
    private final OrderService     orderService     = new OrderService();
    private final DashboardService dashboardService = new DashboardService();
    private final UserService      userService      = new UserService();
    private final LogDAO           logDAO           = new LogDAO();

    /**
     * Dispatches a request to the correct handler.
     *
     * @param request   the parsed client request
     * @param clientIp  the remote client IP (for logging)
     * @return the response to send back to the client
     */
    public Response dispatch(Request request, String clientIp) {
        String action = request.getAction();
        log.info("Dispatching action='{}' from {}", action, clientIp);

        try {
            return switch (action) {

                // ── Public actions (no token required) ─────
                case Actions.LOGIN -> handleLogin(request, clientIp);
                case Actions.REGISTER -> handleRegister(request, clientIp);

                // ── Authenticated actions ───────────────────
                case Actions.LOGOUT             -> handleLogout(request, clientIp);
                case Actions.GET_PRODUCTS       -> handleGetProducts(request);
                case Actions.GET_PRODUCT_BY_ID  -> handleGetProductById(request);
                case Actions.ADD_PRODUCT        -> handleAddProduct(request, clientIp);
                case Actions.UPDATE_PRODUCT     -> handleUpdateProduct(request, clientIp);
                case Actions.DELETE_PRODUCT     -> handleDeleteProduct(request, clientIp);
                case Actions.SEARCH_PRODUCTS    -> handleSearchProducts(request);
                case Actions.EXPORT_PRODUCTS_XML -> handleExportXml(request, clientIp);
                case Actions.IMPORT_PRODUCTS_XML -> handleImportXml(request, clientIp);

                case Actions.GET_CUSTOMERS      -> handleGetCustomers(request);
                case Actions.GET_CUSTOMER_BY_ID -> handleGetCustomerById(request);
                case Actions.ADD_CUSTOMER       -> handleAddCustomer(request, clientIp);
                case Actions.UPDATE_CUSTOMER    -> handleUpdateCustomer(request, clientIp);
                case Actions.DELETE_CUSTOMER    -> handleDeleteCustomer(request, clientIp);
                case Actions.SEARCH_CUSTOMERS   -> handleSearchCustomers(request);

                case Actions.GET_ORDERS         -> handleGetOrders(request);
                case Actions.GET_ORDER_DETAIL   -> handleGetOrderDetail(request);
                case Actions.CREATE_ORDER       -> handleCreateOrder(request, clientIp);

                case Actions.GET_USERS          -> handleGetUsers(request);
                case Actions.ADD_USER           -> handleAddUser(request, clientIp);
                case Actions.UPDATE_USER        -> handleUpdateUser(request, clientIp);
                case Actions.DELETE_USER        -> handleDeleteUser(request, clientIp);

                case Actions.GET_DASHBOARD_DATA -> handleGetDashboardData(request);
                case Actions.GET_LOGS           -> handleGetLogs(request);

                default -> Response.error("Unknown action: " + action);
            };

        } catch (SecurityException e) {
            log.warn("Security violation: action={}, msg={}", action, e.getMessage());
            return Response.error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Response.error("Validation error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled exception for action={}", action, e);
            StructuredLogger.error(action, "SYSTEM", "Server error: " + e.getMessage(), clientIp);
            return Response.error("Server error: " + e.getMessage());
        }
    }

    // ── Token validation helpers ─────────────────────────────

    private User requireAuth(Request req) {
        User user = TokenManager.getUser(req.getToken());
        if (user == null) throw new SecurityException("Session expired or invalid. Please login again.");
        return user;
    }

    private User requireAdmin(Request req) {
        User user = requireAuth(req);
        if (!"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Access denied: Admin role required.");
        }
        return user;
    }

    // ── Action handlers ──────────────────────────────────────

    private Response handleLogin(Request req, String ip) throws Exception {
        LoginRequest lr = GSON.fromJson(req.getPayload(), LoginRequest.class);
        LoginResponse resp = authService.login(lr.username, lr.password);
        StructuredLogger.info(Actions.LOGIN, lr.username, "Login successful", ip);
        return Response.ok("Login successful", GSON.toJson(resp));
    }

    private Response handleRegister(Request req, String ip) throws Exception {
        RegisterRequest rr = GSON.fromJson(req.getPayload(), RegisterRequest.class);
        User newUser = new User();
        newUser.setUsername(rr.username);
        newUser.setFullName(rr.fullName);
        newUser.setRole("EMPLOYEE");
        newUser.setActive(true);
        User saved = userService.add(newUser, rr.password);
        StructuredLogger.info(Actions.REGISTER, rr.username, "New user registered", ip);
        log.info("New user registered: username='{}'", rr.username);
        return Response.ok("Đăng ký thành công! Hãy đăng nhập.", GSON.toJson(saved));
    }

    private Response handleLogout(Request req, String ip) {
        User user = requireAuth(req);
        authService.logout(req.getToken());
        StructuredLogger.info(Actions.LOGOUT, user.getUsername(), "Logout", ip);
        return Response.ok("Logged out successfully", null);
    }

    private Response handleGetProducts(Request req) throws SQLException {
        requireAuth(req);
        List<Product> products = productService.findAll();
        return Response.ok(GSON.toJson(products));
    }

    private Response handleGetProductById(Request req) throws SQLException {
        requireAuth(req);
        int id = Integer.parseInt(req.getPayload());
        return productService.findById(id)
                .map(p -> Response.ok(GSON.toJson(p)))
                .orElse(Response.error("Product not found: id=" + id));
    }

    private Response handleAddProduct(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        Product product = GSON.fromJson(req.getPayload(), Product.class);
        Product saved = productService.add(product);
        StructuredLogger.info(Actions.ADD_PRODUCT, user.getUsername(),
                "Added product: " + saved.getProductCode() + " - " + saved.getName(), ip);
        return Response.ok("Product added successfully", GSON.toJson(saved));
    }

    private Response handleUpdateProduct(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        Product product = GSON.fromJson(req.getPayload(), Product.class);
        Product updated = productService.update(product);
        StructuredLogger.info(Actions.UPDATE_PRODUCT, user.getUsername(),
                "Updated product: " + updated.getProductCode(), ip);
        return Response.ok("Product updated successfully", GSON.toJson(updated));
    }

    private Response handleDeleteProduct(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        int id = Integer.parseInt(req.getPayload());
        productService.delete(id);
        StructuredLogger.info(Actions.DELETE_PRODUCT, user.getUsername(), "Deleted product id=" + id, ip);
        return Response.ok("Product deleted", null);
    }

    private Response handleSearchProducts(Request req) throws SQLException {
        requireAuth(req);
        List<Product> results = productService.search(req.getPayload());
        return Response.ok(GSON.toJson(results));
    }

    private Response handleExportXml(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        String xml = productService.exportToXml();
        StructuredLogger.info(Actions.EXPORT_PRODUCTS_XML, user.getUsername(), "Product XML exported", ip);
        return Response.ok("Export successful", xml);
    }

    private Response handleImportXml(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        int count = productService.importFromXml(req.getPayload());
        StructuredLogger.info(Actions.IMPORT_PRODUCTS_XML, user.getUsername(),
                "Imported " + count + " products from XML", ip);
        return Response.ok("Imported " + count + " products successfully", String.valueOf(count));
    }

    private Response handleGetCustomers(Request req) throws SQLException {
        requireAuth(req);
        return Response.ok(GSON.toJson(customerService.findAll()));
    }

    private Response handleGetCustomerById(Request req) throws SQLException {
        requireAuth(req);
        int id = Integer.parseInt(req.getPayload());
        return customerService.findById(id)
                .map(c -> Response.ok(GSON.toJson(c)))
                .orElse(Response.error("Customer not found: id=" + id));
    }

    private Response handleAddCustomer(Request req, String ip) throws Exception {
        User user = requireAuth(req);
        Customer customer = GSON.fromJson(req.getPayload(), Customer.class);
        Customer saved = customerService.add(customer);
        StructuredLogger.info(Actions.ADD_CUSTOMER, user.getUsername(),
                "Added customer: " + saved.getCustomerName(), ip);
        return Response.ok("Customer added", GSON.toJson(saved));
    }

    private Response handleUpdateCustomer(Request req, String ip) throws Exception {
        User user = requireAuth(req);
        Customer customer = GSON.fromJson(req.getPayload(), Customer.class);
        Customer updated = customerService.update(customer);
        StructuredLogger.info(Actions.UPDATE_CUSTOMER, user.getUsername(),
                "Updated customer id=" + updated.getId(), ip);
        return Response.ok("Customer updated", GSON.toJson(updated));
    }

    private Response handleDeleteCustomer(Request req, String ip) throws Exception {
        User user = requireAdmin(req);
        int id = Integer.parseInt(req.getPayload());
        customerService.delete(id);
        StructuredLogger.info(Actions.DELETE_CUSTOMER, user.getUsername(), "Deleted customer id=" + id, ip);
        return Response.ok("Customer deleted", null);
    }

    private Response handleSearchCustomers(Request req) throws SQLException {
        requireAuth(req);
        return Response.ok(GSON.toJson(customerService.search(req.getPayload())));
    }

    private Response handleGetOrders(Request req) throws SQLException {
        requireAuth(req);
        return Response.ok(GSON.toJson(orderService.findAll()));
    }

    private Response handleGetOrderDetail(Request req) throws SQLException {
        requireAuth(req);
        int id = Integer.parseInt(req.getPayload());
        return orderService.findById(id)
                .map(o -> Response.ok(GSON.toJson(o)))
                .orElse(Response.error("Order not found: id=" + id));
    }

    private Response handleCreateOrder(Request req, String ip) throws Exception {
        User user = requireAuth(req);
        OrderRequest orderReq = GSON.fromJson(req.getPayload(), OrderRequest.class);
        orderReq.setUserId(user.getId()); // Always use the server's authenticated user ID
        Order order = orderService.createOrder(orderReq);
        StructuredLogger.info(Actions.CREATE_ORDER, user.getUsername(),
                "Order #" + order.getId() + " created. Total: " + order.getTotalAmount() + " VND", ip);
        return Response.ok("Order created successfully", GSON.toJson(order));
    }

    private Response handleGetUsers(Request req) throws SQLException {
        requireAdmin(req);
        return Response.ok(GSON.toJson(userService.findAll()));
    }

    private Response handleAddUser(Request req, String ip) throws Exception {
        User admin = requireAdmin(req);
        AddUserRequest aur = GSON.fromJson(req.getPayload(), AddUserRequest.class);
        User saved = userService.add(aur.user, aur.rawPassword);
        StructuredLogger.info(Actions.ADD_USER, admin.getUsername(), "Added user: " + saved.getUsername(), ip);
        return Response.ok("User added", GSON.toJson(saved));
    }

    private Response handleUpdateUser(Request req, String ip) throws Exception {
        User admin = requireAdmin(req);
        User user = GSON.fromJson(req.getPayload(), User.class);
        User updated = userService.update(user);
        StructuredLogger.info(Actions.UPDATE_USER, admin.getUsername(), "Updated user id=" + updated.getId(), ip);
        return Response.ok("User updated", GSON.toJson(updated));
    }

    private Response handleDeleteUser(Request req, String ip) throws Exception {
        User admin = requireAdmin(req);
        int id = Integer.parseInt(req.getPayload());
        userService.delete(id);
        StructuredLogger.info(Actions.DELETE_USER, admin.getUsername(), "Deleted user id=" + id, ip);
        return Response.ok("User deleted", null);
    }

    private Response handleGetDashboardData(Request req) throws Exception {
        requireAdmin(req);
        DashboardData data = dashboardService.getDashboardData();
        return Response.ok(GSON.toJson(data));
    }

    private Response handleGetLogs(Request req) throws SQLException {
        requireAdmin(req);
        return Response.ok(GSON.toJson(logDAO.findAll()));
    }

    // ── Private inner request models ─────────────────────────

    /** Payload for LOGIN action */
    private static class LoginRequest {
        String username;
        String password;
    }

    /** Payload for ADD_USER action */
    private static class AddUserRequest {
        User   user;
        String rawPassword;
    }

    /** Payload for REGISTER action */
    private static class RegisterRequest {
        String username;
        String password;
        String fullName;
    }
}
