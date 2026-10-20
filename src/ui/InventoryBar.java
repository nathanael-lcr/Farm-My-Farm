package ui;

import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.layout.HBox;

public class InventoryBar extends VBox {

    private static final double BAR_HEIGHT = 100;
    private static final double ANIM_MS = 250;

    private static final String SLOT_STYLE_NORMAL =
            "-fx-background-image: url('/assets/button_square.png');" +
                    "-fx-background-size: stretch;" +
                    "-fx-background-repeat: no-repeat;" +
                    "-fx-background-color: transparent;" +
                    "-fx-cursor: hand;";

    private static final String SLOT_STYLE_SELECTED =
            "-fx-background-image: url('/assets/button_square.png');" +
                    "-fx-background-size: stretch;" +
                    "-fx-background-repeat: no-repeat;" +
                    "-fx-background-color: transparent;" +
                    "-fx-effect: dropshadow(three-pass-box, #ffecc0, 14, 0.8, 0, 0);" +
                    "-fx-cursor: hand;";

    private static final String SLOT_STYLE_HOVER =
            "-fx-background-image: url('/assets/button_square.png');" +
                    "-fx-background-size: stretch;" +
                    "-fx-background-repeat: no-repeat;" +
                    "-fx-background-color: transparent;" +
                    "-fx-effect: dropshadow(three-pass-box, #ffecc0, 10, 0.5, 0, 0);" +
                    "-fx-cursor: hand;";

    private boolean open = false;
    private Button toggleButton;
    private HBox slotsRow;

    private ItemSelectHandler selectHandler;
    private String selectedKey = null;  // clé de l'item actuellement sélectionné

    public interface ItemSelectHandler {
        void onSelect(String item);
    }

    public InventoryBar() {
        toggleButton = new Button("▲");
        toggleButton.setStyle(
                "-fx-background-image: url('/assets/button.png');" +
                        "-fx-background-size: 100% 100%;" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-repeat: no-repeat;" +
                        "-fx-text-fill: #491d15;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 5 20;" +
                        "-fx-cursor: hand;"
        );
        toggleButton.setPrefHeight(40);
        toggleButton.setPrefWidth(90);
        toggleButton.setOnAction(e -> toggle());

        slotsRow = new HBox(10);
        slotsRow.setAlignment(Pos.CENTER);
        slotsRow.setPrefHeight(BAR_HEIGHT);

        setAlignment(Pos.CENTER);
        setSpacing(30);
        getChildren().addAll(toggleButton, slotsRow);

        this.setTranslateY(20);
    }

    public void setOnItemSelect(ItemSelectHandler handler) {
        this.selectHandler = handler;
    }

    public void bindToggleButton(Button button) {
        this.toggleButton = button;
        this.toggleButton.setOnAction(e -> toggle());
    }

    public void addItem(String key, String label, String tooltipText, String iconPath) {
        Button slot = new Button();
        slot.setId(key);
        slot.setPrefSize(90, 85);
        slot.setStyle(SLOT_STYLE_NORMAL);

        slot.setOnMouseEntered(e -> {
            if (!key.equals(selectedKey)) slot.setStyle(SLOT_STYLE_HOVER);
        });
        slot.setOnMouseExited(e -> {
            if (!key.equals(selectedKey)) slot.setStyle(SLOT_STYLE_NORMAL);
        });

        if (iconPath != null && !iconPath.isEmpty()) {
            try {
                Image img = new Image(getClass().getResourceAsStream(iconPath));
                ImageView icon = new ImageView(img);
                icon.setFitWidth(70);
                icon.setFitHeight(70);
                icon.setPreserveRatio(true);
                slot.setGraphic(icon);
                slot.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } catch (Exception e) {
                System.err.println("Image introuvable : " + iconPath);
            }
        }

        Tooltip tooltip = new Tooltip(label + "\n" + tooltipText);
        tooltip.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        slot.setTooltip(tooltip);
        slot.getProperties().put("baseDesc", tooltipText);

        slot.setOnAction(e -> {
            selectItem(key);  // sélection visuelle + callback
        });

        slotsRow.getChildren().add(slot);
    }

    /**
     * Sélectionne programmatiquement un item par sa clé,
     * met à jour le style et déclenche le handler.
     */
    public void selectItem(String key) {
        // Réinitialise le style de l'ancien item sélectionné
        if (selectedKey != null) {
            slotsRow.getChildren().stream()
                    .filter(n -> selectedKey.equals(n.getId()))
                    .findFirst()
                    .ifPresent(n -> ((Button) n).setStyle(SLOT_STYLE_NORMAL));
        }

        selectedKey = key;

        // Applique le style "sélectionné" au nouveau bouton
        slotsRow.getChildren().stream()
                .filter(n -> key.equals(n.getId()))
                .findFirst()
                .ifPresent(n -> ((Button) n).setStyle(SLOT_STYLE_SELECTED));

        if (selectHandler != null) selectHandler.onSelect(key);
    }

    public void updateItem(String key, String newLabel) {
        slotsRow.getChildren().stream()
                .filter(node -> key.equals(node.getId()))
                .findFirst()
                .ifPresent(node -> {
                    Button slot = (Button) node;
                    String baseDesc = (String) slot.getProperties().get("baseDesc");
                    slot.getTooltip().setText(newLabel + "\n" + baseDesc);
                });
    }

    public boolean hasItem(String key) {
        return slotsRow.getChildren().stream()
                .anyMatch(node -> key.equals(node.getId()));
    }

    public void removeItem(String key) {
        if (key.equals(selectedKey)) selectedKey = null;
        slotsRow.getChildren().removeIf(node -> key.equals(node.getId()));
    }

    public void toggle() {
        open = !open;
        double distanceToSlide = BAR_HEIGHT + 30;

        TranslateTransition slide = new TranslateTransition(Duration.millis(ANIM_MS), this);
        slide.setToY(open ? -distanceToSlide : 20);

        RotateTransition rotate = new RotateTransition(Duration.millis(ANIM_MS), toggleButton);
        rotate.setToAngle(open ? 180 : 0);

        slide.play();
        rotate.play();
    }

    public void clearAll() {
        if (selectedKey != null &&
                !selectedKey.equals("Récolte") &&
                !selectedKey.equals("Champ") &&
                !selectedKey.equals("Achat terrain")) {
            selectedKey = null;
        }
        slotsRow.getChildren().removeIf(node -> {
            String id = node.getId();
            return id != null
                    && !id.equals("Récolte")
                    && !id.equals("Champ")
                    && !id.equals("Achat terrain");
        });
    }

    public boolean isOpen() {
        return open;
    }
}