package com.minimart.client.ui;

import com.minimart.client.ClientMain;
import com.minimart.client.service.ClientService;
import com.minimart.common.dto.OrderRequest;
import com.minimart.common.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;

/**
 * Controller for the Employee POS checkout screen (POSView.fxml).
 *
 * <p>Features:
 * <ul>
 *   <li>Product search & browse with card tiles</li>
 *   <li>Click product to add to cart (quantity dialog)</li>
 *   <li>Dynamic cart with remove buttons</li>
 *   <li>Customer selection (or walk-in)</li>
 *   <li>Checkout via async {@link Task} → CREATE_ORDER</li>
 * </ul>
 * </p>
 */
public class POSController implements Initializable {

    @FXML private Label             lblCashier;
    @FXML private TextField         txtSearch;
    @FXML private FlowPane          productGrid;
    @FXML private ComboBox<Customer>cmbCustomer;
    @FXML private TableView<OrderItem> cartTable;
    @FXML private TableColumn<OrderItem,String>     colCartName;
    @FXML private TableColumn<OrderItem,Integer>    colCartQty;
    @FXML private TableColumn<OrderItem,BigDecimal> colCartPrice;
    @FXML private TableColumn<OrderItem,BigDecimal> colCartTotal;
    @FXML private TableColumn<OrderItem,Void>       colCartDel;
    @FXML private Label             lblTotal;
    @FXML private Label             lblItemCount;
    @FXML private TextField         txtNote;
    @FXML private Button            btnCheckout;
    @FXML private ProgressIndicator checkoutSpinner;
    @FXML private Label             lblCheckoutStatus;

