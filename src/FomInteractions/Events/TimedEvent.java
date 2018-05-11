package FomInteractions.Events;

import hla.rti1516e.LogicalTime;

public abstract class TimedEvent {

    private LogicalTime time;
    private EventType type;

    public TimedEvent(LogicalTime time, EventType type) {
        this.time = time;
        this.type = type;
    }

    public LogicalTime getTime() {
        return time;
    }

    public EventType getType() {
        return type;
    }
}
