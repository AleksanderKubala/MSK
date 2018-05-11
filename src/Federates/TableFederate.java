package Federates;

import Ambassadors.TableAmbassador;
import FomInteractions.Interaction;
import FomInteractions.InteractionComparator;
import FomInteractions.TableInteraction;
import FomObjects.Table;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TableFederate extends BasicFederate {

    private Map<Integer, Table> tableInstanceMap;
    private int tableInstancesCount;
    private int minimumSeats;
    private int maximumSeats;

    private ObjectClassHandle tableClassHandle;
    private AttributeHandle tableNumberHandle;
    private AttributeHandle freeSeatsNowHandle;

    TableFederate(int tableInstancesCount, int minimumSeats, int maximumSeats) {
        super();
        signature = "TableFederate";
        tableInstanceMap = new HashMap<>();
        this.tableInstancesCount = tableInstancesCount;
        this.minimumSeats = minimumSeats;
        this.maximumSeats = maximumSeats;
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        tableClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Table");
        tableNumberHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "tableNumber");
        freeSeatsNowHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "freeSeatsNow");

        AttributeHandleSet tableAttributeHandleSet = rtiAmbassador.getAttributeHandleSetFactory().create();
        tableAttributeHandleSet.add(tableNumberHandle);
        tableAttributeHandleSet.add(freeSeatsNowHandle);

        rtiAmbassador.publishObjectClassAttributes(tableClassHandle, tableAttributeHandleSet);

        InteractionClassHandle seatTakenIHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.TableInteraction.SeatTaken");
        InteractionClassHandle seatFreedIHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.TableInteraction.SeatFreed");

        ParameterHandle tableNumberHandle = rtiAmbassador.getParameterHandle(seatTakenIHandle, "tableNumber");

        ((TableAmbassador)federateAmbassador).seatTakenIHandle = seatTakenIHandle;
        ((TableAmbassador)federateAmbassador).seatFreedIHandle = seatFreedIHandle;
        ((TableAmbassador)federateAmbassador).tableNumberHandle = tableNumberHandle;

        rtiAmbassador.subscribeInteractionClass(seatTakenIHandle);
        rtiAmbassador.subscribeInteractionClass(seatFreedIHandle);
    }

    @Override
    protected void runFederate(String federateName) throws RTIexception {

        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        federateAmbassador = new TableAmbassador(this);

        rtiAmbassador.connect(federateAmbassador, CallbackModel.HLA_EVOKED);
        createFederation();

        joinFederation("Table");

        timeFactory = (HLAfloat64TimeFactory) rtiAmbassador.getTimeFactory();

        registerSynchronizationPoint();
        awaitFederationSynchronization();

        setTimePolicy(true, true);
        publishAndSubscribe();
        registerTableInstances();
        setTableInstancesAttributes(federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead());

        while(federateAmbassador.isRunning()) {
            double newTime = federateAmbassador.getFederateTime() + federateAmbassador.getFederateTimeStep();
            advanceTime(newTime);

            if(federateAmbassador.federationEvents.size() > 0) {
                federateAmbassador.federationEvents.sort(new InteractionComparator());
                for(Interaction interaction: federateAmbassador.federationEvents) {
                    double time = ((HLAfloat64Time)(interaction.getTime())).getValue();
                    federateAmbassador.setFederateTime(time);
                    switch(interaction.getType()) {
                        case SEAT_FREED:
                            seatFreed(time, ((TableInteraction)interaction).getTableNumber());
                            break;
                        case SEAT_TAKEN:
                            seatTaken(time, ((TableInteraction)interaction).getTableNumber());
                            break;
                    }
                }
                federateAmbassador.federationEvents.clear();
            }

            if(federateAmbassador.getGrantedTime() == newTime) {
                newTime += federateAmbassador.getFederateLookahead();
                federateAmbassador.setFederateTime(newTime);
            }

            rtiAmbassador.evokeMultipleCallbacks(0.1, 0.2);
        }

        Collection<Table> tables = tableInstanceMap.values();
        for(Table table: tables) {
            deleteObject(table.getInstanceHandle());
        }

        finish();

    }

    private void registerTableInstances() throws RTIexception{

        Random random = new Random();

        for(int i = 0; i < tableInstancesCount; i++) {
            ObjectInstanceHandle handle = rtiAmbassador.registerObjectInstance(tableClassHandle);
            tableInstanceMap.put(i, new Table(handle, i, minimumSeats + random.nextInt((maximumSeats - minimumSeats + 1))));
            log("Created Table Instance (time: " + (federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead()) + ")");
        }
    }

    private void setTableInstancesAttributes(double time) throws RTIexception {

        for(int i = 0; i < tableInstanceMap.size(); i++) {
            AttributeHandleValueMap attributes = rtiAmbassador.getAttributeHandleValueMapFactory().create(2);

            HLAinteger32BE tableNumberValue = encoderFactory.createHLAinteger32BE(tableInstanceMap.get(i).getTableNumber());
            HLAinteger32BE freeSeatsNowValue = encoderFactory.createHLAinteger32BE(tableInstanceMap.get(i).getFreeSeatsNow());

            attributes.put(tableNumberHandle, tableNumberValue.toByteArray());
            attributes.put(freeSeatsNowHandle, freeSeatsNowValue.toByteArray());
            LogicalTime logicalTime = convertTime(time);
            rtiAmbassador.updateAttributeValues(tableInstanceMap.get(i).getInstanceHandle(), attributes, "setting tables parameters".getBytes(), logicalTime);
            log("Updated Table Instance (time: " + time + ")");
        }

    }

    private void seatTaken(double time, int tableNumber) throws RTIexception {
        Table table = tableInstanceMap.get(tableNumber);
        freeSeatNumberChanged(time, table.getInstanceHandle(), table.getFreeSeatsNow() - 1);
    }

    private void seatFreed(double time, int tableNumber) throws RTIexception {
        Table table = tableInstanceMap.get(tableNumber);
        freeSeatNumberChanged(time, table.getInstanceHandle(), table.getFreeSeatsNow() + 1);
    }

    private void freeSeatNumberChanged(double time, ObjectInstanceHandle instanceHandle, int freeSeats) throws RTIexception {
        AttributeHandleValueMap attributes = rtiAmbassador.getAttributeHandleValueMapFactory().create(1);

        HLAinteger32BE freeSeatsNowValue = encoderFactory.createHLAinteger32BE(freeSeats);

        attributes.put(freeSeatsNowHandle, freeSeatsNowValue.toByteArray());
        LogicalTime logicalTime = convertTime(time);
        rtiAmbassador.updateAttributeValues(instanceHandle, attributes, "updating tables seats".getBytes(), logicalTime);
    }

    public static void main(String[] args) {
        try {
            new TableFederate(6, 2, 4).runFederate("Table");
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

    }
}
