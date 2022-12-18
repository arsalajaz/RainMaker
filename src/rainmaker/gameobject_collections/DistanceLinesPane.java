package rainmaker.gameobject_collections;

import javafx.scene.layout.Pane;
import rainmaker.gameobjects.DistanceLine;
import rainmaker.gameobjects.GameObject;

public class DistanceLinesPane extends Pane {
    public DistanceLinesPane() {
        setVisible(false);
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public void add(GameObject obj1, GameObject obj2) {
        getChildren().add(new DistanceLine(obj1, obj2));
    }

    public void removeIfInvolves(GameObject obj) {
        getChildren().removeIf(node -> {
            if (node instanceof DistanceLine) {
                DistanceLine line = (DistanceLine) node;
                return line.hasObject(obj);
            }
            return false;
        });
    }

    public void clear() {
        getChildren().clear();
    }
}
