package rainmaker.gameobjects;

import javafx.scene.Group;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.List;

public abstract class GameObject extends Group {
    protected Translate myTranslation;
    protected Rotate myRotation;
    protected Scale myScale;

    // List of shapes that make up this object, used for accurate collision
    // detection
    protected final List<Shape> shapes = new ArrayList<>();

    public GameObject() {
        myTranslation = new Translate();
        myRotation = new Rotate();
        myScale = new Scale();

        this.getTransforms().addAll(myTranslation, myRotation, myScale);
    }

    public void rotate(double degrees, double pivotX, double pivotY) {
        myRotation.setAngle(degrees);
        myRotation.setPivotX(pivotX);
        myRotation.setPivotY(pivotY);
    }

    public void scale(double sx, double sy) {
        myScale.setX(sx);
        myScale.setY(sy);
    }

    public void translate(double tx, double ty) {
        myTranslation.setX(tx);
        myTranslation.setY(ty);
    }

    public boolean intersects(GameObject object) {
        for (Shape shape : shapes) {
            for (Shape otherShape : object.shapes) {
                if (Shape.intersect(shape, otherShape).getBoundsInLocal()
                        .isEmpty()) continue;
                return true;

            }
        }
        return false;
    }

    /**
     * Return true if interests with the other objects within the given
     * proportion of the object's area or more
     */
    public boolean intersects(GameObject object, double proportion) {
        for (Shape shape : shapes) {
            for (Shape otherShape : object.shapes) {
                if (Shape.intersect(shape, otherShape).getBoundsInLocal()
                        .isEmpty())
                    continue;

                double currProportion = Shape.intersect(shape, otherShape)
                        .getBoundsInLocal().getWidth() /
                        shape.getBoundsInLocal().getWidth();
                if (currProportion >= currProportion) {
                    return true;
                }
            }
        }
        return false;
    }
}
