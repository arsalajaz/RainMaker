import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.*;

enum BladeState {
    STOPPED, INCREASING_SPEED, DECREASING_SPEED, AT_MAX_SPEED
}

enum CloudState {
    SPAWNED, ALIVE, DEAD
}

interface Updatable {
    void update(double FrameTime);
}

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

class Game extends Pane {
    private static final Vector COPTER_INITIAL_POS =
            new Vector(GameApp.GAME_WIDTH / 2, 100);
    private static final Point2D PAD_INITIAL_POSITION =
            new Point2D(GameApp.GAME_WIDTH / 2, 100);
    private static final double COPTER_RADIUS = 15;
    private static final double PAD_RADIUS = GameApp.GAME_WIDTH / 10;
    private final Alert gameOverAlert;
    private final Runnable stageClose;
    private final AnimationTimer animationTimer;
    HashSet<KeyCode> keysDown = new HashSet<>();
    private Helicopter helicopter;
    private Helipad helipad;
    private Clouds clouds;
    private Pond pond;

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

            }
        };

        animationTimer.start();
    }

    public static double rand(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    private void update(double FrameTime) {
        helicopter.update(FrameTime);
        clouds.update(FrameTime);
        pond.update(FrameTime);
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

        helicopter = new Helicopter(5000, COPTER_INITIAL_POS, 25000);
        helicopter.setOnCrash(this::onCopterCrash);

        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);

        clouds = new Clouds();

        double randPondArea = rand(1500, 2500);
        double pondRadius = Pond.getRadius(randPondArea);
        pond = new Pond(randPoint(pondRadius, pondRadius), randPondArea);

        getChildren().add(new ImageBackground(GameApp.GAME_WIDTH,
                GameApp.GAME_HEIGHT));
        getChildren().addAll(helipad, pond, clouds, helicopter);
        getChildren().addAll(
                pond.getBoundingRect(),
                helipad.getBoundingRect(),

                helicopter.getBoundingRect()
        );
    }

    // Returns a random point within the top 2/3 of the window
    private Point2D randPoint(double width, double height) {
        double x, y;
        x = rand(width, GameApp.GAME_WIDTH - width);
        y = rand(2 * GameApp.GAME_HEIGHT / 3, GameApp.GAME_HEIGHT) - height;
        return new Point2D(x, y);
    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());

        if (isKeyDown(KeyCode.UP)) helicopter.speedUp();
        if (isKeyDown(KeyCode.DOWN)) helicopter.speedDown();
        if (isKeyDown(KeyCode.RIGHT)) helicopter.turnRight();
        if (isKeyDown(KeyCode.LEFT)) helicopter.turnLeft();
        if (event.getCode() == KeyCode.I) {
            if (helicopter.getState() == helicopter.getOffState())
                helicopter.startEngine(helipad);
            else
                helicopter.stopEngine();
        }
        if (event.getCode() == KeyCode.B) {
            for (Node node : getChildren()) {
                if (node instanceof GameObject)
                    ((GameObject) node).toggleBoundingBox();
            }
        }
        if (event.getCode() == KeyCode.R) init();
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private boolean isKeyDown(KeyCode k) {
        return keysDown.contains(k);
    }

}

abstract class GameObject extends Group {
    private final Rectangle boundingRect = new Rectangle();
    protected Translate myTranslation;
    protected Rotate myRotation;
    protected Scale myScale;

    public GameObject() {
        boundingRect.setFill(Color.TRANSPARENT);
        boundingRect.setStrokeWidth(1);
        boundingRect.setStroke(Color.YELLOW);
        boundingRect.setVisible(false);

        myTranslation = new Translate();
        myRotation = new Rotate();
        myScale = new Scale();


        this.getTransforms().addAll(myTranslation, myRotation, myScale);
    }

    public void rotate(double degrees, double pivotX, double pivotY) {
        myRotation.setAngle(degrees);
        myRotation.setPivotX(pivotX);
        myRotation.setPivotY(pivotY);
    }

    public void scale(double sx, double sy) {
        myScale.setX(sx);
        myScale.setY(sy);
    }

    public void translate(double tx, double ty) {
        myTranslation.setX(tx);
        myTranslation.setY(ty);
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
        return !Shape.intersect(this.getShape(), object.getShape())
                .getBoundsInLocal().isEmpty();
    }

    abstract Shape getShape();
}

