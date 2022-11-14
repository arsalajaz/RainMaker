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
import javafx.scene.shape.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.HashSet;

public class GameApp extends Application {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 800;
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
    public static void main(String args[]) { launch(args);}
}

class Game extends Pane {
    HashSet<KeyCode> keysDown = new HashSet<>();
    private static final Vector COPTER_INITIAL_POS =
            new Vector(GameApp.GAME_WIDTH/2, 100);

    private static final Point2D PAD_INITIAL_POSITION =
            new Point2D(GameApp.GAME_WIDTH/2, 100);

    private static final double COPTER_RADIUS = 15;
    private static final double PAD_RADIUS = GameApp.GAME_WIDTH/10;
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

                if(/*helicopter.interest(cloud) && */ isKeyDown(KeyCode.SPACE)
                && cloudSeedingTime >= 0.1) {
                    helicopter.seedCloud(cloud);
                    cloudSeedingTime = 0;
                }

                if(cloud.isRaining() && rainRate >= 0.2) {
                    cloud.rain();
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

        helicopter = new Helicopter(5000, COPTER_INITIAL_POS, 25000);
        helicopter.setOnCrash(this::onCopterCrash);

        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);

        double randCloudRadius = rand(30,60);
        cloud = new Cloud(randPoint(randCloudRadius,randCloudRadius), randCloudRadius);

        double randPondArea = rand(1500,2500);
        double pondRadius = Pond.getRadius(randPondArea);
        pond = new Pond(randPoint(pondRadius,pondRadius), randPondArea);

