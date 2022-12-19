package rainmaker.gameobject_collections;

import javafx.geometry.Bounds;
import rainmaker.Updatable;
import rainmaker.gameobjects.DistanceLine;
import rainmaker.gameobjects.Pond;

import java.util.ArrayList;

public class Ponds extends GameObjectPane<Pond> implements Updatable {
    private static final int TOTAL_PONDS = 3;
    private final ArrayList<Bounds> obstacles;

    public Ponds(ArrayList<Bounds> obstacles) {
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
        return DistanceLine.getDistance(pond1, pond2);
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
            if (p.intersects(pond)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update(double FrameTime) {
        // not using iterator to avoid concurrent modification exception
        for (int i = 0; i < getChildren().size(); i++) {
            ((Pond) getChildren().get(i)).update(FrameTime);
        }
    }

    public double getAvgWaterLevel() {
        double total = 0;
        for (Pond pond : this) {
            total += pond.getCurrentWaterLevel();
        }
        return total / getChildren().size();
    }
}