class Pond extends GameObject implements Updatable {
    private final GameText waterLevelText = new GameText();
    Circle pondShape = new Circle();
    private double pondArea;
    private int waterLevel = 0;

    public Pond(Point2D initialPosition, double initialArea) {
        pondArea = initialArea;
        pondShape.setRadius(getRadius());
        pondShape.setFill(Color.BLUE);

        waterLevelText.setFill(Color.WHITE);

        getChildren().addAll(pondShape, waterLevelText);
        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());
    }

    public static double getRadius(double area) {
        return Math.sqrt(area / Math.PI);
    }

    public double getRadius() {
        return Math.sqrt(pondArea / Math.PI);
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
        pondShape.setRadius(getRadius());

        waterLevelText.setText(String.valueOf(waterLevel));
        waterLevelText.setTranslateX(-waterLevelText.getLayoutBounds()
                .getWidth() / 2);
        waterLevelText.setTranslateY(waterLevelText.getLayoutBounds()
                .getHeight() / 4);

        updateBoundingRect();
    }
}

class Cloud extends GameObject implements Updatable {
    private static final double WIND_SPEED = 0.4;
    private static final double WIND_DIRECTION = 45;

    private CloudState state = CloudState.SPAWNED;

    private Vector position;
    private Vector velocity;
    private final Circle cloud;
    private final GameText percentText;
    private int saturation = 0;
    private double speedOffset = Game.rand(40,70);

    public Cloud(Point2D position, double initialRadius) {
        cloud = new Circle(initialRadius, Color.rgb(155, 155, 155));

        percentText = new GameText();
        percentText.setFill(Color.BLUE);

        getChildren().addAll(cloud, percentText);

        this.position = new Vector(position.getX(), position.getY());
    }

    public boolean isRaining() {
        return saturation > 0;
    }

    public CloudState getState() {
        return state;
    }

    public void rain() {
        saturation--;
    }

    public void saturate() {
        if (saturation >= 100) return;
        saturation++;
    }

    @Override
    Shape getShape() {
        return cloud;
    }

    @Override
    public void update(double FrameTime) {
        velocity = new Vector(WIND_SPEED * FrameTime * speedOffset,
                Math.toRadians(WIND_DIRECTION), true);
        position = position.add(velocity);

        double radius = cloud.getRadius();
        // if the cloud is within the bounds of the screen then it is alive
        if (position.getX() > 0 - radius && position.getX() < 800 + radius &&
                position.getY() > 0 - radius && position.getY() < 800 + radius) {
            state = CloudState.ALIVE;
        }
        // if the cloud goes off the screen, it dies
        else if (position.getX() < 0 - radius && velocity.getX() < 0 ||
                position.getX() > 800 + radius && velocity.getX() > 0 ||
                position.getY() < 0 - radius && velocity.getY() < 0 ||
                position.getY() > 800 + radius && velocity.getY() > 0) {
            state = CloudState.DEAD;
        }


        setTranslateX(position.getX());
        setTranslateY(position.getY());

        cloud.setFill(Color.rgb(155 - saturation, 155 - saturation,
                155 - saturation));
        percentText.setText(saturation + "%");
        percentText.setTranslateX(-percentText.getLayoutBounds().getWidth() / 2);
        percentText.setTranslateY(percentText.getLayoutBounds().getHeight() / 2);

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
        Rectangle border = new Rectangle(bounds.getMinX() - 10,
                bounds.getMinY() - 10,
                bounds.getWidth() + 20, bounds.getHeight() + 20);
        border.setStroke(Color.GRAY);
        border.setStrokeWidth(2);
        border.setFill(Color.TRANSPARENT);

        getChildren().addAll(pad, border);

        translate(intialPosition.getX(), intialPosition.getY());
        updateBoundingRect();

    }

    @Override
    Shape getShape() {
        return pad;
    }
}

class Helicopter extends GameObject implements Updatable {
    private static final double MAX_SPEED = 10;
    private static final double MIN_SPEED = -2;
    private static final double ACCELERATION = 0.1;
    private final HelicopterState offState;
    private final HelicopterState startingState;
    private final HelicopterState readyState;
    private final HelicopterState stoppingState;
    private final boolean landed = true;
    private final GameText fuelText;
    private final GameText stateText;
    private final HeloBody heloBody;
    private final HeloBlade heloBlade;
    private HelicopterState currState;
    private double heading = 0;
    private double speed = 0;
    private Vector position;
    private int water;
    private int fuel;
    private Runnable onCrashAction;

