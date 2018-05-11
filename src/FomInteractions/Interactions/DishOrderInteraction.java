package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import hla.rti1516e.LogicalTime;

public class DishOrderInteraction extends ClientInteraction{

    private int dishNumber;

    public DishOrderInteraction(LogicalTime time, EventType type, int clientNumber, int dishNumber) {
        super(time, type, clientNumber);
        this.dishNumber = dishNumber;
    }

    public int getDishNumber() {
        return dishNumber;
    }
}
