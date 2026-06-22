package com.minimart.client.ui;

import com.minimart.client.ClientMain;
import com.minimart.client.service.ClientService;
import com.minimart.common.dto.DashboardData;
import com.minimart.common.dto.DashboardData.LowStockItem;
import com.minimart.common.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Admin Dashboard (AdminDashboard.fxml).
 *
 * <p>Dynamically renders different content panels into {@code contentStack}
 * based on the selected navigation button, avoiding multiple FXML files
 * for sub-panels and keeping navigation snappy.</p>
 */
public class AdminDashboardController implements Initializable {

    // ── FXML injections ──────────────────────────────────────
    @FXML private Label              lblCurrentUser;
    @FXML private Label              lblCurrentRole;
    @FXML private Label              lblPageTitle;
    @FXML private Label              lblUserChip;
    @FXML private ProgressIndicator  mainSpinner;
    @FXML private StackPane          contentStack;

    // Sidebar nav buttons
    @FXML private Button btnDashboard, btnProducts, btnCustomers,
                         btnOrders, btnUsers, btnLogs;

    private final ClientService  cs  = ClientService.getInstance();
    private final NumberFormat   nf  = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = cs.getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getFullName());
            lblCurrentRole.setText(user.getRole());
            lblUserChip.setText(user.getUsername() + "  " + user.getRole());
        }
        showDashboard(); // Load dashboard on start
    }

    // ── Navigation handlers ──────────────────────────────────

    @FXML private void showDashboard() {
        setActiveNav(btnDashboard);
        lblPageTitle.setText("Bảng điều khiển");
        loadAsync("Đang tải dashboard...", cs::getDashboardData, this::buildDashboardPane);
    }

    @FXML private void showProducts() {
        setActiveNav(btnProducts);
        lblPageTitle.setText("Quản lý Sản phẩm");
        loadAsync("Đang tải sản phẩm...", cs::getProducts, this::buildProductsPane);
    }

    @FXML private void showCustomers() {
        setActiveNav(btnCustomers);
        lblPageTitle.setText("Quản lý Khách hàng");
        loadAsync("Đang tải khách hàng...", cs::getCustomers, this::buildCustomersPane);
    }

    @FXML private void showOrders() {
        setActiveNav(btnOrders);
        lblPageTitle.setText("Quản lý Đơn hàng");
        loadAsync("Đang tải đơn hàng...", cs::getOrders, this::buildOrdersPane);
    }

    @FXML private void showUsers() {
        setActiveNav(btnUsers);
        lblPageTitle.setText("Quản lý Tài khoản");
        loadAsync("Đang tải người dùng...", cs::getUsers, this::buildUsersPane);
    }

    @FXML private void showLogs() {
        setActiveNav(btnLogs);
        lblPageTitle.setText("Nhật ký hệ thống");
        loadAsync("Đang tải nhật ký...", cs::getLogs, this::buildLogsPane);
    }

    @FXML private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn đăng xuất?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Đăng xuất");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception { cs.logout(); return null; }
                };
                task.setOnSucceeded(e -> {
                    try { ClientMain.showLoginScreen(); } catch (Exception ignored) {}
                });
                new Thread(task).start();
            }
        });
    }

    // ── Generic async loader ─────────────────────────────────

    @FunctionalInterface interface DataLoader<T> { T load() throws Exception; }
    @FunctionalInterface interface PaneBuilder<T> { javafx.scene.Node build(T data); }

    private <T> void loadAsync(String loadingMsg, DataLoader<T> loader, PaneBuilder<T> builder) {
        contentStack.getChildren().clear();
        Label loading = new Label("⏳  " + loadingMsg);
        loading.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px;");
        contentStack.getChildren().add(loading);
        mainSpinner.setVisible(true);

        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return loader.load(); }
        };
        task.setOnSucceeded(e -> {
            mainSpinner.setVisible(false);
            contentStack.getChildren().setAll(builder.build(task.getValue()));
        });
        task.setOnFailed(e -> {
            mainSpinner.setVisible(false);
            String err = task.getException() != null ? task.getException().getMessage() : "Error";
            Label errLabel = new Label("❌  " + err);
            errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size:14px;");
            contentStack.getChildren().setAll(errLabel);
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Dashboard pane ───────────────────────────────────────

    private javafx.scene.Node buildDashboardPane(DashboardData data) {
        VBox root = new VBox(20);

        // KPI row
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
            kpiCard("💰", formatVnd(data.getTotalRevenue()),  "Tổng doanh thu", "#10b981"),
            kpiCard("🧾", String.valueOf(data.getTotalOrders()), "Tổng đơn hàng",   "#3b82f6"),
            kpiCard("📦", String.valueOf(data.getTotalProducts()), "Sản phẩm",       "#fbbf24"),
            kpiCard("👥", String.valueOf(data.getTotalCustomers()), "Khách hàng",    "#ec4899")
        );
        for (javafx.scene.Node c : kpiRow.getChildren()) HBox.setHgrow(c, Priority.ALWAYS);

        // Charts row
        HBox chartsRow = new HBox(16);

        // Bar chart — daily revenue
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Ngày");
        yAxis.setLabel("VND");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Doanh thu 30 ngày qua");
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        barChart.setPrefHeight(300);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (data.getDailyRevenue() != null) {
            data.getDailyRevenue().forEach((date, amount) ->
                series.getData().add(new XYChart.Data<>(date.substring(5), amount)));
        }
        barChart.getData().add(series);

        VBox chartPanel = panelWrap("📈  Biểu đồ doanh thu", barChart);
        HBox.setHgrow(chartPanel, Priority.ALWAYS);

        // Low-stock table
        TableView<LowStockItem> lowTable = new TableView<>();
        lowTable.setPrefHeight(300);
        TableColumn<LowStockItem, String> colCode  = col("Mã SP", LowStockItem::getProductCode, 80);
        TableColumn<LowStockItem, String> colName  = col("Tên sản phẩm", LowStockItem::getName, 180);
        TableColumn<LowStockItem, Integer> colQty  = new TableColumn<>("Tồn kho");
        colQty.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getStockQuantity()));
        colQty.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(item));
                setStyle(item <= 5 ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;"
                                   : "-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
            }
        });
        colQty.setPrefWidth(80);
        TableColumn<LowStockItem, String> colUnit = col("ĐVT", LowStockItem::getUnit, 60);
        lowTable.getColumns().addAll(colCode, colName, colQty, colUnit);
        if (data.getLowStockProducts() != null) {
            lowTable.setItems(FXCollections.observableArrayList(data.getLowStockProducts()));
        }
        VBox lowPanel = panelWrap("⚠️  Cảnh báo tồn kho thấp", lowTable);
        lowPanel.setPrefWidth(380);

        chartsRow.getChildren().addAll(chartPanel, lowPanel);

        root.getChildren().addAll(kpiRow, chartsRow);
        return root;
    }

    // ── Products Grid pane (Phase 7 visual overhaul) ──────────

    private javafx.scene.Node buildProductsPane(List<Product> products) {
        VBox root = new VBox(14);

        // Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Tìm kiếm sản phẩm...");
        searchField.getStyleClass().add("search-field");
        Button btnAdd    = new Button("➕  Thêm mới"); btnAdd.getStyleClass().add("btn-primary");
        Button btnExport = new Button("📤  Xuất XML");  btnExport.getStyleClass().add("btn-secondary");
        Button btnImport = new Button("📥  Nhập XML");  btnImport.getStyleClass().add("btn-secondary");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(searchField, spacer, btnImport, btnExport, btnAdd);

        // Responsive FlowPane for cards
        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: transparent; -fx-padding: 4;");

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Initial render
        renderAdminProductGrid(grid, products, products);

        // Search trigger
        searchField.textProperty().addListener((obs, o, keyword) -> {
            if (keyword.isBlank()) {
                renderAdminProductGrid(grid, products, products);
            } else {
                String kw = keyword.toLowerCase();
                renderAdminProductGrid(grid, products.stream().filter(p ->
                    p.getName().toLowerCase().contains(kw) ||
                    p.getProductCode().toLowerCase().contains(kw)).toList(), products);
            }
        });

        btnAdd.setOnAction(e -> showAddProductDialog(grid, products));
        btnExport.setOnAction(e -> handleExportXml());
        btnImport.setOnAction(e -> handleImportXml(grid, products));

        root.getChildren().addAll(toolbar, scroll);
        return root;
    }

    private void renderAdminProductGrid(FlowPane grid, List<Product> displayed, List<Product> allProducts) {
        grid.getChildren().clear();
        for (Product p : displayed) {
            grid.getChildren().add(buildAdminProductCard(p, grid, allProducts));
        }
    }

    private VBox buildAdminProductCard(Product p, FlowPane grid, List<Product> allProducts) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 10, 10, 10));
        card.setPrefWidth(160);
        card.setPrefHeight(230);
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

        // ── Product Info ──────────────────────────────────────
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
        name.setPrefHeight(36);

        Label price = new Label(formatVnd(p.getPrice()));
        price.getStyleClass().add("product-card-price");

        Label stock = new Label("Còn: " + p.getStockQuantity() + " " + p.getUnit());
        stock.getStyleClass().add("product-card-stock");
        if (p.getStockQuantity() <= 5) {
            stock.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }

        // ── Admin Actions Row ─────────────────────────────────
        Button editBtn = new Button("✏️");
        editBtn.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #34d399; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;");
        editBtn.setOnAction(e -> showEditProductDialog(p, grid, allProducts));

        Button delBtn = new Button("🗑️");
        delBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #f87171; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px;");
        delBtn.setOnAction(e -> confirmDeleteProduct(p, grid, allProducts));

        HBox actionsRow = new HBox(12, editBtn, delBtn);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(imgContainer, code, badge, name, price, stock, actionsRow);

        // Tactile Hover scale zoom
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

        // Grey out inactive products
        if (!"ACTIVE".equals(p.getStatus())) {
            card.setOpacity(0.5);
        }

        return card;
    }

    // ── Customers pane ───────────────────────────────────────

    private javafx.scene.Node buildCustomersPane(List<Customer> customers) {
        VBox root = new VBox(14);
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Tìm kiếm khách hàng...");
        searchField.getStyleClass().add("search-field");
        Button btnAdd = new Button("➕  Thêm khách hàng");
        btnAdd.getStyleClass().add("btn-primary");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(searchField, spacer, btnAdd);

        TableView<Customer> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(customers));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Customer,Integer> colId   = col("ID",           Customer::getId,           50);
        TableColumn<Customer,String>  colName = col("Tên khách hàng",Customer::getCustomerName,200);
        TableColumn<Customer,String>  colPhone= col("Số điện thoại",Customer::getPhoneNumber, 130);
        TableColumn<Customer,String>  colEmail= col("Email",        Customer::getEmail,        160);
        TableColumn<Customer,String>  colAddr = col("Địa chỉ",      Customer::getAddress,      160);

        TableColumn<Customer, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button edit = new Button("✏️");
            final Button del  = new Button("🗑️");
            {
                edit.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #34d399; -fx-background-radius:6; -fx-cursor:hand;");
                del.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #f87171; -fx-background-radius:6; -fx-cursor:hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Customer c = getTableView().getItems().get(getIndex());
                    edit.setOnAction(e -> showEditCustomerDialog(c, getTableView(), customers));
                    del.setOnAction(e -> confirmDeleteCustomer(c, getTableView(), customers));
                    setGraphic(new HBox(6, edit, del));
                }
            }
        });
        colActions.setPrefWidth(90);

        table.getColumns().addAll(colId, colName, colPhone, colEmail, colAddr, colActions);

        searchField.textProperty().addListener((obs, o, kw) ->
            table.setItems(FXCollections.observableArrayList(
                kw.isBlank() ? customers : customers.stream()
                    .filter(c -> c.getCustomerName().toLowerCase().contains(kw.toLowerCase())).toList())));

        btnAdd.setOnAction(e -> showAddCustomerDialog(table, customers));

        root.getChildren().addAll(toolbar, table);
        return root;
    }

    private void showAddCustomerDialog(TableView<Customer> table, List<Customer> allCustomers) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Thêm khách hàng mới");
        styleDialog(dialog);
        GridPane formGrid = customerFormGrid(null);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractCustomer(formGrid, null) : null);
        dialog.showAndWait().ifPresent(c -> {
            Task<Customer> task = new Task<>() { @Override protected Customer call() throws Exception { return cs.addCustomer(c); } };
            task.setOnSucceeded(e -> {
                allCustomers.add(task.getValue());
                table.setItems(FXCollections.observableArrayList(allCustomers));
            });
            task.setOnFailed(e -> showError("Thêm khách hàng thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void showEditCustomerDialog(Customer customer, TableView<Customer> table, List<Customer> allCustomers) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa khách hàng");
        styleDialog(dialog);
        GridPane formGrid = customerFormGrid(customer);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractCustomer(formGrid, customer) : null);
        dialog.showAndWait().ifPresent(c -> {
            Task<Customer> task = new Task<>() { @Override protected Customer call() throws Exception { return cs.updateCustomer(c); } };
            task.setOnSucceeded(e -> {
                int idx = allCustomers.indexOf(customer);
                if (idx >= 0) allCustomers.set(idx, task.getValue());
                table.setItems(FXCollections.observableArrayList(allCustomers));
            });
            task.setOnFailed(e -> showError("Cập nhật thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void confirmDeleteCustomer(Customer customer, TableView<Customer> table, List<Customer> allCustomers) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa khách hàng: " + customer.getCustomerName() + "?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { cs.deleteCustomer(customer.getId()); return null; } };
                task.setOnSucceeded(e -> {
                    allCustomers.remove(customer);
                    table.setItems(FXCollections.observableArrayList(allCustomers));
                });
                task.setOnFailed(e -> showError("Xóa khách hàng thất bại", task.getException()));
                new Thread(task).start();
            }
        });
    }

    private GridPane customerFormGrid(Customer c) {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:#111827;");

        // 1. Tên khách hàng
        Label lblName = new Label("Tên khách hàng:");
        lblName.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfName = new TextField(c != null ? c.getCustomerName() : "");
        tfName.setId("cust_name");
        tfName.setPrefWidth(320);
        grid.add(lblName, 0, 0); grid.add(tfName, 1, 0);

        // 2. Số điện thoại
        Label lblPhone = new Label("Số điện thoại:");
        lblPhone.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfPhone = new TextField(c != null ? c.getPhoneNumber() : "");
        tfPhone.setId("cust_phone");
        grid.add(lblPhone, 0, 1); grid.add(tfPhone, 1, 1);

        // 3. Email
        Label lblEmail = new Label("Email:");
        lblEmail.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfEmail = new TextField(c != null ? c.getEmail() : "");
        tfEmail.setId("cust_email");
        grid.add(lblEmail, 0, 2); grid.add(tfEmail, 1, 2);

        // 4. Địa chỉ
        Label lblAddr = new Label("Địa chỉ:");
        lblAddr.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfAddr = new TextField(c != null ? c.getAddress() : "");
        tfAddr.setId("cust_addr");
        grid.add(lblAddr, 0, 3); grid.add(tfAddr, 1, 3);

        return grid;
    }

    private Customer extractCustomer(GridPane grid, Customer existing) {
        Customer c = existing != null ? existing : new Customer();
        TextField tfName = (TextField) grid.lookup("#cust_name");
        TextField tfPhone = (TextField) grid.lookup("#cust_phone");
        TextField tfEmail = (TextField) grid.lookup("#cust_email");
        TextField tfAddr = (TextField) grid.lookup("#cust_addr");

        c.setCustomerName(tfName.getText().trim());
        c.setPhoneNumber(tfPhone.getText().trim());
        c.setEmail(tfEmail.getText().trim());
        c.setAddress(tfAddr.getText().trim());
        return c;
    }

    // ── Orders pane ──────────────────────────────────────────

    private javafx.scene.Node buildOrdersPane(List<Order> orders) {
        VBox root = new VBox(14);

        TableView<Order> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(orders));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Order,Integer> colId   = col("Mã ĐH",    Order::getId,          60);
        TableColumn<Order,String>  colDate = new TableColumn<>("Ngày đặt");
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getOrderDate() != null ? cd.getValue().getOrderDate().format(DTF) : ""));
        colDate.setPrefWidth(140);
        TableColumn<Order,String>  colCashier = col("Thu ngân",   Order::getCashierName, 120);
        TableColumn<Order,String>  colCust    = col("Khách hàng", Order::getCustomerName,140);
        TableColumn<Order,BigDecimal> colTotal = col("Tổng tiền", Order::getTotalAmount, 140);
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(formatVnd(item));
                setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        });
        colTotal.setPrefWidth(140);

        TableColumn<Order, Void> colDetail = new TableColumn<>("Chi tiết");
        colDetail.setCellFactory(tc -> new TableCell<>() {
            final Button btn = new Button("👁  Xem");
            { btn.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #34d399; -fx-background-radius:6; -fx-cursor:hand;"); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) btn.setOnAction(e ->
                    showOrderDetail(getTableView().getItems().get(getIndex())));
                setGraphic(empty ? null : btn);
            }
        });
        colDetail.setPrefWidth(90);

        table.getColumns().addAll(colId, colDate, colCashier, colCust, colTotal, colDetail);
        root.getChildren().add(table);
        return root;
    }

    // ── Users pane ───────────────────────────────────────────

    private javafx.scene.Node buildUsersPane(List<User> users) {
        VBox root = new VBox(14);
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Button btnAdd = new Button("➕  Thêm tài khoản"); btnAdd.getStyleClass().add("btn-primary");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(spacer, btnAdd);

        TableView<User> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(users));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<User, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button edit = new Button("✏️");
            final Button del  = new Button("🗑️");
            {
                edit.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #34d399; -fx-background-radius:6; -fx-cursor:hand;");
                del.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #f87171; -fx-background-radius:6; -fx-cursor:hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    edit.setOnAction(e -> showEditUserDialog(u, getTableView(), users));
                    del.setOnAction(e -> confirmDeleteUser(u, getTableView(), users));
                    setGraphic(new HBox(6, edit, del));
                }
            }
        });
        colActions.setPrefWidth(90);

        table.getColumns().addAll(
            col("ID",           User::getId,       50),
            col("Tên đăng nhập",User::getUsername, 130),
            col("Họ tên",       User::getFullName, 180),
            col("Vai trò",      User::getRole,     90),
            buildActiveColumn(),
            colActions
        );

        btnAdd.setOnAction(e -> showAddUserDialog(table, users));

        root.getChildren().addAll(toolbar, table);
        return root;
    }

    private void showAddUserDialog(TableView<User> table, List<User> allUsers) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Thêm tài khoản mới");
        styleDialog(dialog);
        GridPane formGrid = userFormGrid(null);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractUser(formGrid, null) : null);
        dialog.showAndWait().ifPresent(u -> {
            String rawPassword = extractUserPassword(formGrid);
            if (rawPassword == null || rawPassword.length() < 6) {
                showError("Lỗi dữ liệu", new IllegalArgumentException("Mật khẩu phải từ 6 ký tự trở lên"));
                return;
            }
            Task<User> task = new Task<>() { @Override protected User call() throws Exception { return cs.addUser(u, rawPassword); } };
            task.setOnSucceeded(e -> {
                allUsers.add(task.getValue());
                table.setItems(FXCollections.observableArrayList(allUsers));
            });
            task.setOnFailed(e -> showError("Thêm tài khoản thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void showEditUserDialog(User user, TableView<User> table, List<User> allUsers) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa tài khoản");
        styleDialog(dialog);
        GridPane formGrid = userFormGrid(user);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractUser(formGrid, user) : null);
        dialog.showAndWait().ifPresent(u -> {
            Task<User> task = new Task<>() { @Override protected User call() throws Exception { return cs.updateUser(u); } };
            task.setOnSucceeded(e -> {
                int idx = allUsers.indexOf(user);
                if (idx >= 0) allUsers.set(idx, task.getValue());
                table.setItems(FXCollections.observableArrayList(allUsers));
            });
            task.setOnFailed(e -> showError("Cập nhật tài khoản thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void confirmDeleteUser(User user, TableView<User> table, List<User> allUsers) {
        if (cs.getCurrentUser() != null && cs.getCurrentUser().getId() == user.getId()) {
            showError("Không hợp lệ", new IllegalArgumentException("Bạn không thể tự xóa tài khoản của chính mình!"));
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa tài khoản: " + user.getUsername() + "?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { cs.deleteUser(user.getId()); return null; } };
                task.setOnSucceeded(e -> {
                    allUsers.remove(user);
                    table.setItems(FXCollections.observableArrayList(allUsers));
                });
                task.setOnFailed(e -> showError("Xóa tài khoản thất bại", task.getException()));
                new Thread(task).start();
            }
        });
    }

    private GridPane userFormGrid(User u) {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:#111827;");

        // 1. Tên đăng nhập
        Label lblUsername = new Label("Tên đăng nhập:");
        lblUsername.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfUsername = new TextField(u != null ? u.getUsername() : "");
        tfUsername.setId("user_username");
        tfUsername.setPrefWidth(320);
        if (u != null) {
            tfUsername.setEditable(false);
            tfUsername.setDisable(true);
        }
        grid.add(lblUsername, 0, 0); grid.add(tfUsername, 1, 0);

        // 2. Họ tên
        Label lblFullName = new Label("Họ tên:");
        lblFullName.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfFullName = new TextField(u != null ? u.getFullName() : "");
        tfFullName.setId("user_fullname");
        grid.add(lblFullName, 0, 1); grid.add(tfFullName, 1, 1);

        // 3. Vai trò
        Label lblRole = new Label("Vai trò:");
        lblRole.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.setItems(FXCollections.observableArrayList("ADMIN", "EMPLOYEE"));
        cmbRole.setValue(u != null ? u.getRole() : "EMPLOYEE");
        cmbRole.setId("user_role");
        cmbRole.setPrefWidth(320);
        grid.add(lblRole, 0, 2); grid.add(cmbRole, 1, 2);

        // 4. Trạng thái (Hoạt động / Bị khóa)
        Label lblActive = new Label("Trạng thái:");
        lblActive.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        CheckBox cbActive = new CheckBox("Kích hoạt tài khoản");
        cbActive.setStyle("-fx-text-fill:#e2e8f0;");
        cbActive.setSelected(u == null || u.isActive());
        cbActive.setId("user_active");
        grid.add(lblActive, 0, 3); grid.add(cbActive, 1, 3);

        // 5. Mật khẩu (Chỉ cho tài khoản mới)
        if (u == null) {
            Label lblPassword = new Label("Mật khẩu:");
            lblPassword.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
            PasswordField pfPassword = new PasswordField();
            pfPassword.setId("user_password");
            pfPassword.setPrefWidth(320);
            grid.add(lblPassword, 0, 4); grid.add(pfPassword, 1, 4);
        }

        return grid;
    }

    private User extractUser(GridPane grid, User existing) {
        User u = existing != null ? existing : new User();
        TextField tfUsername = (TextField) grid.lookup("#user_username");
        TextField tfFullName = (TextField) grid.lookup("#user_fullname");
        @SuppressWarnings("unchecked")
        ComboBox<String> cmbRole = (ComboBox<String>) grid.lookup("#user_role");
        CheckBox cbActive = (CheckBox) grid.lookup("#user_active");

        u.setUsername(tfUsername.getText().trim());
        u.setFullName(tfFullName.getText().trim());
        u.setRole(cmbRole.getValue());
        u.setActive(cbActive.isSelected());
        return u;
    }

    private String extractUserPassword(GridPane grid) {
        PasswordField pfPassword = (PasswordField) grid.lookup("#user_password");
        return pfPassword != null ? pfPassword.getText() : null;
    }

    private TableColumn<User, Boolean> buildActiveColumn() {
        TableColumn<User, Boolean> col = new TableColumn<>("Trạng thái");
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().isActive()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item ? "Hoạt động" : "Bị khóa");
                badge.getStyleClass().add(item ? "badge-active" : "badge-inactive");
                setGraphic(badge);
            }
        });
        col.setPrefWidth(100);
        return col;
    }

    // ── Logs pane ────────────────────────────────────────────

    private javafx.scene.Node buildLogsPane(List<SystemLog> logs) {
        VBox root = new VBox(14);

        TableView<SystemLog> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(logs));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SystemLog,String> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getLogTime() != null ? cd.getValue().getLogTime().format(DTF) : ""));
        colTime.setPrefWidth(130);

        TableColumn<SystemLog,String> colLevel = col("Mức", SystemLog::getLevel, 70);
        colLevel.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("ERROR".equals(item)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else if ("WARN".equals(item)) {
                    setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                }
            }
        });
        colLevel.setPrefWidth(70);

        table.getColumns().addAll(
            colTime, colLevel,
            col("Hành động", SystemLog::getAction,   110),
            col("Người dùng",SystemLog::getUsername,  100),
            col("Nội dung",  SystemLog::getMessage,   350),
            col("IP",        SystemLog::getIpAddress,  100)
        );

        root.getChildren().add(table);
        return root;
    }

    // ── Product dialogs (Category and Image fields overhaul) ──

    private void showAddProductDialog(FlowPane grid, List<Product> allProducts) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Thêm sản phẩm mới");
        styleDialog(dialog);
        GridPane formGrid = productFormGrid(null);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractProduct(formGrid, null) : null);
        dialog.showAndWait().ifPresent(p -> {
            Task<Product> task = new Task<>() { @Override protected Product call() throws Exception { return cs.addProduct(p); } };
            task.setOnSucceeded(e -> {
                allProducts.add(task.getValue());
                renderAdminProductGrid(grid, allProducts, allProducts);
            });
            task.setOnFailed(e -> showError("Thêm sản phẩm thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void showEditProductDialog(Product product, FlowPane grid, List<Product> allProducts) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa sản phẩm");
        styleDialog(dialog);
        GridPane formGrid = productFormGrid(product);
        dialog.getDialogPane().setContent(formGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: linear-gradient(to right,#10b981,#059669); -fx-text-fill:white; -fx-background-radius:6; -fx-font-weight:bold;");

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? extractProduct(formGrid, product) : null);
        dialog.showAndWait().ifPresent(p -> {
            Task<Product> task = new Task<>() { @Override protected Product call() throws Exception { return cs.updateProduct(p); } };
            task.setOnSucceeded(e -> {
                int idx = allProducts.indexOf(product);
                if (idx >= 0) allProducts.set(idx, task.getValue());
                renderAdminProductGrid(grid, allProducts, allProducts);
            });
            task.setOnFailed(e -> showError("Cập nhật thất bại", task.getException()));
            new Thread(task).start();
        });
    }

    private void confirmDeleteProduct(Product product, FlowPane grid, List<Product> allProducts) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa sản phẩm: " + product.getName() + "?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Xác nhận xóa");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { cs.deleteProduct(product.getId()); return null; } };
                task.setOnSucceeded(e -> {
                    allProducts.remove(product);
                    renderAdminProductGrid(grid, allProducts, allProducts);
                });
                task.setOnFailed(e -> showError("Xóa sản phẩm thất bại", task.getException()));
                new Thread(task).start();
            }
        });
    }

    private GridPane productFormGrid(Product p) {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color:#111827;");

        // 1. Mã sản phẩm
        Label lblCode = new Label("Mã sản phẩm:");
        lblCode.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfCode = new TextField(p != null ? p.getProductCode() : "");
        tfCode.setId("field_code");
        tfCode.setPrefWidth(320);
        grid.add(lblCode, 0, 0); grid.add(tfCode, 1, 0);

        // 2. Tên sản phẩm
        Label lblName = new Label("Tên sản phẩm:");
        lblName.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfName = new TextField(p != null ? p.getName() : "");
        tfName.setId("field_name");
        grid.add(lblName, 0, 1); grid.add(tfName, 1, 1);

        // 3. Danh mục
        Label lblCat = new Label("Danh mục:");
        lblCat.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        ComboBox<String> cmbCat = new ComboBox<>();
        cmbCat.setItems(FXCollections.observableArrayList(
            "Đồ uống", "Thực phẩm đóng gói", "Bánh kẹo & Ăn vặt", "Hóa mỹ phẩm & Đồ gia dụng", "Thực phẩm tươi & Sữa"
        ));
        if (p != null && p.getCategory() != null) {
            cmbCat.setValue(p.getCategory());
        } else {
            cmbCat.setValue("Đồ uống");
        }
        cmbCat.setId("field_category");
        cmbCat.setPrefWidth(320);
        grid.add(lblCat, 0, 2); grid.add(cmbCat, 1, 2);

        // 4. Đơn vị tính
        Label lblUnit = new Label("Đơn vị tính:");
        lblUnit.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfUnit = new TextField(p != null ? p.getUnit() : "Cái");
        tfUnit.setId("field_unit");
        grid.add(lblUnit, 0, 3); grid.add(tfUnit, 1, 3);

        // 5. Đơn giá
        Label lblPrice = new Label("Đơn giá:");
        lblPrice.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfPrice = new TextField(p != null ? p.getPrice().toPlainString() : "");
        tfPrice.setId("field_price");
        grid.add(lblPrice, 0, 4); grid.add(tfPrice, 1, 4);

        // 6. Tồn kho
        Label lblStock = new Label("Tồn kho:");
        lblStock.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfStock = new TextField(p != null ? String.valueOf(p.getStockQuantity()) : "0");
        tfStock.setId("field_stock");
        grid.add(lblStock, 0, 5); grid.add(tfStock, 1, 5);

        // 7. Đường dẫn ảnh
        Label lblImg = new Label("Đường dẫn ảnh:");
        lblImg.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        HBox imgBox = new HBox(8);
        imgBox.setAlignment(Pos.CENTER_LEFT);
        TextField tfImg = new TextField(p != null && p.getImagePath() != null ? p.getImagePath() : "");
        tfImg.setId("field_image");
        tfImg.setPrefWidth(220);
        Button btnBrowseImg = new Button("Chọn file...");
        btnBrowseImg.setStyle("-fx-background-color:#1f2937; -fx-text-fill:#f8fafc; -fx-border-color:#374151; -fx-border-radius:6; -fx-background-radius:6; -fx-cursor:hand;");
        btnBrowseImg.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Chọn hình ảnh sản phẩm");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File selectedFile = fc.showOpenDialog(grid.getScene().getWindow());
            if (selectedFile != null) {
                tfImg.setText(selectedFile.getAbsolutePath());
            }
        });
        imgBox.getChildren().addAll(tfImg, btnBrowseImg);
        grid.add(lblImg, 0, 6); grid.add(imgBox, 1, 6);

        // 8. Mô tả
        Label lblDesc = new Label("Mô tả:");
        lblDesc.setStyle("-fx-text-fill:#e2e8f0; -fx-font-weight:bold;");
        TextField tfDesc = new TextField(p != null && p.getDescription() != null ? p.getDescription() : "");
        tfDesc.setId("field_desc");
        grid.add(lblDesc, 0, 7); grid.add(tfDesc, 1, 7);

        return grid;
    }

    private Product extractProduct(GridPane grid, Product existing) {
        Product p = existing != null ? existing : new Product();
        TextField tfCode = (TextField) grid.lookup("#field_code");
        TextField tfName = (TextField) grid.lookup("#field_name");
        @SuppressWarnings("unchecked")
        ComboBox<String> cmbCat = (ComboBox<String>) grid.lookup("#field_category");
        TextField tfUnit = (TextField) grid.lookup("#field_unit");
        TextField tfPrice = (TextField) grid.lookup("#field_price");
        TextField tfStock = (TextField) grid.lookup("#field_stock");
        TextField tfImg = (TextField) grid.lookup("#field_image");
        TextField tfDesc = (TextField) grid.lookup("#field_desc");

        p.setProductCode(tfCode.getText().trim());
        p.setName(tfName.getText().trim());
        p.setCategory(cmbCat.getValue() != null ? cmbCat.getValue() : "Khác");
        p.setUnit(tfUnit.getText().trim());
        try { p.setPrice(new BigDecimal(tfPrice.getText().trim().replace(",", ""))); } catch (Exception ignored) {}
        try { p.setStockQuantity(Integer.parseInt(tfStock.getText().trim())); } catch (Exception ignored) {}
        p.setImagePath(tfImg.getText().trim().isEmpty() ? null : tfImg.getText().trim());
        p.setDescription(tfDesc.getText().trim());
        p.setStatus("ACTIVE");
        return p;
    }

    private void showOrderDetail(Order order) {
        Task<Order> task = new Task<>() { @Override protected Order call() throws Exception { return cs.getOrderDetail(order.getId()); } };
        task.setOnSucceeded(e -> {
            Order detail = task.getValue();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Chi tiết đơn hàng #" + detail.getId());
            alert.setHeaderText("Đơn hàng #" + detail.getId() + " — " +
                    (detail.getOrderDate() != null ? detail.getOrderDate().format(DTF) : ""));
            styleDialog(alert);

            StringBuilder sb = new StringBuilder();
            sb.append("Thu ngân: ").append(detail.getCashierName()).append("\n");
            sb.append("Khách hàng: ").append(
                    detail.getCustomerName() != null ? detail.getCustomerName() : "Khách vãng lai").append("\n\n");
            sb.append("─────────────────────────────────\n");
            if (detail.getItems() != null) {
                for (OrderItem item : detail.getItems()) {
                    sb.append(String.format("%-25s x%d × %s = %s\n",
                            item.getProductName(), item.getQuantity(),
                            formatVnd(item.getPriceAtSale()), formatVnd(item.getSubtotal())));
                }
            }
            sb.append("─────────────────────────────────\n");
            sb.append("TỔNG CỘNG: ").append(formatVnd(detail.getTotalAmount()));

            alert.setContentText(sb.toString());
            alert.showAndWait();
        });
        new Thread(task).start();
    }

    // ── XML Export / Import ──────────────────────────────────

    private void handleExportXml() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Lưu file XML");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("XML files", "*.xml"));
        fc.setInitialFileName("products_export.xml");
        File file = fc.showSaveDialog(ClientMain.getPrimaryStage());
        if (file == null) return;

        Task<String> task = new Task<>() { @Override protected String call() throws Exception { return cs.exportProductsXml(); } };
        task.setOnSucceeded(e -> {
            try {
                Files.writeString(file.toPath(), task.getValue());
                showInfo("Xuất XML thành công!", "Đã lưu " + file.getAbsolutePath());
            } catch (Exception ex) { showError("Ghi file thất bại", ex); }
        });
        task.setOnFailed(e -> showError("Xuất XML thất bại", task.getException()));
        new Thread(task).start();
    }

    private void handleImportXml(FlowPane grid, List<Product> allProducts) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Chọn file XML để nhập");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = fc.showOpenDialog(ClientMain.getPrimaryStage());
        if (file == null) return;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                String xml = Files.readString(file.toPath());
                return cs.importProductsXml(xml);
            }
        };
        task.setOnSucceeded(e -> {
            showInfo("Nhập XML thành công!", "Đã nhập " + task.getValue() + " sản phẩm.");
            showProducts(); // Reload Grid
        });
        task.setOnFailed(e -> showError("Nhập XML thất bại", task.getException()));
        new Thread(task).start();
    }

    // ── Utility helpers ──────────────────────────────────────

    private void setActiveNav(Button active) {
        for (Button b : List.of(btnDashboard, btnProducts, btnCustomers, btnOrders, btnUsers, btnLogs)) {
            b.getStyleClass().removeAll("nav-button-active");
        }
        active.getStyleClass().add("nav-button-active");
    }

    private VBox panelWrap(String title, javafx.scene.Node content) {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel");
        Label titleLabel = new Label(title); titleLabel.getStyleClass().add("panel-title");
        panel.getChildren().addAll(titleLabel, content);
        return panel;
    }

    private HBox kpiCard(String icon, String value, String label, String color) {
        HBox card = new HBox(14);
        card.getStyleClass().add("kpi-card");
        card.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon); iconLabel.setStyle("-fx-font-size:28px;");
        VBox info = new VBox(4);
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        Label lblLabel = new Label(label); lblLabel.getStyleClass().add("kpi-label");
        info.getChildren().addAll(valLabel, lblLabel);
        card.getChildren().addAll(iconLabel, info);
        return card;
    }

    private <S, T> TableColumn<S, T> col(String title, java.util.function.Function<S, T> extractor, double width) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(extractor.apply(cd.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return nf.format(amount) + " ₫";
    }

    private void showError(String header, Throwable ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(header);
            alert.setContentText(ex != null ? ex.getMessage() : "Lỗi không xác định");
            styleDialog(alert);
            alert.showAndWait();
        });
    }

    private void showInfo(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(header);
            alert.setContentText(content);
            styleDialog(alert);
            alert.showAndWait();
        });
    }

    private void styleDialog(Dialog<?> d) {
        d.getDialogPane().setStyle("-fx-background-color: #111827;");
        d.getDialogPane().getScene().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
    }
}
