package rainmaker.gameobjects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.AudioClip;
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

enum BlimpState {
    CREATED,
    IN_VIEW,
    DEAD
}

public class Blimp extends TransientGameObject implements Updatable {
    private static final double BODY_WIDTH = 180;
    private static final double BODY_HEIGHT = 70;
    private static final double PROPELLER_WIDTH = 30;
    private static final double PROPELLER_HEIGHT = 80;
    private final double SPEED = RandomGenerator.getRandomDouble(2,4);
    private final AudioClip REFUEL_SOUND = new AudioClip(
            new File("src/resources/refuel_sound.wav").toURI().toString());

    private final double HEADING = 0;
    private GameText fuelText = new GameText();

    private Circle refuelingLight = new Circle(5, Color.RED);
    private double fuel;
    private static final Image BODY_IMG = new Image("/resources/blimp.png");
    private Rectangle BODY_SHAPE;
    private boolean refueling = false;
    private static final Image[] propellerFrames = new Image[7];
    private final ImageView propellerView;
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
                Game.GAME_HEIGHT-BODY_HEIGHT / 2);

        Vector spawnPosition = new Vector(x, y);
        return new Blimp(fuel, spawnPosition);
    }

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
            if(!REFUEL_SOUND.isPlaying()) {
                REFUEL_SOUND.play();
            }
        } else {
            refuelingLight.setFill(Color.RED);
            if(REFUEL_SOUND.isPlaying()) {
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
    }

    public double siphonFuel(double amount) {
        if (fuel < amount) {
            amount = fuel;
        }
        fuel -= amount;
        return amount;
    }
}

