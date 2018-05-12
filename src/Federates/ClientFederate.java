package Federates;

import Ambassadors.ClientAmbassador;
import FomInteractions.Events.EventType;
import FomInteractions.Events.TimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Events.UpdateSubscribedObjects;
import FomObjects.Dish;
import FomObjects.Table;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientFederate extends BasicFederate {

    private Map<Integer, Table> tableInstanceMap;
    private Map<Integer, Dish> dishInstanceMap;
    private List<Integer> clients;

    public InteractionClassHandle clientLeftQueueHandle;
    public InteractionClassHandle clientWaitingHandle;
    public ParameterHandle clientNumberParamHandle;

    public InteractionClassHandle orderPlacedHandle;
    public ParameterHandle dishNumberParamHandle;

    public InteractionClassHandle seatTakenHandle;
    public InteractionClassHandle seatFreedHandle;
    public ParameterHandle tableNumberParamHandle;

    public ClientFederate() {
        tableInstanceMap = new HashMap<>();
        dishInstanceMap = new HashMap<>();
        clients = new ArrayList<>();
        signature = "ClientFederate";
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        clientLeftQueueHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientLeftQueue");
        clientWaitingHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientServiced");
        clientNumberParamHandle = rtiAmbassador.getParameterHandle(clientWaitingHandle, "clientNumber");

        orderPlacedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.DishOrderInteraction.OrderPlaced");
        dishNumberParamHandle = rtiAmbassador.getParameterHandle( orderPlacedHandle, "dishNumber");

        seatTakenHandle = rtiAmbassador.getInteractionClassHandle( "InteractionRoot.TableInteraction.SeatTaken");
        seatFreedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.TableInteraction.SeatFreed");
        tableNumberParamHandle = rtiAmbassador.getParameterHandle(seatFreedHandle, "tableNumber");

        rtiAmbassador.publishInteractionClass(clientWaitingHandle);
        rtiAmbassador.publishInteractionClass(clientLeftQueueHandle);
        rtiAmbassador.publishInteractionClass(orderPlacedHandle);
        rtiAmbassador.publishInteractionClass(seatFreedHandle);
        rtiAmbassador.publishInteractionClass(seatTakenHandle);

        ObjectClassHandle tableClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Table");
        AttributeHandle tableNumberHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "tableNumber");
        AttributeHandle freeSeatsNowHandle = rtiAmbassador.getAttributeHandle(tableClassHandle, "freeSeatsNow");
        AttributeHandleSet tableAttributeHandleSet = rtiAmbassador.getAttributeHandleSetFactory().create();
        tableAttributeHandleSet.add(tableNumberHandle);
        tableAttributeHandleSet.add(freeSeatsNowHandle);

        ObjectClassHandle dishClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Dish");
        AttributeHandle dishNumberHandle = rtiAmbassador.getAttributeHandle(dishClassHandle, "dishNumber");
        AttributeHandle consumptionTimeHandle = rtiAmbassador.getAttributeHandle(dishClassHandle, "consumptionTime");
        AttributeHandleSet dishAttributeHandleSet = rtiAmbassador.getAttributeHandleSetFactory().create();
        dishAttributeHandleSet.add(dishNumberHandle);
        dishAttributeHandleSet.add(consumptionTimeHandle);

        InteractionClassHandle clientServicedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientServiced");
        ParameterHandle clientNumberHandle = rtiAmbassador.getParameterHandle(clientServicedHandle, "clientNumber");
        ParameterHandle waiterNumberHandle = rtiAmbassador.getParameterHandle(clientServicedHandle, "waiterNumber");

        InteractionClassHandle orderFilledHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.DishOrderInteraction.OrderFilled");
        ParameterHandle dishNumberParamHandle = rtiAmbassador.getParameterHandle(orderFilledHandle, "dishNumber");

        ((ClientAmbassador)federateAmbassador).tableClassHandle = tableClassHandle;
        ((ClientAmbassador)federateAmbassador).tableNumberAttrHandle = tableNumberHandle;
        ((ClientAmbassador)federateAmbassador).freeSeatsNowAttrHandle = freeSeatsNowHandle;
        ((ClientAmbassador)federateAmbassador).dishClassHandle = dishClassHandle;
        ((ClientAmbassador)federateAmbassador).dishNumberAttrHandle = dishNumberHandle;
        ((ClientAmbassador)federateAmbassador).consumptionTimeAttrHandle = consumptionTimeHandle;
        ((ClientAmbassador)federateAmbassador).clientServicedHandle = clientServicedHandle;
        ((ClientAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberHandle;
        ((ClientAmbassador)federateAmbassador).waiterNumberParamHandle = waiterNumberHandle;
        ((ClientAmbassador)federateAmbassador).orderFilledHandle = orderFilledHandle;
        ((ClientAmbassador)federateAmbassador).dishNumberParamHandle = dishNumberParamHandle;

        rtiAmbassador.subscribeObjectClassAttributes(tableClassHandle, tableAttributeHandleSet);
        rtiAmbassador.subscribeObjectClassAttributes(dishClassHandle, dishAttributeHandleSet);
        rtiAmbassador.subscribeInteractionClass(clientServicedHandle);
        rtiAmbassador.subscribeInteractionClass(orderFilledHandle);
    }

    @Override
    protected void runFederate(String federateName) throws RTIexception {
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        federateAmbassador = new ClientAmbassador(this);

        rtiAmbassador.connect(federateAmbassador, CallbackModel.HLA_EVOKED);
        createFederation();

        joinFederation("Client");

        timeFactory = (HLAfloat64TimeFactory) rtiAmbassador.getTimeFactory();

        registerSynchronizationPoint();
        waitForUser(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        awaitFederationSynchronization();

        setTimePolicy(true, true);
        publishAndSubscribe();

        while(federateAmbassador.isRunning()) {

            double newTime = federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead();
            advanceTime(newTime);

            if(federateAmbassador.federationEvents.size() > 0) {
                federateAmbassador.federationEvents.sort(new TimedEventComparator());
                for(TimedEvent event: federateAmbassador.federationEvents) {
                    double time = ((HLAfloat64Time)(event.getTime())).getValue();
                    federateAmbassador.setFederateTime(time);
                    switch(event.getType()) {
                        case ORDER_FILLED:
                            log("Done nothing with OrderFilled Interaction. Check runFederate()");
                            break;
                        case CLIENT_SERVICED:
                            log("Done nothing with ClientServiced Interaction. Check runFederate()");
                            break;
                        case OBJECT_UPDATE:
                            UpdateSubscribedObjects update = (UpdateSubscribedObjects)event;
                            switch(update.getObjectType()) {
                                case TABLE:
                                    updateTable(time, (Table)update.getObject());
                                    break;
                                case DISH:
                                    updateDish(time, (Dish)update.getObject());
                                    break;
                            }
                    }
                }
                federateAmbassador.federationEvents.clear();
            }



            if(federateAmbassador.getGrantedTime() == newTime) {
                newTime += federateAmbassador.getFederateLookahead();
                federateAmbassador.setFederateTime(newTime);
            }

            rtiAmbassador.evokeMultipleCallbacks(0.1, 0.2);

            if(!tableInstanceMap.isEmpty()) {
                UserInput();
            }
            else {
                log("Awaiting for Table Instances...");
            }
        }
    }

    private void UserInput() throws RTIexception {

        double time = federateAmbassador.getFederateTime();
        String number;
        System.out.println("\nSelect action (time: " + time + "):");
        System.out.println("(1) Take seat");
        System.out.println("(2) Free seat");

        String input = waitForUser("\t");
        switch(input){
            case "1":
                System.out.print("TableNumber: ");
                number = waitForUser("");
                sendTableInteraction(time, Integer.parseInt(number), EventType.SEAT_TAKEN);
                break;
            case "2":
                System.out.print("TableNumber: ");
                number = waitForUser("");
                sendTableInteraction(time, Integer.parseInt(number), EventType.SEAT_FREED);
                break;
        }
    }

    private void sendTableInteraction(double time, int tableNumber, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
        HLAinteger32BE tableNumberValue = encoderFactory.createHLAinteger32BE(tableNumber);
        params.put(tableNumberParamHandle, tableNumberValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime( time);
        switch(type) {
            case SEAT_TAKEN:
                log("(time: " + time + "): Client taken place at Table " + tableNumber);
                rtiAmbassador.sendInteraction( seatTakenHandle, params, generateTag(), timeValue );
                break;
            case SEAT_FREED:
                log("(time: " + time + "): Client freed place at Table " + tableNumber);
                rtiAmbassador.sendInteraction( seatFreedHandle, params, generateTag(), timeValue );
                break;
        }
    }

    private void updateTable(double time, Table table) {
        Table cachedTable = tableInstanceMap.putIfAbsent(table.getTableNumber(), table);
        if(cachedTable != null)
            cachedTable.setFreeSeatsNow(table.getFreeSeatsNow());
        log("Received Table instance: TableNumber: " + table.getTableNumber() + ", freeSeats: " + table.getFreeSeatsNow());
    }

    private void updateDish(double time, Dish dish) {
        Dish cachedDish = dishInstanceMap.putIfAbsent(dish.getDishNumber(), dish);
    }

    public static void main(String[] args) {
        try {
            new ClientFederate().runFederate("Client");
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}