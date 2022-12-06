package Temp;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.*;
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

class Game extends Pane implements CloudsListener {
    private final Bounds gameBounds =
            new BoundingBox(0, 0, GameApp.GAME_WIDTH, GameApp.GAME_HEIGHT);
    private final Vector COPTER_INITIAL_POS =
            new Vector(gameBounds.getWidth() / 2, 100);
    private final Point2D PAD_INITIAL_POSITION =
            new Point2D(gameBounds.getHeight() / 2, 100);
    private static final double PAD_RADIUS = GameApp.GAME_WIDTH / 14;
    private final Runnable stageClose;
    private final AnimationTimer animationTimer;
    HashSet<KeyCode> keysDown = new HashSet<>();
    private Helicopter helicopter;
    private Helipad helipad;
    private Clouds clouds;
    private Ponds ponds;
    private BoundingBoxPane boundingBoxes;
    private DistanceLinesPane distanceLines;

    public Game(Runnable stageClose) {
        setScaleY(-1);
        this.stageClose = stageClose;
        clouds = new Clouds();
        clouds.addListener(this);
        init();
        animationTimer = new AnimationTimer() {
            double old = -1;

            @Override
            public void handle(long now) {
                if (old < 0) { old = now; return; }
                double FrameTime = (now - old) / 1e9;
                old = now;

                update(FrameTime);

                for (Cloud cloud : clouds) {
                    if(!cloud.isRaining()) continue;
                    for (Pond pond : ponds) {
                        int distance = (int)DistanceLine.getDistance(cloud, pond);
                        double pondDiameter = pond.getRadius() * 2;
                        double maxDistance = pondDiameter * 4;
                        if (distance >= maxDistance) continue;
                        double saturationProp =
                                (double)cloud.getSaturation()/100;
                        double distanceProp = 1 - (distance / maxDistance);
                        pond.addWater(distanceProp * saturationProp * FrameTime * 2);
                    }
                }
            }
        };

        animationTimer.start();
    }


    private void update(double FrameTime) {
        helicopter.update(FrameTime);
        ponds.update(FrameTime);
    }

    void handleCopterCrash() {
        animationTimer.stop();
        String msg = "Temp.Game Over! Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                stageClose.run();
            }
        });
        alert.show();
    }

    void handleCopterLanded() {
        if(ponds.getAvgWaterLevel() < 80) return;

        animationTimer.stop();

        double score = (ponds.getAvgWaterLevel()/100) * (double)helicopter.getFuel();
        String msg = "You Win! Your score is " + (int)score + ". " +
                "Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                stageClose.run();
            }
        });
        alert.show();
    }

    private void init() {
        getChildren().clear();

        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);

        helicopter = new Helicopter(5000, COPTER_INITIAL_POS, 25000);
        helicopter.setOnCrash(this::handleCopterCrash);
        helicopter.setOnLandedAction(this::handleCopterLanded);
        helicopter.setLandingLocation(helipad);

        // A pond won't spawn on the helipad, can be used to add more obstacles
        ArrayList<Bounds> pondObstacles = new ArrayList<>();
        pondObstacles.add(helipad.getBoundsInParent());
        ponds = new Ponds(gameBounds, pondObstacles);

        ImageBackground background = new ImageBackground(GameApp.GAME_WIDTH,
                GameApp.GAME_HEIGHT);

        boundingBoxes = new BoundingBoxPane();
        distanceLines = new DistanceLinesPane();

        boundingBoxes.addAll(helicopter, helipad);
        for (Pond pond : ponds) { boundingBoxes.add(pond); }
        for (Cloud cloud : clouds) {
            boundingBoxes.add(cloud);
            for(Pond pond : ponds) {
                distanceLines.add(cloud, pond);
            }
        }


        getChildren().addAll(background, helipad, ponds, clouds, helicopter);
        getChildren().addAll(boundingBoxes, distanceLines);
    }
    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());

        if (isKeyDown(KeyCode.UP)) helicopter.speedUp();
        if (isKeyDown(KeyCode.DOWN)) helicopter.speedDown();
        if (isKeyDown(KeyCode.RIGHT)) helicopter.turnRight();
        if (isKeyDown(KeyCode.LEFT)) helicopter.turnLeft();
        if (isKeyDown(KeyCode.SPACE)) {
            for (Cloud cloud : clouds) {
                if (helicopter.interest(cloud)) {
                    helicopter.seedCloud(cloud);
                }
            }
        }
        if (event.getCode() == KeyCode.I) {
            if (helicopter.getState() == helicopter.getOffState())
                helicopter.takeOff();
            else
                helicopter.land();
        }
        if (event.getCode() == KeyCode.B) boundingBoxes.toggleVisibility();
        if (event.getCode() == KeyCode.D) distanceLines.toggleVisibility();
        if (event.getCode() == KeyCode.R) init();
        if (event.getCode() == KeyCode.C) System.gc();

    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());
    }

    private boolean isKeyDown(KeyCode k) { return keysDown.contains(k); }

    @Override
    public void onCloudDestroyed(Cloud cloud) {
        boundingBoxes.remove(cloud);
        distanceLines.removeIfInvolves(cloud);
    }

    @Override
    public void onCloudSpawned(Cloud cloud) {
        boundingBoxes.add(cloud);
        for(Pond pond : ponds) {
            distanceLines.add(cloud, pond);
        }
    }
}

