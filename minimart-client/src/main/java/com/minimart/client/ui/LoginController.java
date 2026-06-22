package com.minimart.client.ui;

import com.minimart.client.ClientMain;
import com.minimart.client.service.ClientService;
import com.minimart.common.constants.Roles;
import com.minimart.common.dto.LoginResponse;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Login screen (LoginView.fxml).
 *
 * <p>Uses a JavaFX {@link Task} to perform the network login call
 * on a background thread, keeping the UI responsive.</p>
 */
public class LoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    // ── Login Card Components ───────────────────────────────
    @FXML private VBox        loginCard;
    @FXML private Label       lblStatus;
    @FXML private TextField   txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button      btnLogin;
    @FXML private ProgressIndicator spinner;
    @FXML private Label       lblVersion;

    // ── Register Card Components ────────────────────────────
    @FXML private VBox        registerCard;
    @FXML private TextField   txtRegFullName;
    @FXML private TextField   txtRegUsername;
    @FXML private PasswordField txtRegPassword;
    @FXML private Label       lblRegStatus;
    @FXML private Button      btnRegister;
    @FXML private ProgressIndicator spinnerReg;

    private final ClientService clientService = ClientService.getInstance();
    private final javafx.beans.property.BooleanProperty isLoading = new javafx.beans.property.SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblVersion.setText("Mini Mart v1.0 — © 2026");
        spinner.setVisible(false);
        spinnerReg.setVisible(false);
        lblStatus.setText("");
        lblRegStatus.setText("");

        // Allow pressing Enter to navigate/login
        txtUsername.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtPassword.requestFocus(); });
        txtPassword.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });

        // Enter navigation for Registration
        txtRegFullName.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtRegUsername.requestFocus(); });
        txtRegUsername.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) txtRegPassword.requestFocus(); });
        txtRegPassword.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) { if (!btnRegister.isDisabled()) handleRegister(); } });

        // Input validation — enable login button only when both fields are non-empty and not loading
        btnLogin.disableProperty().bind(
                txtUsername.textProperty().isEmpty()
                        .or(txtPassword.textProperty().isEmpty())
                        .or(isLoading));

        // Input validation — enable register button when details are filled (password >= 6 chars) and not loading
        btnRegister.disableProperty().bind(
                txtRegFullName.textProperty().isEmpty()
                        .or(txtRegUsername.textProperty().isEmpty())
                        .or(txtRegPassword.textProperty().length().lessThan(6))
                        .or(isLoading));

        // Animate login card on load
        loginCard.setOpacity(0);
        loginCard.setTranslateY(20);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(500), loginCard);
        ft.setToValue(1.0);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(500), loginCard);
        tt.setToY(0);
        new javafx.animation.ParallelTransition(ft, tt).play();
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        log.info("Login button clicked for user: {}", username);
        setLoading(true);
        setStatus("", false);

        // ── Background Task ──────────────────────────────────
        Task<LoginResponse> loginTask = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                log.info("Background login task started for user: {}", username);
                try {
                    return clientService.login(username, password);
                } catch (Exception e) {
                    log.error("Exception in background login service call: {}", e.getMessage(), e);
                    throw e;
                }
            }
        };

        loginTask.setOnSucceeded(event -> {
            log.info("Login task succeeded for user: {}", username);
            setLoading(false);
            LoginResponse lr = loginTask.getValue();
            try {
                if (Roles.ADMIN.equals(lr.getUser().getRole())) {
                    ClientMain.showAdminDashboard();
                } else {
                    ClientMain.showPOSScreen();
                }
            } catch (Exception e) {
                log.error("Failed to transition scene after login: {}", e.getMessage(), e);
                setStatus("Failed to load main screen: " + e.getMessage(), true);
            }
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            log.warn("Login task failed for user: {}. Error: {}", username, ex != null ? ex.getMessage() : "unknown", ex);
            setLoading(false);
            setStatus(ex != null ? ex.getMessage() : "Login failed", true);
            // Shake animation on error
            shakeCard();
        });

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRegister() {
        String fullName = txtRegFullName.getText().trim();
        String username = txtRegUsername.getText().trim();
        String password = txtRegPassword.getText();

        if (fullName.isEmpty() || username.isEmpty() || password.length() < 6) {
            setRegStatus("Vui lòng điền đầy đủ thông tin (mật khẩu >= 6 ký tự)", true);
            return;
        }

        log.info("Register button clicked for user: {}", username);
        setLoading(true);
        setRegStatus("", false);

        Task<Void> regTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clientService.register(username, password, fullName);
                return null;
            }
        };

        regTask.setOnSucceeded(event -> {
            log.info("Registration task succeeded for user: {}", username);
            setLoading(false);
            // Pre-fill login details and switch to login card
            txtUsername.setText(username);
            txtPassword.setText(password);
            setStatus("Đăng ký thành công! Đang tự động điền thông tin đăng nhập...", false);
            showLoginCard();
        });

        regTask.setOnFailed(event -> {
            Throwable ex = regTask.getException();
            log.warn("Registration task failed for user: {}. Error: {}", username, ex != null ? ex.getMessage() : "unknown");
            setLoading(false);
            setRegStatus(ex != null ? ex.getMessage() : "Đăng ký thất bại", true);
            shakeRegCard();
        });

        Thread thread = new Thread(regTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void showRegisterCard() {
        lblStatus.setText("");
        lblRegStatus.setText("");

        // Transition from loginCard to registerCard
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(250), loginCard);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            loginCard.setVisible(false);
            loginCard.setManaged(false);

            registerCard.setVisible(true);
            registerCard.setManaged(true);
            registerCard.setOpacity(0.0);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(250), registerCard);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    @FXML
    private void showLoginCard() {
        lblStatus.setText("");
        lblRegStatus.setText("");

        // Transition from registerCard to loginCard
        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(250), registerCard);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            registerCard.setVisible(false);
            registerCard.setManaged(false);

            loginCard.setVisible(true);
            loginCard.setManaged(true);
            loginCard.setOpacity(0.0);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(250), loginCard);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ── UI helpers ───────────────────────────────────────────

    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        spinnerReg.setVisible(loading);
        isLoading.set(loading);
        txtUsername.setDisable(loading);
        txtPassword.setDisable(loading);
        txtRegFullName.setDisable(loading);
        txtRegUsername.setDisable(loading);
        txtRegPassword.setDisable(loading);
    }

    private void setStatus(String message, boolean error) {
        lblStatus.setText(message);
        lblStatus.getStyleClass().removeAll("status-error", "status-success");
        if (!message.isEmpty()) {
            lblStatus.getStyleClass().add(error ? "status-error" : "status-success");
        }
    }

    private void setRegStatus(String message, boolean error) {
        lblRegStatus.setText(message);
        lblRegStatus.getStyleClass().removeAll("status-error", "status-success");
        if (!message.isEmpty()) {
            lblRegStatus.getStyleClass().add(error ? "status-error" : "status-success");
        }
    }

    private void shakeCard() {
        javafx.animation.TranslateTransition shake =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), loginCard);
        shake.setFromX(0); shake.setByX(10); shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    private void shakeRegCard() {
        javafx.animation.TranslateTransition shake =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), registerCard);
        shake.setFromX(0); shake.setByX(10); shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
