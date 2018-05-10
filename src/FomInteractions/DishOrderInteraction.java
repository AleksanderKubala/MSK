package FomInteractions;

public class DishOrderInteraction extends ClientInteraction{

    private int dishNumber;

    public DishOrderInteraction(int clientNumber, int dishNumber) {
        super(clientNumber);
        this.dishNumber = dishNumber;
    }

    public int getDishNumber() {
        return dishNumber;
    }
}
