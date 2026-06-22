package com.minimart.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minimart.common.dto.Request;
import com.minimart.common.dto.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * Manages the persistent TCP connection to the Mini Mart Server.
 *
 * <p>Singleton — one shared connection per client application instance.</p>
 * <p>Protocol: newline-delimited JSON (one Request per line, one Response per line).</p>
 *
 * <p><b>Thread-safety:</b> {@link #sendRequest(Request)} is synchronized so that
 * multiple JavaFX background tasks cannot interleave their messages on the socket.</p>
 */
public class ServerCommunicator {

    private static final Logger log = LoggerFactory.getLogger(ServerCommunicator.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                            (src, type, ctx) -> ctx.serialize(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>)
                            (json, type, ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    // ── Singleton ────────────────────────────────────────────
    private static ServerCommunicator instance;

    private Socket       socket;
    private PrintWriter  writer;
    private BufferedReader reader;
    private boolean      connected = false;

    // Connection settings (overridable via system properties)
    private final String serverHost;
    private final int    serverPort;

    private ServerCommunicator() {
        this.serverHost = System.getProperty("server.host", "localhost");
        this.serverPort = Integer.parseInt(System.getProperty("server.port", "9999"));
    }

    /** Returns the shared singleton instance. */
    public static synchronized ServerCommunicator getInstance() {
        if (instance == null) {
            instance = new ServerCommunicator();
        }
        return instance;
    }

    // ── Connection lifecycle ─────────────────────────────────

    /**
     * Opens a TCP connection to the server.
     *
     * @throws IOException if connection fails
     */
    public synchronized void connect() throws IOException {
        if (connected) return;
        socket = new Socket(serverHost, serverPort);
        socket.setSoTimeout(30_000); // 30s read timeout
        writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        log.info("Connected to server at {}:{}", serverHost, serverPort);
    }

    /**
     * Closes the TCP connection.
     */
    public synchronized void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        log.info("Disconnected from server");
    }

    public boolean isConnected() { return connected && socket != null && !socket.isClosed(); }

    // ── Request/Response ────────────────────────────────────

    /**
     * Sends a {@link Request} to the server and waits for a {@link Response}.
     *
     * <p>Synchronized so concurrent JavaFX Tasks do not interleave socket writes.
     * Each Task must wait for the previous request to complete before sending the next.</p>
     *
     * @param request the request to send
     * @return the server's response
     * @throws IOException if the socket is broken (caller should reconnect/logout)
     */
    public synchronized Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            log.error("sendRequest: Socket is not connected");
            throw new IOException("Not connected to server. Please restart the application.");
        }

        // Serialize request to a single JSON line
        String requestJson = GSON.toJson(request);
        log.info("→ Sending request: action={}, token={}", request.getAction(), request.getToken());
        writer.println(requestJson);

        // Read the server's response (single JSON line)
        log.info("Waiting for response...");
        String responseLine = reader.readLine();
        if (responseLine == null) {
            log.error("Received null response (EOF) from server");
            connected = false;
            throw new IOException("Server closed the connection unexpectedly.");
        }
        log.info("← Received response line length: {}", responseLine.length());
        return GSON.fromJson(responseLine, Response.class);
    }
}
