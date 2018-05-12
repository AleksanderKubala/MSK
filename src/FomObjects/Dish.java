package FomObjects;

import hla.rti1516e.ObjectInstanceHandle;

public class Dish extends BasicFomObject{

    private int dishNumber;
    private double consumptionTime;
    private double preparationTime;

    public Dish(ObjectInstanceHandle instanceHandle, int dishNumber, double consumptionTime, double preparationTime) {
        super(instanceHandle);
        this.dishNumber = dishNumber;
        this.consumptionTime = consumptionTime;
        this.preparationTime = preparationTime;
    }

    public int getDishNumber() {
        return dishNumber;
    }

    public double getConsumptionTime() {
        return consumptionTime;
    }

    public double getPreparationTime() {
        return preparationTime;
    }
}
