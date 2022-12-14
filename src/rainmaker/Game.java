package rainmaker;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import rainmaker.gameobject_collections.*;
import rainmaker.gameobjects.*;
import rainmaker.services.Vector;

import java.util.ArrayList;

public class Game extends Pane {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 800;
    public static final double UNIVERSAL_SPEED_MULTIPLIER = 30;
    private static final Game INSTANCE = new Game();
    private final double PAD_RADIUS = GAME_WIDTH / 14;
    private final Vector PAD_INIT_POS = new Vector(GAME_WIDTH / 2, 100);
    private final Vector COPTER_INIT_POS = PAD_INIT_POS;
    private final AnimationTimer animationTimer;
    private final Pane groundObjects = new Pane();
    private final Pane airObjects = new Pane();
    private final Clouds clouds;
    private final Blimps blimps;
    private final BoundingBoxPane boundingBoxes = new BoundingBoxPane();
    private final DistanceLinesPane distanceLines = new DistanceLinesPane();
    private final Wind wind = new Wind();
    Runnable onCloseRequest;
    private Helicopter helicopter;
    private Helipad helipad;
    private Ponds ponds;

    private Game() {
        setScaleY(-1);

        // Singletons, can be cleared on reset, no need to recreate
        clouds = new Clouds();
        blimps = new Blimps();

        init();

        animationTimer = new AnimationTimer() {
            double old = -1;
            int frameCount = 0;

            @Override
            public void handle(long now) {
                frameCount++;
                if (old < 0) {
                    old = now;
                    return;
                }
                double FrameTime = (now - old) / 1e9;
                old = now;
                update(FrameTime);
            }
        };

        animationTimer.start();
    }

    public static Game getInstance() {
        return INSTANCE;
    }

    public void speedUpHelicopter() {
        helicopter.speedUp();
    }

    public void speedDownHelicopter() {
        helicopter.speedDown();
    }

    public void turnLeftHelicopter() {
        helicopter.turnLeft();
    }

    public void turnRightHelicopter() {
        helicopter.turnRight();
    }

    public void toggleBoundingBoxes() {
        boundingBoxes.toggleVisibility();
    }

    public void toggleHelicopterIgnition() {
        helicopter.toggleIgnition();
    }

    public void toggleDistanceLines() {
        distanceLines.toggleVisibility();
    }

    private void update(double frameTime) {
        helicopter.update(frameTime);
        ponds.update(frameTime);
        fillPonds(frameTime);
        checkBlimpHeliRefueling(frameTime);
        provideBlimpsWithHeliDistance();

        // wind, clouds, and blimps have their own timers
    }

    private void provideBlimpsWithHeliDistance() {
        for (Blimp blimp : blimps) {
            double distance = DistanceLine.getDistance(helicopter, blimp);
            blimp.updateDistanceFromMainPlayer(distance);
        }
    }

    private void checkBlimpHeliRefueling(double frameTime) {
        for (Blimp blimp : blimps) {
            boolean isOverBlimp = helicopter.intersects(blimp);
            if (!isOverBlimp) {
                blimp.isRefueling(false);
                continue;
            }
            if (Math.abs(helicopter.getSpeed() - blimp.getSpeed()) > 0.5) {
                blimp.isRefueling(false);
                continue;
            }
            // also check if their heading angle is within 20 degrees
            if (smallestDifferenceBetweenAngles(helicopter.getHeading(),
                    blimp.getHeading()) > 20) {
                blimp.isRefueling(false);
                continue;
            }

            double siphonedFuel = blimp.siphonFuel(frameTime * 1000);
            helicopter.refuel(siphonedFuel);
            blimp.isRefueling(true);
        }
    }

    private double smallestDifferenceBetweenAngles(double a, double b) {
        double difference = Math.abs(a - b);
        if (difference > 180) {
            difference = 360 - difference;
        }
        return difference;
    }

    private void fillPonds(double frameTime) {
        for (Cloud cloud : clouds) {
            if (!cloud.isRaining()) continue;
            for (Pond pond : ponds) {
                int distance = (int) DistanceLine.getDistance(cloud, pond);
                double pondDiameter = pond.getRadius() * 2;
                double maxDistance = pondDiameter * 4;
                if (distance >= maxDistance) continue;
                double saturationProp =
                        (double) cloud.getSaturation() / 100;
                double distanceProp = 1 - (distance / maxDistance);
                pond.addWater(distanceProp * saturationProp * frameTime * 2);
            }
        }
    }

