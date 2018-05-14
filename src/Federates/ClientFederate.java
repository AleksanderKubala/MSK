package Federates;

import Ambassadors.ClientAmbassador;
import Federates.FederatesInternal.Client;
import FomInteractions.Events.*;
import FomInteractions.Interactions.ClientInteraction;
import FomInteractions.Interactions.DishOrderInteraction;
import FomInteractions.Interactions.WaiterInteraction;
import FomObjects.Dish;
import FomObjects.Table;
import com.sun.deploy.util.Waiter;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.*;

public class ClientFederate extends BasicFederate {

    private Map<Integer, Table> tableInstanceMap;
    private Map<Integer, Dish> dishInstanceMap;
    private Map<Integer, Client> clientsQueue;
    private Map<Integer, Client> clientsInside;

    public InteractionClassHandle clientLeftQueueHandle;
    public InteractionClassHandle clientWaitingHandle;
    public InteractionClassHandle clientArrivedHandle;
    public ParameterHandle clientNumberParamHandle;

    public InteractionClassHandle orderPlacedHandle;
    public ParameterHandle dishNumberParamHandle;

    public InteractionClassHandle seatTakenHandle;
    public InteractionClassHandle seatFreedHandle;
    public ParameterHandle tableNumberParamHandle;

    public InteractionClassHandle finishHandle;

    private double impatienceProbability;
    private int minImpatienceTime;
    private int maxImpatienceTime;
    private int minGenerationTime;
    private int maxGenerationTime;
    private int maxMealsToEat;
    private double nextMealProbability;
    private int consumptionTimeDeviation;

    private int globalClientId;

