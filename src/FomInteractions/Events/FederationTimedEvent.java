package FomInteractions.Events;

import hla.rti1516e.LogicalTime;

public abstract class FederationTimedEvent extends FederationEvent{

    private LogicalTime time;


    public FederationTimedEvent(LogicalTime time, EventType type) {
        super(type);
        this.time = time;
    }

    public LogicalTime getTime() {
        return time;
    }

}
