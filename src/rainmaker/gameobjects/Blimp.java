package rainmaker.gameobjects;

import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

enum BlimpState {
    CREATED,
    IN_VIEW,
    DEAD
}

public class Blimp extends GameObject{
    private BlimpState state = BlimpState.CREATED;

    private double fuel;
    private Rectangle blimpShape;

    public Blimp(double fuel) {
        super();

        this.fuel = fuel;

        ImagePattern img = new ImagePattern(new Image("/resources/blimp.png"));
        blimpShape = new Rectangle();
        blimpShape.setFill(img);
        blimpShape.setWidth(100);
        blimpShape.setHeight(100);

        getChildren().add(blimpShape);
        shapes.add(blimpShape);
    }

}

