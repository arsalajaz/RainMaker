package rainmaker.services;

import javafx.animation.AnimationTimer;
import javafx.scene.input.KeyCode;

/**
 * A timer that fires an event when a key is pressed and held down for a
 * specified amount of time. The event is always fired at the beginning of the
 * key press, and then every time the specified interval has elapsed.
 * Purpose: To allow the helicopter acceleration/turning and cloud seeding to
 * feel consistent across different computers.
 */
public class KeyPressTimer {
    private KeyCode key;
    private boolean isPressed;
    private long lastKeyPressTime = 0;
    private long keyPressDelay = 0;
    private boolean justPressed = false;
    Runnable keyPressAction;

    public KeyPressTimer(KeyCode key, long keyPressDelay) {
        this.keyPressDelay = keyPressDelay;
        this.key = key;

        isPressed = false;
        justPressed = false;

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        timer.start();
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

        lastKeyPressTime = System.currentTimeMillis();
        isPressed = true;
        justPressed = true;
    }

    public void setKeyPressAction(Runnable keyPressAction) {
        this.keyPressAction = keyPressAction;
    }

    private void update() {

        // if the key is pressed and the key press delay has passed, run the key press action
        if ((isPressed && System.currentTimeMillis() - lastKeyPressTime > keyPressDelay) || justPressed) {
            keyPressAction.run();
            lastKeyPressTime = System.currentTimeMillis();
            justPressed = false;
        }
    }
}

