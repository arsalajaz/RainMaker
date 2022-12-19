package rainmaker.gameobjects;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import rainmaker.Game;
import rainmaker.Updatable;
import rainmaker.services.Vector;

import java.io.File;

public class Helicopter extends GameObject implements Updatable {
    public static final double MAX_SPEED = 10;
    public static final double MIN_SPEED = -2;
    public static final double ACCELERATION = 0.1;
    public static final double ROTATION_CHANGE = 1;
    public static final AudioClip SEEDING_SOUND =
            new AudioClip(new File("src/resources/cloud_seeding_sound.wav").toURI()
                    .toString());
    private static final double HOOVER_FUEL_CONSUMPTION = 25;
    private static final Media FLYING_SOUND_MEDIA = new Media(
            new File("src/resources/copter_flying_hoovering.mp3").toURI().toString());
    public static final MediaPlayer FLYING_SOUND =
            new MediaPlayer(FLYING_SOUND_MEDIA);
    private static final Media TAKEOFF_SOUND_MEDIA = new Media(
            new File("src/resources/takeoff_sound.wav").toURI().toString());
    public static final MediaPlayer TAKEOFF_SOUND =
            new MediaPlayer(TAKEOFF_SOUND_MEDIA);
    private static final Media LANDING_SOUND_MEDIA = new Media(
            new File("src/resources/copter_landing_sound.mp3").toURI().toString());
    public static final MediaPlayer LANDING_SOUND =
            new MediaPlayer(LANDING_SOUND_MEDIA);
    private final GameText fuelText;
    private final GameText stateText;
    private final HeloBody heloBody;
    private final HeloBlade heloBlade;
    protected Duration takeOffCurrentTime = Duration.ZERO;
    protected Duration landingCurrentTime = Duration.ZERO;
    private HelicopterState currState;
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

        FLYING_SOUND.setCycleCount(MediaPlayer.INDEFINITE);
        FLYING_SOUND.setVolume(0);
        FLYING_SOUND.play();

        TAKEOFF_SOUND.setVolume(0);
        LANDING_SOUND.setVolume(0);
        SEEDING_SOUND.setVolume(1);

        getChildren().addAll(heloBody, heloBlade, fuelText, stateText);

        position = initialPosition;

        currState = new OffState();

        shapes.add(heloBody);
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
                .multiply(frameTime * Game.UNIVERSAL_SPEED_MULTIPLIER);

