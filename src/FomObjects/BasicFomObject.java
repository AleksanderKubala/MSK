package FomObjects;

public abstract class BasicFomObject {

    private int instanceHandle;

    BasicFomObject(int instanceHandle) {
        this.instanceHandle = instanceHandle;
    }

    public int getInstanceHandle() {
        return instanceHandle;
    }
}
