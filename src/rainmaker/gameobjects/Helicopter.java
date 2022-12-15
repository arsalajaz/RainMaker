package rainmaker.gameobjects;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import rainmaker.Updatable;
import rainmaker.Vector;

import java.io.File;

public class Helicopter extends GameObject implements Updatable {
    private static final Media ENGINE_SOUND_MEDIA = new Media(
            new File("src/resources/helicopter_audio.wav").toURI().toString());
    public static final MediaPlayer ENGINE_SOUND =
            new MediaPlayer(ENGINE_SOUND_MEDIA);
    public static final double MAX_SPEED = 10;
    public static final double MIN_SPEED = -2;
    public static final double ACCELERATION = 0.1;
    public final HelicopterState OFF_STATE = new HelicopterOffState(this);
    public final HelicopterState STARTING_STATE =
            new HelicopterStartingState(this);
    public final HelicopterState READY_STATE = new HelicopterReadyState(this);
    public final HelicopterState STOPPING_STATE =
            new HelicopterStoppingState(this);

    private HelicopterState currState;
    private final GameText fuelText;
    private final GameText stateText;
    private final HeloBody heloBody;
    private final HeloBlade heloBlade;
    private double heading = 0;
    private double speed = 0;
    private Vector position;
    private double fuel;
    private Runnable onCrashAction;
    private Helipad helipad;
    private Runnable onLandedAction;
    private Runnable onFlyingAction;

    public Helicopter(Vector initialPosition, int initialFuel) {
        fuel = initialFuel;

        heloBody = new HeloBody();
        heloBlade = new HeloBlade();
        fuelText = new GameText();
        stateText = new GameText();

        fuelText.setFill(Color.RED);
        stateText.setFill(Color.RED);

        ENGINE_SOUND.setCycleCount(MediaPlayer.INDEFINITE);
        ENGINE_SOUND.setVolume(0);
        ENGINE_SOUND.play();

        getChildren().addAll(heloBody, heloBlade, fuelText, stateText);

        position = initialPosition;

        currState = OFF_STATE;

        heloBlade.setOnMaxRotationalSpeed(() -> {
            setState(READY_STATE);
            if(onFlyingAction != null) {
                onFlyingAction.run();
            }
        });
        heloBlade.setOnStoppedRotating(() -> {
            setState(OFF_STATE);
            if(onLandedAction != null) {
                onLandedAction.run();
            }
        });

        shapes.add(heloBody);
        shapes.add(heloBlade);
    }

    public void startSpinningBlades() {
        heloBlade.startSpinning();
    }

    public void stopSpinningBlades() {
        heloBlade.stopSpinning();
    }

    public void toggleIgnition() {
        currState.toggleIgnition();
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
        double angle = Math.toRadians(getCartesianAngle());
        Vector velocity = new Vector(speed, angle, true)
                .multiply(frameTime * 30);

        position = position.add(velocity);
    }

    @Override
    public void update(double frameTime) {
        calculateNewPosition(frameTime);
        move();
        updateLabels();

        currState.consumeFuel(frameTime);
        currState.playSound();
    }

    private void updateLabels() {
        fuelText.setText("F: " + (int) fuel);
        fuelText.setTranslateX(-fuelText.getLayoutBounds().getWidth() / 2);
        fuelText.setTranslateY(-30);

        stateText.setText(currState.toString());
        stateText.setTranslateX(-stateText.getLayoutBounds().getWidth() / 2);
        stateText.setTranslateY(-30 - fuelText.getLayoutBounds().getHeight());
    }

    public void setOnFlyingAction(Runnable onFlyingAction) {
        this.onFlyingAction = onFlyingAction;
    }

