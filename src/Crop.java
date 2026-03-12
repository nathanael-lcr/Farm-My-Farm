import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Crop {

    private double timeToGrow;
    private double timeRemaining;
    private int currentGrowth = 0;
    private boolean finished = false;

    private String type;

    private StringProperty sprite = new SimpleStringProperty("0");


    public Crop(int timeToGrow, String type) {
        this.timeToGrow = timeToGrow;
        this.timeRemaining = timeToGrow;
        this.type = type;
    }

    public void grow(double deltaTime) {
        if (finished) return;

        timeRemaining -= deltaTime;

        if (timeRemaining <= 0) {
            finished = true;
            timeRemaining = 0;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public StringProperty spriteProperty() {
        return sprite;
    }

    public String getType() {
        return type;
    }

    public String getSprite() {
        return sprite.get();
    }
}