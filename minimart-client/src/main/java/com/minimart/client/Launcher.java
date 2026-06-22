package com.minimart.client;

/**
 * Entry point launcher to bypass the JavaFX runtime components check
 * when running from a shaded/fat JAR file.
 */
public class Launcher {
    public static void main(String[] args) {
        ClientMain.main(args);
    }
}
