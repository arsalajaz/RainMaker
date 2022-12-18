package rainmaker.gameobjects;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import rainmaker.Observer;
import rainmaker.Updatable;
import rainmaker.services.BezierOval;
import rainmaker.services.RandomGenerator;
import rainmaker.services.Vector;

public class Cloud extends TransientGameObject implements Updatable, Observer {
    private static final double SATURATION_LOSS_DELAY_IN_SECS = 1;
    private final BezierOval shape;
    private final GameText infoText;
    private final double speedOffset = RandomGenerator.getRandomDouble(0.5,
            1.5);
    private int saturation = 0;
    private double rainTimeElapsed = 0;

    public Cloud(Vector initPos, Point2D shapeSize) {
        super(initPos);

        shape = new BezierOval.Builder(shapeSize.getX(), shapeSize.getY())
                .setStartAngle(RandomGenerator.getRandomInt(0, 360))
                .setAngleIncrementMin(60)
                .setAngleIncrementMax(72)
                .setOverlapAngle(72)
                .setMinOffsetFromOval(10)
                .setMaxOffsetFromOval(20)
                .build();

        shape.setFill(Color.rgb(255, 255, 255));
        shape.setStroke(Color.BLACK);
        shape.setStrokeWidth(1);

        infoText = new GameText();
        infoText.setFill(Color.BLUE);

        setSpeed((speedOffset));
        setHeading(0);

        getChildren().addAll(shape, infoText);

        for (Node node : shape.getChildren()) {
            if (node instanceof Shape) {
                shapes.add((Shape) node);
            }
        }
        infoText.setText("0%");
        infoText.setTranslateX(-infoText.getLayoutBounds().getWidth() / 2);
        infoText.setTranslateY(infoText.getLayoutBounds().getHeight() / 2);
    }

    public static Cloud createRandomCloud(boolean onScreen) {
        double radiusX = RandomGenerator.getRandomDouble(50, 60);
        double radiusY = RandomGenerator.getRandomDouble(30, 40);
        double x = onScreen ? RandomGenerator.getRandomDouble(radiusX,
                800 - radiusX) : -radiusX - 10;
        double y = RandomGenerator.getRandomDouble(radiusY, 800 - radiusY);
        Vector position = new Vector(x, y);
        return new Cloud(position, new Point2D(radiusX, radiusY));
    }

    public boolean isRaining() {
        return saturation >= 30;
    }

    public int getSaturation() {
        return saturation;
    }

    private void rain() {
        if (saturation <= 0) return;
        saturation--;
    }

    public void saturate() {
        if (saturation >= 100) return;
        saturation++;
    }

    @Override
    public void update(double FrameTime) {
        move(FrameTime);

        rainTimeElapsed += FrameTime;
        //every second, cloud losses 1% saturation
        if (rainTimeElapsed >= SATURATION_LOSS_DELAY_IN_SECS) {
            rainTimeElapsed = 0;
            rain();
        }

        shape.setFill(Color.rgb(255 - saturation, 255 - saturation,
                255 - saturation));
        infoText.setText(saturation + "%");
    }

    @Override
    public void update(Object o) {
        if (o instanceof Wind wind) {
            setSpeed(wind.getSpeed() + speedOffset);
            setHeading(wind.getDirection());
        }
    }
}
