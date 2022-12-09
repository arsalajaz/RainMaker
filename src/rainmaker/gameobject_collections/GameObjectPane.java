package rainmaker.gameobject_collections;

import javafx.scene.layout.Pane;
import rainmaker.gameobjects.GameObject;

import java.util.Iterator;

/**
 * Temp.Game object pane that stores collection of one type of game objects and
 * updatable
 */

class GameObjectPane<T extends GameObject> extends Pane implements Iterable<T> {

    public void add(T object) {
        getChildren().addAll(object);
    }

    public void remove(T object) {
        getChildren().removeAll(object);
    }

    public void clear() {
        getChildren().clear();
    }

    //public List getObjects() { return getChildren(); }

    @Override
    public Iterator iterator() {
        return getChildren().iterator();
    }
}
