package rainmaker.gameobject_collections;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import rainmaker.services.RandomGenerator;
import rainmaker.Updatable;
import rainmaker.gameobjects.Pond;

import java.util.ArrayList;

public class Ponds extends GameObjectPane<Pond> implements Updatable {

    private double avgWaterLevel = 0;
    private static final int TOTAL_PONDS = 3;
    private final Bounds windowBounds;
    private final ArrayList<Bounds> obstacles;

    public Ponds(Bounds bounds, ArrayList<Bounds> obstacles) {
        this.windowBounds = bounds;
        this.obstacles = obstacles;
        while (getChildren().size() < TOTAL_PONDS) {
            Pond pond = Pond.generatePond();
            if (overlapsAnotherPond(pond) || overlapsObstacle(pond)
                    || closeToAnotherPond(pond)) continue;
            add(pond);
        }
    }

    private boolean closeToAnotherPond(Pond pond) {
        for (Pond p : this) {
            if (distanceBetween(p, pond) < 200) return true;
        }
        return false;
    }

    private double distanceBetween(Pond pond1, Pond pond2) {
        double x1 = pond1.getBoundsInParent().getCenterX();
        double y1 = pond1.getBoundsInParent().getCenterY();

        double x2 = pond2.getBoundsInParent().getCenterX();
        double y2 = pond2.getBoundsInParent().getCenterY();

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private boolean overlapsObstacle(Pond pond) {
        for (Bounds obstacle : obstacles) {
            if (pond.getBoundsInParent().intersects(obstacle)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsAnotherPond(Pond pond) {
        for (Pond p : this) {
            if (p.interest(pond)) {
                return true;
            }
        }
        return false;
    }

    public static double convertAreaToRadius(double area) {
        return Math.sqrt(area / Math.PI);
    }

    private Point2D randomSpawnPoint(double radius) {
        return new Point2D(
                RandomGenerator.getRandomDouble(radius, windowBounds.getWidth() - radius),
                RandomGenerator.getRandomDouble(radius, windowBounds.getHeight() - radius)
        );
    }

    @Override
    public void update(double FrameTime) {
        // not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            ((Pond) getChildren().get(i)).update(FrameTime);
        }

        // update average water level
        double totalWaterLevel = 0;
        for (Pond pond : this) {
            totalWaterLevel += pond.getCurrentWaterLevel();
        }
        avgWaterLevel = totalWaterLevel / getChildren().size();
    }

    public double getAvgWaterLevel() {
        return avgWaterLevel;
    }
}
