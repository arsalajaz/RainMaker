package rainmaker.gameobjects;

import javafx.animation.AnimationTimer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import rainmaker.Observer;
import rainmaker.Subject;
import rainmaker.services.RandomGenerator;

import java.io.File;
import java.util.ArrayList;

public class Wind implements Subject {
    private static final double MAX_SPEED = 2;
    private static final double MIN_SPEED = 0.2;
    private static final double MAX_DURATION_FOR_CHANGE = 10;
    private static final double MIN_DURATION_FOR_CHANGE = 5;
    private static final Media WIND_SOUND_MEDIA = new Media(
            new File("src/resources/wind_sound.wav")
                    .toURI()
                    .toString());
    private static final MediaPlayer WIND_SOUND = new MediaPlayer(
            WIND_SOUND_MEDIA);
    private final double direction;
    private ArrayList<Observer> observers = new ArrayList<>();
    private double speed = RandomGenerator.getRandomDouble(MIN_SPEED, MAX_SPEED);

    public Wind() {
        this.speed = 0;
        this.direction = 0;

        WIND_SOUND.setCycleCount(MediaPlayer.INDEFINITE);
        WIND_SOUND.setVolume(speed / MAX_SPEED);
        if (WIND_SOUND.getStatus() != MediaPlayer.Status.PLAYING) {
            WIND_SOUND.play();
        }

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
                    WIND_SOUND.setVolume(speed/MAX_SPEED);
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