    private final ClientService cs  = ClientService.getInstance();
    private final NumberFormat  nf  = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));

    private List<Product>              allProducts = new ArrayList<>();
    private final ObservableList<OrderItem> cart  = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = cs.getCurrentUser();
        if (user != null) lblCashier.setText("👤 " + user.getFullName() + " (" + user.getRole() + ")");

        setupCartTable();
        loadInitialData();
    }

    // ── Data loading ─────────────────────────────────────────

    private void loadInitialData() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                allProducts = cs.getProducts();
                List<Customer> customers = cs.getCustomers();
                javafx.application.Platform.runLater(() -> {
                    renderProductGrid(allProducts);
                    cmbCustomer.setItems(FXCollections.observableArrayList(customers));
                    cmbCustomer.setConverter(new javafx.util.StringConverter<>() {
                        @Override public String toString(Customer c) {
                            return c == null ? "Khách vãng lai" : c.getCustomerName() + " — " + c.getPhoneNumber();
                        }
                        @Override public Customer fromString(String s) { return null; }
                    });
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void renderProductGrid(List<Product> products) {
        productGrid.getChildren().clear();
        for (Product p : products) {
            if (!"ACTIVE".equals(p.getStatus())) continue;
            productGrid.getChildren().add(buildProductCard(p));
        }
    }

    // ── Product search ───────────────────────────────────────

    @FXML
    private void handleProductSearch() {
        String kw = txtSearch.getText().toLowerCase().trim();
        if (kw.isEmpty()) {
            renderProductGrid(allProducts);
        } else {
            renderProductGrid(allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(kw) ||
                             p.getProductCode().toLowerCase().contains(kw))
                .toList());
        }
    }

    // ── Product card tile ─────────────────────────────────────

    private VBox buildProductCard(Product p) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 10, 10, 10));
        card.setPrefWidth(160);
        card.setPrefHeight(200);
        card.getStyleClass().add("product-card");

        // ── Get Category Colors for badges ────────────────────
        String cat = p.getCategory();
        String bgColor = "#e2e8f0";
        String textColor = "#475569";
        if (cat != null) {
            if (cat.contains("Đồ uống")) { bgColor = "#bae6fd"; textColor = "#0369a1"; }
            else if (cat.contains("Thực phẩm đóng gói")) { bgColor = "#fed7aa"; textColor = "#c2410c"; }
            else if (cat.contains("Bánh kẹo")) { bgColor = "#fef08a"; textColor = "#a16207"; }
            else if (cat.contains("Hóa mỹ phẩm")) { bgColor = "#e9d5ff"; textColor = "#7e22ce"; }
            else if (cat.contains("tươi") || cat.contains("Sữa")) { bgColor = "#fbcfe8"; textColor = "#be185d"; }
        }

        // ── Load Product Image ─────────────────────────
        javafx.scene.Node imgContainer = UIHelper.createProductImageNode(p, 130, 65);

        // ── Card Details ──────────────────────────────────────
        Label code = new Label(p.getProductCode());
        code.getStyleClass().add("product-card-code");

        Label badge = new Label(p.getCategory());
        badge.getStyleClass().add("card-badge");
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 2 6;");

        Label name = new Label(p.getName());
        name.getStyleClass().add("product-card-title");
        name.setWrapText(true);
        name.setAlignment(Pos.CENTER);
        name.setMaxWidth(140);
        name.setPrefHeight(36); // Keep heights uniform

        Label price = new Label(formatVnd(p.getPrice()));
        price.getStyleClass().add("product-card-price");

        Label stock = new Label("Còn: " + p.getStockQuantity() + " " + p.getUnit());
        stock.getStyleClass().add("product-card-stock");
        if (p.getStockQuantity() <= 5) {
            stock.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }

        card.getChildren().addAll(imgContainer, code, badge, name, price, stock);

        // ── Tactile Hover Scale Animation ─────────────────────
        card.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(120), card);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        card.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(120), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        card.setOnMouseClicked(e -> addToCart(p));

        // Disable out-of-stock
        if (p.getStockQuantity() <= 0) {
            card.setDisable(true);
            card.setOpacity(0.35);
        }
        return card;
    }

    // ── Cart management ──────────────────────────────────────

    private void setupCartTable() {
        colCartName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));
        colCartQty.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getQuantity()));
        colCartPrice.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getPriceAtSale()));
        colCartPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatVnd(v));
            }
        });
        colCartTotal.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getSubtotal()));
        colCartTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(formatVnd(v));
                setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
            }
        });
        colCartDel.setCellFactory(tc -> new TableCell<>() {
            final Button btn = new Button("✕");
            { btn.setStyle("-fx-background-color: rgba(255,71,87,0.2); -fx-background-radius:5; -fx-text-fill:#ff4757; -fx-cursor:hand; -fx-padding:3 7;"); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (!empty) btn.setOnAction(e -> {
                    cart.remove(getTableView().getItems().get(getIndex()));
                    updateTotal();
                });
                setGraphic(empty ? null : btn);
            }
        });
        cartTable.setItems(cart);
    }

    private void addToCart(Product p) {
        // Ask for quantity
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Thêm vào giỏ");
        dialog.setHeaderText(p.getName());
        dialog.setContentText("Số lượng (tối đa " + p.getStockQuantity() + "):");
        dialog.getDialogPane().setStyle("-fx-background-color:#1a1d2e;");
        dialog.getDialogPane().getScene().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        dialog.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0 || qty > p.getStockQuantity()) {
                    showAlert("Số lượng không hợp lệ (1 – " + p.getStockQuantity() + ")");
                    return;
                }
                // If already in cart, update quantity
                Optional<OrderItem> existing = cart.stream()
                        .filter(item -> item.getProductId() == p.getId()).findFirst();
                if (existing.isPresent()) {
                    OrderItem item = existing.get();
                    int newQty = item.getQuantity() + qty;
                    if (newQty > p.getStockQuantity()) {
                        showAlert("Tổng số lượng vượt quá tồn kho!");
                        return;
                    }
                    item.setQuantity(newQty);
                    item.setSubtotal(item.getPriceAtSale().multiply(BigDecimal.valueOf(newQty)));
                    cartTable.refresh();
                } else {
                    OrderItem item = new OrderItem(p.getId(), p.getName(), qty, p.getPrice());
                    item.setProductCode(p.getProductCode());
                    cart.add(item);
                }
                updateTotal();
            } catch (NumberFormatException e) {
                showAlert("Vui lòng nhập số hợp lệ");
            }
        });
    }

    @FXML
    private void clearCart() {
        cart.clear();
        updateTotal();
        lblCheckoutStatus.setText("");
    }

    private void updateTotal() {
        BigDecimal total = cart.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotal.setText(formatVnd(total));
        lblItemCount.setText(String.valueOf(cart.size()));
    }

    // ── Checkout ──────────────────────────────────────────────

    @FXML
    private void handleCheckout() {
        if (cart.isEmpty()) {
            showAlert("Giỏ hàng đang trống!");
            return;
        }

        Customer selectedCustomer = cmbCustomer.getValue();
        OrderRequest req = new OrderRequest(
                cs.getCurrentUser().getId(),
                selectedCustomer != null ? selectedCustomer.getId() : null,
                new ArrayList<>(cart),
                txtNote.getText().trim()
        );

        setCheckoutLoading(true);
        lblCheckoutStatus.setText("");

        Task<Order> task = new Task<>() {
            @Override protected Order call() throws Exception { return cs.createOrder(req); }
        };

        task.setOnSucceeded(e -> {
            setCheckoutLoading(false);
            Order order = task.getValue();
            lblCheckoutStatus.setText("✅  Thanh toán thành công! Đơn #" + order.getId());
            lblCheckoutStatus.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");

            // Show receipt
            showReceipt(order);
            // Clear cart
            cart.clear();
            updateTotal();
            txtNote.clear();
            cmbCustomer.setValue(null);
            // Reload products to reflect new stock
            loadInitialData();
        });

        task.setOnFailed(e -> {
            setCheckoutLoading(false);
            String err = task.getException() != null ? task.getException().getMessage() : "Lỗi thanh toán";
            lblCheckoutStatus.setText("❌  " + err);
            lblCheckoutStatus.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
        });

        new Thread(task).start();
    }

    private void setCheckoutLoading(boolean loading) {
        checkoutSpinner.setVisible(loading);
        btnCheckout.setDisable(loading);
    }

    private void showReceipt(Order order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hóa đơn thanh toán");
        alert.setHeaderText("✅  Thanh toán thành công — Đơn #" + order.getId());
        alert.getDialogPane().setStyle("-fx-background-color:#1a1d2e;");
        alert.getDialogPane().getScene().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());

        StringBuilder sb = new StringBuilder();
        sb.append("=== HOÁ ĐƠN ===\n\n");
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                sb.append(String.format("%-20s x%d  %s\n",
                        item.getProductName(), item.getQuantity(), formatVnd(item.getSubtotal())));
            }
        }
        sb.append("\n───────────────────────\n");
        sb.append("TỔNG CỘNG: ").append(formatVnd(order.getTotalAmount())).append("\n");
        sb.append("Cảm ơn quý khách! 🙏");
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // ── Logout ───────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { cs.logout(); return null; } };
        task.setOnSucceeded(e -> {
            try { ClientMain.showLoginScreen(); } catch (Exception ignored) {}
        });
        new Thread(task).start();
    }

    // ── Helpers ──────────────────────────────────────────────

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return nf.format(amount) + " ₫";
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("Cảnh báo");
        a.getDialogPane().setStyle("-fx-background-color:#1a1d2e;");
        a.showAndWait();
    }
}
