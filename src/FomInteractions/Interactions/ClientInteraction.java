package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationTimedEvent;
import hla.rti1516e.LogicalTime;

public class ClientInteraction extends FederationTimedEvent {

    private int clientNumber;

    public ClientInteraction(LogicalTime time, EventType type, int clientNumber) {
        super(time, type);
        this.clientNumber = clientNumber;
    }

    public int getClientNumber() {
        return clientNumber;
    }
}
