package rainmaker.gameobject_collections;

import javafx.scene.layout.Pane;
import rainmaker.gameobjects.GameObject;
import rainmaker.gameobjects.ObjectBoundingBox;

public class BoundingBoxPane extends Pane {
    public BoundingBoxPane() {
        setVisible(false);
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public void add(GameObject obj) {
        getChildren().add(new ObjectBoundingBox(obj));
    }

    public void addAll(GameObject... objects) {
        for (GameObject obj : objects) {
            add(obj);
        }
    }

    public void removeFor(GameObject obj) {
        getChildren().removeIf(node -> {
            if (node instanceof ObjectBoundingBox) {
                ObjectBoundingBox box = (ObjectBoundingBox) node;
                return box.getObject() == obj;
            }
            return false;
        });
    }

    public void clear() {
        getChildren().clear();
    }
}
