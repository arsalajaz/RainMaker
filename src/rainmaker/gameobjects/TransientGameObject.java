package rainmaker.gameobjects;

import rainmaker.Game;
import rainmaker.services.Vector;

public class TransientGameObject extends GameObject {
    protected Vector position;
    private double speed;
    private double heading;
    private Vector velocity;
    private State state = new CreatedState();

    public TransientGameObject(Vector initPos, double speed, double heading) {
        super();
        this.position = initPos;
        this.speed = speed;
        this.heading = heading;

        this.velocity = new Vector(0, 0);

        translate(initPos.getX(), initPos.getY());
    }

    public TransientGameObject(Vector initPos) {
        this(initPos, 0, 0);
    }

    protected void setHeading(double heading) {
        this.heading = heading;
    }

    protected void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getSpeed() {
        return speed;
    }

    public double getHeading() {
        return heading;
    }

    public boolean isDead() {
        return state instanceof DeadState;
    }

    private boolean isWithinBounds() {
        double cloudWidth = getLayoutBounds().getWidth();
        double cloudHeight = getLayoutBounds().getHeight();
        return position.getX() > -cloudWidth / 2 &&
                position.getX() < Game.GAME_WIDTH + cloudWidth / 2 &&
                position.getY() > -cloudHeight / 2 &&
                position.getY() < Game.GAME_HEIGHT + cloudHeight / 2;
    }

    private boolean shouldDie() {
        double cloudWidth = getLayoutBounds().getWidth();
        double cloudHeight = getLayoutBounds().getHeight();
        return position.getX() < -cloudWidth / 2 && velocity.getX() < 0 ||
                position.getX() > Game.GAME_WIDTH + cloudWidth / 2 &&
                        velocity.getX() > 0 ||
                position.getY() < -cloudHeight / 2 &&
                        velocity.getY() < 0 ||
                position.getY() > Game.GAME_HEIGHT + cloudHeight / 2 &&
                        velocity.getY() > 0;
    }

    protected void move(double frameTime) {

        state.nextFrame(frameTime);
        debug(frameTime);
    }

    double elapsedTime = 0;
    private void debug(double frameTime) {
        elapsedTime += frameTime;

        //if not dead within 60 seconds, log everything
        if(elapsedTime >= 60 && !(state instanceof DeadState)) {
            // log all the variables and states and private methods
            System.out.println("TransientGameObject: " + this);
            System.out.println("TransientGameObject: " + this.position);
            System.out.println("TransientGameObject: " + this.speed);
            System.out.println("TransientGameObject: " + this.heading);
            System.out.println("TransientGameObject: " + this.velocity);
            System.out.println("TransientGameObject: " + this.state);
            System.out.println("TransientGameObject: " + this.state.getClass());
            //log private method results
            System.out.println("TransientGameObject: " + isWithinBounds());
            System.out.println("TransientGameObject: " + shouldDie());
        }
    }

    abstract class State {
        public abstract void nextFrame(double frameTime);
        protected void move(double frameTime) {
            velocity =
                    new Vector(speed*Game.UNIVERSAL_SPEED_MULTIPLIER,
                            Math.toRadians(heading), true);
            velocity = velocity.multiply(frameTime);
            position = position.add(velocity);
            translate(position.getX(), position.getY());
        }
    }

    class CreatedState extends State {
        public void nextFrame(double frameTime) {
            if (isWithinBounds()) {
                state = new InViewState();
            }

            if(shouldDie()) {
                state = new DeadState();
            }

            move(frameTime);
        }
    }

    class InViewState extends State {
        public void nextFrame(double frameTime) {
            if (shouldDie()) {
                state = new DeadState();
            }
            move(frameTime);
        }
    }

    class DeadState extends State {
        public void nextFrame(double frameTime) {
            // Do nothing
        }
    }
}

