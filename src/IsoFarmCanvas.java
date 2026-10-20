import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class IsoFarmCanvas extends Canvas {

    // ─── Caméra — FX thread uniquement ───────────────────────────────────────
    private double camX = 0, camY = 0, zoom = 1.0;

    private static final double ZOOM_MIN    = 0.6;   // limite dézoom pour garder les perfs
    private static final double ZOOM_MAX    = 2.5;
    private static final double BASE_TILE_W = 80.0;

    // Pan (molette enfoncée = bouton du milieu)
    private boolean panActive = false;
    private double  panStartX, panStartY, panCamX0, panCamY0;

    // Drag action tuile (clic gauche maintenu)
    private boolean tileActionActive = false;
    private int     lastDragRow = -1, lastDragCol = -1;

    private double tileW()   { return BASE_TILE_W * zoom; }
    private double tileH()   { return tileW() / 2.0; }
    private double originX() { return getWidth()  / 2.0 + camX; }
    private double originY() { return getHeight() / 4.0 + camY; }

    // ─── Grille ───────────────────────────────────────────────────────────────
    private final int      size;
    private final Soil[][] farm;

    // ─── Pipeline double-buffer ───────────────────────────────────────────────
    private final AtomicReference<List<RenderCommand>> readyFrame = new AtomicReference<>(null);
    private final AtomicBoolean dirtyFlag  = new AtomicBoolean(true);
    private final AtomicBoolean workerBusy = new AtomicBoolean(false);

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "iso-worker");
        t.setDaemon(true);
        return t;
    });

    // ─── Cache sol — FX thread uniquement ────────────────────────────────────
    private final Image[] soilCache   = new Image[6];
    private double        soilCacheTW = -1;

    // ─── Hover — FX thread uniquement ────────────────────────────────────────
    private int hoverRow = -1, hoverCol = -1;

    // ─── Couleurs ─────────────────────────────────────────────────────────────
    private static final Color BG             = Color.web("#5c8a3f");
    private static final Color HOVER_FREE     = Color.web("#ffffff", 0.25);
    private static final Color HOVER_BUYABLE  = Color.web("#f5e642", 0.55);
    private static final Color HOVER_READY    = Color.web("#f5c842", 0.70);
    private static final Color HOVER_GROWING  = Color.web("#8b5e3c", 0.70);
    private static final Color GROW_SHADOW    = Color.web("#000000", 0.05);
    private static final Color UNOWNED_TINT   = Color.web("#000000", 0.40);
    private static final Color STROKE_NORMAL  = Color.web("#000000", 0.30);
    private static final Color STROKE_HOVER   = Color.web("#ffffff", 0.70);
    private static final Color STROKE_UNOWNED = Color.web("#ff4444", 0.60);

    // ─── Callbacks ────────────────────────────────────────────────────────────
    public interface TileClickHandler   { void onClick(int row, int col, boolean isFirstClick); }
    public interface AnimalClickHandler { void onInteract(Animal animal, double sx, double sy); }

    private TileClickHandler   tileClickHandler;
    private AnimalClickHandler animalClickHandler;

    /**
     * NE PAS utiliser setOnTileClick depuis l'extérieur.
     * Utiliser registerTileClickHandler / registerAnimalClickHandler
     * pour ne pas écraser les handlers internes du canvas.
     */
    public void registerTileClickHandler(TileClickHandler h)   { this.tileClickHandler = h; }
    public void registerAnimalClickHandler(AnimalClickHandler h){ this.animalClickHandler = h; }

    // Alias pour compatibilité avec le code existant
    public void setOnTileClick(TileClickHandler h)     { registerTileClickHandler(h); }
    public void setOnAnimalClick(AnimalClickHandler h) { registerAnimalClickHandler(h); }

    // ─── Sprites ──────────────────────────────────────────────────────────────
    private static final Image[] SOIL_TEXTURES = new Image[6];
    private static class CropSprites { Image growing, ready; CropSprites(Image g,Image r){growing=g;ready=r;} }
    private static final java.util.Map<String,CropSprites> CROP_SPRITES_MAP = new java.util.HashMap<>();
    private static Image ANIMAL_SPRITE, BUBBLE_HUNGRY, BUBBLE_READY;

    static {
        try {
            SOIL_TEXTURES[0] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil.png"));
            SOIL_TEXTURES[1] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil1.png"));
            SOIL_TEXTURES[2] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/soil2.png"));
            SOIL_TEXTURES[3] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field.png"));
            SOIL_TEXTURES[4] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field2.png"));
            SOIL_TEXTURES[5] = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/field3.png"));
            ANIMAL_SPRITE  = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/rooster.png"));
            BUBBLE_HUNGRY  = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/bubble.png"));
            BUBBLE_READY   = new Image(IsoFarmCanvas.class.getResourceAsStream("/assets/money.png"));
        } catch (Exception e) { System.err.println("Textures manquantes"); }
        loadCropSprites("Sativa",      "/assets/plant.png",  "/assets/plant2.png");
        loadCropSprites("Purple Haze", "/assets/purple.png", "/assets/purple2.png");
        loadCropSprites("Blue Haze",   "/assets/blue.png",   "/assets/blue2.png");
    }
    private static void loadCropSprites(String n, String g, String r) {
        try { CROP_SPRITES_MAP.put(n, new CropSprites(
                new Image(IsoFarmCanvas.class.getResourceAsStream(g)),
                new Image(IsoFarmCanvas.class.getResourceAsStream(r))));
        } catch (Exception e) { System.err.println("Sprites manquants : " + n); }
    }

    // ─── Animaux ──────────────────────────────────────────────────────────────
    private record AnimalSnapshot(double row, double col, boolean facingRight,
                                  boolean hungry, boolean hasProduct) {}
    private List<AnimalSnapshot> animalSnapshots = List.of();
    private List<Animal>         animalList       = new ArrayList<>();

    public void setAnimals(List<Animal> animals) {
        animalList = animals;
        List<AnimalSnapshot> snaps = new ArrayList<>(animals.size());
        for (Animal a : animals)
            snaps.add(new AnimalSnapshot(a.getRow(), a.getCol(),
                    a.isFacingRight(), a.isHungry(), a.hasProduct()));
        animalSnapshots = snaps;
        markDirty();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────────
    public IsoFarmCanvas(int size, Soil[][] farm) {
        super(800, 600);
        this.size = size;
        this.farm = farm;
        centreCamera();

        widthProperty().addListener((o,ov,nv)  -> { rebuildSoilCache(); markDirty(); });
        heightProperty().addListener((o,ov,nv) -> markDirty());

        // ── Zoom centré sur le curseur ─────────────────────────────────────
        setOnScroll(e -> {
            double mx = e.getX(), my = e.getY();
            double factor  = e.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1;
            double newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * factor));
            if (newZoom == zoom) return;
            double ratio = newZoom / zoom;
            camX = mx - getWidth()/2.0  - ratio * (mx - getWidth()/2.0  - camX);
            camY = my - getHeight()/4.0 - ratio * (my - getHeight()/4.0 - camY);
            zoom = newZoom;
            rebuildSoilCache();
            markDirty();
        });

        // ── Mouse pressed ─────────────────────────────────────────────────
        setOnMousePressed(e -> {
            // ── BOUTON DU MILIEU → pan ──
            // On teste e.getButton() == MIDDLE, pas isMiddleButtonDown()
            // car au moment du PRESSED l'état "down" n'est pas encore propagé
            if (e.getButton() == MouseButton.MIDDLE) {
                panActive = true;
                panStartX = e.getX(); panStartY = e.getY();
                panCamX0  = camX;     panCamY0  = camY;
                setCursor(Cursor.MOVE);
                return;
            }

            // ── BOUTON GAUCHE → jeu ──
            if (e.getButton() != MouseButton.PRIMARY) return;

            // Animal en priorité
            int[] ac = pickAnimalTile(e.getX(), e.getY());
            if (ac != null) {
                for (Animal a : animalList)
                    if ((int)a.getRow() == ac[0] && (int)a.getCol() == ac[1]) {
                        if (animalClickHandler != null)
                            animalClickHandler.onInteract(a, e.getX(), e.getY());
                        break;
                    }
                return;
            }

            // Tuile
            int[] cell = pickTile(e.getX(), e.getY());
            if (cell != null && tileClickHandler != null) {
                tileActionActive = true;
                lastDragRow = cell[0];
                lastDragCol = cell[1];
                tileClickHandler.onClick(cell[0], cell[1], true);
            }
        });

        // ── Mouse dragged ─────────────────────────────────────────────────
        setOnMouseDragged(e -> {
            // Pan (molette maintenue)
            if (panActive) {
                camX = panCamX0 + (e.getX() - panStartX);
                camY = panCamY0 + (e.getY() - panStartY);
                markDirty();
                return;
            }

            // Action sur tuiles (gauche maintenu)
            if (tileActionActive && tileClickHandler != null) {
                int[] cell = pickTile(e.getX(), e.getY());
                if (cell != null && (cell[0] != lastDragRow || cell[1] != lastDragCol)) {
                    lastDragRow = cell[0];
                    lastDragCol = cell[1];
                    tileClickHandler.onClick(cell[0], cell[1], false);
                }
            }
        });

        // ── Mouse released ────────────────────────────────────────────────
        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                panActive = false;
                setCursor(Cursor.DEFAULT);
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                tileActionActive = false;
                lastDragRow = -1;
                lastDragCol = -1;
            }
        });

        // ── Mouse moved (hover) ───────────────────────────────────────────
        setOnMouseMoved(e -> {
            if (pickAnimalTile(e.getX(), e.getY()) != null) {
                setCursor(Cursor.HAND);
                setHover(-1, -1);
            } else {
                setCursor(Cursor.DEFAULT);
                int[] cell = pickTile(e.getX(), e.getY());
                setHover(cell != null ? cell[0] : -1,
                        cell != null ? cell[1] : -1);
            }
        });

        setOnMouseExited(e -> { setCursor(Cursor.DEFAULT); setHover(-1, -1); });
    }

    // ─── Cache sol ────────────────────────────────────────────────────────────
    private void rebuildSoilCache() {
        double tw = tileW(), th = tw / 2.0;
        if (Math.abs(tw - soilCacheTW) < 1.0) return;
        soilCacheTW = tw;
        for (int i = 0; i < 6; i++) {
            Canvas tmp = new Canvas(tw, th);
            GraphicsContext g = tmp.getGraphicsContext2D();
            double cx=tw/2, cy=th/2, hw=tw/2, hh=th/2;
            g.beginPath();
            g.moveTo(cx,cy-hh); g.lineTo(cx+hw,cy); g.lineTo(cx,cy+hh); g.lineTo(cx-hw,cy);
            g.closePath(); g.clip();
            if (SOIL_TEXTURES[i]!=null && !SOIL_TEXTURES[i].isError()) {
                double W=SOIL_TEXTURES[i].getWidth(), H=SOIL_TEXTURES[i].getHeight();
                g.setTransform(new Affine(hw/W, hw/H, 0, -hh/W, hh/H, cy));
                g.drawImage(SOIL_TEXTURES[i], 0, 0, W, H);
            }
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            soilCache[i] = tmp.snapshot(sp, null);
        }
    }

    // ─── API publique ─────────────────────────────────────────────────────────
    public void markDirty()             { dirtyFlag.set(true); }
    public void requestRedraw()         { markDirty(); }
    public void redraw()                { markDirty(); }
    public void markDirty(int r, int c) { markDirty(); }
    public void invalidateFull()        { rebuildSoilCache(); markDirty(); }
    public void shutdown()              { worker.shutdownNow(); }

    public void centreCamera() {
        camX = 0;
        camY = -(size * BASE_TILE_W / 4.0);
    }

    // ─── Boucle principale (FX thread) ────────────────────────────────────────
    public void renderFrame() {
        if (soilCacheTW < 0 && getWidth() > 0) rebuildSoilCache();
        if (soilCacheTW < 0) return;

        if (dirtyFlag.compareAndSet(true, false) && workerBusy.compareAndSet(false, true)) {
            final double  sCamX = camX, sCamY = camY, sZoom = zoom;
            final double  sW    = getWidth(), sH = getHeight();
            final int     sHR   = hoverRow,  sHC = hoverCol;
            final Image[] sSoil = soilCache.clone();
            final List<AnimalSnapshot> sAnim = animalSnapshots;

            worker.submit(() -> {
                try {
                    List<RenderCommand> cmds = buildFrame(
                            sCamX, sCamY, sZoom, sW, sH, sHR, sHC, sSoil, sAnim);
                    readyFrame.set(cmds);
                    Platform.runLater(this::blitFrame);
                } finally {
                    workerBusy.set(false);
                    if (dirtyFlag.get()) Platform.runLater(this::renderFrame);
                }
            });
        }
    }

    // ─── Blit (FX thread) ─────────────────────────────────────────────────────
    private void blitFrame() {
        List<RenderCommand> cmds = readyFrame.getAndSet(null);
        if (cmds == null) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BG);
        gc.fillRect(0, 0, getWidth(), getHeight());

        Font lastFont = null;
        for (RenderCommand cmd : cmds) {
            switch (cmd.type) {
                case DRAW_IMAGE ->
                        gc.drawImage(cmd.image, cmd.imgX, cmd.imgY, cmd.imgW, cmd.imgH);
                case FILL_POLYGON -> {
                    gc.setFill(cmd.fillColor);
                    gc.fillPolygon(cmd.polyXs, cmd.polyYs, 4);
                }
                case STROKE_POLYGON -> {
                    gc.setStroke(cmd.strokeColor);
                    gc.setLineWidth(cmd.strokeWidth);
                    gc.strokePolygon(cmd.polyXs, cmd.polyYs, 4);
                }
                case FILL_TEXT -> {
                    Font f = Font.font(cmd.fontSize);
                    if (!f.equals(lastFont)) {
                        gc.setFont(f);
                        gc.setTextAlign(TextAlignment.CENTER);
                        lastFont = f;
                    }
                    gc.setFill(cmd.fillColor);
                    gc.fillText(cmd.text, cmd.textX, cmd.textY);
                }
            }
        }
    }

    // ─── Worker : buildFrame ──────────────────────────────────────────────────
    private List<RenderCommand> buildFrame(
            double camX, double camY, double zoom,
            double W, double H,
            int hRow, int hCol,
            Image[] soilCache,
            List<AnimalSnapshot> animals) {

        double tw = BASE_TILE_W * zoom;
        double th = tw / 2.0;
        double ox = W / 2.0 + camX;
        double oy = H / 4.0 + camY;

        List<RenderCommand> cmds = new ArrayList<>(1024);
        int[] vb = visibleBounds(ox, oy, tw, th, W, H);

        for (int sum = vb[0]+vb[2]; sum <= vb[1]+vb[3]; sum++)
            for (int row = Math.max(vb[0],sum-vb[3]); row <= Math.min(vb[1],sum-vb[2]); row++) {
                int col = sum - row;
                if (col >= 0 && col < size)
                    buildTile(cmds, row, col, ox, oy, tw, th, hRow, hCol, soilCache);
            }

        buildAnimals(cmds, animals, ox, oy, tw, th);
        return cmds;
    }

    private void buildTile(List<RenderCommand> cmds,
                           int row, int col,
                           double ox, double oy, double tw, double th,
                           int hRow, int hCol,
                           Image[] soilCache) {
        Soil soil = farm[row][col];
        boolean hov = (row==hRow && col==hCol);

        double cx = ox + (col-row)*(tw/2.0);
        double cy = oy + (col+row)*(th/2.0);
        double hw = tw/2, hh = th/2;
        double[] xs = {cx, cx+hw, cx, cx-hw};
        double[] ys = {cy-hh, cy, cy+hh, cy};

        int texIdx = soil.isField()
                ? 3 + (Math.abs(Objects.hash(row,col,"field")) % 3)
                : Math.abs(Objects.hash(row,col)) % 3;

        Image soilImg = soilCache[texIdx];
        if (soilImg != null)
            cmds.add(RenderCommand.image(soilImg, cx-hw, cy-hh, tw, th));
        else
            cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), Color.SADDLEBROWN));

        if (!soil.isOwned()) {
            cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), UNOWNED_TINT));
            if (hov) cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), HOVER_BUYABLE));
        } else if (soil.isFree()) {
            if (hov) cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), HOVER_FREE));
        } else if (soil.crop.isFinished()) {
            if (hov) cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), HOVER_READY));
        } else {
            cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), GROW_SHADOW));
            if (hov) cmds.add(RenderCommand.fillPoly(xs.clone(), ys.clone(), HOVER_GROWING));
        }

        Color sc = !soil.isOwned()
                ? (hov ? STROKE_HOVER : STROKE_UNOWNED)
                : (hov ? STROKE_HOVER : STROKE_NORMAL);
        double lw = hov ? (soil.isOwned()?4.0:3.0) : (soil.isOwned()?2.0:1.5);
        cmds.add(RenderCommand.strokePoly(xs.clone(), ys.clone(), sc, lw));

        if (!soil.isOwned() && hov) {
            double fs = Math.max(8, tw*0.18);
            cmds.add(RenderCommand.text(soil.getPurchasePrice()+" $", cx, cy+fs*0.4, fs, Color.YELLOW));
        }

        if (soil.isOwned() && !soil.isFree() && soil.crop != null) {
            CropSprites sp = CROP_SPRITES_MAP.get(soil.crop.getType());
            if (sp != null) {
                Image img = soil.crop.isFinished() ? sp.ready : sp.growing;
                if (img!=null && !img.isError()) {
                    double iw=tw*0.8, ih=img.getHeight()*(iw/img.getWidth());
                    cmds.add(RenderCommand.image(img, cx-iw/2.0, (cy+hh)-ih, iw, ih));
                }
            }
        }
    }

    private void buildAnimals(List<RenderCommand> cmds, List<AnimalSnapshot> animals,
                              double ox, double oy, double tw, double th) {
        if (ANIMAL_SPRITE == null) return;
        for (AnimalSnapshot a : animals) {
            double cx = ox+(a.col()-a.row())*(tw/2.0);
            double cy = oy+(a.col()+a.row())*(th/2.0);
            double w  = tw*0.5;
            double h  = w*(ANIMAL_SPRITE.getHeight()/ANIMAL_SPRITE.getWidth());
            double dy = cy-h+(th/4);
            double imgX = a.facingRight() ? cx-w/2 : cx+w/2;
            double imgW = a.facingRight() ? w : -w;
            cmds.add(RenderCommand.image(ANIMAL_SPRITE, imgX, dy, imgW, h));
            Image bubble = a.hungry()?BUBBLE_HUNGRY : a.hasProduct()?BUBBLE_READY : null;
            if (bubble!=null) {
                double bw=tw*0.6, bh=bw*(bubble.getHeight()/bubble.getWidth());
                cmds.add(RenderCommand.image(bubble, cx-bw/2+60, dy-bh-5, bw, bh));
            }
        }
    }

    // ─── Frustum culling ─────────────────────────────────────────────────────
    private int[] visibleBounds(double ox, double oy, double tw, double th, double W, double H) {
        double minS=1e9,maxS=-1e9,minD=1e9,maxD=-1e9;
        double[] px={0,W,0,W}, py={0,0,H,H};
        for (int i=0;i<4;i++) {
            double s=(py[i]-oy)*2.0/th, d=(px[i]-ox)*2.0/tw;
            if(s<minS)minS=s; if(s>maxS)maxS=s;
            if(d<minD)minD=d; if(d>maxD)maxD=d;
        }
        int m=2;
        return new int[]{
                Math.max(0,      (int)((minS-maxD)/2)-m),
                Math.min(size-1, (int)((maxS-minD)/2)+m),
                Math.max(0,      (int)((minS+minD)/2)-m),
                Math.min(size-1, (int)((maxS+maxD)/2)+m)
        };
    }

    // ─── Pick (FX thread) ─────────────────────────────────────────────────────
    private int[] pickTile(double px, double py) {
        double ox=originX(), oy=originY(), tw=tileW(), th=tileH();
        int[] vb = visibleBounds(ox, oy, tw, th, getWidth(), getHeight());
        for (int sum=vb[1]+vb[3]; sum>=vb[0]+vb[2]; sum--)
            for (int row=Math.min(vb[1],sum-vb[2]); row>=Math.max(vb[0],sum-vb[3]); row--) {
                int col=sum-row;
                if (col>=0&&col<size&&isInsideTile(px,py,row,col,ox,oy,tw,th))
                    return new int[]{row,col};
            }
        return null;
    }

    public int[] pickAnimalTile(double mx, double my) {
        if (ANIMAL_SPRITE==null) return null;
        double ox=originX(), oy=originY(), tw=tileW(), th=tileH();
        for (AnimalSnapshot a : animalSnapshots) {
            double cx=ox+(a.col()-a.row())*(tw/2);
            double cy=oy+(a.col()+a.row())*(th/2);
            double w=tw*0.5, h=w*(ANIMAL_SPRITE.getHeight()/ANIMAL_SPRITE.getWidth());
            if (mx>=cx-w/2&&mx<=cx+w/2&&my>=cy-h&&my<=cy)
                return new int[]{(int)a.row(),(int)a.col()};
        }
        return null;
    }

    private boolean isInsideTile(double px, double py, int row, int col,
                                 double ox, double oy, double tw, double th) {
        double cx=ox+(col-row)*(tw/2), cy=oy+(col+row)*(th/2);
        double hw=tw/2, hh=th/2;
        if (px<cx-hw||px>cx+hw||py<cy-hh||py>cy+hh) return false;
        return pip(px,py,new double[]{cx,cx+hw,cx,cx-hw},new double[]{cy-hh,cy,cy+hh,cy});
    }

    private boolean pip(double px, double py, double[] xs, double[] ys) {
        boolean neg=false,pos=false;
        for (int i=0;i<xs.length;i++){
            int j=(i+1)%xs.length;
            double c=(xs[j]-xs[i])*(py-ys[i])-(ys[j]-ys[i])*(px-xs[i]);
            if(c<0)neg=true; else if(c>0)pos=true;
            if(neg&&pos)return false;
        }
        return true;
    }

    private void setHover(int row, int col) {
        if (row==hoverRow&&col==hoverCol) return;
        hoverRow=row; hoverCol=col; markDirty();
    }
}