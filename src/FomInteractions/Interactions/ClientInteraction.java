package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import FomInteractions.Events.TimedEvent;
import hla.rti1516e.LogicalTime;

public class ClientInteraction extends TimedEvent {

    private int clientNumber;

    public ClientInteraction(LogicalTime time, EventType type, int clientNumber) {
        super(time, type);
        this.clientNumber = clientNumber;
    }

    public int getClientNumber() {
        return clientNumber;
    }
}
