import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.HashSet;

public class GameApp extends Application {
    private static final Point2D WINDOW_SIZE = new Point2D(400, 800);

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

    private Helicopter helicopter;
    public Game() {
        setScaleY(-1);
        setBackground(Background.fill(Color.BLACK));

        helicopter = new Helicopter(15, 5000, new Point2D(100,100), 25000);
        helicopter.showLayoutBounds();

        getChildren().add(helicopter);

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

                helicopter.update(FrameTime);



            }
        };

        loop.start();
    }

    private void update() {

    }

    private void init() {

    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());
        if(isKeyDown(KeyCode.UP)) helicopter.moveForward();
        if(isKeyDown(KeyCode.DOWN)) helicopter.moveBackward();
        if(isKeyDown(KeyCode.RIGHT)) helicopter.turnRight();
        if(isKeyDown(KeyCode.LEFT)) helicopter.turnLeft();
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private boolean isKeyDown(KeyCode k) {
        return keysDown.contains(k);
    }

}

class GameObject extends Group {
    private Rectangle bounds = new Rectangle();

    public GameObject() {
        bounds.setFill(Color.TRANSPARENT);
        bounds.setStrokeWidth(1);
        bounds.setStroke(Color.YELLOW);

        getChildren().add(bounds);
    }

    void showLayoutBounds() {
        bounds.setVisible(true);
    }
    protected void updateLayoutBounds() {
        Bounds groupBounds = getLayoutBounds();
        bounds.setX(groupBounds.getMinX());
        bounds.setY(groupBounds.getMinY());
        bounds.setWidth(groupBounds.getWidth());
        bounds.setHeight(groupBounds.getHeight());
    }
    void hideLayoutBounds() {
        bounds.setVisible(false);
    }
}

class Pond extends GameObject {

}

class Cloud extends GameObject {

}

class Helipad extends GameObject {
    Circle pad;

    public Helipad(double radius) {
        pad = new Circle(radius);
        pad.setStroke(Color.YELLOW);
    }
}

class Helicopter extends GameObject implements Updatable{
    private double heading = 0;
    private boolean ignition = false;
    private double speed = 0;
    private int water;
    private int fuel;

    private Circle body;
    private Line nose;

    public Helicopter(double bodyRadius, int initialWater,
                      Point2D initialPosition, int initialFuel) {

        water = initialWater;
        fuel = initialFuel;

        body = new Circle(bodyRadius, Color.YELLOW);
        nose = new Line(0, 0, 0, bodyRadius * 2);
        nose.setStroke(Color.YELLOW);

        getChildren().addAll(body, nose);

        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());

        updateLayoutBounds();
    }
    public void toggleIgnition() {
        ignition = !ignition;
    }

    public void moveForward() {
        speed = speed < 10.0 ? speed + 0.1 : speed;
    }

    public void moveBackward() {
        speed = speed > -2.0 ? speed - 0.1 : speed;
    }

    public void turnLeft() {
        heading += 15;
    }

    public void turnRight() {
        heading -= 15;
    }

    @Override
    public void update(double FrameTime) {
        setRotate(heading);

        double x, y;
        x = (Math.cos((heading + 90) * (Math.PI/180)) * (speed*FrameTime*4));
        y = (Math.sin((heading + 90) * (Math.PI/180)) * (speed*FrameTime*4));

        setTranslateX(getTranslateX() + x);
        setTranslateY(getTranslateY() + y);

        System.out.println(getTranslateX());
        System.out.println(getTranslateY());
    }
}

interface Updatable {
    void update(double FrameTime);
}