abstract class GameObject extends Group {
    protected Translate myTranslation;
    protected Rotate myRotation;
    protected Scale myScale;

    public GameObject() {
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
    private double waterLevel = 0;

    public Pond(Point2D initialPosition, int initialWater, double initialArea) {
        pondArea = initialArea;
        pondShape.setRadius(getRadius());
        pondShape.setFill(Color.BLUE);

        this.waterLevel = initialWater;
        waterLevelText.setText(String.valueOf(waterLevel));
        waterLevelText.setFill(Color.WHITE);

        getChildren().addAll(pondShape, waterLevelText);
        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());
    }

    public static Pond generatePond() {
        int waterLevel = RandomGenerator.getRandomInt(10,30);
        int initialArea = waterLevel * 100;
        double radius = getRadius(initialArea);
        double x = RandomGenerator.getRandomDouble(radius,
                GameApp.GAME_WIDTH-radius);
        double y = RandomGenerator.getRandomDouble(radius,
                GameApp.GAME_WIDTH-radius);
        return new Pond(new Point2D(x, y), waterLevel, initialArea);
    }

    public static double getRadius(double area) {
        return Math.sqrt(area / Math.PI);
    }

    public double getRadius() {
        return Math.sqrt(pondArea / Math.PI);
    }

    public void addWater(double water) {
        pondArea += 100*water;
        waterLevel += water;
    }

    public double getCurrentWaterLevel() {
        return waterLevel;
    }

    @Override
    Shape getShape() {
        return pondShape;
    }

    @Override
    public void update(double FrameTime) {
        pondShape.setRadius(getRadius());

        waterLevelText.setText(String.valueOf((int)waterLevel));
        waterLevelText.setTranslateX(-waterLevelText.getLayoutBounds()
                .getWidth() / 2);
        waterLevelText.setTranslateY(waterLevelText.getLayoutBounds()
                .getHeight() / 4);

    }
}

class Cloud extends GameObject implements Updatable {
    private final double MIN_RADIUS = 40;
    private final double MAX_RADIUS = 70;
    private static final double WIND_SPEED = 0.4;
    private static final double WIND_DIRECTION = 0;
    private CloudState state = CloudState.SPAWNED;
    private Vector position;
    private Vector velocity;
    private final Circle cloud;
    private final Shape cloudShape;
    private final GameText percentText;
    private int saturation = 0;
    private double speedOffset = RandomGenerator.getRandomDouble(40, 70);

