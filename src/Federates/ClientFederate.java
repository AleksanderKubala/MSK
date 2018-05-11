package Federates;

import FomObjects.Table;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.exceptions.RTIexception;

import java.util.HashMap;
import java.util.Map;

public class ClientFederate extends BasicFederate {

    private Map<Integer, Table> tableInstanceMap;

    private ObjectClassHandle tableClassHandle;
    private AttributeHandle tableNumberHandle;
    private AttributeHandle freeSeatsNowHandle;

    public ClientFederate() {
        tableInstanceMap = new HashMap<>();
        signature = "ClientFederate";
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {


    }

    @Override
    protected void runFederate(String federateName) throws RTIexception {

    }

}