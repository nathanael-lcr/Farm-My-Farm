package save;

import java.io.Serializable;

public class TileSaveData implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean isField;
    public boolean hasCrop;
    public String cropType;
    public boolean isOwned;
    public double growthTimer;
}