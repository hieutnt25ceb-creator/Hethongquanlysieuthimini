package com.minimart.client.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minimart.client.network.ServerCommunicator;
import com.minimart.common.constants.Actions;
import com.minimart.common.dto.*;
import com.minimart.common.model.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Client-side service layer — the ONLY class that calls {@link ServerCommunicator}.
 *
 * <p>All UI controllers and background Tasks call methods on this class.
 * No direct network code is allowed outside of this class.</p>
 *
 * <p>Methods are blocking — they must be called from a background thread
 * (JavaFX Task), never from the JavaFX Application Thread.</p>
 */
public class ClientService {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                            (src, type, ctx) -> ctx.serialize(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>)
                            (json, type, ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private static ClientService instance;

    /** Active session token — set on login, cleared on logout */
    private String sessionToken;
    /** Currently logged-in user */
    private User currentUser;

    private ClientService() {}

    public static synchronized ClientService getInstance() {
        if (instance == null) instance = new ClientService();
        return instance;
    }

    // ── Session helpers ──────────────────────────────────────

    public String getSessionToken()       { return sessionToken; }
    public User   getCurrentUser()        { return currentUser; }
    public boolean isAdmin()              { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }

    private Request authenticated(String action, String payload) {
        return new Request(action, payload, sessionToken);
    }

    // ── Connection ───────────────────────────────────────────

    public void connect() throws IOException {
        ServerCommunicator.getInstance().connect();
    }

    public void disconnect() {
        ServerCommunicator.getInstance().disconnect();
        sessionToken = null;
        currentUser  = null;
    }

    // ── Authentication ───────────────────────────────────────

    /**
     * Logs in and stores the session token + user profile on success.
     */
    public LoginResponse login(String username, String password) throws IOException {
        String payload = GSON.toJson(new LoginPayload(username, password));
        Request  req  = new Request(Actions.LOGIN, payload);
        Response resp = ServerCommunicator.getInstance().sendRequest(req);
        if (!resp.isSuccess()) throw new RuntimeException(resp.getMessage());

        LoginResponse lr = GSON.fromJson(resp.getData(), LoginResponse.class);
        this.sessionToken = lr.getToken();
        this.currentUser  = lr.getUser();
        return lr;
    }

    public void logout() throws IOException {
        if (sessionToken != null) {
            ServerCommunicator.getInstance().sendRequest(authenticated(Actions.LOGOUT, null));
        }
        sessionToken = null;
        currentUser  = null;
    }

    /**
     * Registers a new EMPLOYEE account.
     *
     * @param username plain username
     * @param password plain password (will be hashed server-side)
     * @param fullName display name
     * @throws RuntimeException if server rejects the request
     */
    public void register(String username, String password, String fullName) throws IOException {
        String payload = GSON.toJson(new RegisterPayload(username, password, fullName));
        Request req = new Request(Actions.REGISTER, payload);
        Response resp = ServerCommunicator.getInstance().sendRequest(req);
        if (!resp.isSuccess()) throw new RuntimeException(resp.getMessage());
    }

    // ── Products ─────────────────────────────────────────────

    public List<Product> getProducts() throws IOException {
        Response resp = send(authenticated(Actions.GET_PRODUCTS, null));
        Type type = new TypeToken<List<Product>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public List<Product> searchProducts(String keyword) throws IOException {
        Response resp = send(authenticated(Actions.SEARCH_PRODUCTS, keyword));
        Type type = new TypeToken<List<Product>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public Product addProduct(Product product) throws IOException {
        Response resp = send(authenticated(Actions.ADD_PRODUCT, GSON.toJson(product)));
        return GSON.fromJson(resp.getData(), Product.class);
    }

    public Product updateProduct(Product product) throws IOException {
        Response resp = send(authenticated(Actions.UPDATE_PRODUCT, GSON.toJson(product)));
        return GSON.fromJson(resp.getData(), Product.class);
    }

    public void deleteProduct(int id) throws IOException {
        send(authenticated(Actions.DELETE_PRODUCT, String.valueOf(id)));
    }

    public String exportProductsXml() throws IOException {
        Response resp = send(authenticated(Actions.EXPORT_PRODUCTS_XML, null));
        return resp.getData();
    }

    public int importProductsXml(String xmlContent) throws IOException {
        Response resp = send(authenticated(Actions.IMPORT_PRODUCTS_XML, xmlContent));
        return Integer.parseInt(resp.getData());
    }

    // ── Customers ────────────────────────────────────────────

    public List<Customer> getCustomers() throws IOException {
        Response resp = send(authenticated(Actions.GET_CUSTOMERS, null));
        Type type = new TypeToken<List<Customer>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public List<Customer> searchCustomers(String keyword) throws IOException {
        Response resp = send(authenticated(Actions.SEARCH_CUSTOMERS, keyword));
        Type type = new TypeToken<List<Customer>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public Customer addCustomer(Customer customer) throws IOException {
        Response resp = send(authenticated(Actions.ADD_CUSTOMER, GSON.toJson(customer)));
        return GSON.fromJson(resp.getData(), Customer.class);
    }

    public Customer updateCustomer(Customer customer) throws IOException {
        Response resp = send(authenticated(Actions.UPDATE_CUSTOMER, GSON.toJson(customer)));
        return GSON.fromJson(resp.getData(), Customer.class);
    }

    public void deleteCustomer(int id) throws IOException {
        send(authenticated(Actions.DELETE_CUSTOMER, String.valueOf(id)));
    }

    // ── Orders ───────────────────────────────────────────────

    public List<Order> getOrders() throws IOException {
        Response resp = send(authenticated(Actions.GET_ORDERS, null));
        Type type = new TypeToken<List<Order>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public Order getOrderDetail(int id) throws IOException {
        Response resp = send(authenticated(Actions.GET_ORDER_DETAIL, String.valueOf(id)));
        return GSON.fromJson(resp.getData(), Order.class);
    }

    public Order createOrder(OrderRequest orderReq) throws IOException {
        Response resp = send(authenticated(Actions.CREATE_ORDER, GSON.toJson(orderReq)));
        return GSON.fromJson(resp.getData(), Order.class);
    }

    // ── Users ────────────────────────────────────────────────

    public List<User> getUsers() throws IOException {
        Response resp = send(authenticated(Actions.GET_USERS, null));
        Type type = new TypeToken<List<User>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    public User addUser(User user, String rawPassword) throws IOException {
        Response resp = send(authenticated(Actions.ADD_USER, GSON.toJson(new AddUserPayload(user, rawPassword))));
        return GSON.fromJson(resp.getData(), User.class);
    }

    public User updateUser(User user) throws IOException {
        Response resp = send(authenticated(Actions.UPDATE_USER, GSON.toJson(user)));
        return GSON.fromJson(resp.getData(), User.class);
    }

    public void deleteUser(int id) throws IOException {
        send(authenticated(Actions.DELETE_USER, String.valueOf(id)));
    }

    // ── Dashboard ────────────────────────────────────────────

    public DashboardData getDashboardData() throws IOException {
        Response resp = send(authenticated(Actions.GET_DASHBOARD_DATA, null));
        return GSON.fromJson(resp.getData(), DashboardData.class);
    }

    // ── Logs ─────────────────────────────────────────────────

    public List<SystemLog> getLogs() throws IOException {
        Response resp = send(authenticated(Actions.GET_LOGS, null));
        Type type = new TypeToken<List<SystemLog>>(){}.getType();
        return GSON.fromJson(resp.getData(), type);
    }

    // ── Private helper ───────────────────────────────────────

    /**
     * Sends a request and checks the response success flag.
     * Throws RuntimeException if the server returned an error response.
     */
    private Response send(Request request) throws IOException {
        Response resp = ServerCommunicator.getInstance().sendRequest(request);
        if (!resp.isSuccess()) {
            throw new RuntimeException(resp.getMessage());
        }
        return resp;
    }

    // ── Private inner payload classes ─────────────────────────

    private static class LoginPayload {
        final String username;
        final String password;
        LoginPayload(String u, String p) { this.username = u; this.password = p; }
    }

    private static class RegisterPayload {
        final String username;
        final String password;
        final String fullName;
        RegisterPayload(String u, String p, String f) {
            this.username = u; this.password = p; this.fullName = f;
        }
    }

    private static class AddUserPayload {
        final User user;
        final String rawPassword;
        AddUserPayload(User u, String p) {
            this.user = u;
            this.rawPassword = p;
        }
    }
}
