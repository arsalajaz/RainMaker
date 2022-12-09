package rainmaker.gameobjects;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class DistanceLine extends Group {
    private Line line;
    private GameObject object1;
    private GameObject object2;

    private GameText distanceText;

    public DistanceLine(GameObject obj1, GameObject obj2) {
        line = new Line();
        line.setStroke(Color.YELLOW);
        line.setStrokeWidth(1);

        object1 = obj1;
        object2 = obj2;
        distanceText = new GameText();
        distanceText.setFill(Color.YELLOW);

        update();
        object1.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
                    update();
                });
        object2.boundsInParentProperty()
                .addListener((observable, oldValue, newValue) -> {
                    update();
                });

        getChildren().addAll(line, distanceText);
    }

    public boolean hasObjects(GameObject obj1, GameObject obj2) {
        return (object1 == obj1 && object2 == obj2) ||
                (object1 == obj2 && object2 == obj1);
    }

    public boolean hasObject(GameObject obj) {
        return object1 == obj || object2 == obj;
    }

    public double getDistance() {
        double x1 = object1.getBoundsInParent().getCenterX();
        double y1 = object1.getBoundsInParent().getCenterY();
        double x2 = object2.getBoundsInParent().getCenterX();
        double y2 = object2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double getDistance(GameObject obj1, GameObject obj2) {
        double x1 = obj1.getBoundsInParent().getCenterX();
        double y1 = obj1.getBoundsInParent().getCenterY();
        double x2 = obj2.getBoundsInParent().getCenterX();
        double y2 = obj2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Maybe needed if property binding not allowed
     */
    private void update() {
        double x1 = object1.getBoundsInParent().getCenterX();
        double y1 = object1.getBoundsInParent().getCenterY();
        double x2 = object2.getBoundsInParent().getCenterX();
        double y2 = object2.getBoundsInParent().getCenterY();

        line.setStartX(x1);
        line.setStartY(y1);
        line.setEndX(x2);
        line.setEndY(y2);

        double distance = getDistance();
        distanceText.setText(String.format("%.2f", distance));
        distanceText.setX((x1 + x2) / 2);
        distanceText.setY((y1 + y2) / 2);
    }
}
