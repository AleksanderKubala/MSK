package FomObjects;

import hla.rti1516e.ObjectInstanceHandle;

public abstract class BasicFomObject {

    private ObjectInstanceHandle instanceHandle;

    BasicFomObject(ObjectInstanceHandle instanceHandle) {
        this.instanceHandle = instanceHandle;
    }

    public ObjectInstanceHandle getInstanceHandle() {
        return instanceHandle;
    }
}
