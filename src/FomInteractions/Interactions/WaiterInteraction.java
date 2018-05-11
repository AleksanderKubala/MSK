package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import hla.rti1516e.LogicalTime;

public class WaiterInteraction extends ClientInteraction {

    private int waiterNumber;

    public WaiterInteraction(LogicalTime time, EventType type, int clientNumber, int waiterNumber) {
        super(time, type, clientNumber);
        this.waiterNumber = waiterNumber;
    }

    public int getWaiterNumber() {
        return waiterNumber;
    }
}
