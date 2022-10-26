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
        if(event.getCode() == KeyCode.I) helicopter.toggleIgnition();
        if(event.getCode() == KeyCode.B) helicopter.toggleLayoutBounds();
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private boolean isKeyDown(KeyCode k) {
        return keysDown.contains(k);
    }

}

class GameObject extends Group {
    private Rectangle boundingRect = new Rectangle();

    public GameObject() {
        boundingRect.setFill(Color.TRANSPARENT);
        boundingRect.setStrokeWidth(1);
        boundingRect.setStroke(Color.YELLOW);
        boundingRect.setVisible(false);

        getChildren().add(boundingRect);
    }

    void toggleLayoutBounds() {
        boundingRect.setVisible(!boundingRect.isVisible());
    }
    protected void updateLayoutBounds(Group object) {
        Bounds groupBounds = object.getBoundsInParent();
        boundingRect.setX(groupBounds.getMinX());
        boundingRect.setY(groupBounds.getMinY());
        boundingRect.setWidth(groupBounds.getWidth());
        boundingRect.setHeight(groupBounds.getHeight());
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

    Group helicopter;
    private Circle body;
    private Line nose;

    public Helicopter(double bodyRadius, int initialWater,
                      Point2D initialPosition, int initialFuel) {

        water = initialWater;
        fuel = initialFuel;

        body = new Circle(bodyRadius, Color.YELLOW);
        nose = new Line(0, 0, 0, bodyRadius * 2);
        nose.setStroke(Color.YELLOW);

        helicopter = new Group(body, nose);
        getChildren().add(helicopter);
        helicopter.setTranslateX(initialPosition.getX());
        helicopter.setTranslateY(initialPosition.getY());
    }
    public void toggleIgnition() {
        System.out.println(speed);
        if(speed == 0.0)
            ignition = !ignition;
    }

    public void moveForward() {
        if(speed < 10.0 && ignition) speed += 0.1;
    }

    public void moveBackward() {
        if(speed > -2.0 && ignition) speed -= 0.1;
    }

    public void turnLeft() {
        if(ignition) heading += 15;
    }

    public void turnRight() {
        if(ignition) heading -= 15;
    }

    @Override
    public void update(double FrameTime) {
        helicopter.setRotate(heading);

        //Multiplying the speed by the frame time and a constant to keep the
        // speed similar between my gaming pc and laptop that run the game on
        // different fps
        double x, y;
        x = (Math.cos((heading + 90) * (Math.PI/180)) * (speed*FrameTime*30));
        y = (Math.sin((heading + 90) * (Math.PI/180)) * (speed*FrameTime*30));

        helicopter.setTranslateX(helicopter.getTranslateX() + x);
        helicopter.setTranslateY(helicopter.getTranslateY() + y);

        updateLayoutBounds(helicopter);

    }
}

interface Updatable {
    void update(double FrameTime);
}