    public Cloud(Point2D initPosition, Point2D shapeSize) {
        cloud = new Circle(20, Color.rgb(255, 255, 255));
        cloudShape =
                new BezierOval(shapeSize.getX(), shapeSize.getY()).getShape();
        cloudShape.setFill(Color.rgb(255, 255, 255));
        translate(initPosition.getX(), initPosition.getY());

        percentText = new GameText();
        percentText.setFill(Color.BLUE);
        getChildren().addAll(cloudShape, percentText);
        this.position = new Vector(initPosition.getX(), initPosition.getY());
    }

    public boolean isRaining() {
        return saturation >= 30;
    }

    public int getSaturation() {
        return saturation;
    }

    public CloudState getState() {
        return state;
    }

    public void rain() {
        if(saturation <= 0) return;
        saturation--;
    }

    public void saturate() {
        if (saturation >= 100) return;
        saturation++;
    }

    @Override
    Shape getShape() {
        return cloudShape;
    }

    @Override
    public void update(double FrameTime) {
        if(isDead()) return;

        velocity = new Vector(WIND_SPEED * FrameTime * speedOffset,
                Math.toRadians(WIND_DIRECTION), true);
        position = position.add(velocity);

        if (state != CloudState.ALIVE && isWithinBounds()) {
            state = CloudState.ALIVE;
        }
        else if (shouldDie()) {
            state = CloudState.DEAD;
        }


        translate(position.getX(), position.getY());

        cloudShape.setFill(Color.rgb(255 - saturation, 255 - saturation,
                255 - saturation));
        percentText.setText(saturation + "%");
        percentText.setTranslateX(-percentText.getLayoutBounds().getWidth() / 2);
        percentText.setTranslateY(percentText.getLayoutBounds().getHeight() / 2);

    }

    private boolean isWithinBounds() {
        double cloudWidth = cloudShape.getLayoutBounds().getWidth();
        double cloudHeight = cloudShape.getLayoutBounds().getHeight();
        return position.getX() > cloudWidth / 2 &&
                position.getX() < GameApp.GAME_WIDTH - cloudWidth / 2 &&
                position.getY() > cloudHeight / 2 &&
                position.getY() < GameApp.GAME_HEIGHT - cloudHeight / 2;
    }

    private boolean shouldDie() {
        double cloudWidth = cloudShape.getLayoutBounds().getWidth();
        double cloudHeight = cloudShape.getLayoutBounds().getHeight();
        return position.getX() < -cloudWidth/2 && velocity.getX() < 0 ||
                position.getX() > GameApp.GAME_WIDTH + cloudWidth / 2 && velocity.getX() > 0 ||
                position.getY() < -cloudHeight / 2 && velocity.getY() < 0 ||
                position.getY() > GameApp.GAME_HEIGHT + cloudHeight / 2 && velocity.getY() > 0;
    }

    public static Cloud createRandomCloud(boolean onScreen) {
        double radiusX = RandomGenerator.getRandomDouble(50, 60);
        double radiusY = RandomGenerator.getRandomDouble(30, 40);
        double x = onScreen ? RandomGenerator.getRandomDouble(radiusX,
                800-radiusX) : -radiusX-10;
        double y = RandomGenerator.getRandomDouble(radiusY, 800-radiusY);
        Point2D position = new Point2D(x, y);
        return new Cloud(position, new Point2D(radiusX, radiusY));
    }

    public boolean isDead() {
        return state == CloudState.DEAD;
    }
}

class Helipad extends GameObject {
    private final double BORDER_OFFSET = 10;
    Circle pad;

