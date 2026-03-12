import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Soil {

    public Crop crop;
    public int type;

    private boolean isField = false;

    public Soil(Crop crop, int type) {
        this.crop = crop;
        this.type = type;
    }

    public boolean isFree() {
        return crop == null;
    }

    public boolean isField() {
        return isField;
    }

    public void setField(boolean isField) {
        this.isField = isField;
    }

    public StringProperty getDisplayProperty() {
        if (isFree()) {
            return new SimpleStringProperty("Libre");
        } else {
            return crop.spriteProperty();
        }
    }

    public void plant(Crop crop) {
        this.crop = crop;
    }

    public void clear() {
        this.crop = null;
    }
}