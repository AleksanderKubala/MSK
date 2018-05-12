package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationTimedEvent;
import hla.rti1516e.LogicalTime;

public class TableInteraction extends FederationTimedEvent {

    private int tableNumber;

    public TableInteraction(LogicalTime time, EventType type, int tableNumber) {
        super(time, type);
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
