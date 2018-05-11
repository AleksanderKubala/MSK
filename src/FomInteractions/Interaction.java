package FomInteractions;

import hla.rti1516e.LogicalTime;

import java.util.Comparator;

public abstract class Interaction{

    private InteractionType type;
    private LogicalTime time;

    Interaction(InteractionType type, LogicalTime time) {
        this.type = type;
        this.time = time;
    }

    public InteractionType getType() {
        return type;
    }

    public LogicalTime getTime() {
        return time;
    }
}