        position = position.add(velocity);
    }

    @Override
    public void update(double frameTime) {
        calculateNewPosition(frameTime);
        move();
        updateLabels();

        currState.nextFrame(frameTime);
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

    public void setOnLandedAction(Runnable action) {
        this.onLandedAction = action;
    }

    public void seedCloud(Cloud cloud) {
        currState.seedCloud(cloud);
    }

    public void setOnCrash(Runnable action) {
        this.onCrashAction = action;
    }

    public double getFuel() {
        return fuel;
    }

    private void crash() {
        if (onCrashAction != null) {
            onCrashAction.run();
        }
    }

    public void setLandingLocation(Helipad helipad) {
        this.helipad = helipad;
    }

    private boolean hooveringOverHelipad() {
        return helipad != null &&
                helipad.getBoundsInParent().contains(getBoundsInParent());
    }

    public double getSpeed() {
        return speed;
    }

    public void refuel(double siphonedFuel) {
        fuel += siphonedFuel;
    }

    public double getHeading() {
        return getCartesianAngle();
    }

    abstract class HelicopterState {

        abstract void speedUp();

        abstract void speedDown();

        abstract void turnLeft();

        abstract void turnRight();

        abstract void toggleIgnition();

        abstract void seedCloud(Cloud cloud);

        abstract void nextFrame(double frameTime);
    }

    class OffState extends HelicopterState {

        public OffState() {
            landingCurrentTime = Duration.ZERO;
            takeOffCurrentTime = Duration.ZERO;
        }

        @Override
        void speedUp() {
            // do nothing
        }

        @Override
        void speedDown() {
            // do nothing
        }

        @Override
        void toggleIgnition() {
            currState = new StartingState();
        }

        @Override
        void turnLeft() {
            // do nothing
        }

        @Override
        void turnRight() {
            // do nothing
        }

        @Override
        void seedCloud(Cloud cloud) { /* Do nothing - helicopter is off */ }

        @Override
        void nextFrame(double frameTime) {
            // do nothing
        }

        public String toString() {
            return "Off";
        }

    }

    class StartingState extends HelicopterState {
        public StartingState() {
            FLYING_SOUND.setVolume(0);
            double totalSoundDuration =
                    TAKEOFF_SOUND_MEDIA.getDuration().toMillis();
            double landingStoppedAtDuration =
                    landingCurrentTime.toMillis();
            Duration newSoundStartDuration =
                    new Duration(totalSoundDuration - landingStoppedAtDuration);

            if (landingCurrentTime != Duration.ZERO) {
                TAKEOFF_SOUND.setStartTime(newSoundStartDuration);
            } else {
                TAKEOFF_SOUND.setStartTime(Duration.ZERO);
            }

            TAKEOFF_SOUND.play();
            TAKEOFF_SOUND.setVolume(0.3);

            heloBlade.setOnMaxRotationalSpeed(() -> {
                takeOffCurrentTime = LANDING_SOUND_MEDIA.getDuration();
                TAKEOFF_SOUND.stop();

                currState = new ReadyState();
                if (onFlyingAction != null) onFlyingAction.run();
            });
            heloBlade.startSpinning();
        }

        @Override
        void speedUp() {
            // do nothing
        }

        @Override
        void speedDown() {
            // do nothing
        }

        @Override
        void toggleIgnition() {
            takeOffCurrentTime = TAKEOFF_SOUND.getCurrentTime();
            TAKEOFF_SOUND.stop();

            currState = new StoppingState();
        }

        @Override
        void turnLeft() {
            // do nothing
        }

        @Override
        void turnRight() {
            // do nothing
        }

        @Override
        void seedCloud(Cloud cloud) { /* Do nothing - helicopter is starting */}

        @Override
        void nextFrame(double frameTime) {
            fuel = fuel - (HOOVER_FUEL_CONSUMPTION * frameTime);
            if (fuel <= 0) {
                fuel = 0;
                currState = new StoppingState();
                crash();
            }
        }

        public String toString() {
            return "Starting";
        }
    }

    class StoppingState extends HelicopterState {
        public StoppingState() {
            FLYING_SOUND.setVolume(0);
            if (takeOffCurrentTime.toMillis() > 1) {
                LANDING_SOUND.setStartTime(new Duration(
                        LANDING_SOUND_MEDIA.getDuration().toMillis() -
                                takeOffCurrentTime.toMillis()));
            } else {
                LANDING_SOUND.setStartTime(Duration.ZERO);
            }

            LANDING_SOUND.play();
            LANDING_SOUND.setVolume(0.3);
            heloBlade.setOnStoppedRotating(() -> {
                LANDING_SOUND.stop();
                currState = new OffState();
                if (onLandedAction != null) onLandedAction.run();
            });
            heloBlade.stopSpinning();
        }

        @Override
        void speedUp() {
            // do nothing
        }

        @Override
        void speedDown() {
            // do nothing
        }

        @Override
        void toggleIgnition() {
            landingCurrentTime = LANDING_SOUND.getCurrentTime();
            LANDING_SOUND.stop();

            currState = new StartingState();
        }

        @Override
        void turnLeft() {
            // do nothing
        }


        @Override
        void turnRight() {
            // do nothing
        }

        @Override
        void seedCloud(Cloud cloud) {
            // Do nothing
        }

        @Override
        void nextFrame(double frameTime) {
            // do nothing
        }

        public String toString() {
            return "Stopping";
        }
    }

    class ReadyState extends HelicopterState {

        public ReadyState() {
            FLYING_SOUND.setVolume(0.5);
        }

        @Override
        void speedUp() {
            if (speed >= MAX_SPEED) return;
            speed += ACCELERATION;
            speed = Vector.round(speed, 1);
        }

        @Override
        void speedDown() {
            if (speed <= MIN_SPEED) return;
            speed -= ACCELERATION;
            speed = Vector.round(speed, 1);
        }

        @Override
        void toggleIgnition() {
            if (Math.abs(speed) >= 0.1) return;
            if (!hooveringOverHelipad()) return;

            FLYING_SOUND.setVolume(0);
            currState = new StoppingState();
        }

        @Override
        void turnLeft() {
            heading -= ROTATION_CHANGE;
        }

        @Override
        void turnRight() {
            heading += ROTATION_CHANGE;
        }

        @Override
        void seedCloud(Cloud cloud) {
            cloud.saturate();
            SEEDING_SOUND.play();
        }

        @Override
        void nextFrame(double frameTime) {
            double speedConsumption = Math.abs(speed) * HOOVER_FUEL_CONSUMPTION;
            double hoverConsumption = HOOVER_FUEL_CONSUMPTION;

            speedConsumption *= frameTime;
            hoverConsumption *= frameTime;

            fuel = (fuel - speedConsumption - hoverConsumption);

            if (fuel <= 0) {
                fuel = 0;
                crash();
                currState = new StoppingState();
            }
        }

        public String toString() {
            return "Flying";
        }
    }
}

