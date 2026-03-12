package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MoneyUI extends HBox {

    private final Label moneyLabel;

    public MoneyUI() {
        setSpacing(6);

        setPrefHeight(70);
        setPrefWidth(170);

        setAlignment(Pos.CENTER);


        setStyle(
                "-fx-background-image: url('/assets/button.png');" +
                        "-fx-background-size: stretch;" +
                        "-fx-background-color: transparent;" +
                        "-fx-cursor: hand;"
        );
        hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (isHovered) setStyle(getStyle() + "-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);");
            else setStyle(getStyle().replace("-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);", ""));
        });

        ImageView coinIcon = new ImageView();
        try {
            Image coinImage = new Image(
                    getClass().getResourceAsStream("/assets/coin2.png")
            );
            coinIcon.setImage(coinImage);
        } catch (Exception e) {
            System.err.println("Coin icon not found: " + e.getMessage());
        }
        coinIcon.setFitWidth(32);
        coinIcon.setFitHeight(32);
        coinIcon.setPreserveRatio(true);

        moneyLabel = new Label("0");
        moneyLabel.setTextFill(Color.web("#491d15"));
        moneyLabel.setFont(Font.font("AlphaKind", FontWeight.BOLD, 24));

        getChildren().addAll(coinIcon, moneyLabel);
    }

    public void setMoney(int amount) {
        moneyLabel.setText(String.valueOf(amount));
    }

    public Label getMoneyLabel() {
        return moneyLabel;
    }
}