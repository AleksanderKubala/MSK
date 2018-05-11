package FomInteractions;

import hla.rti1516e.LogicalTime;

public class DishOrderInteraction extends ClientInteraction{

    private int dishNumber;

    public DishOrderInteraction(InteractionType type, LogicalTime time, int clientNumber, int dishNumber) {
        super(type, time, clientNumber);
        this.dishNumber = dishNumber;
    }

    public int getDishNumber() {
        return dishNumber;
    }
}
