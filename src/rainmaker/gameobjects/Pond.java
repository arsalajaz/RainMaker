package rainmaker.gameobjects;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import rainmaker.Game;
import rainmaker.Updatable;
import rainmaker.services.BezierOval;
import rainmaker.services.RandomGenerator;

public class Pond extends GameObject implements Updatable {
    private final GameText waterLevelText = new GameText();
    Circle pondShape = new Circle();

    BezierOval shape;

    private double pondArea;
    private double waterLevel = 0;

    public Pond(Point2D initialPosition, int initialWater, double initialArea) {
        pondArea = initialArea;
        pondShape.setRadius(getRadius());
        pondShape.setFill(Color.BLUE);

        shape = new BezierOval.Builder(getRadius(), getRadius())
                .setStartAngle(0)
                .setAngleIncrementMin(72)
                .setAngleIncrementMax(72)
                .setOverlapAngle(0)
                .setMinOffsetFromOval(10)
                .setMaxOffsetFromOval(20)
                .setRandomizeControlAngle(true)
                .build();

        System.out.println("Pond area: " + pondArea);

        shape.setFill(Color.BLUE);

        this.waterLevel = initialWater;
        waterLevelText.setText(String.valueOf(waterLevel));
        waterLevelText.setFill(Color.WHITE);

        getChildren().add(shape);
        getChildren().addAll(waterLevelText);
        pondShape.setVisible(false);
        setTranslateX(initialPosition.getX());
        setTranslateY(initialPosition.getY());

        for(Node node : shape.getChildren()) {
            if(node instanceof Shape) {
                shapes.add((Shape) node);
            }
        }
    }

    public static Pond generatePond() {
        int waterLevel = RandomGenerator.getRandomInt(10, 30);
        int initialArea = waterLevel * 100;
        double radius = getRadius(initialArea);
        double x = RandomGenerator.getRandomDouble(radius,
                Game.GAME_WIDTH - radius);
        double y = RandomGenerator.getRandomDouble(radius,
                Game.GAME_WIDTH - radius);
        return new Pond(new Point2D(x, y), waterLevel, initialArea);
    }

    public static double getRadius(double area) {
        return Math.sqrt(area / Math.PI);
    }

    public double getRadius() {
        return Math.sqrt(pondArea / Math.PI);
    }

    public void addWater(double water) {
        pondArea += 100 * water;
        waterLevel += water;

        // scale the pond

    }

    public double getCurrentWaterLevel() {
        return waterLevel;
    }

    @Override
    public void update(double FrameTime) {
        shape.setScaleX(getRadius() / shape.getRadiusX());
        shape.setScaleY(getRadius() / shape.getRadiusY());
        pondShape.setRadius(getRadius());

        waterLevelText.setText(String.valueOf((int) waterLevel));
        waterLevelText.setTranslateX(-waterLevelText.getLayoutBounds()
                .getWidth() / 2);
        waterLevelText.setTranslateY(waterLevelText.getLayoutBounds()
                .getHeight() / 4);

    }
}
