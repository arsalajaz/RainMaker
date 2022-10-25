import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.HashSet;

public class GameApp extends Application {
    private static final Point2D WINDOW_SIZE = new Point2D(500, 1000);


    @Override
    public void start(Stage stage) throws Exception {
        Game gRoot = new Game();
        Scene scene = new Scene(gRoot, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());

        scene.setOnKeyPressed(gRoot::handleKeyPressed);
        scene.setOnKeyReleased(gRoot::handleKeyReleased);

        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Rain Maker v1");
        stage.show();
    }

    public static void main(String args[]) { launch(args);}

}

class Game extends Pane {
    HashSet<KeyCode> keysDown = new HashSet<>();
    public Game() {

        AnimationTimer loop = new AnimationTimer() {

            double old = -1;

            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }

                double FrameTime = (now - old) / 1e9;
                old = now;
            }
        };

        loop.start();

    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private int isKeyDown(KeyCode k) {
        return keysDown.contains(k) ? 1 : 0;
    }

}

class GameObject extends Group {
}

class Pond extends GameObject {

}

class Cloud extends GameObject {

}

class Helipad extends GameObject {

}

class Helicopter extends GameObject {
    private int fuel;
}