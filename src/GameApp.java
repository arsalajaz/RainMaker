import javafx.animation.AnimationTimer;
import javafx.application.Application;
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
    private static final Vector COPTER_INITIAL_POS =
            new Vector(GameApp.WINDOW_SIZE.getX()/2, 100);

    private static final Point2D PAD_INITIAL_POSITION =
            new Point2D(GameApp.WINDOW_SIZE.getX()/2, 100);

    private static final double COPTER_RADIUS = 15;
    private static final double PAD_RADIUS = GameApp.WINDOW_SIZE.getX()/10;
    private Helicopter helicopter;
    private Helipad helipad;
    private Cloud cloud;
    private Pond pond;
    private Alert gameOverAlert;
    private Runnable stageClose;
    private AnimationTimer animationTimer;
    public Game(Runnable stageClose) {
        setScaleY(-1);
        setBackground(Background.fill(Color.BLACK));

        this.stageClose = stageClose;

        gameOverAlert = new Alert(Alert.AlertType.CONFIRMATION);
        gameOverAlert.setTitle("Confirmation");
        gameOverAlert.setHeaderText("Game Over!");
        gameOverAlert.setContentText("Would you like to play again?");

        init();

        animationTimer = new AnimationTimer() {
            double old = -1;
            double cloudSeedingTime = 0;
            double rainRate = 0;
            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }
                double FrameTime = (now - old) / 1e9;
                old = now;
                cloudSeedingTime += FrameTime;
                rainRate += FrameTime;

                update(FrameTime);

                if(helicopter.interest(cloud) && isKeyDown(KeyCode.SPACE) && cloudSeedingTime >= 0.1) {
                    helicopter.seedCloud(cloud);
                    cloudSeedingTime = 0;
                }

                if(cloud.isRaining() && rainRate >= 0.1) {
                    pond.addWater();
                    rainRate = 0;
                }

            }
        };

        animationTimer.start();
    }

    private void update(double FrameTime) {
        for(Node node : getChildren()) {
            if(node instanceof Updatable) ((Updatable) node).update(FrameTime);
        }
    }

    void onCopterCrash() {
        animationTimer.stop();
        String msg = "Game Over! Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                animationTimer.start();
                init();
            } else {
                stageClose.run();
            }
        });
        alert.show();
    }

    private void init() {
        getChildren().clear();

        helicopter = new Helicopter(COPTER_RADIUS, 5000, COPTER_INITIAL_POS,
                25000);
        helicopter.setOnCrash(this::onCopterCrash);

        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);

        double randCloudRadius = rand(30,60);
        cloud = new Cloud(randPoint(randCloudRadius,randCloudRadius), randCloudRadius);

        double randPondArea = rand(1500,2500);
        double pondRadius = Pond.getRadius(randPondArea);
        pond = new Pond(randPoint(pondRadius,pondRadius), randPondArea);

        getChildren().addAll(helipad, pond, cloud, helicopter);
        getChildren().addAll(
                pond.getBoundingRect(),
                helipad.getBoundingRect(),
                cloud.getBoundingRect(),
                helicopter.getBoundingRect()
        );
    }

    // Returns a random point within the top 2/3 of the window
    private Point2D randPoint(double width, double height) {
        double x,y;
        x = rand(width, GameApp.WINDOW_SIZE.getX()-width);
        y = rand(2*GameApp.WINDOW_SIZE.getY()/3,
                GameApp.WINDOW_SIZE.getY())-height;
        return new Point2D(x,y);
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

class Pond extends GameObject implements Updatable {
    private double pondArea;
    private int waterLevel = 0;
    private GameText waterLevelText = new GameText();
    Circle pondShape = new Circle();

    public Pond(Point2D initialPosition, double initialArea) {
        pondArea = initialArea;
        pondShape.setRadius(getRadius());
        pondShape.setFill(Color.BLUE);

        waterLevelText.setFill(Color.WHITE);

        getChildren().addAll(pondShape, waterLevelText);
        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());
    }

    public double getRadius() {
    	return Math.sqrt(pondArea / Math.PI);
    }

    public static double getRadius(double area) {
    	return Math.sqrt(area / Math.PI);
    }

    public void addWater() {
        pondArea += 50;
        waterLevel++;
    }

    @Override
    Shape getShape() {
        return pondShape;
    }

    @Override
    public void update(double FrameTime) {
        pondShape.setRadius(Math.sqrt(pondArea / Math.PI));

        waterLevelText.setText(String.valueOf(waterLevel));
        waterLevelText.setTranslateX(-waterLevelText.getLayoutBounds()
                .getWidth()/2);
        waterLevelText.setTranslateY(waterLevelText.getLayoutBounds()
                .getHeight()/4);

        updateBoundingRect();
    }
}