    public Runnable getOnFlyingAction() {
        return onLandedAction;
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

    public void setOnCrash(Runnable action) {
        this.onCrashAction = action;
    }

    public void setState(HelicopterState currState) {
        this.currState = currState;
    }

    public double getFuel() {
        return fuel;
    }

    public void setFuel(double fuel) {
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
        return OFF_STATE;
    }

    public HelicopterState getStartingState() {
        return STARTING_STATE;
    }

    public HelicopterState getReadyState() {
        return READY_STATE;
    }

    public HelicopterState getStoppingState() {
        return STOPPING_STATE;
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

    public void setSpeed(double speed) {
        if (speed > MAX_SPEED) return;
        if (speed < MIN_SPEED) return;
        this.speed = speed;
    }
}

abstract class HelicopterState {
    protected Helicopter helicopter;

    public HelicopterState(Helicopter helicopter) {
        this.helicopter = helicopter;
    }

    abstract void speedUp();

    abstract void speedDown();

    abstract void toggleIgnition();

    abstract void turnLeft();

    abstract void turnRight();

    abstract void seedCloud(Cloud cloud);

    abstract void consumeFuel(double rate);

    abstract void playSound();
}

class HelicopterOffState extends HelicopterState {
    public HelicopterOffState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void speedUp() { /* Do nothing - helicopter is off */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is off */ }

    @Override
    void toggleIgnition() {
        helicopter.startSpinningBlades();
        helicopter.setState(helicopter.getStartingState());
    }

    @Override
    void turnLeft() { /* Do nothing - helicopter is off */ }

    @Override
    void turnRight() { /* Do nothing - helicopter is off */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is off */ }

    @Override
    void consumeFuel(double rate) { /* Do nothing - helicopter is off */ }

    @Override
    void playSound() { /* Do nothing - helicopter is off */}

    public String toString() {
        return "Off";
    }

}

class HelicopterStartingState extends HelicopterState {
    public HelicopterStartingState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void speedUp() { /* Do nothing - helicopter is starting */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is starting */}

    @Override
    void toggleIgnition() {
        helicopter.stopSpinningBlades();
        helicopter.setState(helicopter.getStoppingState());
    }

    @Override
    void turnLeft() { /* Do nothing - helicopter is starting */}

    @Override
    void turnRight() { /* Do nothing - helicopter is starting */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is starting */}

    @Override
    void consumeFuel(double rate) {
        double fuel = helicopter.getFuel();
        helicopter.setFuel((fuel - 10 * rate));

        if (helicopter.getFuel() <= 0) {
            helicopter.stopSpinningBlades();
            helicopter.setState(helicopter.getStoppingState());
            helicopter.crash();
        }

    }

    @Override
    void playSound() {
        Helicopter.ENGINE_SOUND.setVolume(
                helicopter.getBlade().getCurrentSpeed() /
                helicopter.getBlade().MAX_ROTATIONAL_SPEED
        );
    }

    public String toString() {
        return "Starting";
    }
}

class HelicopterStoppingState extends HelicopterState {
    public HelicopterStoppingState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void speedUp() { /* Do nothing - helicopter is stopping */ }

    @Override
    void speedDown() { /* Do nothing - helicopter is stopping */ }

    @Override
    void toggleIgnition() {
        helicopter.startSpinningBlades();
        helicopter.setState(helicopter.getStartingState());
    }

    @Override
    void turnLeft() { /* Do nothing - helicopter is stopping */ }


    @Override
    void turnRight() { /* Do nothing - helicopter is stopping */ }

    @Override
    void seedCloud(Cloud cloud) { /* Do nothing - helicopter is stopping */ }

    @Override
    void consumeFuel(double rate) {/* Do nothing - helicopter is stopping */}

    /**
     * Decreases volume by the percentage of blade rotation speed to max
     * speed on each call. Should be called on each frame.
     */
    @Override
    void playSound() {
        Helicopter.ENGINE_SOUND.setVolume(
                helicopter.getBlade().getCurrentSpeed() /
                helicopter.getBlade().MAX_ROTATIONAL_SPEED
        );
    }

    public String toString() {
        return "Stopping";
    }
}

class HelicopterReadyState extends HelicopterState {
    public HelicopterReadyState(Helicopter helicopter) {
        super(helicopter);
    }

    @Override
    void speedUp() {
        double speed = helicopter.getSpeed();
        if (speed >= Helicopter.MAX_SPEED) return;
        helicopter.setSpeed(Vector.round(speed + Helicopter.ACCELERATION, 1));
    }

    @Override
    void speedDown() {
        double speed = helicopter.getSpeed();
        if (speed <= Helicopter.MIN_SPEED) return;
        helicopter.setSpeed(Vector.round(speed - Helicopter.ACCELERATION, 1));
    }

    @Override
    void toggleIgnition() {
        if (Math.abs(helicopter.getSpeed()) >= 0.1) return;
        if (!helicopter.hooveringOverHelipad()) return;

        helicopter.stopSpinningBlades();
        helicopter.setState(helicopter.getStoppingState());
    }

    @Override
    void turnLeft() {
        helicopter.setHeading(helicopter.getHeading() - 1);
    }

    @Override
    void turnRight() {
        helicopter.setHeading(helicopter.getHeading() + 1);
    }

    @Override
    void seedCloud(Cloud cloud) {
        cloud.saturate();
    }

    @Override
    void consumeFuel(double rate) {
        double fuel = helicopter.getFuel();
        double speed = helicopter.getSpeed();
        double speedConsumption = 5 * Math.abs(speed) * rate;
        double hoverConsumption = 20 * rate;
        helicopter.setFuel((fuel - speedConsumption - hoverConsumption));

        if (helicopter.getFuel() <= 0) {
            helicopter.stopSpinningBlades();
            helicopter.setState(helicopter.getStoppingState());
            helicopter.crash();
        }
    }

    @Override
    void playSound() {}

    public String toString() {
        return "Flying";
    }
}