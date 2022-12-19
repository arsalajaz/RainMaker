package rainmaker.gameobjects;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ImageBackground extends Pane {
    private final ImageView background;

    public ImageBackground(double width, double height) {
        background = new ImageView(new Image("/desert_bg_img.jpg"));
        background.setFitWidth(width);
        background.setFitHeight(height);
        getChildren().add(background);
    }
}
