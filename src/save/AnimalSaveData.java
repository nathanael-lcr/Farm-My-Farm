package save;

import java.io.Serializable;

public class AnimalSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public String type;
    public double row;
    public double col;

    public boolean isHungry;
    public boolean hasProduct;
    public double currentTimeToHungry;
    public double currentTimeToProduce;
}