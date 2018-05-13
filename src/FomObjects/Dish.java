package FomObjects;

import hla.rti1516e.ObjectInstanceHandle;

public class Dish extends BasicFomObject{

    private int dishNumber;
    private int consumptionTime;
    private int preparationTime;

    public Dish(ObjectInstanceHandle instanceHandle, int dishNumber, int consumptionTime, int preparationTime) {
        super(instanceHandle);
        this.dishNumber = dishNumber;
        this.consumptionTime = consumptionTime;
        this.preparationTime = preparationTime;
    }

    public int getDishNumber() {
        return dishNumber;
    }

    public int getConsumptionTime() {
        return consumptionTime;
    }

    public int getPreparationTime() {
        return preparationTime;
    }
}
