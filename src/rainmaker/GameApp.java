package rainmaker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameApp extends Application {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 800;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Game gRoot = new Game(stage::close);
        Scene scene = new Scene(gRoot, GAME_WIDTH, GAME_HEIGHT);

        scene.setOnKeyPressed(gRoot::handleKeyPressed);
        scene.setOnKeyReleased(gRoot::handleKeyReleased);

        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Rain Maker v1");
        stage.show();

    }
}

