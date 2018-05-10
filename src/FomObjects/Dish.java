package FomObjects;

public class Dish extends BasicFomObject{

    private int dishNumber;
    private double consumptionTime;

    public Dish(int instanceHandle, int dishNumber, double consumptionTime) {
        super(instanceHandle);
        this.dishNumber = dishNumber;
        this.consumptionTime = consumptionTime;
    }

    public int getDishNumber() {
        return dishNumber;
    }

    public double getConsumptionTime() {
        return consumptionTime;
    }
}
