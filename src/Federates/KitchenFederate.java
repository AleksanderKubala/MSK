package Federates;

import Ambassadors.KitchenAmbassador;
import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Interactions.DishOrderInteraction;
import FomObjects.Dish;
import hla.rti1516e.*;
import hla.rti1516e.encoding.HLAfloat64BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.util.*;

public class KitchenFederate extends BasicFederate {

    public ObjectClassHandle dishClassHandle;
    public AttributeHandle dishNumberAttrHandle;
    public AttributeHandle consumptionTimeAttrHandle;

    public InteractionClassHandle orderFilledHandle;
    public ParameterHandle clientNumberParamHandle;
    public ParameterHandle dishNumberParamHandle;

    private int chefsCount;

    private int minConsumptionTime;
    private int maxConsumptionTime;
    private int minPreparationTime;
    private int maxPreparationTime;

    private int dishInstanceCount;

    private Map<Integer, Dish> dishInstanceMap;
    private List<DishOrderInteraction> dishRequests;
    private List<Integer> chefsList;

    public KitchenFederate(String federateName,
                           int chefsCount,
                           int dishInstancesCount,
                           int minConsumptionTime,
                           int maxConsumptionTime,
                           int minPreparationTime,
                           int maxPreparationTime) {
        super(federateName);
        this.chefsCount = chefsCount;
        this.dishInstanceCount = dishInstancesCount;
        this.minConsumptionTime = minConsumptionTime;
        this.maxConsumptionTime = maxConsumptionTime;
        this.minPreparationTime = minPreparationTime;
        this.maxPreparationTime = maxPreparationTime;
        dishInstanceMap = new HashMap<>();
        dishRequests = new ArrayList<>();
        chefsList = new ArrayList<>();
        for(int i = 0; i < chefsCount; i++) {
            chefsList.add(i);
        }
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        dishClassHandle = rtiAmbassador.getObjectClassHandle("ObjectRoot.Dish");
        dishNumberAttrHandle = rtiAmbassador.getAttributeHandle(dishClassHandle, "dishNumber");
        consumptionTimeAttrHandle = rtiAmbassador.getAttributeHandle(dishClassHandle, "consumptionTime");

        AttributeHandleSet dishClassAttrSet = rtiAmbassador.getAttributeHandleSetFactory().create();
        dishClassAttrSet.add(dishNumberAttrHandle);
        dishClassAttrSet.add(consumptionTimeAttrHandle);

        orderFilledHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.DishOrderInteraction.OrderFilled");
        clientNumberParamHandle = rtiAmbassador.getParameterHandle(orderFilledHandle, "clientNumber");
        dishNumberParamHandle = rtiAmbassador.getParameterHandle(orderFilledHandle, "dishNumber");

        rtiAmbassador.publishObjectClassAttributes(dishClassHandle, dishClassAttrSet);
        rtiAmbassador.publishInteractionClass(orderFilledHandle);

        InteractionClassHandle orderPlacedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.DishOrderInteraction.OrderPlaced");
        InteractionClassHandle finishHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.FinishInteraction");

        ((KitchenAmbassador)federateAmbassador).orderPlacedHandle = orderPlacedHandle;
        ((KitchenAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberParamHandle;
        ((KitchenAmbassador)federateAmbassador).dishNumberParamHandle = dishNumberParamHandle;
        ((KitchenAmbassador)federateAmbassador).finishHandle = finishHandle;

        rtiAmbassador.subscribeInteractionClass(orderPlacedHandle);
        rtiAmbassador.subscribeInteractionClass(finishHandle);
    }

    @Override
    protected void setFederateAmbassador() throws RTIexception {
        federateAmbassador = new KitchenAmbassador(this);
    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {

    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case ORDER_PLACED:
                DishOrderInteraction orderPlaced = (DishOrderInteraction)event;
                dishRequests.add(orderPlaced);
                break;
            case FINISH:
                log("Received Finishing interaction. Finishing.");
                federateAmbassador.stop();
                break;
        }
    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case ORDER_FILLED:
                DishOrderInteraction orderFilled = (DishOrderInteraction)event;
                chefsList.add(orderFilled.getChefNumber());
                double time = convertLogicalTime(orderFilled.getTime());
                log("(time: " + time + "): Order from client " + orderFilled.getClientNumber() + " filled by Chef " + orderFilled.getChefNumber());
                sendDishOrderInteraction(getNextTime(), orderFilled.getClientNumber(), orderFilled.getDishNumber(), EventType.ORDER_FILLED);
                break;
        }
    }

    @Override
    protected void afterSynchronization() throws RTIexception {
        registerDishInstances();
        setDishInstancesAttributes(getNextTime());
    }

    public void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();
        configurateFederate(timeConstrained, timeRegulating);
        afterSynchronization();

        boolean internalEventsPending = false;
        double nextInternalEventTime = 0.0;

        while(federateAmbassador.isRunning()) {

            double timeToAdvance = getNextTime();

            if(!dishRequests.isEmpty()) {
                int counter = 0;
                for(int i = 0, j = chefsList.size(); (i < dishRequests.size()) && (j > 0); i++, j--) {
                    DishOrderInteraction request = dishRequests.get(i);
                    prepareDish(request.getClientNumber(), request.getDishNumber(), chefsList.get(i));
                    counter++;
                }
                for(int i = 0; i < counter; i++) {
                    dishRequests.remove(0);
                    chefsList.remove(0);
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
        }

        Collection<Dish> dishes = dishInstanceMap.values();
        for(Dish dish: dishes) {
            deleteObject(dish.getInstanceHandle());;
        }

        finish();
    }

    private void registerDishInstances() throws RTIexception{

        Random random = new Random();

        for(int i = 0; i < dishInstanceCount; i++) {
            ObjectInstanceHandle handle = rtiAmbassador.registerObjectInstance(dishClassHandle);
            int consumption = minConsumptionTime + random.nextInt(maxConsumptionTime - minConsumptionTime) + 1;
            int preparation = minPreparationTime + random.nextInt(maxPreparationTime - minPreparationTime) + 1;

            dishInstanceMap.put(i, new Dish(handle, i, consumption, preparation));
            log("Created Dish Instance (time: " + (federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead()) + ")");
            //log("Consumption time: " + consumption);
        }
    }

    private void setDishInstancesAttributes(double time) throws RTIexception {

        for(int i = 0; i < dishInstanceMap.size(); i++) {
            AttributeHandleValueMap attributes = rtiAmbassador.getAttributeHandleValueMapFactory().create(2);

            HLAinteger32BE dishNumberValue = encoderFactory.createHLAinteger32BE(dishInstanceMap.get(i).getDishNumber());
            HLAinteger32BE consumptionTimeValue = encoderFactory.createHLAinteger32BE(dishInstanceMap.get(i).getConsumptionTime());

            attributes.put(dishNumberAttrHandle, dishNumberValue.toByteArray());
            attributes.put(consumptionTimeAttrHandle, consumptionTimeValue.toByteArray());
            LogicalTime logicalTime = convertTime(time);
            rtiAmbassador.updateAttributeValues(dishInstanceMap.get(i).getInstanceHandle(), attributes, "setting tables parameters".getBytes(), logicalTime);
            log("Updated Table Instance (time: " + time + ")");
        }
    }

    private void sendDishOrderInteraction(double time, int clientNumber, int dishNumber, EventType type) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(2);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        HLAinteger32BE dishNumbeValue = encoderFactory.createHLAinteger32BE(dishNumber);
        params.put(clientNumberParamHandle, clientNumberValue.toByteArray());
        params.put(dishNumberParamHandle, dishNumbeValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime( time);

        switch(type) {
            case ORDER_FILLED:
                rtiAmbassador.sendInteraction(orderFilledHandle, params, generateTag(), timeValue);
                break;
        }
    }

    private void prepareDish(int clientNumber, int dishNumber, int chefNumber) {
        double time = getNextTime();
        double preparationTime = dishInstanceMap.get(dishNumber).getPreparationTime();
        double orderFillTime = getNextTime() + preparationTime;
        LogicalTime orderFilledEventTime = convertTime(orderFillTime);
        DishOrderInteraction orderFilled = new DishOrderInteraction(
                orderFilledEventTime,
                EventType.ORDER_FILLED,
                clientNumber,
                dishNumber,
                chefNumber);
        internalEvents.add(orderFilled);
        log("(time: " + time + "): Chef " + chefNumber + " is preparing dish for Client " + clientNumber);
    }

    public static void main(String args[]) {
        String federateName = "KitchenFederate";
        int chefsCount = 2;
        int dishInstancesCount = 5;
        int minConsumptionTime = 20;
        int maxConsumptionTime = 35;
        int minPreparationTime = 15;
        int maxPreparationTime = 40;
        try {
            new KitchenFederate(
                    federateName,
                    chefsCount,
                    dishInstancesCount,
                    minConsumptionTime,
                    maxConsumptionTime,
                    minPreparationTime,
                    maxPreparationTime
            ).runFederate(true, true);
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }
}
