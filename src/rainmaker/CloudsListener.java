package rainmaker;

import rainmaker.gameobjects.Cloud;

public interface CloudsListener {
    void onCloudDestroyed(Cloud cloud);

    void onCloudSpawned(Cloud cloud);
}
