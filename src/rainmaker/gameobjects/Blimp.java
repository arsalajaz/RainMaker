package rainmaker.gameobjects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import rainmaker.Game;
import rainmaker.Updatable;
import rainmaker.services.RandomGenerator;
import rainmaker.services.Vector;

enum BlimpState {
    CREATED,
    IN_VIEW,
    DEAD
}

public class Blimp extends GameObject implements Updatable {
    private static final double BODY_WIDTH = 180;
    private static final double BODY_HEIGHT = 70;
    private static final double PROPELLER_WIDTH = 30;
    private static final double PROPELLER_HEIGHT = 80;
    private GameText fuelText = new GameText();
    private BlimpState state = BlimpState.CREATED;
    private Vector position;
    private Vector velocity;
    private final double fuel;
    private final Rectangle blimpShape;
    private static final Image[] propellerFrames = new Image[7];
    private final ImageView propellerView;
    private int propellerIndex = 0;

    public Blimp(double fuel, Vector spawnPosition) {
        super();

        for (int i = 0; i < propellerFrames.length; i++) {
            String path = "/resources/blimp_propeller/" + i + ".png";
            propellerFrames[i] = new Image(path);
        }

        this.fuel = fuel;

        ImagePattern img = new ImagePattern(new Image("/resources/blimp.png"));
        blimpShape = new Rectangle();
        blimpShape.setFill(img);
        blimpShape.setWidth(BODY_WIDTH);
        blimpShape.setHeight(BODY_HEIGHT);
        blimpShape.setTranslateX(-BODY_WIDTH / 2);
        blimpShape.setTranslateY(-BODY_HEIGHT / 2);

        fuelText.setFill(Color.YELLOW);
        fuelText.setText(String.valueOf((int) fuel));
        fuelText.setTranslateX(-fuelText.getBoundsInParent().getWidth() / 2);
        fuelText.setTranslateY(fuelText.getBoundsInParent().getHeight() / 2);

        propellerView = new ImageView(propellerFrames[0]);
        propellerView.setFitWidth(PROPELLER_WIDTH);
        propellerView.setFitHeight(PROPELLER_HEIGHT);
        propellerView.setTranslateX(-blimpShape.getWidth() / 2 - 15);
        propellerView.setTranslateY(-blimpShape.getHeight() / 2 - 5);

        getChildren().addAll(blimpShape, propellerView, fuelText);
        shapes.add(blimpShape);

        position = spawnPosition;
        setTranslateX(position.getX());
        setTranslateY(position.getY());

        setupPropellerAnimation();
    }

    public static Blimp getRandomBlimp() {
        double fuel = RandomGenerator.getRandomDouble(2000, 10000);
        fuel = Math.round(fuel / 1000) * 1000;

        double x, y;
        x = -(BODY_WIDTH + PROPELLER_WIDTH);
        y = RandomGenerator.getRandomDouble(BODY_HEIGHT / 2,
                Game.GAME_HEIGHT-BODY_HEIGHT / 2);

        Vector spawnPosition = new Vector(x, y);
        return new Blimp(fuel, spawnPosition);
    }

    private void setupPropellerAnimation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(30),
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

    @Override
    public void update(double FrameTime) {
        if (state == BlimpState.DEAD) {
            return;
        }

        fuelText.setText(String.valueOf((int) fuel));
        fuelText.setTranslateX(-fuelText.getBoundsInParent().getWidth() / 2);
        fuelText.setTranslateY(fuelText.getBoundsInParent().getHeight() / 2);

        velocity = new Vector(100, 0, true);
        velocity = velocity.multiply(FrameTime);
        position = position.add(velocity);

        setTranslateX(position.getX());
        setTranslateY(position.getY());

        if (state != BlimpState.IN_VIEW && isWithinBounds()) {
            state = BlimpState.IN_VIEW;
        }

        if (state != BlimpState.DEAD && shouldDie()) {
            state = BlimpState.DEAD;
        }
    }

    private boolean isWithinBounds() {
        double cloudWidth = getLayoutBounds().getWidth();
        double cloudHeight = getLayoutBounds().getHeight();
        return position.getX() > cloudWidth / 2 &&
                position.getX() < Game.GAME_WIDTH - cloudWidth / 2 &&
                position.getY() > cloudHeight / 2 &&
                position.getY() < Game.GAME_HEIGHT - cloudHeight / 2;
    }

    private boolean shouldDie() {
        double cloudWidth = getLayoutBounds().getWidth();
        double cloudHeight = getLayoutBounds().getHeight();
        return position.getX() < -cloudWidth / 2 && velocity.getX() < 0 ||
                position.getX() > Game.GAME_WIDTH + cloudWidth / 2 &&
                        velocity.getX() > 0 ||
                position.getY() < -cloudHeight / 2 && velocity.getY() < 0 ||
                position.getY() > Game.GAME_HEIGHT + cloudHeight / 2 &&
                        velocity.getY() > 0;
    }

    public boolean isDead() {
        return state == BlimpState.DEAD;
    }
}

