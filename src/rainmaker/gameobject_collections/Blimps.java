package rainmaker.gameobject_collections;

import javafx.animation.AnimationTimer;
import rainmaker.Game;
import rainmaker.Updatable;
import rainmaker.gameobjects.Blimp;
import rainmaker.services.CoinSide;
import rainmaker.services.RandomGenerator;

public class Blimps extends GameObjectPane<Blimp> implements Updatable {
    private static final int MAX_BLIMPS = 2;
    private static final int MIN_BLIMPS = 0;

    public Blimps() {
        AnimationTimer timer = new AnimationTimer() {
            double old = -1;

            @Override
            public void handle(long now) {
                if (old < 0) {
                    old = now;
                    return;
                }
                double frameTime = (now - old) / 1e9;
                old = now;
                update(frameTime);
            }
        };
        timer.start();
    }

    private double elapsed = 0;
    @Override
    public void update(double frameTime) {
        // loop through all blimps, remove if dead
        for (int i = 0; i < getChildren().size(); i++) {
            Blimp blimp = (Blimp) getChildren().get(i);
            blimp.update(frameTime);
            if (blimp.isDead()) {
                remove(blimp);
                Game.getInstance().handleBlimpRemoved(blimp);
            }
        }

        if (getChildren().size() < MAX_BLIMPS) {
            elapsed += frameTime;
            if (elapsed > 3) {
                elapsed = 0;
                if (RandomGenerator.flipCoin() == CoinSide.HEADS) {
                    Blimp blimp = Blimp.getRandomBlimp();
                    add(blimp);
                    Game.getInstance().handleBlimpAdded(blimp);
                }
            }
        }


    }
}

