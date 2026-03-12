import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.File;

public class Main extends Application {

    private Stage mainWindow;


    public static Player joueur1 = new Player("Michel le fermier");
    public static Inventory inventory1 = new Inventory(joueur1);

    @Override
    public void start(Stage primaryStage) {
        this.mainWindow = primaryStage;
        mainWindow.setTitle("Ma Super Ferme");

        mainWindow.setMaximized(true);

        showMainMenu();

        mainWindow.show();
    }

    public void showMainMenu() {
        VBox menuRoot = new VBox(30);
        menuRoot.setAlignment(Pos.CENTER);

        menuRoot.setStyle(
                "-fx-background-image: url('/assets/bg.png'); " +
                        "-fx-background-size: cover; " +
                        "-fx-background-repeat: no-repeat; " +
                        "-fx-background-position: center center;"
        );

        ImageView logoView = new ImageView();
        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/assets/logo.png"));
            logoView.setImage(logoImg);

            logoView.setFitWidth(400);
            logoView.setPreserveRatio(true);

            logoView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        } catch (Exception e) {
            System.err.println("Impossible de charger le logo. Vérifiez le chemin !");
        }

        Button btnNewGame = createMenuButton("Nouvelle Partie");
        Button btnLoadGame = createMenuButton("Charger la Partie");
        Button btnQuit = createMenuButton("Quitter");

        File saveFile = new File("savegame.dat");
        if (!saveFile.exists()) {
            btnLoadGame.setDisable(true);
        }

        btnNewGame.setOnAction(e -> startGame(false));
        btnLoadGame.setOnAction(e -> startGame(true));
        btnQuit.setOnAction(e -> System.exit(0));

        menuRoot.getChildren().addAll(logoView, btnNewGame, btnLoadGame, btnQuit);

        Scene menuScene = new Scene(menuRoot);
        menuRoot.setTranslateY(-50);
        mainWindow.setScene(menuScene);
    }

    private void startGame(boolean loadSave) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/Interface.fxml")); // Vérifiez bien le chemin !
            Parent gameRoot = loader.load();

            FarmController controller = loader.getController();

            if (loadSave) {
                controller.loadSavedGame();
            }

            mainWindow.getScene().setRoot(gameRoot);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de Farm.fxml");
        }
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(250, 80);
        btn.setStyle(
                "-fx-background-image: url('/assets/button.png');" +
                        "-fx-background-size: stretch;" +
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #491d15;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 22;" +
                        "-fx-cursor: hand;"
        );
        btn.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
            if (isHovered) btn.setStyle(btn.getStyle() + "-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);");
            else btn.setStyle(btn.getStyle().replace("-fx-effect: dropshadow(three-pass-box, white, 10, 0.5, 0, 0);", ""));
        });
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}