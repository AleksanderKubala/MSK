package Federates.FederatesInternal;

import FomInteractions.Events.FederationTimedEvent;

public class Client {

    private int clientNumber;
    private FederationTimedEvent clientImpatient;

    public Client(int clientNumber, FederationTimedEvent clientImpatient) {
        this.clientNumber = clientNumber;
        this.clientImpatient = clientImpatient;
    }

    public int getClientNumber() {
        return clientNumber;
    }

    public FederationTimedEvent getClientImpatient() {
        return clientImpatient;
    }
}
