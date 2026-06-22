package com.minimart.server;

import com.minimart.server.config.ConnectionManager;
import com.minimart.server.log.StructuredLogger;
import com.minimart.server.network.ClientHandler;
import com.minimart.server.network.RequestDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for the Mini Mart Server application.
 *
 * <p>Starts a TCP {@link ServerSocket} and uses a fixed-size {@link ExecutorService}
 * thread pool to handle concurrent client connections.</p>
 *
 * <p>Override defaults with JVM system properties:</p>
 * <pre>
 *   -Dserver.port=9999
 *   -Dserver.threads=50
 *   -Ddb.url=jdbc:mysql://localhost:3306/minimart_db
 *   -Ddb.username=root
 *   -Ddb.password=secret
 * </pre>
 */
public class ServerMain {

    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    /** Default port the server listens on */
    private static final int DEFAULT_PORT    = 9999;
    /** Max concurrent client threads in the thread pool */
    private static final int DEFAULT_THREADS = 50;

    public static void main(String[] args) {
        int port    = Integer.parseInt(System.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        int threads = Integer.parseInt(System.getProperty("server.threads", String.valueOf(DEFAULT_THREADS)));

        log.info("=== Mini Mart Server Starting ===");
        log.info("Port: {}, Thread pool size: {}", port, threads);

        // Shared dispatcher (services are stateless — safe to share)
        RequestDispatcher dispatcher = new RequestDispatcher();

        // Thread pool to handle concurrent clients
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);

        // Register shutdown hook for clean teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received — stopping server...");
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
            ConnectionManager.shutdown();
            log.info("Server stopped.");
        }));

        StructuredLogger.info("SERVER_START", null,
                "Server started on port " + port + " with " + threads + " threads", "localhost");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server listening on port {}. Waiting for clients...", port);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(300_000); // 5-minute read timeout per client
                    threadPool.submit(new ClientHandler(clientSocket, dispatcher));
                } catch (IOException e) {
                    if (serverSocket.isClosed()) break;
                    log.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to start server on port {}: {}", port, e.getMessage());
            System.exit(1);
        }
    }
}
