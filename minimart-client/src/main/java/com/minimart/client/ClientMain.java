package com.minimart.client;

import com.minimart.client.network.ServerCommunicator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX Application entry point for the Mini Mart Client.
 *
 * <p>Loads the Login screen first. Navigation to the main dashboard
 * is handled after successful authentication.</p>
 */
public class ClientMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(ClientMain.class);

    /** Primary stage — held statically so controllers can perform scene swaps */
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Connect to the server immediately on startup
        try {
            ServerCommunicator.getInstance().connect();
            log.info("Connected to Mini Mart Server");
        } catch (IOException e) {
            log.error("Cannot connect to server: {}", e.getMessage());
            // Show the login screen anyway — it will display a connection error
        }

        showLoginScreen();

        stage.setTitle("Mini Mart Management System");
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        // Clean up on application exit
        ServerCommunicator.getInstance().disconnect();
        log.info("Client application stopped");
    }

    // ── Scene navigation helpers ─────────────────────────────

    /** Loads and shows the Login screen. */
    public static void showLoginScreen() throws IOException {
        loadScene("/fxml/LoginView.fxml", 480, 580);
        primaryStage.setResizable(false);
    }

    /** Loads and shows the Admin Dashboard. */
    public static void showAdminDashboard() throws IOException {
        loadScene("/fxml/AdminDashboard.fxml", 1280, 800);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
    }

    /** Loads and shows the Employee POS screen. */
    public static void showPOSScreen() throws IOException {
        loadScene("/fxml/POSView.fxml", 1280, 800);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
    }

    private static void loadScene(String fxmlPath, double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource(fxmlPath));
        loader.setCharset(java.nio.charset.StandardCharsets.UTF_8);
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
                Objects.requireNonNull(ClientMain.class.getResource("/css/app.css")).toExternalForm());
        primaryStage.setScene(scene);
    }

    public static Stage getPrimaryStage() { return primaryStage; }
}
