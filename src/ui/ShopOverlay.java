package ui;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

public class ShopOverlay extends StackPane {

    public record ShopItem(String name, int price, Consumer<ShopItem> onBuy) {}

    private final FlowPane buyGrid  = new FlowPane();
    private final FlowPane sellGrid = new FlowPane();
    private Button buyTab, sellTab;
    private ScrollPane buyScroll, sellScroll;

    public ShopOverlay() {
        setVisible(false);

        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.color(0, 0, 0, 0.6));
        backdrop.widthProperty().bind(widthProperty());
        backdrop.heightProperty().bind(heightProperty());
        backdrop.setOnMouseClicked(e -> hide());

        VBox panel = new VBox();
        panel.setMaxWidth(600);
        panel.setMaxHeight(400);
        panel.setOnMouseClicked(Event::consume);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(50);
        header.setPadding(new Insets(10));

        Label title = new Label("Boutique");
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold; -fx-font-family: 'AlphaKind'");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("X");
        close.setOnAction(e -> hide());
        close.setPrefSize(40, 40);
        close.setMinSize(40, 40);
        close.setMaxSize(40, 40);
        close.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-family: 'AlphaKind';" +
                        "-fx-background-image: url('/assets/button_square.png');" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-size: contain;" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-background-position: center;" +
                        "-fx-font-size: 20;"
        );


        header.getChildren().addAll(title, spacer, close);

        HBox tabs = new HBox();
        buyTab  = new Button("Acheter");
        sellTab = new Button("Vendre");


        buyTab.setOnAction(e  -> switchTab(true));
        sellTab.setOnAction(e -> switchTab(false));

        tabs.getChildren().addAll(buyTab, sellTab);

        for (FlowPane grid : new FlowPane[]{buyGrid, sellGrid}) {
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
        }

        buyScroll  = new ScrollPane(buyGrid);
        sellScroll = new ScrollPane(sellGrid);
        for (ScrollPane s : new ScrollPane[]{buyScroll, sellScroll}) {
            s.setFitToWidth(true);
            VBox.setVgrow(s, Priority.ALWAYS);
        }

        panel.getChildren().addAll(header, tabs, buyScroll);
        getChildren().addAll(backdrop, panel);

        switchTab(true);
    }

    private void switchTab(boolean isBuy) {
        VBox panel = (VBox) getChildren().get(1);

        panel.getChildren().remove(panel.getChildren().size() - 1);
        panel.getChildren().add(isBuy ? buyScroll : sellScroll);

        String active   = "-fx-font-family: 'AlphaKind'; -fx-background-color: #e8a838; -fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-font-size: 14;";
        String inactive = "-fx-font-family: 'AlphaKind'; -fx-background-color: #444; -fx-text-fill: #aaa; -fx-font-size: 14;";
        buyTab.setStyle(isBuy  ? active : inactive);
        sellTab.setStyle(isBuy ? inactive : active);
    }

    public void addBuyItem(ShopItem item) {
        buyGrid.getChildren().add(buildCard(item, "Acheter"));
    }

    public void addSellItem(ShopItem item) {
        sellGrid.getChildren().add(buildCard(item, "Vendre"));
    }

    private VBox buildCard(ShopItem item, String actionLabel) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(120);

        Label name = new Label(item.name());
        name.setTextFill(Color.BLACK);
        name.setFont(Font.font("AlphaKind", FontWeight.BOLD, 14));

        Button btn = new Button(actionLabel + " (" + item.price() + ")");
        btn.setOnAction(e -> item.onBuy().accept(item));

        card.getChildren().addAll(name, btn);
        return card;
    }

    public void show() { setVisible(true); }
    public void hide() { setVisible(false); }
}