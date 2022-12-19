package rainmaker.gameobjects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import rainmaker.Game;
import rainmaker.Updatable;
import rainmaker.services.RandomGenerator;
import rainmaker.services.Vector;

import java.io.File;

public class Blimp extends TransientGameObject implements Updatable {
    private static final double BODY_WIDTH = 180;
    private static final double BODY_HEIGHT = 70;
    private static final double PROPELLER_WIDTH = 30;
    private static final double PROPELLER_HEIGHT = 80;
    private static final Media REFUEL_SOUND_MEDIA = new Media(
            new File("src/resources/refuel_sound.wav").toURI().toString());
    private static final Media ENGINE_SOUND_MEDIA = new Media(
            new File("src/resources/blimp_engine_sound.wav").toURI().toString());
    private static final Image BODY_IMG = new Image("/blimp_body_img.png");
    private static final Image[] propellerFrames = new Image[7];
    private final double SPEED = RandomGenerator.getRandomDouble(3, 5);
    private final MediaPlayer REFUEL_SOUND =
            new MediaPlayer(REFUEL_SOUND_MEDIA);
    private final MediaPlayer ENGINE_SOUND =
            new MediaPlayer(ENGINE_SOUND_MEDIA);
    private final double HEADING = 0;
    private final ImageView propellerView;
    private double distanceFromMainPlayer;
    private final GameText fuelText = new GameText();
    private final Circle refuelingLight = new Circle(5, Color.RED);
    private double fuel;
    private final Rectangle BODY_SHAPE;
    private boolean refueling = false;
    private int propellerIndex = 0;

    public Blimp(double fuel, Vector spawnPosition) {
        super(spawnPosition);

        for (int i = 0; i < propellerFrames.length; i++) {
            String path = "/resources/blimp_propeller/" + i + ".png";
            propellerFrames[i] = new Image(path);
        }

        this.fuel = fuel;

        ImagePattern img = new ImagePattern(BODY_IMG);
        BODY_SHAPE = new Rectangle();
        BODY_SHAPE.setFill(img);
        BODY_SHAPE.setWidth(BODY_WIDTH);
        BODY_SHAPE.setHeight(BODY_HEIGHT);
        BODY_SHAPE.setTranslateX(-BODY_WIDTH / 2);
        BODY_SHAPE.setTranslateY(-BODY_HEIGHT / 2);


        fuelText.setFill(Color.YELLOW);
        fuelText.setText(String.valueOf((int) fuel));
        fuelText.setTranslateX(-fuelText.getBoundsInParent().getWidth() / 2);
        fuelText.setTranslateY(fuelText.getBoundsInParent().getHeight() / 2);

        refuelingLight.setTranslateX(BODY_WIDTH / 2 - 20);
        refuelingLight.setTranslateY(0);

        propellerView = new ImageView(propellerFrames[0]);
        propellerView.setFitWidth(PROPELLER_WIDTH);
        propellerView.setFitHeight(PROPELLER_HEIGHT);
        propellerView.setTranslateX(-BODY_SHAPE.getWidth() / 2 - 15);
        propellerView.setTranslateY(-BODY_SHAPE.getHeight() / 2 - 5);

        setHeading(HEADING);
        setSpeed(SPEED);

        getChildren().addAll(BODY_SHAPE, propellerView, fuelText,
                refuelingLight);
        shapes.add(BODY_SHAPE);

        setupPropellerAnimation();
    }

    public static Blimp getRandomBlimp() {
        double fuel = RandomGenerator.getRandomDouble(5000, 10000);
        fuel = Math.round(fuel / 1000) * 1000;

        double x, y;
        x = -(BODY_WIDTH + PROPELLER_WIDTH);
        y = RandomGenerator.getRandomDouble(BODY_HEIGHT / 2,
                Game.GAME_HEIGHT - BODY_HEIGHT / 2);

        Vector spawnPosition = new Vector(x, y);
        return new Blimp(fuel, spawnPosition);
    }

    /**
     * Uses this distance to play the engine sound at the correct volume.
     */
    public void updateDistanceFromMainPlayer(double distance) {
        distanceFromMainPlayer = distance;
        updateEngineAudioVolume();
    }

    /**
     * Every 50ms, the propeller image displayed is changed to the next one
     */
    private void setupPropellerAnimation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50),
                e -> {
                    propellerIndex++;
                    if (propellerIndex >= propellerFrames.length) {
                        propellerIndex = 0;
                    }
                    propellerView.setImage(propellerFrames[propellerIndex]);
                }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void isRefueling(boolean refueling) {
        this.refueling = refueling;
        if (refueling) {
            refuelingLight.setFill(Color.GREEN);
            if (!REFUEL_SOUND.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                REFUEL_SOUND.play();
            }
        } else {
            refuelingLight.setFill(Color.RED);
            if (REFUEL_SOUND.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                REFUEL_SOUND.stop();
            }
        }
    }

    @Override
    public void update(double frameTime) {
        fuelText.setText(String.valueOf((int) fuel));
        fuelText.setTranslateX(-fuelText.getBoundsInParent().getWidth() / 2);
        fuelText.setTranslateY(fuelText.getBoundsInParent().getHeight() / 2);

        move(frameTime);
        checkAndPlayIfAudioShouldPlay();
    }

    //update engine audio volume based on distance from main player
    private void updateEngineAudioVolume() {
        double volume = 1 - (distanceFromMainPlayer / Game.GAME_WIDTH);
        if (volume < 0) {
            volume = 0;
        }
        ENGINE_SOUND.setVolume(volume);
    }


    private void checkAndPlayIfAudioShouldPlay() {
        if (isDead()) {
            if (ENGINE_SOUND.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                ENGINE_SOUND.stop();
            }
            if (REFUEL_SOUND.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                REFUEL_SOUND.stop();
            }
        } else {
            if (!ENGINE_SOUND.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                ENGINE_SOUND.play();
            }
        }
    }

    public double siphonFuel(double amount) {
        if (fuel < amount) {
            amount = fuel;
        }
        fuel -= amount;
        return amount;
    }
}

