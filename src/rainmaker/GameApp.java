package rainmaker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import rainmaker.services.KeyPressTimer;
import rainmaker.services.TimedKeysCollection;

public class GameApp extends Application {
    private Game game;
    private TimedKeysCollection timedKeysCollection = new TimedKeysCollection();
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws Exception {
        game = Game.getInstance();
        game.setOnCloseRequest(stage::close);

        setupHelicopterKeyTimers();

        Scene scene = new Scene(game, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        scene.setOnKeyPressed((event) -> {
            switch (event.getCode()) {
                case R: game.init();                        break;
                case B: game.toggleBoundingBoxes();         break;
                case D: game.toggleDistanceLines();         break;
                case I: game.toggleHelicopterIgnition();    break;
                case C: System.gc();                        break;

                default: timedKeysCollection.keyPressed(event.getCode()); break;
            }
            timedKeysCollection.keyPressed(event.getCode());
        });

        scene.setOnKeyReleased((event) -> {
            timedKeysCollection.keyReleased(event.getCode());
        });

        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Rain Maker");
        stage.show();

    }

    /**
     * KeyPressTimers allow control of how often a key pressed event is fired
     * when a key is held down. This is useful for controlling helicopter
     * acceleration and the rate of seeding and keeping it consistent across
     * different computers.
     */
    private void setupHelicopterKeyTimers() {
        KeyPressTimer upKeyTimer = new KeyPressTimer(KeyCode.UP, 60);
        KeyPressTimer downKeyTimer = new KeyPressTimer(KeyCode.DOWN, 60);
        KeyPressTimer leftKeyTimer = new KeyPressTimer(KeyCode.LEFT, 10);
        KeyPressTimer rightKeyTimer = new KeyPressTimer(KeyCode.RIGHT, 10);
        KeyPressTimer spaceKeyTimer = new KeyPressTimer(KeyCode.SPACE, 80);

        upKeyTimer.setKeyPressAction(() -> game.speedUpHelicopter());
        downKeyTimer.setKeyPressAction(() -> game.speedDownHelicopter());
        leftKeyTimer.setKeyPressAction(() -> game.turnLeftHelicopter());
        rightKeyTimer.setKeyPressAction(() -> game.turnRightHelicopter());
        spaceKeyTimer.setKeyPressAction(() -> game.seedClouds());

        timedKeysCollection.addAll(upKeyTimer, downKeyTimer, leftKeyTimer,
                rightKeyTimer, spaceKeyTimer);
    }
}

