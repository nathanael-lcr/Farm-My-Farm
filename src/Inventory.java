import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private Player joueur;
    private Map<String, Integer> inventory = new HashMap<>();

    public Inventory(Player joueur) {
        this.joueur = joueur;
    }

    public void addToInventory(String item, Integer quantity) {
        inventory.put(item, inventory.getOrDefault(item, 0) + quantity);
    }

    public boolean removeFromInventory(String item, Integer quantity) {
        int current = inventory.getOrDefault(item, 0);

        if (current < quantity) return false;
        inventory.put(item, inventory.get(item) - quantity);
        return true;
    }

    public String listInventory() {
        if (inventory.isEmpty()) {
            return "l'inventaire est vide.";
        }

        StringBuilder liste = new StringBuilder();

        for (var entry : inventory.entrySet()) {
            liste.append(entry.getKey())
                    .append(" / ")
                    .append(entry.getValue())
                    .append("\n");
        }

        return liste.toString();
    }

    public Integer getQuantity(String item) {
        return inventory.getOrDefault(item, 0);
    }

    public Map<String, Integer> getAllItems() {
        return this.inventory;
    }

    public void setAllItems(Map<String, Integer> savedItems) {
        this.inventory.clear();
        if (savedItems != null) {
            this.inventory.putAll(savedItems);
        }
    }
}
