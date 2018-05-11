package FomInteractions;

import hla.rti1516e.LogicalTime;

public class ClientInteraction extends Interaction{

    private int clientNumber;

    public ClientInteraction(InteractionType type, LogicalTime time, int clientNumber) {
        super(type, time);
        this.clientNumber = clientNumber;
    }

    public int getClientNumber() {
        return clientNumber;
    }
}
