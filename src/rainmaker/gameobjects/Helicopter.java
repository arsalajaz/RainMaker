package rainmaker.gameobjects;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import rainmaker.Updatable;
import rainmaker.Vector;

import java.io.File;

public class Helicopter extends GameObject implements Updatable {
    private static final Media ENGINE_SOUND = new Media(
            new File("src/resources/helicopter_audio.wav").toURI().toString());
    private static final MediaPlayer soundPlayer =
            new MediaPlayer(ENGINE_SOUND);
    private static final double MAX_SPEED = 10;
    private static final double MIN_SPEED = -2;
    private static final double ACCELERATION = 0.1;
    private final HelicopterState offState;
    private final HelicopterState startingState;
    private final HelicopterState readyState;
    private final HelicopterState stoppingState;
    private final GameText fuelText;
    private final GameText stateText;
    private final HeloBody heloBody;
    private final HeloBlade heloBlade;
    private HelicopterState currState;
    private double heading = 0;
    private double speed = 0;
    private Vector position;
    private int water;
    private double fuel;
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

        soundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        soundPlayer.setVolume(0);
        soundPlayer.play();

        getChildren().addAll(heloBody, heloBlade, fuelText, stateText);

        position = initialPosition;

        offState = new HelicopterOffState(this);
        startingState = new HelicopterStartingState(this);
        readyState = new HelicopterReadyState(this);
        stoppingState = new HelicopterStoppingState(this);

        currState = offState;
    }

    public static void resetSound() {
        soundPlayer.stop();
        soundPlayer.setVolume(0);
    }


    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public MediaPlayer getSoundPlayer() {
        return soundPlayer;
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

        currState.consumeFuel(frameTime);
        if (currState != readyState)
            currState.playSound(soundPlayer, frameTime);
    }

    private void updateLabels() {
        fuelText.setText("F: " + (int) fuel);
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

    public void setOnCrash(Runnable action) {
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

    abstract void playSound(MediaPlayer mediaPlayer, double frameTime);
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

    @Override
    void playSound(MediaPlayer mediaPlayer, double frameTime) {
        mediaPlayer.setVolume(0);
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
        helicopter.getBlade().setOnMaxRotationalSpeed(() -> {
            helicopter.setState(helicopter.getReadyState());
            System.out.println(helicopter.getFuel());
        });
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
        helicopter.setFuel((fuel - 10 * rate));

        if (helicopter.getFuel() <= 0) {
            helicopter.setState(helicopter.getStoppingState());
            helicopter.getState().stopEngine();
            helicopter.crash();
        }

    }

    @Override
    void playSound(MediaPlayer mediaPlayer, double frameTime) {
        // insrease volume by the percentage of blade roatation speed to max speed
        mediaPlayer.setVolume(helicopter.getBlade().getCurrentSpeed() / helicopter.getBlade().MAX_ROTATIONAL_SPEED);
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
    void consumeFuel(double rate) {/* Do nothing - helicopter is stopping */}

    /**
     * Decreases volume by the percentage of blade rotation speed to max
     * speed on each call. Should be called on each frame.
     */
    @Override
    void playSound(MediaPlayer mediaPlayer, double frameTime) {
        mediaPlayer.setVolume(helicopter.getBlade().getCurrentSpeed() / helicopter.getBlade().MAX_ROTATIONAL_SPEED);
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
        double speedConsumption = 5 * Math.abs(speed) * rate;
        double hoverConsumption = 20 * rate;
        helicopter.setFuel((fuel - speedConsumption - hoverConsumption));

        if (helicopter.getFuel() <= 0) {
            helicopter.setState(helicopter.getStoppingState());
            helicopter.getState().stopEngine();
            helicopter.crash();
        }
    }

    @Override
    void playSound(MediaPlayer mediaPlayer, double frameTime) {
        mediaPlayer.setVolume(1.0);
    }

    public String toString() {
        return "Flying";
    }
}