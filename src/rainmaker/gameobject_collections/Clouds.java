package rainmaker.gameobject_collections;

import javafx.animation.AnimationTimer;
import rainmaker.CloudsListener;
import rainmaker.services.CoinSide;
import rainmaker.services.RandomGenerator;
import rainmaker.Updatable;
import rainmaker.gameobjects.Cloud;

import java.util.ArrayList;
import java.util.List;


public class Clouds extends GameObjectPane<Cloud> implements Updatable {
    private static final int MAX_CLOUDS = 5;
    private static final int MIN_CLOUDS = 2;
    private List<CloudsListener> listeners = new ArrayList<>();

    public Clouds() {
        AnimationTimer timer = new AnimationTimer() {
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

    public void addListener(CloudsListener listener) {
        listeners.add(listener);
    }

    private void notifyCloudSpawned(Cloud cloud) {
        listeners.forEach(l -> l.onCloudSpawned(cloud));
    }

    private void notifyCloudDestroyed(Cloud cloud) {
        listeners.forEach(l -> l.onCloudDestroyed(cloud));
    }

    private double elapsed = 0;
    private double rainElapsed = 0;

    @Override
    public void update(double frameTime) {
        elapsed += frameTime;
        rainElapsed += frameTime;

        // add initial clouds
        if (getChildren().isEmpty()) {
            for (int i = 0; i < MAX_CLOUDS; i++) {
                Cloud cloud = Cloud.createRandomCloud(true);
                add(cloud);
                notifyCloudSpawned(cloud);
            }
            return;
        }
        //every second, cloud losses 1% saturation
        if (rainElapsed >= 0.5) {
            rainElapsed = 0;
            for (Cloud cloud : this) {
                cloud.rain();
            }
        }

        // Not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            Cloud cloud = (Cloud) getChildren().get(i);
            cloud.update(frameTime);

            if (!cloud.isDead()) continue;

            remove(cloud);
            notifyCloudDestroyed(cloud);
        }

        if (getChildren().size() >= MAX_CLOUDS) return;

        if (getChildren().size() <= MIN_CLOUDS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            notifyCloudSpawned(cloud);
        }

        if (elapsed < 5) return;
        elapsed = 0;

        if (RandomGenerator.flipCoin() == CoinSide.HEADS) {
            Cloud cloud = Cloud.createRandomCloud(false);
            add(cloud);
            notifyCloudSpawned(cloud);
        }
    }
}
