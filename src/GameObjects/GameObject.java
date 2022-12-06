package GameObjects;

import javafx.scene.Group;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

abstract class GameObject extends Group {
    protected Translate myTranslation;
    protected Rotate myRotation;
    protected Scale myScale;

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

    public boolean interest(GameObject object) {
        return !Shape.intersect(this.getShape(), object.getShape())
                .getBoundsInLocal().isEmpty();
    }

    abstract Shape getShape();
}