        getChildren().add(new ImageBackground(GameApp.GAME_WIDTH,
                GameApp.GAME_HEIGHT));
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
        x = rand(width, GameApp.GAME_WIDTH-width);
        y = rand(2*GameApp.GAME_HEIGHT/3, GameApp.GAME_HEIGHT) - height;
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
        if(event.getCode() == KeyCode.I) {
            if(helicopter.getState() == helicopter.getOffState())
                helicopter.startEngine(helipad);
            else
                helicopter.stopEngine();
        }
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
        this.getTransforms().addAll(myTranslation,myRotation,myScale);
    }

    public void rotate(double degrees) {
        myRotation.setAngle(degrees);
        myRotation.setPivotX(getLayoutBounds().getWidth()/2);
        myRotation.setPivotY(getLayoutBounds().getWidth()/2);
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
        pondShape.setRadius(getRadius());

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

    public void rain() {
        saturation--;
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
    private HelicopterState currState;

    private HelicopterState offState;
    private HelicopterState startingState;
    private HelicopterState readyState;
    private HelicopterState stoppingState;

    private double heading = 0;
    private double speed = 0;
    private Vector position;
    private boolean ignition = false;
    private boolean landed = true;

    private int water;
    private int fuel;
    private Runnable onCrashAction;
    private GameText fuelLabel;

    private GameText stateLabel;
    private HeloBody heloBody;
    private HeloBlade heloBlade;
    public Helicopter(int initialWater, Vector initialPosition, int initialFuel)
    {
        water = initialWater;
        fuel = initialFuel;

        heloBody = new HeloBody();
        heloBlade = new HeloBlade();

        fuelLabel = new GameText();
        stateLabel = new GameText();

        fuelLabel.setFill(Color.BLUE);
        stateLabel.setFill(Color.BLUE);

        getChildren().addAll(heloBody, heloBlade, fuelLabel, stateLabel);

        position = initialPosition;

        offState = new HelicopterOffState(this);
        startingState = new HelicopterStartingState(this);
        readyState = new HelicopterReadyState(this);
        stoppingState = new HelicopterStoppingState(this);

        currState = offState;
    }

    public void startEngine(Helipad helipad) {
//        if(Math.abs(speed) < 0.1 && ignition) {
//            speed = 0;
//            ignition = false;
//            landed = true;
//            heloBlade.stopSpinning();
//        } else {
//            ignition = true;
//            landed = false;
//            heloBlade.startSpinning();
//        }
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
        return degrees * (Math.PI/180);
    }
    private double getCartesianAngle() {
        return (450-heading)%360;
    }

    private void move() {
        rotate(getCartesianAngle() - 90);
        translate(position.getX(),position.getY());
    }

    private void calculateNewPosition(double frameTime) {
        double angle = convertDegreesToRadians(getCartesianAngle());
        Vector velocity = new Vector(speed, angle, true)
                .multiply(frameTime*30);

        position = position.add(velocity);
    }

    @Override
    public void update(double frameTime) {
        calculateNewPosition(frameTime);
        move();
        updateLabels();
        updateBoundingRect();

        fuel -= Math.abs((speed)*frameTime*30);

        if(fuel <= 0 && ignition) {
            fuel = 0;
            ignition = false;
            if(onCrashAction != null) onCrashAction.run();
        }


    }

    private void updateLabels() {
         double halfGroupWidth = getLayoutBounds().getWidth()/2;

         double halfFuelLabelWidth = fuelLabel.getLayoutBounds()
                .getWidth()/2;

         double halfStateLabelWidth = stateLabel.getLayoutBounds()
                .getWidth()/2;

         double fuelLabelHeight = fuelLabel.getLayoutBounds().getHeight();

        fuelLabel.setText("F: " + fuel);
        fuelLabel.setTranslateX(halfGroupWidth - halfFuelLabelWidth);

        stateLabel.setText(currState.toString());
        stateLabel.setTranslateX(halfGroupWidth - halfStateLabelWidth);
        stateLabel.setTranslateY(-fuelLabelHeight);
    }

    public void seedCloud(Cloud cloud) {
        cloud.saturate();
        water--;

    }

    void setOnCrash(Runnable action) {
        this.onCrashAction = action;
    }

    public void setState(HelicopterState currState) {
        this.currState = currState;
    }

    public HelicopterState getState() {
        return currState;
    }


    @Override
    Shape getShape() {
        return null;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
    public double getSpeed() {
        return speed;
    }
    public void setHeading(double heading) {
        this.heading = heading;
    }
    public double getHeading() {
        return heading;
    }

    public HelicopterState getOffState() {return offState; }
    public HelicopterState getStartingState() { return startingState;}
    public HelicopterState getReadyState() { return readyState; }
    public HelicopterState getStoppingState() { return stoppingState; }

    public HeloBlade getBlade() {
    	return heloBlade;
    }
}

class HeloBody extends ImageView {
    public HeloBody() {
        super(new Image("/Assets/HeloBody.png"));
        setScaleY(-1);
        setFitWidth(120);
        setFitHeight(120);
    }
}

class ImageBackground extends Pane {
    private ImageView background;
    public ImageBackground(double width, double height) {
        background = new ImageView(new Image("/Assets/DesertBG.jpeg"));
        background.setFitWidth(width);
        background.setFitHeight(height);
        getChildren().add(background);
    }

}
enum BladeState {
    STOPPED, INCREASING_SPEED, DECREASING_SPEED, AT_MAX_SPEED
}
class HeloBlade extends ImageView {
    private BladeState currState;
    public static final double MAX_ROTATIONAL_SPEED = 1000;
    private double rotationalSpeed;
    private Runnable onMaxRotationalSpeed;
    private Runnable onStopRotating;

    public HeloBlade() {
        super(new Image("/Assets/blades.png"));
        setScaleY(-1);
        setRotate(45);
        setTranslateX(20);
        setTranslateY(20);

        setFitHeight(80);
        setFitWidth(80);

        currState = BladeState.STOPPED;
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

                if(currState == BladeState.INCREASING_SPEED) {
                    rotationalSpeed += 100;
                    if(rotationalSpeed >= MAX_ROTATIONAL_SPEED) {
                        rotationalSpeed = MAX_ROTATIONAL_SPEED;

                        if(onMaxRotationalSpeed != null)
                            onMaxRotationalSpeed.run();

                        currState = BladeState.AT_MAX_SPEED;
                    }
                } else if(currState == BladeState.DECREASING_SPEED) {
                    rotationalSpeed -= 100;
                    if(rotationalSpeed <= 0) {
                        rotationalSpeed = 0;
                        if(onStopRotating != null) onStopRotating.run();
                        currState = BladeState.STOPPED;
                    }
                }
            }
        };

        loop.start();
    }

    public void startSpinning() {
        if(currState == BladeState.AT_MAX_SPEED) return;
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
        if(currState == BladeState.STOPPED) return;
        currState = BladeState.DECREASING_SPEED;
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
        if(Math.abs(helicopter.getSpeed()) >= 0.1) return;

        helicopter.setState(helicopter.getStoppingState());
        helicopter.getState().stopEngine();
    }

    @Override
    void speedUp() {
        if(helicopter.getSpeed() >= 10) return;
        helicopter.setSpeed(helicopter.getSpeed() + 0.1);
    }

    @Override
    void speedDown() {
        if(helicopter.getSpeed() <= -2) return;
        helicopter.setSpeed(helicopter.getSpeed() - 0.1);
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

    public String toString() {
        return "Stopping";
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
        return String.format(
                "Vector: (x: %.2f, y: %.2f, angle: %.2f, mag: %.2f)",
                x, y, angle, magnitude
        );
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