package save;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public double money;
    public TileSaveData[][] farmGrid;
    public List<AnimalSaveData> animals;
    public Map<String, Integer> inventory;
}