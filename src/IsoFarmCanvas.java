import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class IsoFarmCanvas extends Canvas {

    private double tileW() {
        return getWidth() / (size + 1.5);
    }

    private double tileH() {
        return tileW() / 2.0;
    }

    private double offsetX() {
        return getWidth() / 2.0;
    }

    private double offsetY() {
        return (getHeight() - (size * tileH())) / 2.0 + tileH();
    }

    private final int size;
    private final Soil[][] farm;
    private static final Color HOVER_FREE = Color.web("#ffffff", 0.25);
    private static final Color HOVER_READY = Color.web("#f5c842", 0.7);
    private static final Color HOVER_GROWING = Color.web("#8b5e3c", 0.7);
    private static final Color GROWING_SHADOW = Color.web("#000000", 0.05);
    private static final Color STROKE_HOVER = Color.web("#ffffff", 0.7);
    private static final Color STROKE_NORMAL = Color.web("#000000", 0.3);
    private int hoverRow = -1;
    private int hoverCol = -1;
    private int lastDragRow = -1;
    private int lastDragCol = -1;

    private TileClickHandler clickHandler;
    private boolean isDragging;

    private Image[] cachedSoilTiles = new Image[6];

    private double cachedTileW = -1;

    private void buildCachedSoilTiles() {
        double w = tileW();
        double h = tileH();

        if (cachedSoilTiles[0] != null && cachedTileW == w) return;
        cachedTileW = w;

        for (int i = 0; i < 6; i++) {
            Canvas tmp = new Canvas(w, h);
            GraphicsContext g = tmp.getGraphicsContext2D();

            double cx = w / 2, cy = h / 2, hw = w / 2, hh = h / 2;

            g.beginPath();
            g.moveTo(cx, cy - hh);
            g.lineTo(cx + hw, cy);
            g.lineTo(cx, cy + hh);
            g.lineTo(cx - hw, cy);
            g.closePath();
            g.clip();

            if (SOIL_TEXTURES[i] != null && !SOIL_TEXTURES[i].isError()) {
                double W = SOIL_TEXTURES[i].getWidth();
                double H = SOIL_TEXTURES[i].getHeight();
                Affine affine = new Affine(hw / W, hw / H, 0, -hh / W, hh / H, cy);
                g.setTransform(affine);
                g.drawImage(SOIL_TEXTURES[i], 0, 0, W, H);
            }

            // retirer le fond pour éviter les bords blancs dégueulasses
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            cachedSoilTiles[i] = tmp.snapshot(params, null);
        }
    }

    public interface TileClickHandler {
        void onClick(int row, int col, boolean isFirstClick);
    }
    public interface AnimalClickHandler {
        void onInteract(Animal animal, double screenX, double screenY);
    }

    private AnimalClickHandler animalClickHandler;

    public void setOnAnimalClick(AnimalClickHandler handler) {
        this.animalClickHandler = handler;
    }

    private static final Image[] SOIL_TEXTURES = new Image[6];

    private static class CropSprites {
        Image growing;
        Image ready;

        CropSprites(Image growing, Image ready) {
            this.growing = growing;
            this.ready = ready;
        }
    }

    private static final Map<String, CropSprites> CROP_SPRITES_MAP = new HashMap<>();
    private static Image ANIMAL_SPRITE;
    private static Image BUBBLE_HUNGRY;
    private static Image BUBBLE_READY;

    private java.util.List<Animal> animals;

    public void setAnimals(java.util.List<Animal> animals) {
        this.animals = animals;
    }

    static {
        try {
            SOIL_TEXTURES[0] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil.png"));
            SOIL_TEXTURES[1] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil1.png"));
            SOIL_TEXTURES[2] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil2.png"));
            SOIL_TEXTURES[3] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field.png"));
            SOIL_TEXTURES[4] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field2.png"));
            SOIL_TEXTURES[5] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field3.png"));
            ANIMAL_SPRITE = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/rooster.png"));
            BUBBLE_HUNGRY = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/bubble.png"));
            BUBBLE_READY = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/money.png"));
            System.out.println("pas de souci avec les textures");
        } catch (Exception e) {
            System.err.println("oh oh pas de textures trouvées");
        }
        loadCropSprites("Sativa", "/assets/plant.png", "/assets/plant2.png");
        loadCropSprites("Purple Haze", "/assets/purple.png", "/assets/purple2.png");
        loadCropSprites("Blue Haze", "/assets/blue.png", "/assets/blue2.png");
    }

    private static void loadCropSprites(String cropName, String growingPath, String readyPath) {
        try {
            Image growing = new Image(IsoFarmCanvas.class.getResourceAsStream(growingPath));
            Image ready = new Image(IsoFarmCanvas.class.getResourceAsStream(readyPath));
            CROP_SPRITES_MAP.put(cropName, new CropSprites(growing, ready));
            System.out.println("Sprites chargés pour : " + cropName);
        } catch (Exception e) {
            System.err.println("Sprites introuvables pour : " + cropName + " (vérifiez les chemins)");
        }
    }

    public IsoFarmCanvas(int size, Soil[][] farm) {
        super(600, 400);
        this.size = size;
        this.farm = farm;

        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMousePressed(e -> {
            Animal clickedAnimal = getAnimalAt(e.getX(), e.getY());

            if (clickedAnimal != null) {
                if (animalClickHandler != null) {
                    animalClickHandler.onInteract(clickedAnimal, e.getX(), e.getY());
                }
            } else {
                int[] cell = pickTile(e.getX(), e.getY());
                if (cell != null && clickHandler != null) {
                    lastDragRow = cell[0];
                    lastDragCol = cell[1];
                    clickHandler.onClick(cell[0], cell[1], true);
                }
            }
        });

        setOnMouseDragged(e -> {
            int[] cell = pickTile(e.getX(), e.getY());
            if (cell != null && clickHandler != null) {
                if (cell[0] != lastDragRow || cell[1] != lastDragCol) {
                    lastDragRow = cell[0];
                    lastDragCol = cell[1];
                    clickHandler.onClick(cell[0], cell[1], false);
                }
            }
        });

        setOnMouseReleased(e -> {
            lastDragRow = -1;
            lastDragCol = -1;
        });

        setOnMouseMoved(e -> {
            Animal hoveredAnimal = getAnimalAt(e.getX(), e.getY());

            if (hoveredAnimal != null) {
                setCursor(Cursor.HAND); // On met la petite main !

                if (hoverRow != -1 || hoverCol != -1) {
                    hoverRow = -1;
                    hoverCol = -1;
                    redraw();
                }
            } else {
                setCursor(Cursor.DEFAULT);
                int[] cell = pickTile(e.getX(), e.getY());
                int newRow = cell != null ? cell[0] : -1;
                int newCol = cell != null ? cell[1] : -1;

                if (newRow != hoverRow || newCol != hoverCol) {
                    hoverRow = newRow;
                    hoverCol = newCol;
                    redraw();
                }
            }
        });

        setOnMouseExited(e -> {
            setCursor(Cursor.DEFAULT);
            hoverRow = -1;
            hoverCol = -1;
            redraw();
        });
    }

    public void setOnTileClick(TileClickHandler handler) {
        this.clickHandler = handler;
    }

    private double toScreenX(int row, int col) {
        return offsetX() + (col - row) * (tileW() / 2.0);
    }

    private double toScreenY(int row, int col) {
        return offsetY() + (col + row) * (tileH() / 2.0);
    }

    private double toScreenX(double row, double col) {
        return offsetX() + (col - row) * (tileW() / 2.0);
    }

    private double toScreenY(double row, double col) {
        return offsetY() + (col + row) * (tileH() / 2.0);
    }

    private int[] pickTile(double px, double py) {
        for (int sum = 2 * size - 2; sum >= 0; sum--) {
            for (int row = size - 1; row >= 0; row--) {
                int col = sum - row;
                if (col < 0 || col >= size) continue;

                if (isInsideTile(px, py, row, col)) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }


    private boolean isInsideTile(double px, double py, int row, int col) {
        double cx = toScreenX(row, col);
        double cy = toScreenY(row, col);
        double hw = tileW() / 2.0;
        double hh = tileH() / 2.0;

        double[] xs = {cx, cx + hw, cx, cx - hw};
        double[] ys = {cy - hh, cy, cy + hh, cy};

        return pointInPolygon(px, py, xs, ys);
    }

    private boolean pointInPolygon(double px, double py, double[] xs, double[] ys) {
        int n = xs.length;
        boolean negative = false;
        boolean positive = false;

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;

            double ax = xs[j] - xs[i];
            double ay = ys[j] - ys[i];
            double bx = px - xs[i];
            double by = py - ys[i];

            double resultatProduitVectoriel = ax * by - ay * bx;

            if (resultatProduitVectoriel < 0) negative = true;
            else if (resultatProduitVectoriel > 0) positive = true;

            if (negative && positive) return false;
        }
        return true;
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();

        gc.setFill(Color.web("#5c8a3f"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        for (int sum = 0; sum < 2 * size - 1; sum++) {
            for (int row = 0; row < size; row++) {
                int col = sum - row;
                if (col < 0 || col >= size) continue;
                drawTile(gc, row, col);
            }
        }

        if (animals != null && ANIMAL_SPRITE != null) {
            for (Animal animal : animals) {
                double cx = toScreenX(animal.getRow(), animal.getCol());
                double cy = toScreenY(animal.getRow(), animal.getCol());

                double w = tileW() * 0.5;
                double h = w * (ANIMAL_SPRITE.getHeight() / ANIMAL_SPRITE.getWidth());

                gc.save();
                if (!animal.isFacingRight()) {
                    gc.translate(cx, cy);
                    gc.scale(-1, 1);
                    gc.translate(-cx, -cy);
                }

                double animalDrawY = cy - h + (tileH() / 4);
                gc.drawImage(ANIMAL_SPRITE, cx - w / 2, animalDrawY, w, h);
                gc.restore();
            }
        }

        if (animals != null) {
            for (Animal animal : animals) {

                double cx = toScreenX(animal.getRow(), animal.getCol());
                double cy = toScreenY(animal.getRow(), animal.getCol());
                double w = tileW() * 0.5;
                double h = w * (ANIMAL_SPRITE.getHeight() / ANIMAL_SPRITE.getWidth());
                double animalDrawY = cy - h + (tileH() / 4);

                if (animal.isHungry() && BUBBLE_HUNGRY != null) {
                    double bubbleW = tileW() * 0.6;
                    double bubbleH = bubbleW * (BUBBLE_HUNGRY.getHeight() / BUBBLE_HUNGRY.getWidth());
                    double bubbleX = cx - (bubbleW / 2) + 60;
                    double bubbleY = animalDrawY - bubbleH - 5;

                    gc.drawImage(BUBBLE_HUNGRY, bubbleX, bubbleY, bubbleW, bubbleH);
                }
                else if (animal.hasProduct() && BUBBLE_READY != null) {
                    double bubbleW = tileW() * 0.6;
                    double bubbleH = bubbleW * (BUBBLE_READY.getHeight() / BUBBLE_READY.getWidth());
                    double bubbleX = cx - (bubbleW / 2) + 60;
                    double bubbleY = animalDrawY - bubbleH - 5;

                    gc.drawImage(BUBBLE_READY, bubbleX, bubbleY, bubbleW, bubbleH);
                }
            }
        }
    }

    private void drawTile(GraphicsContext gc, int row, int col) {
        Soil soil = farm[row][col];
        boolean hovered = (row == hoverRow && col == hoverCol);

        double cx = toScreenX(row, col);
        double cy = toScreenY(row, col);
        double hw = tileW() / 2.0;
        double hh = tileH() / 2.0;

        double[] topXs = {cx, cx + hw, cx, cx - hw};
        double[] topYs = {cy - hh, cy, cy + hh, cy};

        buildCachedSoilTiles();

        Image tile;
        if (soil.isField()) {
            int fieldIndex = 3 + (Math.abs(Objects.hash(row, col, "field")) % 3);
            tile = cachedSoilTiles[fieldIndex];
        } else {
            int textureIndex = Math.abs(Objects.hash(row, col)) % 3;
            tile = cachedSoilTiles[textureIndex];
        }

        gc.drawImage(tile, cx - hw, cy - hh, tileW(), tileH());

        gc.save();
        gc.beginPath();
        gc.moveTo(cx, cy - hh);
        gc.lineTo(cx + hw, cy);
        gc.lineTo(cx, cy + hh);
        gc.lineTo(cx - hw, cy);
        gc.closePath();
        gc.clip();

        Color overlay;
        String label;

        if (soil.isFree()) {
            overlay = hovered ? HOVER_FREE : Color.TRANSPARENT;
            label = "";
        } else if (soil.crop.isFinished()) {
            overlay = hovered ? HOVER_READY : Color.TRANSPARENT;
            label = soil.crop.getType();
        } else {
            overlay = hovered ? HOVER_GROWING : GROWING_SHADOW;
            label = hovered ? "Pousse de " + soil.crop.getType() : "";
        }

        if (overlay != Color.TRANSPARENT) {
            gc.setFill(overlay);
            gc.fillPolygon(topXs, topYs, 4);
        }

        gc.restore();

        gc.setStroke(hovered ? STROKE_HOVER : STROKE_NORMAL);
        gc.setLineWidth(hovered ? 4.0 : 2.0);
        gc.strokePolygon(topXs, topYs, 4);

        boolean spriteDrawn = false;

        if (!soil.isFree()) {
            String cropType = soil.crop.getType();

            CropSprites sprites = CROP_SPRITES_MAP.get(cropType);

            if (sprites != null) {
                Image currentSprite = soil.crop.isFinished() ? sprites.ready : sprites.growing;

                if (currentSprite != null && !currentSprite.isError()) {
                    double targetWidth = tileW() * 0.8;
                    double scale = targetWidth / currentSprite.getWidth();
                    double spriteW = currentSprite.getWidth() * scale;
                    double spriteH = currentSprite.getHeight() * scale;

                    double drawX = cx - (spriteW / 2.0);
                    double yOffset = 0.0;
                    double drawY = (cy + (tileH() / 2.0)) - spriteH + yOffset;

                    gc.drawImage(currentSprite, drawX, drawY, spriteW, spriteH);
                    spriteDrawn = true;
                }
            } else {
                System.out.println("pas de sprite chargé pour cette plante");
            }
        }
        if (!label.isEmpty() && !spriteDrawn) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(18));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(label, cx, cy + 7);
        }
    }

    private Animal getAnimalAt(double mouseX, double mouseY) {
        if (animals != null) {
            for (Animal animal : animals) {
                double cx = toScreenX(animal.getRow(), animal.getCol());
                double cy = toScreenY(animal.getRow(), animal.getCol());
                double w = tileW() * 0.5;
                double h = w * (ANIMAL_SPRITE.getHeight() / ANIMAL_SPRITE.getWidth());

                if (mouseX >= cx - w/2 && mouseX <= cx + w/2 && mouseY >= cy - h && mouseY <= cy) {
                    return animal;
                }
            }
        }
        return null;
    }
}