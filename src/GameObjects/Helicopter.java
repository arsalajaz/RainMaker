package GameObjects;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import Helper.Vector;

public class Helicopter extends GameObject implements Updatable {
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

enum BladeState {
    STOPPED, INCREASING_SPEED, DECREASING_SPEED, AT_MAX_SPEED
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

