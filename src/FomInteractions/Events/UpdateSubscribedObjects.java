package FomInteractions.Events;

import FomObjects.BasicFomObject;
import FomObjects.FomObjectType;
import hla.rti1516e.LogicalTime;

public class UpdateSubscribedObjects extends TimedEvent{

    private FomObjectType objectType;
    private BasicFomObject object;

    public UpdateSubscribedObjects(LogicalTime time, EventType eventType, FomObjectType objectType, BasicFomObject object) {
        super(time, eventType);
        this.objectType = objectType;
        this.object = object;
    }

    public FomObjectType getObjectType() {
        return objectType;
    }

    public BasicFomObject getObject() {
        return object;
    }
}
