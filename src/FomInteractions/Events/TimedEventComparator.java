package FomInteractions.Events;

import java.util.Comparator;

public class TimedEventComparator implements Comparator<FederationTimedEvent> {
    @Override
    public int compare(FederationTimedEvent o1, FederationTimedEvent o2) {
        return o1.getTime().compareTo(o2.getTime());
    }
}
