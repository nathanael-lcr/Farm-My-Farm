import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import save.AnimalSaveData;
import save.GameState;
import save.SaveManager;
import save.TileSaveData;
import ui.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FarmController {

    @FXML
    private Pane farmContainer;
    @FXML
    private InventoryBar inventoryBar;
    @FXML
    private MoneyUI moneyUI;
    @FXML
    private ShopOverlay shopOverlay;
    @FXML
    private ShopButton shopButton;
    @FXML
    private FinanceDashboard financeDashboard;

    private Soil[][] farm;

    public enum Tool {
        CROP, HARVEST, FIELD, ANIMAL
    }

    private StackPane pauseMenuOverlay;
    private boolean isPaused = false;

    private Tool activeTool = Tool.HARVEST;
    private String activeCrop = null;
    private IsoFarmCanvas isoCanvas;
    private final int SIZE = 10;

    private List<Animal> Animals = new ArrayList<>();
    private boolean isRemovingFields = false;

    private AnimationTimer gameLoop;

    @FXML
    public void initialize() {
        createFarm();

        shopButton.setOnAction(e -> shopOverlay.show());

        financeDashboard = new FinanceDashboard();
        StackPane.setAlignment(financeDashboard, Pos.CENTER);

        financeDashboard.prefWidthProperty().bind(farmContainer.widthProperty());
        financeDashboard.prefHeightProperty().bind(farmContainer.heightProperty());

        farmContainer.getChildren().add(financeDashboard);
        moneyUI.setOnMouseClicked(e -> {
            financeDashboard.updateBalance(Main.joueur1.getMoney());
            financeDashboard.show();
            financeDashboard.toFront();

        });

        isoCanvas = new IsoFarmCanvas(SIZE, farm);
        isoCanvas.setOnTileClick(this::handleClick);

        isoCanvas.setOnAnimalClick((animal, screenX, screenY) -> {
            if (animal.isHungry()) {
                if (Main.inventory1.getQuantity("Sativa") > 0) {
                    animal.feed();
                } else {
                    spawnFloatingNoFood(screenX, screenY);
                }

            } else if (animal.hasProduct()) {
                if (animal.collectProduct()) {
                    int gain = 10;
                    Main.joueur1.setMoney(Main.joueur1.getMoney() + gain);

                    spawnFloatingMoney(screenX, screenY, gain);
                }
            }
        });


        moneyUI.setMoney(Main.joueur1.getMoney());

        farmContainer.getChildren().add(isoCanvas);

        isoCanvas.widthProperty().bind(farmContainer.widthProperty());
        isoCanvas.heightProperty().bind(farmContainer.heightProperty());

        isoCanvas.setAnimals(Animals);

        createPauseMenu();

        farmContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case ESCAPE -> togglePause();
                        case SPACE -> System.out.println("Space pressed");
                    }
                });
            }
        });

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            private int frames = 0;
            private long lastFpsTime = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    lastFpsTime = now;
                    return;
                }

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                frames++;
                if (now - lastFpsTime >= 1_000_000_000L) {
                    frames = 0;
                    lastFpsTime = now;
                }

                for (Animal animal : Animals) {
                    animal.Update(farm, SIZE, deltaTime, Animals);
                }

                for (int row = 0; row < SIZE; row++) {
                    for (int col = 0; col < SIZE; col++) {
                        Soil soil = farm[row][col];
                        if (!soil.isFree() && !soil.crop.isFinished()) {
                            soil.crop.grow(deltaTime);
                        }
                    }
                }
                isoCanvas.redraw();
            }
        };
        gameLoop.start();

        inventoryBar.addItem("Récolte", "Cisaille", "Outil de récolte", "/assets/shears.png");
        inventoryBar.addItem("Champ", "Champ", "Champ pour les animaux", "/assets/hand.png");

        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Coq", 50, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Sativa", 10, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Purple Haze", 15, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Blue Haze", 20, this::buy));

        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Sativa Mûre", 20, this::sell));
        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Purple Haze Mûre", 35, this::sell));
        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Blue Haze Mûre", 50, this::sell));

        inventoryBar.setOnItemSelect(item -> {
            if (item.equals("Récolte")) {
                activeTool = Tool.HARVEST;
                activeCrop = null;
            } else if (item.equals("Champ")) {
                activeTool = Tool.FIELD;
                activeCrop = null;
            } else if (item.equals("Coq")) {
                activeTool = Tool.ANIMAL;
                activeCrop = item;
            } else {
                activeTool = Tool.CROP;
                activeCrop = item;
            }
        });
    }

    private void createFarm() {
        farm = new Soil[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                farm[row][col] = new Soil(null, 1);
            }
        }
    }

    private void handleClick(int row, int col, boolean isFirstClick) {
        Soil soil = farm[row][col];

        switch (activeTool) {
            case CROP -> {
                if (soil.isFree() && !soil.isField() && activeCrop != null) {
                    if (Main.inventory1.getQuantity(activeCrop) > 0) {
                        soil.plant(new Crop(7, activeCrop));
                        Main.inventory1.removeFromInventory(activeCrop, 1);
                        refreshInventory();
                    }
                }
            }
            case HARVEST -> {
                if (!soil.isFree() && soil.crop.isFinished()) {
                    Main.inventory1.addToInventory(soil.crop.getType() + " Mûre", 1);
                    soil.clear();
                    refreshInventory();
                }
            }
            case FIELD -> {
                if (soil.isFree()) {
                    if (isFirstClick) {
                        isRemovingFields = soil.isField();
                    }

                    if (isRemovingFields) {
                        soil.setField(false);
                    } else {
                        soil.setField(true);
                    }
                }
            }
            case ANIMAL -> {
                if (soil.isField() && activeCrop != null) {
                    if (Main.inventory1.getQuantity(activeCrop) > 0) {

                        Animal nouveauCoq = new Animal(row, col, new Random().nextDouble(30.0 - 20.0 + 1) + 20.0, new Random().nextDouble(20.0 - 15.0 + 1) + 15.0);

                        Animals.add(nouveauCoq);

                        isoCanvas.setAnimals(Animals);

                        Main.inventory1.removeFromInventory(activeCrop, 1);
                        refreshInventory();
                    }
                } else {
                    System.out.println("Vous devez placer le coq sur un champ pour animaux !");
                }
            }
        }
        isoCanvas.redraw();
    }

    private void refreshInventory() {
        updateDynamicItem("Sativa", "Graines de Sativa", "/assets/seed.png");
        updateDynamicItem("Purple Haze", "Graines de Purple Haze", "/assets/seed.png");
        updateDynamicItem("Blue Haze", "Graines de Blue Haze", "/assets/seed.png");
        updateDynamicItem("Coq", "Un beau coq qui rapporte de l'argent", "/assets/coq_icon.png");
        updateDynamicItem("Sativa Mûre", "Sativa prête à vendre", "/assets/soil.png");
        updateDynamicItem("Purple Haze Mûre", "Purple Haze prête à vendre", "/assets/soil.png");
        updateDynamicItem("Blue Haze Mûre", "Blue Haze prête à vendre", "/assets/soil.png");
    }

    private void updateDynamicItem(String itemName, String tooltipDesc, String iconPath) {
        int qty = Main.inventory1.getQuantity(itemName);

        if (qty > 0) {
            if (!inventoryBar.hasItem(itemName)) {
                inventoryBar.addItem(itemName, itemName + " : " + qty, tooltipDesc, iconPath);
            } else {
                inventoryBar.updateItem(itemName, itemName + " : " + qty);
            }
        } else {
            if (inventoryBar.hasItem(itemName)) {
                inventoryBar.removeItem(itemName);

                if (itemName.equals(activeCrop)) {
                    activeCrop = null;
                }
            }
        }
    }

    private void buy(ShopOverlay.ShopItem item) {
        if (Main.joueur1.getMoney() >= item.price()) {
            Main.joueur1.setMoney(Main.joueur1.getMoney() - item.price());
            moneyUI.setMoney(Main.joueur1.getMoney());
            Main.inventory1.addToInventory(item.name(), 1);
            refreshInventory();
        }
    }

    private void sell(ShopOverlay.ShopItem item) {
        if (Main.inventory1.getQuantity(item.name()) > 0) {
            Main.joueur1.setMoney(Main.joueur1.getMoney() + item.price());
            moneyUI.setMoney(Main.joueur1.getMoney());
            Main.inventory1.removeFromInventory(item.name(), 1);
            refreshInventory();
        }
    }

    private void createPauseMenu() {
        pauseMenuOverlay = new StackPane();
        pauseMenuOverlay.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.7), null, null)));
        pauseMenuOverlay.setVisible(false);

        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);

        Button btnResume = createMenuButton("Reprendre");
        Button btnSave = createMenuButton("Sauvegarder");
        Button btnLoad = createMenuButton("Charger");
        Button btnQuit = createMenuButton("Quitter");

        btnResume.setOnAction(e -> togglePause());

        btnSave.setOnAction(e -> {
            GameState state = extractGameState();
            SaveManager.saveGame(state);
            System.out.println("Partie sauvegardée depuis le menu !");
        });

        btnLoad.setOnAction(e -> {
            GameState state = SaveManager.loadGame();
            if (state != null) {
                applyGameState(state);
                togglePause();
            }
        });

        btnQuit.setOnAction(e -> System.exit(0));

        menuBox.getChildren().addAll(btnResume, btnSave, btnLoad, btnQuit);
        pauseMenuOverlay.getChildren().add(menuBox);

        farmContainer.getChildren().add(pauseMenuOverlay);

        pauseMenuOverlay.prefWidthProperty().bind(farmContainer.widthProperty());
        pauseMenuOverlay.prefHeightProperty().bind(farmContainer.heightProperty());
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(200, 80);
        btn.setStyle("-fx-background-image: url('/assets/button.png');" + "-fx-background-size: stretch;" + "-fx-background-color: transparent;" + "-fx-text-fill: #491d15;" + "-fx-font-weight: bold;" + "-fx-font-size: 20;" + "-fx-cursor: hand;");
        btn.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (isHovered)
                btn.setStyle(btn.getStyle() + "-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);");
            else
                btn.setStyle(btn.getStyle().replace("-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);", ""));
        });
        return btn;
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseMenuOverlay.setVisible(isPaused);

        if (isPaused) {
            if (gameLoop != null) gameLoop.stop();
        } else {
            if (gameLoop != null) gameLoop.start();
        }
    }

    private GameState extractGameState() {
        GameState state = new GameState();

        state.money = Main.joueur1.getMoney();

        state.inventory = new java.util.HashMap<>(Main.inventory1.getAllItems());

        state.animals = new ArrayList<>();
        for (Animal a : this.Animals) {
            AnimalSaveData data = new AnimalSaveData();
            data.row = a.getRow();
            data.col = a.getCol();
            data.isHungry = a.isHungry();
            data.hasProduct = a.hasProduct();
            state.animals.add(data);
        }

        state.farmGrid = new TileSaveData[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                TileSaveData tData = new TileSaveData();
                Soil soil = farm[r][c];

                tData.isField = soil.isField();
                tData.hasCrop = !soil.isFree();

                if (tData.hasCrop && soil.crop != null) {
                    tData.cropType = soil.crop.getType();
                }

                state.farmGrid[r][c] = tData;
            }
        }

        return state;
    }

    private void applyGameState(GameState state) {
        moneyUI.setMoney((int) state.money);

        Main.inventory1.setAllItems(state.inventory);

        this.Animals.clear();
        for (AnimalSaveData data : state.animals) {
            Animal a = new Animal((int) data.row, (int) data.col, 20.0, 10.0);

            this.Animals.add(a);
        }
        isoCanvas.setAnimals(this.Animals);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                TileSaveData tData = state.farmGrid[r][c];
                Soil soil = farm[r][c];

                soil.clear();

                soil.setField(tData.isField);

                if (tData.hasCrop) {
                    Crop restoredCrop = new Crop(3, tData.cropType);
                    soil.plant(restoredCrop);
                }
            }
        }

        inventoryBar.clearAll();
        refreshInventory();
        isoCanvas.redraw();
    }

    public void spawnFloatingMoney(double screenX, double screenY, int amount) {
        Label floatingText = new Label("+" + amount + " $");
        floatingText.setStyle("-fx-text-fill: #ffd700;" + // Couleur Or
                "-fx-font-weight: bold;" + "-fx-font-size: 20;" + "-fx-effect: dropshadow(three-pass-box, black, 3, 0.8, 1, 1);");

        floatingText.setLayoutX(screenX);
        floatingText.setLayoutY(screenY);

        farmContainer.getChildren().add(floatingText);
        moneyUI.setMoney(Main.joueur1.getMoney());
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), floatingText);
        moveUp.setByY(-50);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), floatingText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        Main.joueur1.setMoney(Main.joueur1.getMoney() + amount);


        ParallelTransition anim = new ParallelTransition(moveUp, fadeOut);

        anim.setOnFinished(e -> farmContainer.getChildren().remove(floatingText));
        anim.play();
    }

    public void spawnFloatingNoFood(double screenX, double screenY) {
        Label floatingText = new Label("Pas de graines");
        floatingText.setStyle("-fx-text-fill: #f16d6d;" + // Couleur Or
                "-fx-font-weight: bold;" + "-fx-font-size: 20;" + "-fx-effect: dropshadow(three-pass-box, black, 3, 0.8, 1, 1);");

        floatingText.setLayoutX(screenX);
        floatingText.setLayoutY(screenY);

        farmContainer.getChildren().add(floatingText);
        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), floatingText);
        moveUp.setByY(-50);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), floatingText);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);


        ParallelTransition anim = new ParallelTransition(moveUp, fadeOut);

        anim.setOnFinished(e -> farmContainer.getChildren().remove(floatingText));
        anim.play();
    }

    public void loadSavedGame() {
        GameState state = save.SaveManager.loadGame();
        if (state != null) {
            applyGameState(state);
            System.out.println("Partie chargée depuis le menu principal");
        } else {
            System.out.println("Aucune sauvegarde trouvée, nouvelle partie commencée.");
        }
    }
}