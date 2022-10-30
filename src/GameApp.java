import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

        init();
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
        getChildren().clear();
        helicopter = new Helicopter(15, 5000, new Point2D(200,100), 25000);
        Helipad helipad = new Helipad(30, new Point2D(200,100));
        getChildren().addAll(helicopter, helicopter.getBoundingRect(), helipad);
    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());

        if(isKeyDown(KeyCode.UP)) helicopter.speedUp();
        if(isKeyDown(KeyCode.DOWN)) helicopter.speedDown();
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
    }

    void toggleLayoutBounds() {
        boundingRect.setVisible(!boundingRect.isVisible());
    }
    /*
    * Needs to be called when any transformations are applied to the group or
    *  its children
    * */
    protected void updateBoundingRect() {
        Bounds groupBounds = getBoundsInParent();
        boundingRect.setX(groupBounds.getMinX());
        boundingRect.setY(groupBounds.getMinY());
        boundingRect.setWidth(groupBounds.getWidth());
        boundingRect.setHeight(groupBounds.getHeight());
    }

    Rectangle getBoundingRect() {
        return boundingRect;
    }
}

class Pond extends GameObject {

}

class Cloud extends GameObject {

}

class Helipad extends GameObject {
    Circle pad;

    public Helipad(double radius, Point2D intialPosition) {
        pad = new Circle(radius);
        pad.setFill(Color.TRANSPARENT);
        pad.setStroke(Color.YELLOW);
        pad.setStrokeWidth(2);


        Bounds bounds = pad.getBoundsInParent();
        Rectangle border = new Rectangle(bounds.getMinX()-10,
                bounds.getMinY()-10,
                bounds.getWidth()+20, bounds.getHeight()+20);
        border.setStroke(Color.YELLOW);
        border.setStrokeWidth(2);
        border.setFill(Color.TRANSPARENT);


        getChildren().addAll(pad, border);

        setTranslateX(intialPosition.getX());
        setTranslateY(intialPosition.getY());

    }
}

class Helicopter extends GameObject implements Updatable {
    private double heading = 0;
    private boolean ignition = false;
    private double speed = 0;
    private int water;
    private int fuel;
    private GameText fuelLabel;
    private Circle body;
    private Line nose;

    public Helicopter(double bodyRadius, int initialWater,
                      Point2D initialPosition, int initialFuel) {

        water = initialWater;
        fuel = initialFuel;

        body = new Circle(bodyRadius, Color.YELLOW);
        nose = new Line(0, 0, 0, bodyRadius * 2);
        nose.setStrokeWidth(2);
        nose.setStroke(Color.YELLOW);

        fuelLabel = new GameText("Fuel: " + fuel);
        fuelLabel.setTextFill(Color.YELLOW);
        fuelLabel.setTranslateX(-30);
        fuelLabel.setTranslateY(-40);

        getChildren().addAll(body, nose, fuelLabel);

        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());
    }
    public void toggleIgnition() {
        System.out.println(speed);
        if(speed == 0.0)
            ignition = !ignition;
    }

    public void speedUp() {
        if(speed < 10.0 && ignition) speed += 0.1;
    }

    public void speedDown() {
        if(speed > -2.0 && ignition) speed -= 0.1;
    }

    public void turnLeft() {
        if(ignition) heading += 15;
    }

    public void turnRight() {
        if(ignition) heading -= 15;
    }

    private void move(double FrameTime) {
        double x, y;
        x = (Math.cos((heading + 90) * (Math.PI/180)) * (speed*FrameTime*30));
        y = (Math.sin((heading + 90) * (Math.PI/180)) * (speed*FrameTime*30));

        setTranslateX(getTranslateX() + x);
        setTranslateY(getTranslateY() + y);
    }

    @Override
    public void update(double FrameTime) {
        setRotate(heading);

        //Multiplying the speed by the frame time and a constant to keep the
        // speed similar between my gaming pc and laptop that run the game on
        // different fps

        if(fuel > 0) {
            move(FrameTime);
            fuel -= speed;
        } else {
            speed = 0;
            fuel = 0;
        }

        fuelLabel.setText("Fuel: " + fuel);
        updateBoundingRect();

    }
}



class GameText extends Label {
    public GameText(String text) {
        setText(text);
        setScaleY(-1);
    }
}

interface Updatable {
    void update(double FrameTime);
}