package rainmaker.gameobjects;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import rainmaker.*;
import rainmaker.services.BezierOval;
import rainmaker.services.RandomGenerator;

enum CloudState {
    SPAWNED, ALIVE, DEAD
}

public class Cloud extends GameObject implements Updatable {
    private final double MIN_RADIUS = 40;
    private final double MAX_RADIUS = 70;
    private static final double WIND_SPEED = 0.4;
    private static final double WIND_DIRECTION = 0;
    private CloudState state = CloudState.SPAWNED;
    private Vector position;
    private Vector velocity;
    private final Circle cloud;
    private final BezierOval cloudShape;
    private final GameText percentText;
    private int saturation = 0;
    private double speedOffset = RandomGenerator.getRandomDouble(40, 70);

    public Cloud(Point2D initPosition, Point2D shapeSize) {
        cloud = new Circle(20, Color.rgb(255, 255, 255));
        cloudShape = new BezierOval(shapeSize.getX(), shapeSize.getY());
        cloudShape.setFill(Color.rgb(255, 255, 255));
        translate(initPosition.getX(), initPosition.getY());

        percentText = new GameText();
        percentText.setFill(Color.BLUE);
        getChildren().addAll(cloudShape, percentText);
        this.position = new Vector(initPosition.getX(), initPosition.getY());
    }

    public boolean isRaining() {
        return saturation >= 30;
    }

    public int getSaturation() {
        return saturation;
    }

    public CloudState getState() {
        return state;
    }

    public void rain() {
        if (saturation <= 0) return;
        saturation--;
    }

    public void saturate() {
        if (saturation >= 100) return;
        saturation++;
    }

    @Override
    Shape getShape() {
        return null;
    }

    @Override
    public void update(double FrameTime) {
        if (isDead()) return;

        velocity = new Vector(WIND_SPEED * FrameTime * speedOffset,
                Math.toRadians(WIND_DIRECTION), true);
        position = position.add(velocity);

        if (state != CloudState.ALIVE && isWithinBounds()) {
            state = CloudState.ALIVE;
        } else if (shouldDie()) {
            state = CloudState.DEAD;
        }


        translate(position.getX(), position.getY());

        cloudShape.setFill(Color.rgb(255 - saturation, 255 - saturation,
                255 - saturation));
        percentText.setText(saturation + "%");
        percentText.setTranslateX(-percentText.getLayoutBounds().getWidth() / 2);
        percentText.setTranslateY(percentText.getLayoutBounds().getHeight() / 2);

    }

    private boolean isWithinBounds() {
        double cloudWidth = cloudShape.getLayoutBounds().getWidth();
        double cloudHeight = cloudShape.getLayoutBounds().getHeight();
        return position.getX() > cloudWidth / 2 &&
                position.getX() < GameApp.GAME_WIDTH - cloudWidth / 2 &&
                position.getY() > cloudHeight / 2 &&
                position.getY() < GameApp.GAME_HEIGHT - cloudHeight / 2;
    }

    private boolean shouldDie() {
        double cloudWidth = cloudShape.getLayoutBounds().getWidth();
        double cloudHeight = cloudShape.getLayoutBounds().getHeight();
        return position.getX() < -cloudWidth / 2 && velocity.getX() < 0 ||
                position.getX() > GameApp.GAME_WIDTH + cloudWidth / 2 && velocity.getX() > 0 ||
                position.getY() < -cloudHeight / 2 && velocity.getY() < 0 ||
                position.getY() > GameApp.GAME_HEIGHT + cloudHeight / 2 && velocity.getY() > 0;
    }

    public static Cloud createRandomCloud(boolean onScreen) {
        double radiusX = RandomGenerator.getRandomDouble(50, 60);
        double radiusY = RandomGenerator.getRandomDouble(30, 40);
        double x = onScreen ? RandomGenerator.getRandomDouble(radiusX,
                800 - radiusX) : -radiusX - 10;
        double y = RandomGenerator.getRandomDouble(radiusY, 800 - radiusY);
        Point2D position = new Point2D(x, y);
        return new Cloud(position, new Point2D(radiusX, radiusY));
    }

    public boolean isDead() {
        return state == CloudState.DEAD;
    }
}
