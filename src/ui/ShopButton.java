package ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;

public class ShopButton extends Button {

    public ShopButton() {
        setText("Boutique");

        setPrefHeight(70);
        setPrefWidth(170);

        setStyle(
                "-fx-background-image: url('/assets/button.png');" +
                        "-fx-background-size: stretch;" +
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #491d15;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 20;" +
                        "-fx-cursor: hand;"
        );
        hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (isHovered) setStyle(getStyle() + "-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);");
            else setStyle(getStyle().replace("-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);", ""));
        });

    }
}