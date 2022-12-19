package rainmaker.gameobjects;

import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

class HeloBody extends Rectangle {
    public HeloBody() {
        Image bodyImage = new Image("/copter_body.png");
        setFill(new ImagePattern(bodyImage));

        // scale the image down to 14% of its original size
        setWidth(bodyImage.getWidth() * 0.18);
        setHeight(bodyImage.getHeight() * 0.18);
        setScaleY(-1);
        setTranslateX(-15);
        setTranslateY(-65);
    }
}
