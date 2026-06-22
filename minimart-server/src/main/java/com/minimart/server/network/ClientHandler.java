package com.minimart.server.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minimart.common.dto.Request;
import com.minimart.common.dto.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * Handles a single connected client on its own thread.
 *
 * <p>Protocol: newline-delimited JSON over a persistent TCP socket.</p>
 * <ol>
 *   <li>Read one JSON line → deserialize to {@link Request}</li>
 *   <li>Pass to {@link RequestDispatcher}</li>
 *   <li>Serialize {@link Response} → write one JSON line back</li>
 *   <li>Repeat until client disconnects</li>
 * </ol>
 */
public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                            (src, type, ctx) -> ctx.serialize(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>)
                            (json, type, ctx) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    private final Socket            clientSocket;
    private final RequestDispatcher dispatcher;
    private final String            clientIp;

    public ClientHandler(Socket clientSocket, RequestDispatcher dispatcher) {
        this.clientSocket = clientSocket;
        this.dispatcher   = dispatcher;
        this.clientIp     = clientSocket.getRemoteSocketAddress().toString();
    }

    @Override
    public void run() {
        log.info("Client connected: {}", clientIp);

        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)) {

            String line;
            // Keep reading requests until the client disconnects (line == null = EOF)
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                log.info("Received line from {}: {}", clientIp, line);
                Request  request  = null;
                Response response;

                try {
                    request  = GSON.fromJson(line, Request.class);
                    log.info("Parsed request: action={}, token={}", request.getAction(), request.getToken());
                    response = dispatcher.dispatch(request, clientIp);
                } catch (Exception e) {
                    log.error("Failed to parse or dispatch request from {}: {}", clientIp, e.getMessage(), e);
                    response = Response.error("Bad request: " + e.getMessage());
                }

                // Write the response as a single JSON line (client reads one line per response)
                String responseJson = GSON.toJson(response);
                log.info("Sending response to {}: {}", clientIp, responseJson);
                writer.println(responseJson);
            }

        } catch (IOException e) {
            // Client disconnected abruptly
            log.debug("Client disconnected (IOException): {} — {}", clientIp, e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            log.info("Client handler finished: {}", clientIp);
        }
    }
}
