import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Optional;

public class GameApp extends Application {
    public static final Point2D WINDOW_SIZE = new Point2D(400, 800);
    @Override
    public void start(Stage stage) throws Exception {
        Game gRoot = new Game(stage::close);
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
    private static final Point2D COPTER_INITIAL_POSITION =
            new Point2D(GameApp.WINDOW_SIZE.getX()/2, 100);

    private static final Point2D PAD_INITIAL_POSITION =
            new Point2D(GameApp.WINDOW_SIZE.getX()/2, 100);

    private static final double COPTER_RADIUS = 15;
    private static final double PAD_RADIUS = GameApp.WINDOW_SIZE.getX()/10;
    private Helicopter helicopter;
    private Helipad helipad;
    private Cloud cloud;
    private Alert gameOverAlert;
    private Runnable stageClose;
    public Game(Runnable stageClose) {
        setScaleY(-1);
        setBackground(Background.fill(Color.BLACK));

        this.stageClose = stageClose;

        gameOverAlert = new Alert(Alert.AlertType.CONFIRMATION);
        gameOverAlert.setTitle("Confirmation");
        gameOverAlert.setHeaderText("Game Over!");
        gameOverAlert.setContentText("Would you like to play again?");

        init();
        AnimationTimer loop = new AnimationTimer() {
            double old = -1;
            double count = 0;
            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }
                double FrameTime = (now - old) / 1e9;
                old = now;
                count += FrameTime;

                update(FrameTime);

                if(helicopter.interest(cloud) && isKeyDown(KeyCode.SPACE) && count >= 0.1) {
                    cloud.addWater();
                    count = 0;
                }

            }
        };

        loop.start();
    }

    private void update(double FrameTime) {
        for(Node node : getChildren()) {
            if(node instanceof Updatable) ((Updatable) node).update(FrameTime);
        }
    }

    void onCrash() {
        if(!gameOverAlert.isShowing())
            Platform.runLater(() -> {
                Optional<ButtonType> result = gameOverAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    init();
                }
                if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                    stageClose.run();
                }
            });
    }

    private void init() {
        getChildren().clear();
        helicopter = new Helicopter(COPTER_RADIUS, 5000,
                COPTER_INITIAL_POSITION, 25000);
        helicopter.setOnCrash(this::onCrash);
        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);
        cloud = new Cloud(new Point2D(rand(50,
                GameApp.WINDOW_SIZE.getX()-50),
                rand(GameApp.WINDOW_SIZE.getY()/3,GameApp.WINDOW_SIZE.getY())-50));
        getChildren().addAll(helipad, helipad.getBoundingRect(),cloud,
                cloud.getBoundingRect(), helicopter.getBoundingRect(),
                helicopter);
    }

    public static double rand(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());

        if(isKeyDown(KeyCode.UP)) helicopter.speedUp();
        if(isKeyDown(KeyCode.DOWN)) helicopter.speedDown();
        if(isKeyDown(KeyCode.RIGHT)) helicopter.turnRight();
        if(isKeyDown(KeyCode.LEFT)) helicopter.turnLeft();
        if(event.getCode() == KeyCode.I) helicopter.toggleIgnition(helipad);
        if(event.getCode() == KeyCode.B) {
            for(Node node : getChildren()) {
                if(node instanceof GameObject)
                    ((GameObject) node).toggleBoundingBox();
            }
        };
        if(event.getCode() == KeyCode.R) init();
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private boolean isKeyDown(KeyCode k) {
        return keysDown.contains(k);
    }

}

abstract class GameObject extends Group {
    private Rectangle boundingRect = new Rectangle();

    public GameObject() {
        boundingRect.setFill(Color.TRANSPARENT);
        boundingRect.setStrokeWidth(1);
        boundingRect.setStroke(Color.YELLOW);
        boundingRect.setVisible(false);
    }

    void toggleBoundingBox() {
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

    public boolean interest(GameObject object) {
        return !Shape.intersect(this.getShape(),object.getShape())
                .getBoundsInLocal().isEmpty();
    }

    abstract Shape getShape();
}

class Pond extends GameObject {

    @Override
    Shape getShape() {
        return null;
    }
}

class Cloud extends GameObject implements Updatable {
    Circle cloud;

    GameText percentText;
    private int saturation = 0;

    public Cloud(Point2D position) {
        cloud = new Circle(50, Color.rgb(155,155,155));

        percentText = new GameText();
        percentText.setFill(Color.BLUE);

        getChildren().addAll(cloud, percentText);

        setTranslateX(position.getX());
        setTranslateY(position.getY());

    }
    public void addWater() {
        if(saturation >= 100) return;
        saturation++;
    }
    @Override
    Shape getShape() {
        return cloud;
    }

    @Override
    public void update(double FrameTime) {
        cloud.setFill(Color.rgb(155-saturation, 155-saturation,
                155-saturation));
        percentText.setText(saturation + "%");
        percentText.setTranslateX(-percentText.getLayoutBounds().getWidth()/2);
        percentText.setTranslateY(percentText.getLayoutBounds().getHeight()/2);

        updateBoundingRect();

    }
}

class Helipad extends GameObject {
    Circle pad;

    public Helipad(double radius, Point2D intialPosition) {
        pad = new Circle(radius);
        pad.setFill(Color.TRANSPARENT);
        pad.setStroke(Color.GRAY);
        pad.setStrokeWidth(2);

        Bounds bounds = pad.getBoundsInParent();
        Rectangle border = new Rectangle(bounds.getMinX()-10,
                bounds.getMinY()-10,
                bounds.getWidth()+20, bounds.getHeight()+20);
        border.setStroke(Color.GRAY);
        border.setStrokeWidth(2);
        border.setFill(Color.TRANSPARENT);

        getChildren().addAll(pad, border);

        setTranslateX(intialPosition.getX());
        setTranslateY(intialPosition.getY());

        updateBoundingRect();

    }

    @Override
    Shape getShape() {
        return pad;
    }
}

class Helicopter extends GameObject implements Updatable {
    private double heading = 0;
    private boolean ignition = false;
    private boolean landed = true;
    private double speed = 0;
    private int water;
    private int fuel;
    private Runnable action;
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

        fuelLabel = new GameText();
        fuelLabel.setFill(Color.YELLOW);
        fuelLabel.setTranslateY(-bodyRadius - 5);

        getChildren().addAll(body,nose, fuelLabel);

        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());
    }

    public void toggleIgnition(Helipad helipad) {
        if(helipad.getBoundsInParent().contains(getBoundsInParent()) && Math.abs(speed) < 0.1 && ignition) {
            speed = 0;
            ignition = false;
            landed = true;
        } else if(helipad.getBoundsInParent().contains(getBoundsInParent())) {
            ignition = true;
            landed = false;
        }
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

        if(fuel > 0 && ignition) {
            move(FrameTime);
            fuel -= Math.abs((speed+20)*FrameTime*30);
        } else if(fuel <= 0 && !landed) {
            fuel = 0;
            landed = true;
            ignition = false;
            if(action != null) action.run();
        }


        fuelLabel.setText("F: " + fuel);
        fuelLabel.setTranslateX(-fuelLabel.getLayoutBounds().getWidth()/2);

        updateBoundingRect();
    }

    public void seed(Cloud cloud) {

    }

    void setOnCrash(Runnable action) {
        this.action = action;
    }

    @Override
    Shape getShape() {
        return Path.union(body,nose);
    }
}



class GameText extends Text {
    public GameText() {
        setScaleY(-1);
    }
}

interface Updatable {
    void update(double FrameTime);
}