import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Soil {

    public Crop crop;
    public int type;

    private boolean isField = false;
    private boolean owned = false;       // La parcelle appartient-elle au joueur ?
    private int purchasePrice = 100;     // Prix d'achat de la parcelle

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

    public boolean isOwned() {
        return owned;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(int price) {
        this.purchasePrice = price;
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