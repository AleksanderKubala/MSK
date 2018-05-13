package Federates.FederatesInternal;

import FomInteractions.Events.FederationTimedEvent;

public class Client {

    private int clientNumber;
    private FederationTimedEvent clientImpatient;
    private int mealsEaten;
    private int tableNumber;

    public Client(int clientNumber, FederationTimedEvent clientImpatient) {
        this.clientNumber = clientNumber;
        this.clientImpatient = clientImpatient;
        mealsEaten = 0;
        tableNumber = Integer.MIN_VALUE;
    }

    public void mealEaten() {
        mealsEaten++;
    }

    public int getClientNumber() {
        return clientNumber;
    }

    public FederationTimedEvent getClientImpatient() {
        return clientImpatient;
    }

    public int getMealsEaten() {
        return mealsEaten;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }
}
