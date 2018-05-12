package Federates;

import Ambassadors.ClientAmbassador;
import Federates.FederatesInternal.Client;
import FomInteractions.Events.*;
import FomInteractions.Interactions.ClientInteraction;
import FomObjects.Dish;
import FomObjects.Table;
import FomObjects.TableComparator;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.util.*;

public class ClientFederate extends BasicFederate {

    private Map<Integer, Table> tableInstanceMap;
    private Map<Integer, Dish> dishInstanceMap;
    private Map<Integer, Client> clientsQueue;
    private Map<Integer, Client> clientsInside;

    public InteractionClassHandle clientLeftQueueHandle;
    public InteractionClassHandle clientWaitingHandle;
    public ParameterHandle clientNumberParamHandle;

    public InteractionClassHandle orderPlacedHandle;
    public ParameterHandle dishNumberParamHandle;

    public InteractionClassHandle seatTakenHandle;
    public InteractionClassHandle seatFreedHandle;
    public ParameterHandle tableNumberParamHandle;

    private double impatienceProbability;
    private int impatienceMinTime;
    private int impatienceMaxTime;
    private int generationMinTime;
    private int generationMaxTime;

    private int globalClientId;

    public ClientFederate(String federateName,
                          double impatienceProbability,
                          int impatienceAverageTime,
                          int impatienceDeviation,
                          int clientGenerationAverageTime,
                          int clientGenerationDeviation) {
        super(federateName);
        tableInstanceMap = new HashMap<>();
        dishInstanceMap = new HashMap<>();
        clientsQueue = new HashMap<>();
        clientsInside = new HashMap<>();
        this.impatienceProbability = impatienceProbability;
        this.impatienceMinTime = impatienceAverageTime - impatienceDeviation;
        this.impatienceMaxTime = impatienceAverageTime + impatienceDeviation;
        this.generationMaxTime = clientGenerationAverageTime + clientGenerationDeviation;
        this.generationMinTime = clientGenerationAverageTime - clientGenerationDeviation;
        globalClientId = 0;
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
    protected void setFederateAmbassador() throws RTIexception {
        federateAmbassador = new ClientAmbassador(this);
    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {
        switch(event.getType()) {
            case OBJECT_UPDATE:
                UpdateSubscribedObjects update = (UpdateSubscribedObjects) event;
                switch (update.getObjectType()) {
                    case TABLE:
                        updateTable(0.0, (Table) update.getObject());
                        break;
                    case DISH:
                        updateDish(0.0, (Dish) update.getObject());
                        break;
                }
        }
    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case ORDER_FILLED:
                log("Done nothing with OrderFilled Interaction. Check runFederate()");
                break;
            case CLIENT_SERVICED:
                log("Done nothing with ClientServiced Interaction. Check runFederate()");
                break;

        }
    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case CLIENT_ARRIVED:
                ClientInteraction clientArrived = (ClientInteraction)event;
                clientsQueue.put(
                        clientArrived.getClientNumber(),
                        generateClient(convertLogicalTime(event.getTime()), clientArrived.getClientNumber()));
                //sendClientInteraction(getNextTime(), client.getClientNumber(), EventType.CLIENT_ARRIVED);
                generateClientArrivedEvent();
                break;
            case CLIENT_LEFT_QUEUE:
                ClientInteraction clientLeftQueue = (ClientInteraction)event;
                clientLeftQueue(convertLogicalTime(event.getTime()), clientLeftQueue.getClientNumber());
                break;
        }
    }

    @Override
    protected void afterSynchronization() throws RTIexception {
        generateClientArrivedEvent();
    }

    /*
    @Override
    protected void mainLoop() throws RTIexception {
        super.mainLoop();
        userInput();
    }*/

    /*
    protected void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();

        setTimePolicy(timeConstrained, timeRegulating);
        publishAndSubscribe();

        while(federateAmbassador.isRunning()) {

            double newTime = federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead();
            advanceTime(newTime);

            //TODO przerobić komunikację między Client i Table, aby aktualizacja obiektów nie była regulowana czasem
            if(federateAmbassador.federationNonTimedEvents.size() > 0) {
                for(FederationEvent event: federateAmbassador.federationNonTimedEvents) {
                    processFederationNonTimedEvent(event);
                }
                federateAmbassador.federationNonTimedEvents.clear();
            }

            if(federateAmbassador.federationTimedEvents.size() > 0) {
                federateAmbassador.federationTimedEvents.sort(new TimedEventComparator());
                for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                    double time = ((HLAfloat64Time)(event.getTime())).getValue();
                    federateAmbassador.setFederateTime(time);
                    processFederationTimedEvent(event);
                }
                federateAmbassador.federationTimedEvents.clear();
            }


            if(federateAmbassador.getGrantedTime() == newTime) {
                newTime += federateAmbassador.getFederateLookahead();
                federateAmbassador.setFederateTime(newTime);
            }

            rtiAmbassador.evokeMultipleCallbacks(0.1, 0.2);
        }
    }
    */

    protected void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();
        configurateFederate(timeConstrained, timeRegulating);
        afterSynchronization();

