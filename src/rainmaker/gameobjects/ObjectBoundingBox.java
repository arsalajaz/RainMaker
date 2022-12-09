package rainmaker.gameobjects;

import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ObjectBoundingBox extends Rectangle {
    private GameObject object;

    public ObjectBoundingBox(GameObject object) {
        this.object = object;
        setFill(Color.TRANSPARENT);
        setStrokeWidth(1);
        setStroke(Color.YELLOW);

        update(object.getBoundsInParent());

        object.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
                    update(newValue);
                });
    }

    private void update(Bounds bounds) {
        setX(bounds.getMinX());
        setY(bounds.getMinY());
        setWidth(bounds.getWidth());
        setHeight(bounds.getHeight());
    }

    public GameObject getObject() {
        return object;
    }
}
