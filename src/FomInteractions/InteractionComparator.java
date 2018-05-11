package FomInteractions;

import java.util.Comparator;

public class InteractionComparator implements Comparator<Interaction> {
    @Override
    public int compare(Interaction o1, Interaction o2) {
        return o1.getTime().compareTo(o2.getTime());
    }
}
