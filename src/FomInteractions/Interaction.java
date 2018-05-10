package FomInteractions;

public abstract class Interaction {

    private InteractionType type;

    Interaction(InteractionType type) {
        this.type = type;
    }

    public InteractionType getType() {
        return type;
    }
}
