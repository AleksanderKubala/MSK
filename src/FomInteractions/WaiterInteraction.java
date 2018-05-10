package FomInteractions;

public class WaiterInteraction extends ClientInteraction {

    private int waiterNumber;

    public WaiterInteraction(int clientNumber, int waiterNumber) {
        super(clientNumber);
        this.waiterNumber = waiterNumber;
    }

    public int getWaiterNumber() {
        return waiterNumber;
    }
}
