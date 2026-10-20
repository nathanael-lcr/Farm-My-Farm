import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Représente une instruction de dessin pré-calculée par le thread worker.
 * Le thread JavaFX n'a plus qu'à les exécuter dans l'ordre, sans aucun calcul.
 */
public class RenderCommand {

    public enum Type {
        SOIL_TILE,      // drawImage (texture de sol)
        FILL_POLYGON,   // fillPolygon (overlay couleur)
        STROKE_POLYGON, // strokePolygon (contour)
        DRAW_IMAGE,     // drawImage générique (sprite culture, animal, bulle)
        FILL_TEXT       // fillText (prix parcelle)
    }

    public final Type type;

    // SOIL_TILE / DRAW_IMAGE
    public Image image;
    public double imgX, imgY, imgW, imgH;

    // FILL_POLYGON / STROKE_POLYGON
    public double[] polyXs, polyYs;
    public Color    fillColor;
    public Color    strokeColor;
    public double   strokeWidth;

    // FILL_TEXT
    public String text;
    public double textX, textY, fontSize;

    // ── Constructeurs factory ──────────────────────────────────────────────

    public static RenderCommand image(Image img, double x, double y, double w, double h) {
        RenderCommand c = new RenderCommand(Type.DRAW_IMAGE);
        c.image = img; c.imgX = x; c.imgY = y; c.imgW = w; c.imgH = h;
        return c;
    }

    public static RenderCommand fillPoly(double[] xs, double[] ys, Color color) {
        RenderCommand c = new RenderCommand(Type.FILL_POLYGON);
        c.polyXs = xs; c.polyYs = ys; c.fillColor = color;
        return c;
    }

    public static RenderCommand strokePoly(double[] xs, double[] ys, Color color, double width) {
        RenderCommand c = new RenderCommand(Type.STROKE_POLYGON);
        c.polyXs = xs; c.polyYs = ys; c.strokeColor = color; c.strokeWidth = width;
        return c;
    }

    public static RenderCommand text(String txt, double x, double y, double fontSize, Color color) {
        RenderCommand c = new RenderCommand(Type.FILL_TEXT);
        c.text = txt; c.textX = x; c.textY = y; c.fontSize = fontSize; c.fillColor = color;
        return c;
    }

    private RenderCommand(Type type) { this.type = type; }
}