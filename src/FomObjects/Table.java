package FomObjects;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.Comparator;

public class Table extends BasicFomObject{

    private int tableNumber;
    private int freeSeatsNow;

    public Table(ObjectInstanceHandle instanceHandle, int tableNumber, int freeSeatsNow) {
        super(instanceHandle);
        this.tableNumber = tableNumber;
        this.freeSeatsNow = freeSeatsNow;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public int getFreeSeatsNow() {
        return freeSeatsNow;
    }

    public void setFreeSeatsNow(int freeSeatsNow) {
        this.freeSeatsNow = freeSeatsNow;
    }

}
