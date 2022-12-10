package rainmaker;

import javafx.animation.AnimationTimer;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import rainmaker.gameobject_collections.*;
import rainmaker.gameobjects.*;
import rainmaker.services.KeyPressTimer;

import java.util.ArrayList;
import java.util.HashSet;

public class Game extends Pane implements CloudsListener {
    private final Bounds gameBounds =
            new BoundingBox(0, 0, GameApp.GAME_WIDTH, GameApp.GAME_HEIGHT);
    private final Vector COPTER_INITIAL_POS =
            new Vector(gameBounds.getWidth() / 2, 100);
    private final Point2D PAD_INITIAL_POSITION =
            new Point2D(gameBounds.getHeight() / 2, 100);
    private static final double PAD_RADIUS = GameApp.GAME_WIDTH / 14;
    private final Runnable stageClose;
    private final AnimationTimer animationTimer;
    HashSet<KeyCode> keysDown = new HashSet<>();
    private Helicopter helicopter;
    private Helipad helipad;
    private Clouds clouds;
    private Ponds ponds;

    private KeyPressTimer upKeyTimer = new KeyPressTimer(KeyCode.UP, 50);
    private KeyPressTimer downKeyTimer = new KeyPressTimer(KeyCode.DOWN, 50);
    private KeyPressTimer leftKeyTimer = new KeyPressTimer(KeyCode.LEFT, 100);
    private KeyPressTimer rightKeyTimer = new KeyPressTimer(KeyCode.RIGHT, 100);
    private KeyPressTimer spaceKeyTimer = new KeyPressTimer(KeyCode.SPACE, 50);
    private BoundingBoxPane boundingBoxes;
    private DistanceLinesPane distanceLines;

    public Game(Runnable stageClose) {
        setScaleY(-1);
        this.stageClose = stageClose;
        clouds = new Clouds();
        clouds.addListener(this);
        init();

        upKeyTimer.setKeyPressAction(() -> helicopter.speedUp());
        downKeyTimer.setKeyPressAction(() -> helicopter.speedDown());
        leftKeyTimer.setKeyPressAction(() -> helicopter.turnLeft());
        rightKeyTimer.setKeyPressAction(() -> helicopter.turnRight());
        spaceKeyTimer.setKeyPressAction(() -> seedClouds());

        animationTimer = new AnimationTimer() {
            double old = -1;

            @Override
            public void handle(long now) {
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


    private void update(double frameTime) {
        helicopter.update(frameTime);
        ponds.update(frameTime);
        rain(frameTime);
    }

    private void rain(double frameTime) {
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

    public void handleCopterCrash() {
        animationTimer.stop();
        String msg = "Game Over! Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                stageClose.run();
            }
        });
        alert.show();
    }

    public void handleCopterLanded() {
        if (ponds.getAvgWaterLevel() < 80) return;

        animationTimer.stop();

        double score = (ponds.getAvgWaterLevel() / 100) * (double) helicopter.getFuel();
        String msg = "You Win! Your score is " + (int) score + ". " +
                "Would you like to play again?";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.YES, ButtonType.NO);
        alert.setOnHidden(e -> {
            if (alert.getResult() == ButtonType.YES) {
                init();
                animationTimer.start();
            } else {
                stageClose.run();
            }
        });
        alert.show();
    }

    private void init() {
        getChildren().clear();
        Helicopter.resetSound();

        helipad = new Helipad(PAD_RADIUS, PAD_INITIAL_POSITION);

        helicopter = new Helicopter(5000, COPTER_INITIAL_POS, 25000);
        helicopter.setOnCrash(this::handleCopterCrash);
        helicopter.setOnLandedAction(this::handleCopterLanded);
        helicopter.setLandingLocation(helipad);

        // A pond won't spawn on the helipad, can be used to add more obstacles
        ArrayList<Bounds> pondObstacles = new ArrayList<>();
        pondObstacles.add(helipad.getBoundsInParent());
        ponds = new Ponds(gameBounds, pondObstacles);

        ImageBackground background = new ImageBackground(GameApp.GAME_WIDTH,
                GameApp.GAME_HEIGHT);

        boundingBoxes = new BoundingBoxPane();
        distanceLines = new DistanceLinesPane();

        boundingBoxes.addAll(helicopter, helipad);
        for (Pond pond : ponds) {
            boundingBoxes.add(pond);
        }
        for (Cloud cloud : clouds) {
            boundingBoxes.add(cloud);
            for (Pond pond : ponds) {
                distanceLines.add(cloud, pond);
            }
        }


        getChildren().addAll(background, helipad, ponds, clouds, helicopter);
        getChildren().addAll(boundingBoxes, distanceLines);
    }

    public void handleKeyPressed(KeyEvent event) {
        keysDown.add(event.getCode());

        if(isKeyDown(KeyCode.UP)) upKeyTimer.keyPressed();
        if(isKeyDown(KeyCode.DOWN)) downKeyTimer.keyPressed();
        if(isKeyDown(KeyCode.RIGHT)) rightKeyTimer.keyPressed();
        if(isKeyDown(KeyCode.LEFT)) leftKeyTimer.keyPressed();
        if (isKeyDown(KeyCode.SPACE)) spaceKeyTimer.keyPressed();

        if (event.getCode() == KeyCode.I) {
            if (helicopter.getState() == helicopter.getOffState())
                helicopter.takeOff();
            else
                helicopter.land();
        }
        if (event.getCode() == KeyCode.B) boundingBoxes.toggleVisibility();
        if (event.getCode() == KeyCode.D) distanceLines.toggleVisibility();
        if (event.getCode() == KeyCode.R) init();
        if (event.getCode() == KeyCode.C) System.gc();

    }

    private void seedClouds() {
        for (Cloud cloud : clouds) {
            if (helicopter.interest(cloud)) {
                helicopter.seedCloud(cloud);
            }
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        keysDown.remove(event.getCode());

        if(event.getCode() == KeyCode.UP) upKeyTimer.keyReleased();
        if(event.getCode() == KeyCode.DOWN) downKeyTimer.keyReleased();
        if(event.getCode() == KeyCode.RIGHT) rightKeyTimer.keyReleased();
        if(event.getCode() == KeyCode.LEFT) leftKeyTimer.keyReleased();
        if (event.getCode() == KeyCode.SPACE) spaceKeyTimer.keyReleased();
    }

    private boolean isKeyDown(KeyCode k) {
        return keysDown.contains(k);
    }

    @Override
    public void onCloudDestroyed(Cloud cloud) {
        boundingBoxes.remove(cloud);
        distanceLines.removeIfInvolves(cloud);
    }

    @Override
    public void onCloudSpawned(Cloud cloud) {
        boundingBoxes.add(cloud);
        for (Pond pond : ponds) {
            distanceLines.add(cloud, pond);
        }
    }
}
