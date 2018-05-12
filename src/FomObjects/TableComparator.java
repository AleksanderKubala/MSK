package FomObjects;

import java.util.Comparator;

public class TableComparator implements Comparator<Table> {
    @Override
    public int compare(Table o1, Table o2) {
        return Integer.compare(o1.getFreeSeatsNow(), o2.getFreeSeatsNow());
    }
}