    public void handleCloudAdded(Cloud cloud) {
        boundingBoxes.add(cloud);
        for (Pond pond : ponds) {
            distanceLines.add(cloud, pond);
        }
        wind.registerObserver(cloud);
    }

    public void handleCloudRemoved(Cloud cloud) {
        boundingBoxes.removeFor(cloud);
        distanceLines.removeIfInvolves(cloud);
        wind.removeObserver(cloud);
    }

    public void handleBlimpAdded(Blimp blimp) {
        //draw distance lines between blimp and helicopter
        distanceLines.add(helicopter, blimp);
        boundingBoxes.add(blimp);
    }

    public void handleBlimpRemoved(Blimp blimp) {
        //remove distance lines between blimp and helicopter
        distanceLines.removeIfInvolves(blimp);
        boundingBoxes.removeFor(blimp);
    }

    public void handleCopterFlying() {
        //move the helicopter from the ground to the air pane
        groundObjects.getChildren().remove(helicopter);
        if (!airObjects.getChildren().contains(helicopter)) {
            airObjects.getChildren().add(helicopter);
        }
    }

    public void handleCopterCrash() {
        //move the helicopter from the air to the ground pane
        airObjects.getChildren().remove(helicopter);
        if (!groundObjects.getChildren().contains(helicopter))
            groundObjects.getChildren().add(helicopter);

        animationTimer.stop();
        String msg = "You Lost! Helicopter crashed! Would you like to play " +
                "again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                if (onCloseRequest != null) onCloseRequest.run();
            }
        });
        alert.show();
    }

    public void handleCopterLanded() {
        //move the helicopter from the air to the ground
        airObjects.getChildren().remove(helicopter);
        if (!groundObjects.getChildren().contains(helicopter))
            groundObjects.getChildren().add(helicopter);

        // If the player decides to land before reach the desired score, they
        // will have another chance to fly and seeding the clouds
        if (ponds.getAvgWaterLevel() < 80) return;

        animationTimer.stop();

        double score = (ponds.getAvgWaterLevel() / 100) * helicopter.getFuel();
        String msg = "You Win! Your score is " + (int) score + ". " +
                "Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                if (onCloseRequest != null) onCloseRequest.run();
            }
        });
        alert.show();
    }

    public void init() {
        getChildren().clear();
        groundObjects.getChildren().clear();
        airObjects.getChildren().clear();

        boundingBoxes.clear();
        distanceLines.clear();

        clouds.clear();
        blimps.clear();

        helipad = new Helipad(PAD_RADIUS, PAD_INIT_POS);

        helicopter = new Helicopter(COPTER_INIT_POS, 25000);
        helicopter.setOnCrash(this::handleCopterCrash);
        helicopter.setOnLandedAction(this::handleCopterLanded);
        helicopter.setLandingLocation(helipad);
        helicopter.setOnFlyingAction(this::handleCopterFlying);

        // A pond won't spawn on the helipad, can be used to add more obstacles
        ArrayList<Bounds> pondObstacles = new ArrayList<>();
        pondObstacles.add(helipad.getBoundsInParent());
        ponds = new Ponds(pondObstacles);

        ImageBackground background = new ImageBackground(GAME_WIDTH,
                GAME_HEIGHT);


        boundingBoxes.addAll(helicopter, helipad);
        for (Pond pond : ponds) {
            boundingBoxes.add(pond);
        }

        groundObjects.getChildren().addAll(background, ponds, helipad,
                helicopter);
        airObjects.getChildren().addAll(clouds, blimps);

        getChildren().addAll(background, groundObjects, airObjects);
        getChildren().addAll(boundingBoxes, distanceLines);
    }

    public void seedClouds() {
        for (Cloud cloud : clouds) {
            if (helicopter.intersects(cloud)) {
                helicopter.seedCloud(cloud);
            }
        }
    }

    /**
     * The Runnable will be executed when the player chooses to exit the game
     */
    public void setOnCloseRequest(Runnable onCloseRequest) {
        this.onCloseRequest = onCloseRequest;
    }
}