    public Helipad(double radius, Point2D initialPosition) {
        pad = new Circle(radius);
        pad.setFill(Color.DARKGRAY);
        pad.setStroke(Color.GRAY);
        pad.setStrokeWidth(2);

        Bounds bounds = pad.getBoundsInParent();
        Rectangle border = new Rectangle(
                bounds.getMinX() - BORDER_OFFSET,
                bounds.getMinY() - BORDER_OFFSET,
                bounds.getWidth() + BORDER_OFFSET * 2,
                bounds.getHeight() + BORDER_OFFSET * 2
        );
        border.setStroke(Color.GRAY);
        border.setStrokeWidth(2);
        border.setFill(Color.DARKGRAY);

        getChildren().addAll(border, pad);

        translate(initialPosition.getX(), initialPosition.getY());
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
    private Helipad helipad;
    private Runnable onLandedAction;

    public Helicopter(int initialWater, Vector initialPosition, int initialFuel) {
        water = initialWater;
        fuel = initialFuel;

        heloBody = new HeloBody();
        heloBlade = new HeloBlade();

        fuelText = new GameText();
        stateText = new GameText();

        fuelText.setFill(Color.RED);
        stateText.setFill(Color.RED);

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

    public void takeOff() {
        currState.startEngine();
    }

    public void land() {
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

        // increase the size if the helicopter is started and not ready
        if (currState == startingState) {
            scale(getScaleX() + 0.01, getScaleY() + 0.01);
        }

        currState.consumeFuel(frameTime);
    }

    private void updateLabels() {
        fuelText.setText("F: " + fuel);
        fuelText.setTranslateX(-fuelText.getLayoutBounds().getWidth() / 2);
        fuelText.setTranslateY(-30);

        stateText.setText(currState.toString());
        stateText.setTranslateX(-stateText.getLayoutBounds().getWidth() / 2);
        stateText.setTranslateY(-30 - fuelText.getLayoutBounds().getHeight());
    }

    public void setOnLandedAction(Runnable action) {
        this.onLandedAction = action;
    }

    public Runnable getOnLandedAction() {
        return onLandedAction;
    }

    public void seedCloud(Cloud cloud) {
        currState.seedCloud(cloud);
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

    public void setLandingLocation(Helipad helipad) {
        this.helipad = helipad;
    }

    public boolean hooveringOverHelipad() {
        return helipad != null &&
                helipad.getBoundsInParent().contains(getBoundsInParent());
    }
}

/**
 * Temp.Game object pane that stores collection of one type of game objects and
 * updatable
 */

class GameObjectPane<T extends GameObject> extends Pane implements Iterable<T> {

    public void add(T object) {
        getChildren().addAll(object);
    }

    public void remove(T object) {
        getChildren().removeAll(object);
    }

    public void clear() {
        getChildren().clear();
    }

    //public List getObjects() { return getChildren(); }

    @Override
    public Iterator iterator() {
        return getChildren().iterator();
    }
}

interface CloudsListener {
    void onCloudDestroyed(Cloud cloud);
    void onCloudSpawned(Cloud cloud);
}

class Clouds extends GameObjectPane<Cloud> implements Updatable {
    private static final int MAX_CLOUDS = 5;
    private static final int MIN_CLOUDS = 2;
    private List<CloudsListener> listeners = new ArrayList<>();
    public Clouds() {
        AnimationTimer timer = new AnimationTimer() {
            double old = -1;
            @Override
            public void handle(long now) {
                if (old < 0) { old = now; return;}
                double frameTime = (now - old) / 1e9;
                old = now;
                update(frameTime);
            }
        };
        timer.start();
    }

    public void addListener(CloudsListener listener) {
        listeners.add(listener);
    }

    private void notifyCloudSpawned(Cloud cloud) {
        listeners.forEach(l -> l.onCloudSpawned(cloud));
    }

    private void notifyCloudDestroyed(Cloud cloud) {
        listeners.forEach(l -> l.onCloudDestroyed(cloud));
    }

    private double elapsed = 0;
    private double rainElapsed = 0;
    @Override
    public void update(double frameTime) {
        elapsed += frameTime;
        rainElapsed += frameTime;

        // add initial clouds
        if (getChildren().isEmpty()) {
            for (int i = 0; i < MAX_CLOUDS; i++) {
                Cloud cloud = Cloud.createRandomCloud(true);
                add(cloud);
                notifyCloudSpawned(cloud);
            }
            return;
        }
        //every second, cloud losses 1% saturation
        if (rainElapsed >= 0.5) {
            rainElapsed = 0;
            for (Cloud cloud : this) {
                cloud.rain();
            }
        }

        // Not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            Cloud cloud = (Cloud) getChildren().get(i);
            cloud.update(frameTime);

            if (!cloud.isDead()) continue;

            remove(cloud);
            notifyCloudDestroyed(cloud);
        }

        if(getChildren().size() >= MAX_CLOUDS) return;

        if (getChildren().size() <= MIN_CLOUDS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            notifyCloudSpawned(cloud);
        }

        if (elapsed < 5) return;
        elapsed = 0;

        if (RandomGenerator.flipCoin() == CoinSide.HEADS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            notifyCloudSpawned(cloud);
        }
    }
}

class Ponds extends GameObjectPane<Pond> implements Updatable {

    private double avgWaterLevel = 0;
    private static final int TOTAL_PONDS = 3;
    private final Bounds windowBounds;
    private final ArrayList<Bounds> obstacles;
    public Ponds(Bounds bounds, ArrayList<Bounds> obstacles) {
        this.windowBounds = bounds;
        this.obstacles = obstacles;
        while (getChildren().size() < TOTAL_PONDS) {
            Pond pond = Pond.generatePond();
            if (overlapsAnotherPond(pond) || overlapsObstacle(pond)
             || closeToAnotherPond(pond)) continue;
            add(pond);
        }
    }

    private boolean closeToAnotherPond(Pond pond) {
        for (Pond p : this) {
            if (distanceBetween(p, pond) < 200) return true;
        }
        return false;
    }

    private double distanceBetween(Pond pond1, Pond pond2) {
        double x1 =  pond1.getBoundsInParent().getCenterX();
        double y1 =  pond1.getBoundsInParent().getCenterY();

        double x2 =  pond2.getBoundsInParent().getCenterX();
        double y2 =  pond2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    private boolean overlapsObstacle(Pond pond) {
        for (Bounds obstacle : obstacles) {
            if (pond.getBoundsInParent().intersects(obstacle)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsAnotherPond(Pond pond) {
        for (Pond p : this) {
            if (p.interest(pond)) {
                return true;
            }
        }
        return false;
    }

    public static double convertAreaToRadius(double area) {
        return Math.sqrt(area / Math.PI);
    }

    private Point2D randomSpawnPoint(double radius) {
        return new Point2D(
                RandomGenerator.getRandomDouble(radius, windowBounds.getWidth() - radius),
                RandomGenerator.getRandomDouble(radius, windowBounds.getHeight() - radius)
        );
    }

    @Override
    public void update(double FrameTime) {
        // not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            ((Pond)getChildren().get(i)).update(FrameTime);
        }

        // update average water level
        double totalWaterLevel = 0;
        for (Pond pond : this) {
            totalWaterLevel += pond.getCurrentWaterLevel();
        }
        avgWaterLevel = totalWaterLevel / getChildren().size();
    }

    public double getAvgWaterLevel() {
        return avgWaterLevel;
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
        if (!helicopter.hooveringOverHelipad()) return;

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
        cloud.saturate();
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
        helicopter.getBlade().setOnStoppedRotating(() -> {
            helicopter.setState(helicopter.getOffState());
            helicopter.getOnLandedAction().run();
        });
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

/**
* Extends the Circle class so that the bounding box does not increase on
* rotation. Uses a simple enum state.
*/
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

    /**
     * Only runs when the blade reaches its maximum rotational speed
     */
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

class ObjectBoundingBox extends Rectangle {
    private GameObject object;
    public ObjectBoundingBox(GameObject object) {
        this.object = object;
        setFill(Color.TRANSPARENT);
        setStrokeWidth(1);
        setStroke(Color.YELLOW);

        update(object.getBoundsInParent());

        object.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
            update(newValue);
        });
    }

    private void update(Bounds bounds) {
        setX(bounds.getMinX());
        setY(bounds.getMinY());
        setWidth(bounds.getWidth());
        setHeight(bounds.getHeight());
    }

    public GameObject getObject() {
        return object;
    }
}

class BoundingBoxPane extends Pane {
    public BoundingBoxPane() {
        setVisible(false);
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public void add(GameObject obj) {
        getChildren().add(new ObjectBoundingBox(obj));
    }

    public void addAll(GameObject... objects) {
        for (GameObject obj : objects) {
            add(obj);
        }
    }

    public void remove(GameObject obj) {
        getChildren().removeIf(node -> {
            if (node instanceof ObjectBoundingBox) {
                ObjectBoundingBox box = (ObjectBoundingBox) node;
                return box.getObject() == obj;
            }
            return false;
        });
    }
}

class DistanceLine extends Group {
    private Line line;
    private GameObject object1;
    private GameObject object2;

    private GameText distanceText;

    public DistanceLine(GameObject obj1, GameObject obj2) {
        line = new Line();
        line.setStroke(Color.YELLOW);
        line.setStrokeWidth(1);

        object1 = obj1;
        object2 = obj2;
        distanceText = new GameText();
        distanceText.setFill(Color.YELLOW);

        update();
        object1.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
                    update();
        });
        object2.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
                    update();
        });

        getChildren().addAll(line, distanceText);
    }

    public boolean hasObjects(GameObject obj1, GameObject obj2) {
        return (object1 == obj1 && object2 == obj2) ||
                (object1 == obj2 && object2 == obj1);
    }

    public boolean hasObject(GameObject obj) {
        return object1 == obj || object2 == obj;
    }

    public double getDistance() {
        double x1 = object1.getBoundsInParent().getCenterX();
        double y1 = object1.getBoundsInParent().getCenterY();
        double x2 = object2.getBoundsInParent().getCenterX();
        double y2 = object2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double getDistance(GameObject obj1, GameObject obj2) {
        double x1 = obj1.getBoundsInParent().getCenterX();
        double y1 = obj1.getBoundsInParent().getCenterY();
        double x2 = obj2.getBoundsInParent().getCenterX();
        double y2 = obj2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Maybe needed if property binding not allowed
     */
    private void update() {
        double x1 = object1.getBoundsInParent().getCenterX();
        double y1 = object1.getBoundsInParent().getCenterY();
        double x2 = object2.getBoundsInParent().getCenterX();
        double y2 = object2.getBoundsInParent().getCenterY();

        line.setStartX(x1);
        line.setStartY(y1);
        line.setEndX(x2);
        line.setEndY(y2);

        double distance = getDistance();
        distanceText.setText(String.format("%.2f", distance));
        distanceText.setX((x1 + x2) / 2);
        distanceText.setY((y1 + y2) / 2);
    }
}

class DistanceLinesPane extends Pane {
    public DistanceLinesPane() {
        setVisible(false);
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public void add(GameObject obj1, GameObject obj2) {
        getChildren().add(new DistanceLine(obj1, obj2));
    }

    public void addAll(GameObject... objects) {
        for (int i = 0; i < objects.length; i++) {
            for (int j = i + 1; j < objects.length; j++) {
                add(objects[i], objects[j]);
            }
        }
    }

    public void removeIfInvolves(GameObject obj) {
        getChildren().removeIf(node -> {
            if (node instanceof DistanceLine) {
                DistanceLine line = (DistanceLine) node;
                return line.hasObject(obj);
            }
            return false;
        });
    }
}

enum CoinSide {
    HEADS, TAILS
}

class RandomGenerator {
    private static Random random = new Random();
    public static int getRandomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    public static double getRandomDouble(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }
    public static CoinSide flipCoin() {
        return random.nextBoolean() ? CoinSide.HEADS : CoinSide.TAILS;
    }

    public static Point2D generateRandomBetween(Point2D start, Point2D end) {
        return new Point2D(RandomGenerator.getRandomDouble(
                start.getX(), end.getX()),
                RandomGenerator.getRandomDouble(start.getY(), end.getY()));
    }

    public static Point2D getRandomPointAroundLine(Point2D start, Point2D end
            , double minDistance, double maxDistance, boolean aboveLine) {
        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        double distance = RandomGenerator.getRandomDouble(minDistance, maxDistance);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        if (aboveLine) {
            angle += Math.PI / 2;
        } else {
            angle -= Math.PI / 2;
        }

        double x = x1 + distance * Math.cos(angle);
        double y = y1 + distance * Math.sin(angle);

        return new Point2D(x, y);
    }
}

class BezierOval extends Group {
    Ellipse oval;
    ArrayList<Point2D> randPointsOnOval = new ArrayList<>();
    ArrayList<Integer> angles = new ArrayList<>();
    ArrayList<QuadCurve> quadCurves = new ArrayList<>();

    public BezierOval(double radiusX, double radiusY) {
        oval = new Ellipse(radiusX, radiusY);
        oval.setFill(Color.WHITE);
        createShape();
        showControlPoints();
    }

    private void createShape() {
        int angle = 0;
        int angleSum = 0;
        while(angle <= 360 ) {
            double x = oval.getRadiusX() * Math.sin(Math.toRadians(angle));
            double y = oval.getRadiusY() * Math.cos(Math.toRadians(angle));

            randPointsOnOval.add(new Point2D(x,y));
            angles.add(angle);
            angleSum += angle;
            angle += 72;

            if(randPointsOnOval.size() == 1) continue;

            Point2D curr = randPointsOnOval.get(randPointsOnOval.size()-1);
            Point2D prev = randPointsOnOval.get(randPointsOnOval.size()-2);

            int angle1 = angles.get(angles.size()-1);
            int angle2 = angles.get(angles.size()-2);

            QuadCurve curve = new QuadCurve();
            curve.setStartX(prev.getX());
            curve.setStartY(prev.getY());
            curve.setEndX(curr.getX());
            curve.setEndY(curr.getY());


//            double controlX =
//                    oval.getRadiusX() * Math.sin(Math.toRadians((angle1 + angle2) / 2));
//            double controlY =
//                    oval.getRadiusY() * Math.cos(Math.toRadians((angle1 + angle2) / 2));

            double controlX = (prev.getX() + curr.getX()) / 2;
            double controlY = (prev.getY() + curr.getY()) / 2;

            int randOffset = RandomGenerator.getRandomInt(10, 20);
            if(controlY > 0) {
                controlY += randOffset;
            } else {
                controlY -= randOffset;
            }
            if (controlX > 0) {
                controlX += randOffset;
            } else {
                controlX -= randOffset;
            }

            curve.setControlX(controlX);
            curve.setControlY(controlY);
            curve.setStroke(Color.BLACK);
            curve.setStrokeWidth(1);
            curve.setFill(Color.WHITE);

            quadCurves.add(curve);
        }
        getChildren().add(oval);
        getChildren().addAll(quadCurves);


//        getElements().add(new MoveTo(randPointsOnOval.get(0).getX(), randPointsOnOval.get(0).getY()));
//        for(QuadCurve curve : quadCurves) {
//            getElements().add(new QuadCurveTo(curve.getControlX(), curve.getControlY(), curve.getEndX(), curve.getEndY()));
//        }
//        getElements().add(new ClosePath());
//
//        setFill(Color.RED);
    }

    public Shape getShape() {
        //combine all quad curves into one path
        Path path = new Path();
        path.getElements().add(new MoveTo(randPointsOnOval.get(0).getX(), randPointsOnOval.get(0).getY()));
        for(QuadCurve curve : quadCurves) {
            path.getElements().add(new QuadCurveTo(curve.getControlX(), curve.getControlY(), curve.getEndX(), curve.getEndY()));
        }
        path.getElements().add(new ClosePath());
        path.setFill(Color.RED);
        return path;
    }

    private void showControlPoints() {
        for(QuadCurve curve : quadCurves) {
            Circle circle = new Circle(curve.getControlX(), curve.getControlY(), 2);
            circle.setFill(Color.RED);
            getChildren().add(circle);
        }
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
                "Temp.Vector: (x: %.2f, y: %.2f, angle: %.2f, mag: %.2f)",
                x, y, angle, magnitude
        );
    }

}