package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import FomInteractions.Events.TimedEvent;
import hla.rti1516e.LogicalTime;

public class TableInteraction extends TimedEvent {

    private int tableNumber;

    public TableInteraction(LogicalTime time, EventType type, int tableNumber) {
        super(time, type);
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
