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

public class FinanceDashboard extends StackPane {

    private final VBox summaryPane = new VBox(15);
    private final VBox historyList = new VBox(5);
    private ScrollPane historyScroll;
    private Button summaryTab, historyTab;

    private Label balanceLabel;
    private Label totalIncomeLabel;
    private Label totalExpensesLabel;

    private int totalIncome = 0;
    private int totalExpenses = 0;

    public FinanceDashboard() {
        setVisible(false);

        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.color(0, 0, 0, 0.6));
        backdrop.widthProperty().bind(widthProperty());
        backdrop.heightProperty().bind(heightProperty());
        backdrop.setOnMouseClicked(e -> hide());

        VBox panel = new VBox();
        panel.setMaxWidth(500);
        panel.setMaxHeight(400);
        panel.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e8a838; -fx-border-width: 2;");
        panel.setOnMouseClicked(Event::consume);

        HBox header = new HBox();
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 8 8 0 0;");

        Label title = new Label("Tableau de Bord Financier");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("X");
        close.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        close.setOnAction(e -> hide());

        header.getChildren().addAll(title, spacer, close);

        HBox tabs = new HBox();
        summaryTab = new Button("Résumé");
        historyTab = new Button("Historique");

        summaryTab.setMaxWidth(Double.MAX_VALUE);
        historyTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(summaryTab, Priority.ALWAYS);
        HBox.setHgrow(historyTab, Priority.ALWAYS);

        summaryTab.setOnAction(e -> switchTab(true));
        historyTab.setOnAction(e -> switchTab(false));

        tabs.getChildren().addAll(summaryTab, historyTab);

        summaryPane.setPadding(new Insets(20));
        summaryPane.setAlignment(Pos.CENTER);

        balanceLabel = new Label("Solde actuel : 0 $");
        balanceLabel.setStyle("-fx-text-fill: #e8a838; -fx-font-size: 24; -fx-font-weight: bold;");

        totalIncomeLabel = new Label("Revenus totaux : + 0 $");
        totalIncomeLabel.setStyle("-fx-text-fill: #5cb85c; -fx-font-size: 16;");

        totalExpensesLabel = new Label("Dépenses totales : - 0 $");
        totalExpensesLabel.setStyle("-fx-text-fill: #d9534f; -fx-font-size: 16;");

        summaryPane.getChildren().addAll(balanceLabel, totalIncomeLabel, totalExpensesLabel);

        historyList.setPadding(new Insets(10));
        historyScroll = new ScrollPane(historyList);
        historyScroll.setFitToWidth(true);
        historyScroll.setStyle("-fx-background-color: transparent; -fx-background: #2b2b2b;");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, tabs, summaryPane);
        getChildren().addAll(backdrop, panel);

        switchTab(true);
    }

    private void switchTab(boolean isSummary) {
        VBox panel = (VBox) getChildren().get(1);

        panel.getChildren().remove(panel.getChildren().size() - 1);
        panel.getChildren().add(isSummary ? summaryPane : historyScroll);

        String active   = "-fx-background-color: #e8a838; -fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-cursor: hand;";
        String inactive = "-fx-background-color: #444; -fx-text-fill: #aaa; -fx-cursor: hand;";
        summaryTab.setStyle(isSummary  ? active : inactive);
        historyTab.setStyle(isSummary ? inactive : active);
    }


    public void updateBalance(int currentBalance) {
        balanceLabel.setText("Solde actuel : " + currentBalance + " $");
    }

    public void logTransaction(String description, int amount, boolean isIncome) {
        if (isIncome) {
            totalIncome += amount;
            totalIncomeLabel.setText("Revenus totaux : + " + totalIncome + " $");
        } else {
            totalExpenses += amount;
            totalExpensesLabel.setText("Dépenses totales : - " + totalExpenses + " $");
        }

        HBox row = new HBox();
        row.setPadding(new Insets(5));
        row.setStyle("-fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label((isIncome ? "+ " : "- ") + amount + " $");
        amountLabel.setStyle(isIncome ? "-fx-text-fill: #5cb85c; -fx-font-weight: bold;" : "-fx-text-fill: #d9534f; -fx-font-weight: bold;");

        row.getChildren().addAll(descLabel, spacer, amountLabel);
        historyList.getChildren().add(0, row);
    }

    public void show() { setVisible(true); }
    public void hide() { setVisible(false); }
}