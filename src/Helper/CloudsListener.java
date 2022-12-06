package Helper;

import GameObjects.Cloud;

public interface CloudsListener {
    void onCloudDestroyed(Cloud cloud);
    void onCloudSpawned(Cloud cloud);
}


