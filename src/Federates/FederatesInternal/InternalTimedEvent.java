package Federates.FederatesInternal;

import hla.rti1516e.LogicalTime;

public class InternalTimedEvent {

    LogicalTime time;

    public InternalTimedEvent(LogicalTime time) {
        this.time = time;
    }

    public LogicalTime getTime() {
        return time;
    }
}