    public ClientFederate(String federateName,
                          double impatienceProbability,
                          int impatienceAverageTime,
                          int impatienceDeviation,
                          int clientGenerationAverageTime,
                          int clientGenerationDeviation,
                          int maxMealsToEat,
                          double nextMealProbability,
                          int consumptionTimeDeviation) {
        super(federateName);
        tableInstanceMap = new HashMap<>();
        dishInstanceMap = new HashMap<>();
        clientsQueue = new HashMap<>();
        clientsInside = new HashMap<>();
        this.impatienceProbability = impatienceProbability;
        this.minImpatienceTime = impatienceAverageTime - impatienceDeviation;
        this.maxImpatienceTime = impatienceAverageTime + impatienceDeviation;
        this.maxGenerationTime = clientGenerationAverageTime + clientGenerationDeviation;
        this.minGenerationTime = clientGenerationAverageTime - clientGenerationDeviation;
        this.maxMealsToEat = maxMealsToEat;
        this.nextMealProbability = nextMealProbability;
        this.consumptionTimeDeviation = consumptionTimeDeviation;
        globalClientId = 0;
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        clientLeftQueueHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientLeftQueue");
        clientWaitingHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientWaiting");
        clientArrivedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientArrived");
        clientNumberParamHandle = rtiAmbassador.getParameterHandle(clientWaitingHandle, "clientNumber");

        orderPlacedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.DishOrderInteraction.OrderPlaced");
        dishNumberParamHandle = rtiAmbassador.getParameterHandle( orderPlacedHandle, "dishNumber");

        seatTakenHandle = rtiAmbassador.getInteractionClassHandle( "InteractionRoot.ClientInteraction.TableInteraction.SeatTaken");
        seatFreedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.TableInteraction.SeatFreed");
        tableNumberParamHandle = rtiAmbassador.getParameterHandle(seatFreedHandle, "tableNumber");

        finishHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.FinishInteraction");

        rtiAmbassador.publishInteractionClass(clientWaitingHandle);
        rtiAmbassador.publishInteractionClass(clientLeftQueueHandle);
        rtiAmbassador.publishInteractionClass(clientArrivedHandle);
        rtiAmbassador.publishInteractionClass(orderPlacedHandle);
        rtiAmbassador.publishInteractionClass(seatFreedHandle);
        rtiAmbassador.publishInteractionClass(seatTakenHandle);
        rtiAmbassador.publishInteractionClass(finishHandle);

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
                DishOrderInteraction orderFilled = (DishOrderInteraction)event;
                eatMeal(orderFilled.getClientNumber(), orderFilled.getDishNumber());
                break;
            case CLIENT_SERVICED:
                WaiterInteraction clientServiced = (WaiterInteraction)event;
                Random random = new Random();
                int dishIndex = random.nextInt(dishInstanceMap.size());
                int dishNumber = dishInstanceMap.get(dishIndex).getDishNumber();
                placeOrder(getNextTime(), clientServiced.getClientNumber(), dishNumber);
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
                        generateClient(getNextTime(), clientArrived.getClientNumber()));
                sendClientInteraction(getNextTime(), clientArrived.getClientNumber(), EventType.CLIENT_ARRIVED);
                if(federateAmbassador.getFederateTime() <= simulationFinishTime) {
                    generateClientArrivedEvent();
                }
                break;
            case CLIENT_LEFT_QUEUE:
                ClientInteraction clientLeftQueue = (ClientInteraction)event;
                clientLeftQueue(getNextTime(), clientLeftQueue.getClientNumber());
                sendClientInteraction(getNextTime(), clientLeftQueue.getClientNumber(), EventType.CLIENT_LEFT_QUEUE);
                break;
            case CLIENT_FINISHED_MEAL:
                Random random = new Random();
                ClientInteraction clientFinishedMeal = (ClientInteraction)event;
                Client client = clientsInside.get(clientFinishedMeal.getClientNumber());
                client.mealEaten();
                log("(time: " + getNextTime() + "): Client " + client.getClientNumber() + " finished his meal. "
                        + "Total meals eaten: " + client.getMealsEaten());
                boolean nextMeal = true;
                if(client.getMealsEaten() >= maxMealsToEat) {
                    nextMeal = false;
                }
                if(random.nextDouble() <= nextMealProbability) {
                    nextMeal = false;
                }
                if(nextMeal) {
                    sendClientInteraction(getNextTime(), client.getClientNumber(), EventType.CLIENT_WAITING);
                    log("(time: " + getNextTime() + "): Client " + client.getClientNumber() + " decides to eat another meal");
                }
                else {
                    sendTableInteraction(getNextTime(), client.getClientNumber(), client.getTableNumber(), EventType.SEAT_FREED);
                    clientsInside.remove(client.getClientNumber());
                    log("(time: " + getNextTime() + "): Client " + client.getClientNumber() + " left");
                }
                break;
            case ORDER_PLACED:
                DishOrderInteraction orderPlaced = (DishOrderInteraction)event;
                sendDishOrderInteraction(getNextTime(), orderPlaced.getClientNumber(), orderPlaced.getDishNumber(), EventType.ORDER_PLACED);
                break;
        }
    }

    @Override
    protected void afterSynchronization() throws RTIexception {
        generateClientArrivedEvent();
    }

    protected void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();
        configurateFederate(timeConstrained, timeRegulating);
        afterSynchronization();

        double nextInternalEventTime = 0.0;
        boolean internalEventsPending = false;

        while(federateAmbassador.isRunning()) {

            double timeToAdvance = getNextTime();

            if(!clientsQueue.isEmpty()) {
                boolean seated = false;
                List<Integer> clientsSeated = new ArrayList<>();
                if (!tableInstanceMap.isEmpty()) {
                    Collection<Client> clients = clientsQueue.values();
                    for(Client client: clients) {
                        seated = seatClient(client, timeToAdvance);
                        if(seated) {
                            clientsSeated.add(client.getClientNumber());
                        }
                    }
                }
                for(Integer index: clientsSeated) {
                    clientsQueue.remove(index);
                }
            }

            advanceTime(timeToAdvance);

            if(!internalEventsPending) {
                if (internalEvents.size() > 0) {
                    internalEvents.sort(new TimedEventComparator());
                    nextInternalEventTime = ((HLAfloat64Time) internalEvents.get(0).getTime()).getValue();
                    internalEventsPending = true;
                    retrieveCurrentInternalEvents(nextInternalEventTime);
                }
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
                if(internalEventsPending) {
                    if (federateAmbassador.getFederateTime() >= nextInternalEventTime) {
                        for(FederationTimedEvent event: currentInternalEvents) {
                            processNextInternalEvent(event);
                        }
                        currentInternalEvents.clear();
                        internalEventsPending = false;
                    }
                }
            }


            if(federateAmbassador.getFederateTime() >= simulationFinishTime) {
                if (clientsQueue.isEmpty()) {
                    if (clientsInside.isEmpty()) {
                        if (internalEvents.isEmpty()) {
                            sendFinishInteraction(getNextTime(), EventType.FINISH);
                            federateAmbassador.stop();
                        }
                    }
                }
            }
        }

        finish();
    }

    private Client generateClient(double time, int clientNumber) throws RTIexception {
        boolean isImpatient = false;
        Random random = new Random();
        if(random.nextDouble() <= impatienceProbability) {
            isImpatient = true;
        }
        if(isImpatient) {
            int impatienceTime = minImpatienceTime + random.nextInt(maxImpatienceTime - minImpatienceTime) + 1;
            LogicalTime impatienceEventTime = convertTime(federateAmbassador.getFederateTime() + (double)impatienceTime);
            ClientInteraction clientLeftQueue = new ClientInteraction(
                    impatienceEventTime,
                    EventType.CLIENT_LEFT_QUEUE,
                    clientNumber);
            internalEvents.add(clientLeftQueue);
            log("(time: " + time + "): " + "Client " + clientNumber + " has arrived (impatient)");
            return new Client(clientNumber, clientLeftQueue);
        }
        else {
            log("(time: " + time + "): " + "Client " + clientNumber + " has arrived");
            return new Client(clientNumber, null);
        }
    }

    private boolean seatClient(Client client, double time) throws RTIexception {

        boolean found = false;
        Collection<Table> tables = tableInstanceMap.values();
        Iterator<Table> iterator = tables.iterator();
        while((iterator.hasNext()) && (!found)) {
            Table table = iterator.next();
            if(table.isUpToDate()) {
                if (table.getFreeSeatsNow() > 0) {
                    found = true;
                    internalEvents.remove(client.getClientImpatient());
                    clientsInside.put(client.getClientNumber(), client);
                    client.setTableNumber(table.getTableNumber());
                    table.setUpToDate(false);
                    sendTableInteraction(getNextTime(), client.getClientNumber(), table.getTableNumber(), EventType.SEAT_TAKEN);
                    sendClientInteraction(getNextTime(), client.getClientNumber(), EventType.CLIENT_WAITING);
                }
            }
        }
        return found;
    }


    private void sendTableInteraction(double time, int clientNumber, int tableNumber, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(2);
        HLAinteger32BE tableNumberValue = encoderFactory.createHLAinteger32BE(tableNumber);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        params.put(tableNumberParamHandle, tableNumberValue.toByteArray());
        params.put(clientNumberParamHandle, clientNumberValue.toByteArray());

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
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        params.put(clientNumberParamHandle, clientNumberValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime( time);
        switch(type) {
            case CLIENT_ARRIVED:
                //log("(time: " + time + "): Client " + clientNumber + " arrived");
                rtiAmbassador.sendInteraction(clientArrivedHandle, params, generateTag(), timeValue);
                break;
            case CLIENT_LEFT_QUEUE:
                //log("(time: " + time + "): Client " + clientNumber + " left the queue");
                rtiAmbassador.sendInteraction(clientLeftQueueHandle, params, generateTag(), timeValue);
                break;
            case CLIENT_WAITING:
                log("(time: " + time + "): Client " + clientNumber + " waiting for service");
                rtiAmbassador.sendInteraction(clientWaitingHandle, params, generateTag(), timeValue);
                break;
        }
    }

    private void sendDishOrderInteraction(double time, int clientNumber, int dishNumber, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(2);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        HLAinteger32BE dishNumberValue = encoderFactory.createHLAinteger32BE(dishNumber);
        params.put(clientNumberParamHandle, clientNumberValue.toByteArray());
        params.put(dishNumberParamHandle, dishNumberValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime(time);
        switch(type) {
            case ORDER_PLACED:
                //log("(time: " + time + "): Client " + clientNumber + " placed order for Dish " + dishNumber);
                rtiAmbassador.sendInteraction(orderPlacedHandle, params, generateTag(), timeValue);
                break;
        }
    }

    private void sendFinishInteraction(double time, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(1);
        HLAfloat64Time timeValue = timeFactory.makeTime(time);

        switch(type) {
            case FINISH:
                log("(time: " + time + "): Sending FinishInteraction");
                rtiAmbassador.sendInteraction(finishHandle, params, generateTag(), timeValue);
                break;
        }

    }

    private void updateTable(double time, Table table) {
        Table cachedTable = tableInstanceMap.putIfAbsent(table.getTableNumber(), table);
        if(cachedTable != null) {
            cachedTable.setFreeSeatsNow(table.getFreeSeatsNow());
            cachedTable.setUpToDate(true);
        }
        log("Received Table instance: TableNumber: " + table.getTableNumber() + ", freeSeats: " + table.getFreeSeatsNow());
    }

    private void updateDish(double time, Dish dish) {
        Dish cachedDish = dishInstanceMap.putIfAbsent(dish.getDishNumber(), dish);
        //log("Dish consumption time: " + dish.getConsumptionTime());
    }

    private void generateClientArrivedEvent() {
        Random random = new Random();
        double time = getNextTime();
        double clientArrivalTime = getNextTime() + minGenerationTime + random.nextInt(maxGenerationTime - minGenerationTime) + 1;
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

    private void placeOrder(double time, int clientNumber, int dishNumber) {
        LogicalTime orderPlaceTime = convertTime(time);
        DishOrderInteraction placeOrder = new DishOrderInteraction (
                orderPlaceTime,
                EventType.ORDER_PLACED,
                clientNumber,
                dishNumber,
                -1);
        internalEvents.add(placeOrder);
        log("(time: " + getNextTime() + "): Client " + clientNumber
                + " was serviced and placed an order for Dish "
                + dishNumber);
    }

    private void eatMeal(int clientNumber, int dishNumber) {
        Random random = new Random();
        Dish dish = dishInstanceMap.get(dishNumber);
        int dishConsumptionTime = new Double(dish.getConsumptionTime()).intValue();
        int minConsumptionTime = dishConsumptionTime - consumptionTimeDeviation;
        int maxConsumptionTime = dishConsumptionTime + consumptionTimeDeviation;
        double mealFinishTime = getNextTime() + minConsumptionTime + random.nextInt(maxConsumptionTime - minConsumptionTime);
        LogicalTime clientFinishedMealEventTime = convertTime(mealFinishTime);
        ClientInteraction clientFinishedMeal = new ClientInteraction(
          clientFinishedMealEventTime,
          EventType.CLIENT_FINISHED_MEAL,
          clientNumber);
        internalEvents.add(clientFinishedMeal);
        log("(time: " + getNextTime() + "): Client " + clientNumber
                + " received his meal and started eating");
    }

    public static void main(String[] args) {
        try {
            String federateName = "ClientFederate";
            double impatienceProbability = 0.5;
            int impatienceAverageTime = 20;
            int impatienceDeviation = 5;
            int clientGenerationAverageTime = 8;
            int clientGenerationDeviation = 4;
            int maxMealsToEat = 2;
            double nextMealProbability = 0.5;
            int consumptionTimeDeviation = 5;

            new ClientFederate(federateName,
                    impatienceProbability,
                    impatienceAverageTime,
                    impatienceDeviation,
                    clientGenerationAverageTime,
                    clientGenerationDeviation,
                    maxMealsToEat,
                    nextMealProbability,
                    consumptionTimeDeviation).runFederate(true, true);

        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}