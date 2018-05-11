package FomInteractions;

import hla.rti1516e.LogicalTime;

public class WaiterInteraction extends ClientInteraction {

    private int waiterNumber;

    public WaiterInteraction(InteractionType type, LogicalTime time, int clientNumber, int waiterNumber) {
        super(type, time, clientNumber);
        this.waiterNumber = waiterNumber;
    }

    public int getWaiterNumber() {
        return waiterNumber;
    }
}
