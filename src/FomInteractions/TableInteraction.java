package FomInteractions;

import hla.rti1516e.LogicalTime;

public class TableInteraction extends Interaction {

    private int tableNumber;

    public TableInteraction(InteractionType type, LogicalTime time, int tableNumber) {
        super(type, time);
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
