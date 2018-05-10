package FomInteractions;

public class TableInteraction extends Interaction {

    private int tableNumber;

    public TableInteraction(InteractionType type, int tableNumber) {
        super(type);
        this.tableNumber = tableNumber;
    }

    public int getTableNumber() {
        return tableNumber;
    }
}
