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

    @FXML private Pane farmContainer;
    @FXML private InventoryBar inventoryBar;
    @FXML private MoneyUI moneyUI;
    @FXML private ShopOverlay shopOverlay;
    @FXML private ShopButton shopButton;
    @FXML private FinanceDashboard financeDashboard;

    private Soil[][] farm;

    public enum Tool { CROP, HARVEST, FIELD, ANIMAL, BUY_LAND }

    private StackPane pauseMenuOverlay;
    private boolean isPaused = false;

    private Tool   activeTool = Tool.HARVEST;
    private String activeCrop = null;
    private IsoFarmCanvas isoCanvas;

    private final int SIZE             = 100;
    private static final int OWNED_START_SIZE = 5;
    private static final double LOGIC_NS = 1_000_000_000.0 / 20.0;

    private final List<Animal> animals = new ArrayList<>();
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

        // ── Handlers enregistrés via les méthodes dédiées ──────────────────
        // NE PAS appeler isoCanvas.setOnMousePressed() ici — ça écraserait
        // les handlers internes (pan, drag) du canvas.
        isoCanvas.setOnTileClick(this::handleTileClick);
        isoCanvas.setOnAnimalClick(this::handleAnimalClick);

        moneyUI.setMoney(Main.joueur1.getMoney());

        farmContainer.getChildren().add(isoCanvas);
        isoCanvas.widthProperty().bind(farmContainer.widthProperty());
        isoCanvas.heightProperty().bind(farmContainer.heightProperty());
        isoCanvas.setAnimals(animals);

        createPauseMenu();

        farmContainer.sceneProperty().addListener((obs, old, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(ev -> {
                    switch (ev.getCode()) {
                        case ESCAPE -> togglePause();
                        case B -> { activeTool=Tool.BUY_LAND; activeCrop=null; inventoryBar.selectItem("Achat terrain"); }
                        case H -> { activeTool=Tool.HARVEST;  activeCrop=null; inventoryBar.selectItem("Récolte"); }
                    }
                });
            }
        });

        gameLoop = new AnimationTimer() {
            private long lastLogic = 0;
            @Override
            public void handle(long now) {
                if (lastLogic == 0) { lastLogic = now; return; }
                if (now - lastLogic >= LOGIC_NS) {
                    double dt = (now - lastLogic) / 1_000_000_000.0;
                    lastLogic = now;
                    tickLogic(dt);
                }
                isoCanvas.renderFrame();
            }
        };
        gameLoop.start();

        inventoryBar.addItem("Récolte",       "Cisaille",      "Outil de récolte",         "/assets/shears.png");
        inventoryBar.addItem("Champ",         "Champ",         "Champ pour les animaux",   "/assets/hand.png");
        inventoryBar.addItem("Achat terrain", "Achat terrain", "Acheter une parcelle (B)", "/assets/hand.png");

        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Coq",         50, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Sativa",      10, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Purple Haze", 15, this::buy));
        shopOverlay.addBuyItem(new ShopOverlay.ShopItem("Blue Haze",   20, this::buy));

        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Sativa Mûre",      20, this::sell));
        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Purple Haze Mûre", 35, this::sell));
        shopOverlay.addSellItem(new ShopOverlay.ShopItem("Blue Haze Mûre",   50, this::sell));

        inventoryBar.setOnItemSelect(item -> {
            switch (item) {
                case "Récolte"       -> { activeTool=Tool.HARVEST;  activeCrop=null; }
                case "Champ"         -> { activeTool=Tool.FIELD;    activeCrop=null; }
                case "Achat terrain" -> { activeTool=Tool.BUY_LAND; activeCrop=null; }
                case "Coq"           -> { activeTool=Tool.ANIMAL;   activeCrop=item; }
                default              -> { activeTool=Tool.CROP;     activeCrop=item; }
            }
        });
    }

    // ─── Logique de jeu (20 Hz) ───────────────────────────────────────────────
    private void tickLogic(double dt) {
        boolean changed = false;
        for (Animal a : animals) { a.Update(farm, SIZE, dt, animals); changed=true; }
        for (int r=0;r<SIZE;r++)
            for (int c=0;c<SIZE;c++) {
                Soil s = farm[r][c];
                if (!s.isFree() && !s.crop.isFinished()) {
                    boolean was = s.crop.isFinished();
                    s.crop.grow(dt);
                    if (s.crop.isFinished() != was) changed=true;
                }
            }
        if (changed) { isoCanvas.setAnimals(animals); isoCanvas.markDirty(); }
    }

    // ─── Handler clics tuiles ─────────────────────────────────────────────────
    private void handleTileClick(int row, int col, boolean isFirstClick) {
        Soil soil = farm[row][col];
        switch (activeTool) {
            case BUY_LAND -> {
                if (!soil.isOwned()) {
                    int price = soil.getPurchasePrice();
                    if (Main.joueur1.getMoney() >= price) {
                        Main.joueur1.setMoney(Main.joueur1.getMoney() - price);
                        soil.setOwned(true);
                        moneyUI.setMoney(Main.joueur1.getMoney());
                        isoCanvas.markDirty();
                        spawnFloatingLabel("-"+price+" $",
                                farmContainer.getWidth()/2, farmContainer.getHeight()/2, "#ff9966");
                    } else {
                        spawnFloatingLabel("Pas assez d'argent !",
                                farmContainer.getWidth()/2, farmContainer.getHeight()/2, "#ff6666");
                    }
                }
            }
            case CROP -> {
                if (!soil.isOwned()) { spawnNotOwned(); return; }
                if (soil.isFree() && !soil.isField() && activeCrop!=null
                        && Main.inventory1.getQuantity(activeCrop)>0) {
                    soil.plant(new Crop(7, activeCrop));
                    Main.inventory1.removeFromInventory(activeCrop, 1);
                    isoCanvas.markDirty();
                    refreshInventory();
                }
            }
            case HARVEST -> {
                if (soil.isOwned() && !soil.isFree() && soil.crop.isFinished()) {
                    Main.inventory1.addToInventory(soil.crop.getType()+" Mûre", 1);
                    soil.clear();
                    isoCanvas.markDirty();
                    refreshInventory();
                }
            }
            case FIELD -> {
                if (!soil.isOwned()) { spawnNotOwned(); return; }
                if (soil.isFree()) {
                    if (isFirstClick) isRemovingFields = soil.isField();
                    soil.setField(!isRemovingFields);
                    isoCanvas.markDirty();
                }
            }
            case ANIMAL -> {
                if (!soil.isOwned()) { spawnNotOwned(); return; }
                if (soil.isField() && activeCrop!=null
                        && Main.inventory1.getQuantity(activeCrop)>0) {
                    animals.add(new Animal(row, col,
                            new Random().nextDouble(10)+20,
                            new Random().nextDouble(5)+15));
                    isoCanvas.setAnimals(animals);
                    isoCanvas.markDirty();
                    Main.inventory1.removeFromInventory(activeCrop, 1);
                    refreshInventory();
                }
            }
        }
    }

    // ─── Handler clics animaux ────────────────────────────────────────────────
    private void handleAnimalClick(Animal animal, double sx, double sy) {
        if (animal.isHungry()) {
            if (Main.inventory1.getQuantity("Sativa") > 0) {
                animal.feed();
                spawnFloatingLabel("Nourriture !", sx, sy, "#66ff66");
                isoCanvas.setAnimals(animals);
                isoCanvas.markDirty();
            } else {
                spawnFloatingLabel("Pas de graines !", sx, sy, "#f16d6d");
            }
        } else if (animal.hasProduct()) {
            if (animal.collectProduct()) {
                int gain = new Random().nextInt(20)+50;
                Main.joueur1.setMoney(Main.joueur1.getMoney() + gain);
                moneyUI.setMoney(Main.joueur1.getMoney());
                spawnFloatingMoney(sx, sy, gain);
                isoCanvas.setAnimals(animals);
                isoCanvas.markDirty();
            }
        }
    }

    // ─── Ferme ────────────────────────────────────────────────────────────────
    private void createFarm() {
        farm = new Soil[SIZE][SIZE];
        int c=SIZE/2, h=OWNED_START_SIZE/2;
        for (int r=0;r<SIZE;r++)
            for (int col=0;col<SIZE;col++) {
                Soil s = new Soil(null,1);
                s.setOwned(r>=c-h&&r<=c+h&&col>=c-h&&col<=c+h);
                s.setPurchasePrice(computeLandPrice(Math.max(Math.abs(r-c),Math.abs(col-c))));
                farm[r][col]=s;
            }
    }

    private int computeLandPrice(int dist) {
        if (dist<=OWNED_START_SIZE/2) return 0;
        return 50+dist*dist*5;
    }

    private void spawnNotOwned() {
        spawnFloatingLabel("Parcelle non achetée !",
                farmContainer.getWidth()/2, farmContainer.getHeight()/2, "#ff6666");
    }

    // ─── Inventaire ───────────────────────────────────────────────────────────
    private void refreshInventory() {
        updateDynamicItem("Sativa",           "Graines de Sativa",      "/assets/seed.png");
        updateDynamicItem("Purple Haze",      "Graines de Purple Haze", "/assets/seed.png");
        updateDynamicItem("Blue Haze",        "Graines de Blue Haze",   "/assets/seed.png");
        updateDynamicItem("Coq",              "Un beau coq",            "/assets/coq_icon.png");
        updateDynamicItem("Sativa Mûre",      "Prête à vendre",         "/assets/soil.png");
        updateDynamicItem("Purple Haze Mûre", "Prête à vendre",         "/assets/soil.png");
        updateDynamicItem("Blue Haze Mûre",   "Prête à vendre",         "/assets/soil.png");
    }

    private void updateDynamicItem(String name, String desc, String icon) {
        int qty = Main.inventory1.getQuantity(name);
        if (qty>0) {
            if (!inventoryBar.hasItem(name)) inventoryBar.addItem(name,name+" : "+qty,desc,icon);
            else inventoryBar.updateItem(name,name+" : "+qty);
        } else {
            if (inventoryBar.hasItem(name)) {
                inventoryBar.removeItem(name);
                if (name.equals(activeCrop)) activeCrop=null;
            }
        }
    }

    // ─── Boutique ─────────────────────────────────────────────────────────────
    private void buy(ShopOverlay.ShopItem item) {
        if (Main.joueur1.getMoney()>=item.price()) {
            Main.joueur1.setMoney(Main.joueur1.getMoney()-item.price());
            moneyUI.setMoney(Main.joueur1.getMoney());
            Main.inventory1.addToInventory(item.name(),1);
            refreshInventory();
        }
    }
    private void sell(ShopOverlay.ShopItem item) {
        if (Main.inventory1.getQuantity(item.name())>0) {
            Main.joueur1.setMoney(Main.joueur1.getMoney()+item.price());
            moneyUI.setMoney(Main.joueur1.getMoney());
            Main.inventory1.removeFromInventory(item.name(),1);
            refreshInventory();
        }
    }

    // ─── Menu pause ───────────────────────────────────────────────────────────
    private void createPauseMenu() {
        pauseMenuOverlay = new StackPane();
        pauseMenuOverlay.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0.7),null,null)));
        pauseMenuOverlay.setVisible(false);
        VBox box = new VBox(20); box.setAlignment(Pos.CENTER);
        Button r=mb("Reprendre"),s=mb("Sauvegarder"),l=mb("Charger"),q=mb("Quitter");
        r.setOnAction(e->togglePause());
        s.setOnAction(e->SaveManager.saveGame(extractGameState()));
        l.setOnAction(e->{GameState st=SaveManager.loadGame();if(st!=null){applyGameState(st);togglePause();}});
        q.setOnAction(e->{isoCanvas.shutdown();System.exit(0);});
        box.getChildren().addAll(r,s,l,q);
        pauseMenuOverlay.getChildren().add(box);
        farmContainer.getChildren().add(pauseMenuOverlay);
        pauseMenuOverlay.prefWidthProperty().bind(farmContainer.widthProperty());
        pauseMenuOverlay.prefHeightProperty().bind(farmContainer.heightProperty());
    }

    private Button mb(String text) {
        Button btn=new Button(text); btn.setPrefSize(200,80);
        btn.setStyle("-fx-background-image:url('/assets/button.png');-fx-background-size:stretch;" +
                "-fx-background-color:transparent;-fx-text-fill:#491d15;-fx-font-weight:bold;" +
                "-fx-font-size:20;-fx-cursor:hand;");
        btn.hoverProperty().addListener((o,was,is)->{
            if(is)btn.setStyle(btn.getStyle()+"-fx-effect:dropshadow(three-pass-box,white,10,0.5,0,0);");
            else  btn.setStyle(btn.getStyle().replace("-fx-effect:dropshadow(three-pass-box,white,10,0.5,0,0);",""));
        });
        return btn;
    }

    private void togglePause() {
        isPaused=!isPaused;
        pauseMenuOverlay.setVisible(isPaused);
        if (isPaused&&gameLoop!=null) gameLoop.stop();
        else if (!isPaused&&gameLoop!=null) gameLoop.start();
    }

    // ─── Sauvegarde ───────────────────────────────────────────────────────────
    private GameState extractGameState() {
        GameState state=new GameState();
        state.money=Main.joueur1.getMoney();
        state.inventory=new java.util.HashMap<>(Main.inventory1.getAllItems());
        state.animals=new ArrayList<>();
        for (Animal a:animals){AnimalSaveData d=new AnimalSaveData();d.row=a.getRow();d.col=a.getCol();d.isHungry=a.isHungry();d.hasProduct=a.hasProduct();state.animals.add(d);}
        state.farmGrid=new TileSaveData[SIZE][SIZE];
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++){TileSaveData td=new TileSaveData();Soil s=farm[r][c];td.isField=s.isField();td.hasCrop=!s.isFree();td.isOwned=s.isOwned();if(td.hasCrop&&s.crop!=null)td.cropType=s.crop.getType();state.farmGrid[r][c]=td;}
        return state;
    }

    private void applyGameState(GameState state) {
        moneyUI.setMoney((int)state.money);
        Main.inventory1.setAllItems(state.inventory);
        animals.clear();
        for (AnimalSaveData d:state.animals) animals.add(new Animal((int)d.row,(int)d.col,20,10));
        isoCanvas.setAnimals(animals);
        for (int r=0;r<SIZE;r++) for (int c=0;c<SIZE;c++){TileSaveData td=state.farmGrid[r][c];Soil s=farm[r][c];s.clear();s.setField(td.isField);s.setOwned(td.isOwned);if(td.hasCrop)s.plant(new Crop(3,td.cropType));}
        inventoryBar.clearAll();
        refreshInventory();
        isoCanvas.markDirty();
    }

    public void loadSavedGame() {
        GameState state=SaveManager.loadGame();
        if(state!=null) applyGameState(state);
    }

    // ─── Animations flottantes ────────────────────────────────────────────────
    public void spawnFloatingMoney(double x, double y, int amount) {
        spawnFloatingLabel((amount>=0?"+":"")+amount+" $",x,y,amount>=0?"#ffd700":"#ff6666");
    }

    private void spawnFloatingLabel(String text, double x, double y, String color) {
        Label lbl=new Label(text);
        lbl.setStyle("-fx-text-fill:"+color+";-fx-font-weight:bold;-fx-font-size:20;" +
                "-fx-effect:dropshadow(three-pass-box,black,3,0.8,1,1);");
        lbl.setLayoutX(x); lbl.setLayoutY(y);
        farmContainer.getChildren().add(lbl);
        TranslateTransition mv=new TranslateTransition(Duration.seconds(1.5),lbl); mv.setByY(-50);
        FadeTransition fd=new FadeTransition(Duration.seconds(1.5),lbl); fd.setFromValue(1);fd.setToValue(0);
        ParallelTransition pt=new ParallelTransition(mv,fd);
        pt.setOnFinished(e->farmContainer.getChildren().remove(lbl));
        pt.play();
    }
}