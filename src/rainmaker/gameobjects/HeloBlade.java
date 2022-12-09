package rainmaker.gameobjects;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

enum BladeState {
    STOPPED, INCREASING_SPEED, DECREASING_SPEED, AT_MAX_SPEED
}

/**
 * Extends the Circle class so that the bounding box does not increase on
 * rotation. Uses a simple enum state.
 */
public class HeloBlade extends Circle {
    public static final double MAX_ROTATIONAL_SPEED = 1000;
    public static final double INITIAL_ROTATION_ANGLE = 45;
    private static final double ROTATION_ANGLE_INCREMENT = 200;
    private BladeState currState;
    private double rotationalSpeed;
    private Runnable onMaxRotationalSpeed;
    private Runnable onStopRotating;

    public HeloBlade() {
        super(40);

        setFill(new ImagePattern(new Image("/resources/blades.png")));

        currState = BladeState.STOPPED;

        setScaleY(-1);
        setRotate(INITIAL_ROTATION_ANGLE);
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

                if (currState == BladeState.INCREASING_SPEED) {
                    rotationalSpeed += ROTATION_ANGLE_INCREMENT * frameTime;
                    if (rotationalSpeed >= MAX_ROTATIONAL_SPEED) {
                        rotationalSpeed = MAX_ROTATIONAL_SPEED;

                        if (onMaxRotationalSpeed != null)
                            onMaxRotationalSpeed.run();

                        currState = BladeState.AT_MAX_SPEED;
                    }
                } else if (currState == BladeState.DECREASING_SPEED) {
                    rotationalSpeed -= ROTATION_ANGLE_INCREMENT * frameTime;
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

    public double getCurrentSpeed() {
        return rotationalSpeed;
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