        while(federateAmbassador.isRunning()) {
            boolean internalEventsPending = false;
            double timeToAdvance = getNextTime();
            double nextInternalEventTime = 0.0;

            if(!clientsQueue.isEmpty()) {
                if (!tableInstanceMap.isEmpty()) {
                    Collection<Client> clients = clientsQueue.values();
                    for(Client client: clients) {
                        seatClient(client, timeToAdvance);
                    }
                }
            }

            if(internalEvents.size() > 0) {
                internalEvents.sort(new TimedEventComparator());
                timeToAdvance =  ((HLAfloat64Time) internalEvents.get(0).getTime()).getValue();
                nextInternalEventTime = timeToAdvance;
                internalEventsPending = true;
                advanceTime(timeToAdvance);
                retrieveCurrentInternalEvents(timeToAdvance);
            }

            if(federateAmbassador.federationNonTimedEvents.size() > 0) {
                for(FederationEvent event: federateAmbassador.federationNonTimedEvents) {
                    processFederationNonTimedEvent(event);
                }
                federateAmbassador.federationNonTimedEvents.clear();
            }

            if(federateAmbassador.federationTimedEvents.size() > 0) {
                federateAmbassador.federationTimedEvents.sort(new TimedEventComparator());
                for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                    double time = ((HLAfloat64Time)(event.getTime())).getValue();
                    federateAmbassador.setFederateTime(time);
                    processFederationTimedEvent(event);
                }
                federateAmbassador.federationTimedEvents.clear();
            }

            if (federateAmbassador.getGrantedTime() == timeToAdvance) {
                federateAmbassador.setFederateTime(timeToAdvance);
                log("Time advanced to: " + timeToAdvance);
                if(internalEventsPending) {
                    if (federateAmbassador.getFederateTime() >= nextInternalEventTime) {
                        for(FederationTimedEvent event: currentInternalEvents) {
                            processNextInternalEvent(event);
                        }
                        currentInternalEvents.clear();
                    }
                }
            }
        }
    }

    private Client generateClient(double time, int clientNumber) throws RTIexception {
        boolean isImpatient = false;
        Random random = new Random();
        if(random.nextDouble() <= impatienceProbability) {
            isImpatient = true;
        }
        if(isImpatient) {
            int impatienceTime = impatienceMinTime + random.nextInt(impatienceMaxTime - impatienceMinTime) + 1;
            LogicalTime impatienceEventTime = convertTime(federateAmbassador.getFederateTime() + (double)impatienceTime);
            ClientInteraction clientLeftQueue = new ClientInteraction(
                    impatienceEventTime,
                    EventType.CLIENT_LEFT_QUEUE,
                    clientNumber);
            internalEvents.add(clientLeftQueue);
            log("(time: " + time + "): " + "New impatient client " + clientNumber + " has arrived");
            return new Client(clientNumber, clientLeftQueue);
        }
        else {
            log("(time: " + time + "): " + "New client " + clientNumber + " has arrived");
            return new Client(clientNumber, null);
        }
    }

    private void seatClient(Client client, double time) throws RTIexception {

        boolean found = false;
        Collection<Table> tables = tableInstanceMap.values();
        Iterator<Table> iterator = tables.iterator();
        while((iterator.hasNext()) && (!found)) {
            Table table = iterator.next();
            if(table.getFreeSeatsNow() > 0) {
                found = true;
                internalEvents.remove(client.getClientImpatient());
                clientsInside.put(client.getClientNumber(), client);
                clientsQueue.remove(client.getClientNumber());
                sendTableInteraction(getNextTime(), client.getClientNumber(), table.getTableNumber(), EventType.SEAT_TAKEN);
            }
        }
    }


    private void sendTableInteraction(double time, int clientNumber, int tableNumber, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
        HLAinteger32BE tableNumberValue = encoderFactory.createHLAinteger32BE(tableNumber);
        params.put(tableNumberParamHandle, tableNumberValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime( time);
        switch(type) {
            case SEAT_TAKEN:
                log("(time: " + time + "): Client " + clientNumber + " taken place at Table " + tableNumber);
                rtiAmbassador.sendInteraction( seatTakenHandle, params, generateTag(), timeValue );
                break;
            case SEAT_FREED:
                log("(time: " + time + "): Client " + clientNumber + " freed place at Table " + tableNumber);
                rtiAmbassador.sendInteraction( seatFreedHandle, params, generateTag(), timeValue );
                break;
        }
    }

    private void sendClientInteraction(double time, int clientNumber, EventType type) throws RTIexception {

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

    private void generateClientArrivedEvent() {
        Random random = new Random();
        double time = getNextTime();
        double clientArrivalTime = getNextTime() + generationMinTime + random.nextInt(generationMaxTime - generationMinTime) + 1;
        int clientNumber = globalClientId;
        globalClientId++;
        ClientInteraction clientArrival = new ClientInteraction(
                convertTime(clientArrivalTime),
                EventType.CLIENT_ARRIVED,
                clientNumber);
        internalEvents.add(clientArrival);
    }

    private void clientLeftQueue(double time, int clientNumber) {
        log("(time: " + time + "): Client " + clientNumber + " got impatient and left the queue");
        clientsQueue.remove(clientNumber);
    }

    private void retrieveCurrentInternalEvents(double time) {
        boolean searching = true;
        int eventCount = 0;
        for(int i = 0; (i < internalEvents.size()) && searching; i++) {
            FederationTimedEvent event = internalEvents.get(i);
            double eventTime = convertLogicalTime(event.getTime());
            if(eventTime == time) {
                currentInternalEvents.add(event);
                eventCount++;
            } else {
                searching = false;
            }
        }
        for(int i = 0; i < eventCount; i++) {
            internalEvents.remove(0);
        }
    }

    public static void main(String[] args) {
        try {
            String federateName = "ClientFederate";
            double impatienceProbability = 0.5;
            int impatienceAverageTime = 20;
            int impatienceDeviation = 5;
            int clientGenerationAverageTime = 8;
            int clientGenerationDeviation = 4;

            new ClientFederate(federateName,
                    impatienceProbability,
                    impatienceAverageTime,
                    impatienceDeviation,
                    clientGenerationAverageTime,
                    clientGenerationDeviation).runFederate(true, true);

        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}