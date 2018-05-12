package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationTimedEvent;
import hla.rti1516e.LogicalTime;

public class TableInteraction extends ClientInteraction {

    private int tableNumber;

    public TableInteraction(LogicalTime time, EventType type, int clientNumber, int tableNumber) {
        super(time, type, clientNumber);
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
