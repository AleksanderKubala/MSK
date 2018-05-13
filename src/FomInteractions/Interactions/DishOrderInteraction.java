package FomInteractions.Interactions;

import FomInteractions.Events.EventType;
import hla.rti1516e.LogicalTime;

public class DishOrderInteraction extends ClientInteraction{

    private int dishNumber;
    private int chefNumber;

    public DishOrderInteraction(LogicalTime time, EventType type, int clientNumber, int dishNumber, int chefNumber) {
        super(time, type, clientNumber);
        this.dishNumber = dishNumber;
        this.chefNumber = chefNumber;
    }

    public int getDishNumber() {
        return dishNumber;
    }

    public int getChefNumber() {
        return chefNumber;
    }
}