    public Helicopter(int initialWater, Vector initialPosition, int initialFuel) {
        water = initialWater;
        fuel = initialFuel;

        heloBody = new HeloBody();
        heloBlade = new HeloBlade();

        fuelText = new GameText();
        stateText = new GameText();

        fuelText.setFill(Color.BLUE);
        stateText.setFill(Color.BLUE);

        getChildren().addAll(heloBody, heloBlade, fuelText, stateText);

        position = initialPosition;

        offState = new HelicopterOffState(this);
        startingState = new HelicopterStartingState(this);
        readyState = new HelicopterReadyState(this);
        stoppingState = new HelicopterStoppingState(this);

        currState = offState;
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public void startEngine(Helipad helipad) {
        currState.startEngine();
    }

    public void stopEngine() {
        currState.stopEngine();
    }

    public void speedUp() {
        currState.speedUp();
    }

    public void speedDown() {
        currState.speedDown();
    }

    public void turnLeft() {
        currState.turnLeft();
    }

    public void turnRight() {
        currState.turnRight();
    }

    private double convertDegreesToRadians(double degrees) {
        return degrees * (Math.PI / 180);
    }

    private double getCartesianAngle() {
        return (450 - heading) % 360;
    }

    private void move() {
        rotate(
                getCartesianAngle() - 90,
                heloBlade.getTranslateX(),
                heloBlade.getTranslateY()
        );
        translate(
                position.getX(),
                position.getY()
        );
    }

    private void calculateNewPosition(double frameTime) {
        double angle = convertDegreesToRadians(getCartesianAngle());
        Vector velocity = new Vector(speed, angle, true)
                .multiply(frameTime * 30);

        position = position.add(velocity);
    }

    @Override
    public void update(double frameTime) {
        calculateNewPosition(frameTime);
        move();
        updateLabels();
        updateBoundingRect();

        currState.consumeFuel(frameTime);
    }

    private void updateLabels() {
        fuelText.setText("F: " + fuel);
        fuelText.setTranslateX(-fuelText.getLayoutBounds().getWidth() / 2);
        fuelText.setTranslateY(-65);

        stateText.setText(currState.toString());
        stateText.setTranslateX(-stateText.getLayoutBounds().getWidth() / 2);
        stateText.setTranslateY(-65 - fuelText.getLayoutBounds().getHeight());
    }

    public void seedCloud(Cloud cloud) {
        cloud.saturate();
        water--;
    }

    void setOnCrash(Runnable action) {
        this.onCrashAction = action;
    }

    public HelicopterState getState() {
        return currState;
    }

    public void setState(HelicopterState currState) {
        this.currState = currState;
    }

    @Override
    Shape getShape() {
        return heloBody;
    }

    public void accelerate() {
        if (speed >= MAX_SPEED) return;
        speed = round(speed + ACCELERATION, 1);
    }

    public void decelerate() {
        if (this.speed <= MIN_SPEED) return;
        speed = round(speed - ACCELERATION, 1);
    }

    public int getFuel() {
        return fuel;
    }

    public void setFuel(int fuel) {
        this.fuel = fuel;
    }

    public double getSpeed() {
        return speed;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public HelicopterState getOffState() {
        return offState;
    }

    public HelicopterState getStartingState() {
        return startingState;
    }

    public HelicopterState getReadyState() {
        return readyState;
    }

    public HelicopterState getStoppingState() {
        return stoppingState;
    }

    public HeloBlade getBlade() {
        return heloBlade;
    }

    public void crash() {
        if (onCrashAction != null) {
            onCrashAction.run();
        }
    }
}

/**
 * Game object pane that stores collection of one type of game objects and
 * updtable
 */

class GameObjectPane<T extends GameObject> extends Pane implements Iterable {
    private final List<T> objects = new ArrayList<>();

    public void add(T object) {
        objects.add(object);
        getChildren().add(object);
    }

    public void remove(T object) {
        objects.remove(object);
        getChildren().remove(object);
    }

    public void clear() {
        objects.clear();
        getChildren().clear();
    }

    public List<T> getObjects() {
        return objects;
    }

    @Override
    public Iterator iterator() {
        return objects.iterator();
    }
}

class Clouds extends GameObjectPane<Cloud> implements Updatable {
    private static final int MAX_CLOUDS = 5;
    private static final int MIN_CLOUDS = 2;


    private static final double CLOUD_RADIUS = 50;

    private final Random random = new Random();

    public Clouds() {
        // Create initial clouds
        for (int i = 0; i < MAX_CLOUDS; i++) {
            add(new Cloud(randomSpawnPoint(), CLOUD_RADIUS));
        }
    }

    private Point2D randomSpawnPoint() {
        return new Point2D(-CLOUD_RADIUS, Game.rand(0, 800));
    }


    @Override
    public void update(double frameTime) {
        for (int i = 0; i < getObjects().size(); i++) {
            if (getObjects().get(i).getState() == CloudState.DEAD) {
                remove(getObjects().get(i));
                if (getObjects().size() < MAX_CLOUDS) {
                    if (getObjects().size() <= MIN_CLOUDS) {
                        add(new Cloud(randomSpawnPoint(), CLOUD_RADIUS));
                    } else if (Math.random() < 0.5) {
                        add(new Cloud(randomSpawnPoint(), CLOUD_RADIUS));
                    }
                }
            }
            else getObjects().get(i).update(frameTime);

        }


    }
}

class ImageBackground extends Pane {
    private final ImageView background;

    public ImageBackground(double width, double height) {
        background = new ImageView(new Image("/Assets/Desert.jpg"));
        background.setFitWidth(width);
        background.setFitHeight(height);
        getChildren().add(background);
    }

}

//implement state pattern for helicopter
abstract class HelicopterState {
    protected Helicopter helicopter;

    public HelicopterState(Helicopter helicopter) {
        this.helicopter = helicopter;
    }

    abstract void startEngine();

    abstract void stopEngine();

    abstract void speedUp();

    abstract void speedDown();

    abstract void turnLeft();

    abstract void turnRight();

    abstract void seedCloud(Cloud cloud);

    abstract void consumeFuel(double rate);
}

class HelicopterOffState extends HelicopterState {
    public HelicopterOffState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void startEngine() {
        helicopter.setState(helicopter.getStartingState());
        helicopter.getState().startEngine();
    }

    @Override
    void stopEngine() { /* Do nothing - engine already off */}

    @Override
    void speedUp() { /* Do nothing - helicopter is off */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is off */ }

    @Override
    void turnLeft() { /* Do nothing - helicopter is off */ }

    @Override
    void turnRight() { /* Do nothing - helicopter is off */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is off */}

    @Override
    void consumeFuel(double rate) {
        /* Do nothing - helicopter is off */
    }

    public String toString() {
        return "Off";
    }

}

class HelicopterStartingState extends HelicopterState {
    public HelicopterStartingState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void startEngine() {
        helicopter.getBlade().setOnMaxRotationalSpeed(() ->
                helicopter.setState(helicopter.getReadyState())
        );
        helicopter.getBlade().startSpinning();
    }

    @Override
    void stopEngine() {
        helicopter.setState(helicopter.getStoppingState());
        helicopter.getState().stopEngine();
    }

    @Override
    void speedUp() { /* Do nothing - helicopter is starting */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is starting */}

    @Override
    void turnLeft() { /* Do nothing - helicopter is starting */}

    @Override
    void turnRight() { /* Do nothing - helicopter is starting */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is starting */}

    @Override
    void consumeFuel(double rate) {
        double fuel = helicopter.getFuel();
        helicopter.setFuel((int) (fuel - (3 * rate)));

        if (helicopter.getFuel() <= 0) {
            helicopter.setState(helicopter.getStoppingState());
            helicopter.getState().stopEngine();
            helicopter.crash();
        }
    }

    public String toString() {
        return "Starting";
    }
}

class HelicopterReadyState extends HelicopterState {
    public HelicopterReadyState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void startEngine() {
        //do nothing - engine is already running
    }

    @Override
    void stopEngine() {
        if (Math.abs(helicopter.getSpeed()) >= 0.1) return;

        helicopter.setState(helicopter.getStoppingState());
        helicopter.getState().stopEngine();
    }

    @Override
    void speedUp() {
        helicopter.accelerate();
    }

    @Override
    void speedDown() {
        helicopter.decelerate();
    }

    @Override
    void turnLeft() {
        helicopter.setHeading(helicopter.getHeading() - 15);
    }

    @Override
    void turnRight() {
        helicopter.setHeading(helicopter.getHeading() + 15);
    }

    @Override
    void seedCloud(Cloud cloud) {

    }

    @Override
    void consumeFuel(double rate) {
        double fuel = helicopter.getFuel();
        double speed = helicopter.getSpeed();
        helicopter.setFuel((int) (fuel - Math.abs(speed) * rate - (3 * rate)));

        if (helicopter.getFuel() <= 0) {
            helicopter.setState(helicopter.getStoppingState());
            helicopter.getState().stopEngine();
            helicopter.crash();
        }
    }

    public String toString() {
        return "Flying";
    }
}

class HelicopterStoppingState extends HelicopterState {
    public HelicopterStoppingState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void startEngine() {
        helicopter.setState(helicopter.getStartingState());
    }

    @Override
    void stopEngine() {
        helicopter.getBlade().setOnStoppedRotating(() ->
                helicopter.setState(helicopter.getOffState())
        );
        helicopter.getBlade().stopSpinning();
    }

    @Override
    void speedUp() { /* Do nothing - helicopter is stopping */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is stopping */ }

    @Override
    void turnLeft() { /* Do nothing - helicopter is stopping */ }


    @Override
    void turnRight() { /* Do nothing - helicopter is stopping */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is stopping */ }

    @Override
    void consumeFuel(double rate) {
        /* Do nothing - helicopter is stopping */
    }

    public String toString() {
        return "Stopping";
    }
}


class HeloBody extends Rectangle {
    public HeloBody() {
        Image bodyImage = new Image("/Assets/HelicopterBody.png");
        setFill(new ImagePattern(bodyImage));

        // scale the image down to 14% of its original size
        setWidth(bodyImage.getWidth() * 0.18);
        setHeight(bodyImage.getHeight() * 0.18);
        setScaleY(-1);
        setTranslateX(-15);
        setTranslateY(-65);
    }
}


class HeloBlade extends Circle {
    public static final double MAX_ROTATIONAL_SPEED = 1000;
    public static final double INITIAL_ROTATION = 45;
    private BladeState currState;
    private double rotationalSpeed;
    private Runnable onMaxRotationalSpeed;
    private Runnable onStopRotating;

    public HeloBlade() {
        super(40);

        setFill(new ImagePattern(new Image("/Assets/blades.png")));

        currState = BladeState.STOPPED;

        setScaleY(-1);
        setRotate(INITIAL_ROTATION);


        AnimationTimer loop = new AnimationTimer() {
            double old = 0;
            double elapsed = 0;

            @Override
            public void handle(long now) {
                if (old == 0) {
                    old = now;
                    return;
                }
                double frameTime = (now - old) / 1e9;
                old = now;
                elapsed += frameTime;

                setRotate(getRotate() - rotationalSpeed * frameTime);

                if (elapsed < 0.5) return;
                elapsed = 0;

                if (currState == BladeState.INCREASING_SPEED) {
                    rotationalSpeed += 100;
                    if (rotationalSpeed >= MAX_ROTATIONAL_SPEED) {
                        rotationalSpeed = MAX_ROTATIONAL_SPEED;

                        if (onMaxRotationalSpeed != null)
                            onMaxRotationalSpeed.run();

                        currState = BladeState.AT_MAX_SPEED;
                    }
                } else if (currState == BladeState.DECREASING_SPEED) {
                    rotationalSpeed -= 100;
                    if (rotationalSpeed <= 0) {
                        rotationalSpeed = 0;
                        if (onStopRotating != null)
                            onStopRotating.run();
                        currState = BladeState.STOPPED;
                    }
                }
            }
        };

        loop.start();
    }

    public void startSpinning() {
        if (currState == BladeState.AT_MAX_SPEED) return;
        currState = BladeState.INCREASING_SPEED;
    }

    /**
     * Only runs when the blade comes to a stop from spinning
     */
    public void setOnStoppedRotating(Runnable action) {
        this.onStopRotating = action;
    }

    public void setOnMaxRotationalSpeed(Runnable action) {
        this.onMaxRotationalSpeed = action;
    }

    public void stopSpinning() {
        if (currState == BladeState.STOPPED) return;
        currState = BladeState.DECREASING_SPEED;
    }
}

class GameText extends Text {
    public GameText() {
        setScaleY(-1);
    }
}

class Vector {
    private final double x;
    private final double y;
    private final double magnitude;
    private final double angle;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;

        magnitude = Math.sqrt(x * x + y * y);
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
        return String.format(
                "Vector: (x: %.2f, y: %.2f, angle: %.2f, mag: %.2f)",
                x, y, angle, magnitude
        );
    }

}