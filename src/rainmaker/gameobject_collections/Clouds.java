package rainmaker.gameobject_collections;

import javafx.animation.AnimationTimer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import rainmaker.Game;
import rainmaker.Observer;
import rainmaker.Updatable;
import rainmaker.gameobjects.Cloud;
import rainmaker.services.CoinSide;
import rainmaker.services.RandomGenerator;

import java.io.File;
import java.util.ArrayList;


public class Clouds extends GameObjectPane<Cloud> implements Updatable {
    private static final int MAX_CLOUDS = 5;
    private static final int MIN_CLOUDS = 2;
    private static final Media RAIN_SOUND_PLAYER =
            new Media(new File("src/resources/rain_sound.wav").toURI().toString());
    private static final MediaPlayer RAIN_SOUND =
            new MediaPlayer(RAIN_SOUND_PLAYER);

    private ArrayList<Observer> observers = new ArrayList<>();
    private double elapsed = 0;

    AnimationTimer timer;

    public Clouds() {
        RAIN_SOUND.setCycleCount(MediaPlayer.INDEFINITE);
        RAIN_SOUND.setVolume(0);
        if (RAIN_SOUND.getStatus() != MediaPlayer.Status.PLAYING) {
            RAIN_SOUND.play();
        }
        timer = new AnimationTimer() {
            double old = -1;

            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }
                double frameTime = (now - old) / 1e9;
                old = now;
                update(frameTime);
            }
        };
        timer.start();
    }

    @Override
    public void update(double frameTime) {
        elapsed += frameTime;

        // add initial clouds
        if (getChildren().isEmpty()) {
            for (int i = 0; i < MAX_CLOUDS; i++) {
                Cloud cloud = Cloud.createRandomCloud(true);
                add(cloud);
                Game.getInstance().handleCloudAdded(cloud);
            }
            return;
        }

        boolean isRaining = false;
        // Not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            Cloud cloud = (Cloud) getChildren().get(i);
            cloud.update(frameTime);

            if (cloud.isRaining()) {
                isRaining = true;
            }

            if (!cloud.isDead()) continue;

            remove(cloud);
            Game.getInstance().handleCloudRemoved(cloud);
        }

        if (isRaining) RAIN_SOUND.setVolume(1);
        else RAIN_SOUND.setVolume(0);

        if (getChildren().size() >= MAX_CLOUDS) return;

        if (getChildren().size() <= MIN_CLOUDS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            Game.getInstance().handleCloudAdded(cloud);
        }

        if (elapsed < 5) return;
        elapsed = 0;

        if (RandomGenerator.flipCoin() == CoinSide.HEADS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            Game.getInstance().handleCloudAdded(cloud);
        }
    }
}
