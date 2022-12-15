package rainmaker.services;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class TimedKeysCollection {
    private List<KeyPressTimer> timers = new ArrayList<>();
    public TimedKeysCollection(KeyPressTimer... timers) {
        for (KeyPressTimer timer : timers) {
            this.timers.add(timer);
        }
    }

    public boolean containsTimerFor(KeyCode key) {
        for (KeyPressTimer timer : timers) {
            if (timer.getKey() == key) {
                return true;
            }
        }
        return false;
    }

    public void addAll(KeyPressTimer... timers) {
        for (KeyPressTimer timer : timers) {
            this.timers.add(timer);
        }
    }

    public void addTimer(KeyPressTimer timer) {
        timers.add(timer);
    }

    public void keyPressed(KeyCode key) {
        for (KeyPressTimer timer : timers) {
            if (timer.getKey() == key) {
                timer.keyPressed();
            }
        }
    }

    public void keyReleased(KeyCode key) {
        for (KeyPressTimer timer : timers) {
            if (timer.getKey() == key) {
                timer.keyReleased();
            }
        }
    }
}