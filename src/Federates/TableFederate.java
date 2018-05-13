package Federates;

import Ambassadors.TableAmbassador;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Interactions.TableInteraction;
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
    private AttributeHandle tableNumberAttrHandle;
    private AttributeHandle freeSeatsNowAttrHandle;

    TableFederate(String federateName, int tableInstancesCount, int minimumSeats, int maximumSeats) {
        super(federateName);
        tableInstanceMap = new HashMap<>();
        this.tableInstancesCount = tableInstancesCount;
        this.minimumSeats = minimumSeats;
        this.maximumSeats = maximumSeats;
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        tableClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Table");
        tableNumberAttrHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "tableNumber");
        freeSeatsNowAttrHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "freeSeatsNow");

        AttributeHandleSet tableAttributeHandleSet = rtiAmbassador.getAttributeHandleSetFactory().create();
        tableAttributeHandleSet.add(tableNumberAttrHandle);
        tableAttributeHandleSet.add(freeSeatsNowAttrHandle);

        rtiAmbassador.publishObjectClassAttributes(tableClassHandle, tableAttributeHandleSet);

        InteractionClassHandle seatTakenHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.TableInteraction.SeatTaken");
        InteractionClassHandle seatFreedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.TableInteraction.SeatFreed");

        ParameterHandle tableNumberHandle = rtiAmbassador.getParameterHandle(seatTakenHandle, "tableNumber");
        ParameterHandle clientNumberParamHandle = rtiAmbassador.getParameterHandle(seatTakenHandle, "clientNumber");

        ((TableAmbassador)federateAmbassador).seatTakenHandle = seatTakenHandle;
        ((TableAmbassador)federateAmbassador).seatFreedHandle = seatFreedHandle;
        ((TableAmbassador)federateAmbassador).tableNumberParamHandle = tableNumberHandle;
        ((TableAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberParamHandle;

        rtiAmbassador.subscribeInteractionClass(seatTakenHandle);
        rtiAmbassador.subscribeInteractionClass(seatFreedHandle);
    }

    @Override
    protected void setFederateAmbassador() throws RTIexception {
        federateAmbassador = new TableAmbassador(this);
    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {

    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {
        double time = convertLogicalTime(event.getTime());
        switch(event.getType()) {
            case SEAT_FREED:
                seatFreed(time + federateAmbassador.getFederateLookahead(), ((TableInteraction)event).getTableNumber());
                break;
            case SEAT_TAKEN:
                seatTaken(time + federateAmbassador.getFederateLookahead(), ((TableInteraction)event).getTableNumber());
                break;
            case FINISH:
                federateAmbassador.stop();
                break;
        }
    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {

    }

    @Override
    protected void afterSynchronization() throws RTIexception {
        registerTableInstances();
        setTableInstancesAttributes(federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead());
    }


    protected void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {

        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        federateAmbassador = new TableAmbassador(this);

        rtiAmbassador.connect(federateAmbassador, CallbackModel.HLA_EVOKED);
        createFederation();

        joinFederation("Table");

        timeFactory = (HLAfloat64TimeFactory) rtiAmbassador.getTimeFactory();

        registerSynchronizationPoint();
        waitForUser(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        awaitFederationSynchronization();

        setTimePolicy(timeConstrained, timeRegulating);
        publishAndSubscribe();
        registerTableInstances();
        setTableInstancesAttributes(federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead());

        while(federateAmbassador.isRunning()) {

            double newTime = federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead();
            advanceTime(newTime);

            if(federateAmbassador.federationTimedEvents.size() > 0) {
                federateAmbassador.federationTimedEvents.sort(new TimedEventComparator());
                for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                    double time = ((HLAfloat64Time)(event.getTime())).getValue();
                    federateAmbassador.setFederateTime(time);
                    switch(event.getType()) {
                        case SEAT_FREED:
                            seatFreed(time + federateAmbassador.getFederateLookahead(), ((TableInteraction)event).getTableNumber());
                            break;
                        case SEAT_TAKEN:
                            seatTaken(time + federateAmbassador.getFederateLookahead(), ((TableInteraction)event).getTableNumber());
                            break;
                    }
                }
                federateAmbassador.federationTimedEvents.clear();
            }



            if(federateAmbassador.getGrantedTime() == newTime) {
                //newTime += federateAmbassador.getFederateLookahead();
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

            attributes.put(tableNumberAttrHandle, tableNumberValue.toByteArray());
            attributes.put(freeSeatsNowAttrHandle, freeSeatsNowValue.toByteArray());
            LogicalTime logicalTime = convertTime(time);
            rtiAmbassador.updateAttributeValues(tableInstanceMap.get(i).getInstanceHandle(), attributes, "setting tables parameters".getBytes(), logicalTime);
            log("Updated Table Instance (time: " + time + ")");
        }

    }

    private void seatTaken(double time, int tableNumber) throws RTIexception {
        Table table = tableInstanceMap.get(tableNumber);
        table.setFreeSeatsNow(table.getFreeSeatsNow() - 1);
        freeSeatNumberChanged(time, table.getInstanceHandle(), tableNumber, table.getFreeSeatsNow());
        log("(time: " + time + "): Table " + tableNumber + ": freeSeatsNow: " + (table.getFreeSeatsNow()));
    }

    private void seatFreed(double time, int tableNumber) throws RTIexception {
        Table table = tableInstanceMap.get(tableNumber);
        table.setFreeSeatsNow(table.getFreeSeatsNow() + 1);
        freeSeatNumberChanged(time, table.getInstanceHandle(), tableNumber, table.getFreeSeatsNow());
        log("(time: " + time + "): Table " + tableNumber + ": freeSeatsNow: " + (table.getFreeSeatsNow()));
    }

    private void freeSeatNumberChanged(double time, ObjectInstanceHandle instanceHandle, int tableNumber, int freeSeats) throws RTIexception {
        AttributeHandleValueMap attributes = rtiAmbassador.getAttributeHandleValueMapFactory().create(1);

        HLAinteger32BE freeSeatsNowValue = encoderFactory.createHLAinteger32BE(freeSeats);
        HLAinteger32BE tableNumberValue = encoderFactory.createHLAinteger32BE(tableNumber);

        attributes.put(tableNumberAttrHandle, tableNumberValue.toByteArray());
        attributes.put(freeSeatsNowAttrHandle, freeSeatsNowValue.toByteArray());
        LogicalTime logicalTime = convertTime(time);
        rtiAmbassador.updateAttributeValues(instanceHandle, attributes, "updating tables seats".getBytes(), logicalTime);
    }

    public static void main(String[] args) {
        try {
            new TableFederate(
                    "TableFederate",
                    6,
                    2,
                    4)
                    .runFederate(
                            true,
                            true);
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

    }
}
