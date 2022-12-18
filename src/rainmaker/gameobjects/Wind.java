package rainmaker.gameobjects;

import javafx.animation.AnimationTimer;
import rainmaker.Observer;
import rainmaker.Subject;
import rainmaker.services.RandomGenerator;

import java.util.ArrayList;

public class Wind implements Subject {
    private static final double MAX_SPEED = 2;
    private static final double MIN_SPEED = 0.2;
    private static final double MAX_DURATION_FOR_CHANGE = 10;
    private static final double MIN_DURATION_FOR_CHANGE = 5;
    private final double direction;
    private ArrayList<Observer> observers = new ArrayList<>();
    private double speed = RandomGenerator.getRandomDouble(MIN_SPEED, MAX_SPEED);

    public Wind() {
        this.speed = 0;
        this.direction = 0;

        //Wind changes randomly every 10 seconds, Implement Animation Timer
        AnimationTimer timer = new AnimationTimer() {
            double old = -1;
            double elapsed = 0;
            double randomDuration = RandomGenerator.getRandomDouble(
                    MIN_DURATION_FOR_CHANGE, MAX_DURATION_FOR_CHANGE);

            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }
                double frameTime = (now - old) / 1e9;
                old = now;

                elapsed += frameTime;
                if (elapsed > randomDuration) {
                    elapsed = 0;
                    randomDuration = RandomGenerator.getRandomDouble(5, 10);
                    speed = RandomGenerator
                            .getRandomDouble(MIN_SPEED, MAX_SPEED);
                    notifyObservers();
                }
            }
        };
        timer.start();
    }

    public double getSpeed() {
        return speed;
    }

    public double getDirection() {
        return direction;
    }

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
        observer.update(this);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }
}
