package FomInteractions.Events;

public class FederationEvent {

    private EventType type;

    public FederationEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }
}
