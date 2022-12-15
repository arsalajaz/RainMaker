package rainmaker.gameobjects;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class Helipad extends GameObject {
    private final double BORDER_OFFSET = 10;
    Circle pad;

    public Helipad(double radius, Point2D initialPosition) {
        pad = new Circle(radius);
        pad.setFill(Color.DARKGRAY);
        pad.setStroke(Color.GRAY);
        pad.setStrokeWidth(2);

        Bounds bounds = pad.getBoundsInParent();
        Rectangle border = new Rectangle(
                bounds.getMinX() - BORDER_OFFSET,
                bounds.getMinY() - BORDER_OFFSET,
                bounds.getWidth() + BORDER_OFFSET * 2,
                bounds.getHeight() + BORDER_OFFSET * 2
        );
        border.setStroke(Color.GRAY);
        border.setStrokeWidth(2);
        border.setFill(Color.DARKGRAY);

        shapes.add(pad);

        getChildren().addAll(border, pad);

        translate(initialPosition.getX(), initialPosition.getY());
    }
}
