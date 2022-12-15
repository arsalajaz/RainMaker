package rainmaker.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

/**
 * A timer that fires an event when a key is pressed and held down for a
 * specified amount of time. The event is always fired at the beginning of the
 * key press, and then every time the specified interval has elapsed.
 * Purpose: To allow the helicopter acceleration/turning and cloud seeding to
 * feel consistent across different computers.
 */
public class KeyPressTimer {

    private Timeline timeline;
    private KeyCode key;
    private boolean isPressed;
    private boolean justPressed = false;
    Runnable keyPressAction;

    public KeyPressTimer(KeyCode key, long keyPressDelay) {
        timeline = new Timeline(new KeyFrame(Duration.millis(keyPressDelay),
                event -> {
            if ((isPressed && keyPressAction != null) || justPressed) {
                keyPressAction.run();
                justPressed = false;
            }
        }));

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.playFromStart();

        this.key = key;

        isPressed = false;
        justPressed = false;

    }

    public KeyCode getKey() {
        return key;
    }

    public void keyReleased() {
        if(!isPressed) return;

        isPressed = false;
    }

    public void keyPressed() {
        if(isPressed) return;

        isPressed = true;
        justPressed = true;
    }

    public void setKeyPressAction(Runnable keyPressAction) {
        this.keyPressAction = keyPressAction;
    }
}

