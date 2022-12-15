package rainmaker.gameobjects;

public class TransientGameObject extends GameObject {
    private State state;
    private State createdState;
    private State inViewState;
    private State deadState;


    public TransientGameObject() {
        super();

        createdState = new CreatedState();
        inViewState = new InViewState();
        deadState = new DeadState();

        state = createdState;
    }

    public boolean isDead() {
        return state == deadState;
    }

    public void update() {
        state.update();
    }
}

abstract class State {
    public abstract void update();
}

class CreatedState extends State {
    @Override
    public void update() {
        System.out.println("Created");
    }
}

class InViewState extends State {
    @Override
    public void update() {
        System.out.println("In View");
    }
}

class DeadState extends State {
    @Override
    public void update() {
        System.out.println("Dead");
    }
}