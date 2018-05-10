package Federates;

import Ambassadors.TableAmbassador;
import FomObjects.Table;
import hla.rti.AttributeHandleSet;
import hla.rti.LogicalTime;
import hla.rti.RTIexception;
import hla.rti.SuppliedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TableFederate extends BasicFederate {

    private Map<Integer, Table> tableNumberInstanceMap;
    private int tableInstancesCount;
    private int minimumSeats;
    private int maximumSeats;

    private int tableClassHandle;
    private int tableNumberHandle;
    private int freeSeatsNowHandle;

    TableFederate(int tableInstancesCount, int minimumSeats, int maximumSeats) {
        super();
        signature = "TableFederate";
        tableNumberInstanceMap = new HashMap<>();
        this.tableInstancesCount = tableInstancesCount;
        this.minimumSeats = minimumSeats;
        this.maximumSeats = maximumSeats;
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        tableClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Table");
        tableNumberHandle = rtiAmbassador.getAttributeHandle("tableNumber", tableClassHandle);
        freeSeatsNowHandle = rtiAmbassador.getAttributeHandle("freeSeatsNow", tableClassHandle);

        AttributeHandleSet tableAttributeHandleSet = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        tableAttributeHandleSet.add(tableNumberHandle);
        tableAttributeHandleSet.add(freeSeatsNowHandle);

        rtiAmbassador.publishObjectClass(tableClassHandle, tableAttributeHandleSet);

        int seatTakenIHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.TableInteraction.SeatTaken");
        int seatFreedIHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.TableInteraction.SeatFreed");

        ((TableAmbassador)federateAmbassador).seatTakenIHandle = seatTakenIHandle;
        ((TableAmbassador)federateAmbassador).seatFreedIHandle = seatFreedIHandle;

        rtiAmbassador.subscribeInteractionClass(seatTakenIHandle);
        rtiAmbassador.subscribeInteractionClass(seatFreedIHandle);
    }

    @Override
    protected void runFederate(String federateName) throws RTIexception {

    }

    private void registerTableInstances() throws RTIexception{

        Random random = new Random();
        int tableClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Table");

        for(int i = 0; i < tableInstancesCount; i++) {
            int handle = rtiAmbassador.registerObjectInstance(tableClassHandle);
            tableNumberInstanceMap.put(i, new Table(handle, i, minimumSeats + random.nextInt((maximumSeats - minimumSeats + 1))));
        }
    }

    private void setTableInstancesAttributes(double time) throws RTIexception {

        for(int i = 0; i < tableNumberInstanceMap.size(); i++) {
            SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

            byte[] tableNumberValue = EncodingHelpers.encodeInt(tableNumberInstanceMap.get(i).getTableNumber());
            byte[] freeSeatsNowValue = EncodingHelpers.encodeInt(tableNumberInstanceMap.get(i).getFreeSeatsNow());

            attributes.add(tableNumberHandle, tableNumberValue);
            attributes.add(freeSeatsNowHandle, freeSeatsNowValue);
            LogicalTime logicalTime = convertTime(time);
            rtiAmbassador.updateAttributeValues(tableNumberInstanceMap.get(i).getInstanceHandle(), attributes, "actualize stock".getBytes(), logicalTime);
        }

    }

    private void seatTaken(double time, int tableNumber) throws RTIexception {
        Table table = tableNumberInstanceMap.get(tableNumber);
        freeSeatNumberChanged(time, table.getInstanceHandle(), table.getFreeSeatsNow() - 1);
    }

    private void seatFreed(double time, int tableNumber) throws RTIexception {
        Table table = tableNumberInstanceMap.get(tableNumber);
        freeSeatNumberChanged(time, table.getInstanceHandle(), table.getFreeSeatsNow() + 1);
    }

    private void freeSeatNumberChanged(double time, int instanceHandle, int freeSeats) throws RTIexception {
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        byte[] freeSeatsNowValue = EncodingHelpers.encodeInt(freeSeats);

        attributes.add(freeSeatsNowHandle, freeSeatsNowValue);
        LogicalTime logicalTime = convertTime(time);
        rtiAmbassador.updateAttributeValues(instanceHandle, attributes, "actualize stock".getBytes(), logicalTime);
    }
}
