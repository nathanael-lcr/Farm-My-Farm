package save;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class SaveManager {

    private static final String SAVE_FILE = "farm_save.dat";

    public static void saveGame(GameState state) {
        try (FileOutputStream fos = new FileOutputStream(SAVE_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(state);
            System.out.println("Partie sauvegardée avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static GameState loadGame() {
        try (FileInputStream fis = new FileInputStream(SAVE_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            GameState state = (GameState) ois.readObject();
            System.out.println("Partie chargée avec succès !");
            return state;

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Aucune sauvegarde trouvée ou erreur de lecture. Nouvelle partie.");
            return null;
        }
    }
}