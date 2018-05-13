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

    private double minConsumptionTime;
    private double maxConsumptionTime;
    private double minPreparationTime;
    private double maxPreparationTime;

    private int dishInstanceCount;

    private Map<Integer, Dish> dishInstanceMap;
    private List<FederationTimedEvent> internalEvents;

    public KitchenFederate(String federateName,
                           int dishInstancesCount,
                           double minConsumptionTime,
                           double maxConsumptionTime,
                           double minPreparationTime,
                           double maxPreparationTime) {
        super(federateName);
        this.dishInstanceCount = dishInstancesCount;
        this.minConsumptionTime = Math.floor(minConsumptionTime);
        this.maxConsumptionTime = Math.floor(maxConsumptionTime);
        this.minPreparationTime = Math.floor(minPreparationTime);
        this.maxPreparationTime = Math.floor(maxPreparationTime);
        dishInstanceMap = new HashMap<>();
        internalEvents = new ArrayList<>();
        //federateAmbassador.setFederateLookahead(minPreparationTime);
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

        ((KitchenAmbassador)federateAmbassador).orderPlacedHandle = orderPlacedHandle;
        ((KitchenAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberParamHandle;
        ((KitchenAmbassador)federateAmbassador).dishNumberParamHandle = dishNumberParamHandle;

        rtiAmbassador.subscribeInteractionClass(orderPlacedHandle);
    }

    @Override
    protected void setFederateAmbassador() throws RTIexception {

    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {

    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {

    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {

    }

    @Override
    protected void afterSynchronization() throws RTIexception {

    }


    private void registerDishInstances() throws RTIexception{

        Random random = new Random();
        int minConsumption = new Double(minConsumptionTime).intValue();
        int maxConsumption = new Double(maxConsumptionTime).intValue();
        int minPreparation = new Double(minConsumptionTime).intValue();
        int maxPreparation = new Double(maxConsumptionTime).intValue();

        for(int i = 0; i < dishInstanceCount; i++) {
            ObjectInstanceHandle handle = rtiAmbassador.registerObjectInstance(dishClassHandle);
            int consumption = minConsumption + random.nextInt(maxConsumption - minConsumption) + 1;
            int preparation = minPreparation + random.nextInt(maxPreparation - minPreparation) + 1;

            dishInstanceMap.put(i, new Dish(handle, i, (double)consumption, (double)preparation));
            log("Created Dish Instance (time: " + (federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead()) + ")");
        }
    }

    private void setDishInstancesAttributes(double time) throws RTIexception {

        for(int i = 0; i < dishInstanceMap.size(); i++) {
            AttributeHandleValueMap attributes = rtiAmbassador.getAttributeHandleValueMapFactory().create(2);

            HLAinteger32BE dishNumberValue = encoderFactory.createHLAinteger32BE(dishInstanceMap.get(i).getDishNumber());
            HLAfloat64BE consumptionTimeValue = encoderFactory.createHLAfloat64BE(dishInstanceMap.get(i).getConsumptionTime());

            attributes.put(dishNumberAttrHandle, dishNumberValue.toByteArray());
            attributes.put(consumptionTimeAttrHandle, consumptionTimeValue.toByteArray());
            LogicalTime logicalTime = convertTime(time);
            rtiAmbassador.updateAttributeValues(dishInstanceMap.get(i).getInstanceHandle(), attributes, "setting tables parameters".getBytes(), logicalTime);
            log("Updated Table Instance (time: " + time + ")");
        }
    }

    private void sendOrderFilled(double time, int clientNumber, int dishNumber) throws RTIexception {
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(2);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        HLAinteger32BE dishNumbeValue = encoderFactory.createHLAinteger32BE(dishNumber);
        params.put(clientNumberParamHandle, clientNumberValue.toByteArray());
        params.put(dishNumberParamHandle, dishNumbeValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime( time);
        log("(time: " + time + "): Order from client " + clientNumber + " filled");
        rtiAmbassador.sendInteraction(orderFilledHandle, params, generateTag(), timeValue);
    }

    private void orderPlaced(double time, DishOrderInteraction orderPlaced) {
        double preparationTime = dishInstanceMap.get(orderPlaced.getDishNumber()).getPreparationTime();
        LogicalTime orderFillTime = convertTime(preparationTime);
        DishOrderInteraction orderFilled = new DishOrderInteraction(
                orderFillTime,
                EventType.ORDER_FILLED,
                orderPlaced.getClientNumber(),
                orderPlaced.getDishNumber()
        );
        internalEvents.add(orderFilled);
    }


}