class Cloud extends GameObject implements Updatable {
    Circle cloud;
    GameText percentText;
    private int saturation = 0;

    public Cloud(Point2D position, double initialRadius) {
        cloud = new Circle(initialRadius, Color.rgb(155,155,155));

        percentText = new GameText();
        percentText.setFill(Color.BLUE);

        getChildren().addAll(cloud, percentText);

        setTranslateX(position.getX());
        setTranslateY(position.getY());
    }
    public boolean isRaining() {
        return saturation > 0;
    }
    public void saturate() {
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
    private double speed = 0;
    private Vector position;
    private boolean ignition = false;
    private boolean landed = true;

    private int water;
    private int fuel;
    private Runnable onCrashAction;
    private GameText fuelLabel;
    private Circle body;
    private Line nose;
    public Helicopter(double bodyRadius, int initialWater,
                      Vector initialPosition, int initialFuel) {

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

        position = initialPosition;
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
        if(ignition) heading -= 15;
    }

    public void turnRight() {
        if(ignition) heading += 15;
    }

    private double convertDegreesToRadians(double degrees) {
        return degrees * (Math.PI/180);
    }
    private double getCartesianAngle() {
        return (450-heading)%360;
    }

    @Override
    public void update(double frameTime) {
        setRotate(getCartesianAngle() - 90);

        double headingRadians = convertDegreesToRadians(getCartesianAngle());
        Vector velocity = new Vector(speed, headingRadians, true)
                .multiply(frameTime*30);

        position = position.add(velocity);

        if(fuel > 0 && ignition) {
            fuel -= Math.abs((speed+20)*frameTime*30);
        } else if(fuel <= 0 && ignition) {
            fuel = 0;
            ignition = false;
            if(onCrashAction != null) onCrashAction.run();
        }

        setTranslateX(position.getX());
        setTranslateY(position.getY());

        fuelLabel.setText("F: " + fuel);
        fuelLabel.setTranslateX(-fuelLabel.getLayoutBounds().getWidth()/2);

        updateBoundingRect();
    }

    public void seedCloud(Cloud cloud) {
        cloud.saturate();
        water--;

    }

    void setOnCrash(Runnable action) {
        this.onCrashAction = action;
    }

    @Override
    Shape getShape() {
        return Path.union(body,nose);
    }
}

// Vector class that can convert angle and magnitude to x and y components
// and vice versa
class Vector {
    private double x;
    private double y;
    private double magnitude;
    private double angle;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;

        magnitude = Math.sqrt(x*x + y*y);
        angle = Math.atan2(y, x);
    }
    public Vector(double magnitude, double angle, boolean polar) {
        this.magnitude = magnitude;
        this.angle = angle;

        x = magnitude * Math.cos(angle);
        y = magnitude * Math.sin(angle);
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getMagnitude() {
        return magnitude;
    }
    public double getAngle() {
        return angle;
    }
    public Vector add(Vector v) {
        return new Vector(x + v.getX(), y + v.getY());
    }

    public Vector multiply(double scalar) {
        return new Vector(x * scalar, y * scalar);
    }

    public Vector reflectAcrossXAxis() {
        return new Vector(x, -y);
    }

    public Vector reflectAcrossYAxis() {
        return new Vector(-x, y);
    }

    public String toString() {
        return String.format("Vector: (x: %.2f, y: %.2f, " +
                "angle: %.2f, mag: %.2f)", x, y, angle, magnitude);
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