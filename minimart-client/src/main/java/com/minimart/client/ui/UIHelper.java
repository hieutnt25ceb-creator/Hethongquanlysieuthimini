package com.minimart.client.ui;

import com.minimart.common.model.Product;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;

/**
 * Utility helper class for client UI elements.
 */
public class UIHelper {

    /**
     * Creates an image Node for a product. Attempts to load the product's imagePath,
     * falling back to a category-themed 2-letter abbreviation badge on any failure or empty path.
     */
    public static Node createProductImageNode(Product p, double w, double h) {
        String imagePath = p.getImagePath();
        ImageView imageView = null;

        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                Image img = null;
                String trimmedPath = imagePath.trim();
                if (trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://")) {
                    // Load web URL in background
                    img = new Image(trimmedPath, true);
                } else {
                    File file = new File(trimmedPath);
                    if (file.exists() && file.isFile()) {
                        // Load local file in background
                        img = new Image(file.toURI().toURL().toExternalForm(), true);
                    } else {
                        // Try loading as classpath resource
                        URL res = UIHelper.class.getResource(trimmedPath);
                        if (res != null) {
                            img = new Image(res.toExternalForm(), true);
                        }
                    }
                }

                if (img != null) {
                    imageView = new ImageView(img);
                    imageView.setFitWidth(w);
                    imageView.setFitHeight(h);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                }
            } catch (Exception e) {
                System.err.println("Error initiating image load for product " + p.getId() + " path: " + imagePath + ". Error: " + e.getMessage());
            }
        }

        // Get category-based fallback colors
        String cat = p.getCategory();
        String bgColor = "#e2e8f0";
        String textColor = "#475569";
        if (cat != null) {
            if (cat.contains("Đồ uống")) {
                bgColor = "#bae6fd";
                textColor = "#0369a1";
            } else if (cat.contains("Thực phẩm đóng gói")) {
                bgColor = "#fed7aa";
                textColor = "#c2410c";
            } else if (cat.contains("Bánh kẹo")) {
                bgColor = "#fef08a";
                textColor = "#a16207";
            } else if (cat.contains("Hóa mỹ phẩm")) {
                bgColor = "#e9d5ff";
                textColor = "#7e22ce";
            } else if (cat.contains("tươi") || cat.contains("Sữa")) {
                bgColor = "#fbcfe8";
                textColor = "#be185d";
            }
        }

        StackPane container = new StackPane();
        container.setPrefSize(w, h);
        container.setMinSize(w, h);
        container.setMaxSize(w, h);
        container.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");

        if (imageView != null) {
            Image imgObj = imageView.getImage();
            container.getChildren().add(imageView);

            // Add round corners clip
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            container.setClip(clip);

            // Set final variables for lambda
            String finalTextColor = textColor;
            
            // Listen for load failure
            imgObj.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> {
                        container.getChildren().clear();
                        container.setClip(null);
                        Label abbrLabel = createAbbrLabel(p, finalTextColor);
                        container.getChildren().add(abbrLabel);
                    });
                }
            });

            // If it is already failed
            if (imgObj.isError()) {
                container.getChildren().clear();
                container.setClip(null);
                Label abbrLabel = createAbbrLabel(p, textColor);
                container.getChildren().add(abbrLabel);
            }
        } else {
            Label abbrLabel = createAbbrLabel(p, textColor);
            container.getChildren().add(abbrLabel);
        }

        return container;
    }

    private static Label createAbbrLabel(Product p, String textColor) {
        String nameStr = p.getName();
        String abbr = "SP";
        if (nameStr != null && nameStr.trim().length() >= 2) {
            abbr = nameStr.trim().substring(0, 2).toUpperCase();
        }
        Label abbrLabel = new Label(abbr);
        abbrLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        return abbrLabel;
    }
}
