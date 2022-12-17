package rainmaker;

import javafx.scene.shape.Shape;

import java.util.ArrayList;

public interface MultiShapeObject {
    ArrayList<Shape> shapes = new ArrayList<>();

    default void addShape(Shape shape) {
        shapes.add(shape);
    }

    default void removeShape(Shape shape) {
        shapes.remove(shape);
    }

    default ArrayList<Shape> getShapes() {
        return shapes;
    }

    default boolean intersects(MultiShapeObject other) {
        for (Shape shape : shapes) {
            for (Shape otherShape : other.getShapes()) {
                if (shape.intersects(otherShape.getBoundsInLocal())) {
                    return true;
                }
            }
        }
        return false;
    }
}
