package FomInteractions.Events;

import FomInteractions.Events.TimedEvent;

import java.util.Comparator;

public class TimedEventComparator implements Comparator<TimedEvent> {
    @Override
    public int compare(TimedEvent o1, TimedEvent o2) {
        return o1.getTime().compareTo(o2.getTime());
    }
}
