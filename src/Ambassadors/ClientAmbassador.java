package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.EventType;
import FomInteractions.Events.UpdateSubscribedObjects;
import FomInteractions.Interactions.DishOrderInteraction;
import FomInteractions.Interactions.WaiterInteraction;
import FomObjects.Dish;
import FomObjects.FomObjectType;
import FomObjects.Table;
import hla.rti1516e.*;
import hla.rti1516e.encoding.ByteWrapper;

public class ClientAmbassador extends BasicAmbassador{

    public ObjectClassHandle tableClassHandle;
    public AttributeHandle tableNumberAttrHandle;
    public AttributeHandle freeSeatsNowAttrHandle;

    public ObjectClassHandle dishClassHandle;
    public AttributeHandle dishNumberAttrHandle;
    public AttributeHandle consumptionTimeAttrHandle;

    public InteractionClassHandle clientServicedHandle;
    public ParameterHandle clientNumberParamHandle;
    public ParameterHandle waiterNumberParamHandle;
    public InteractionClassHandle orderFilledHandle;
    public ParameterHandle dishNumberParamHandle;

    public ClientAmbassador(BasicFederate federate) {
        super(federate);
        signature = "ClientAmbassador";
    }

    @Override
    public void discoverObjectInstance( ObjectInstanceHandle theObject,
                                        ObjectClassHandle theObjectClass,
                                        String objectName )
    {
        log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName );
        instanceClassMap.putIfAbsent(theObject, theObjectClass);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo) {

        StringBuilder builder = new StringBuilder("Interaction received (time: " + time.toString() + "): ");
        if(interactionClass.equals(clientServicedHandle)) {
            builder.append("Client Serviced, ");
            int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
            int waiterNumber = theParameters.getValueReference(waiterNumberParamHandle).getInt();
            builder.append("Client number: " + clientNumber);
            WaiterInteraction interaction = new WaiterInteraction(time, EventType.CLIENT_SERVICED, clientNumber, waiterNumber);
            federationEvents.add(interaction);
        }
        if (interactionClass.equals(orderFilledHandle)) {
            builder.append("Order filled, ");
            int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
            int dishNumber = theParameters.getValueReference(dishNumberParamHandle).getInt();
            builder.append("Client number: " + clientNumber);
            DishOrderInteraction interaction = new DishOrderInteraction(time, EventType.ORDER_FILLED, clientNumber, dishNumber);
            federationEvents.add(interaction);
        }

        log(builder.toString());
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrdering,
                                        TransportationTypeHandle theTransport,
                                        LogicalTime time,
                                        OrderType receivedOrdering,
                                        SupplementalReflectInfo reflectInfo )
    {
        StringBuilder builder = new StringBuilder("Attribute reflection: ");
        ObjectClassHandle classHandle = instanceClassMap.get(theObject);
        if(classHandle.equals(tableClassHandle)) {
            builder.append("Table: (time: " + time + ")");
            federationEvents.add( new UpdateSubscribedObjects(
                    time,
                    EventType.OBJECT_UPDATE,
                    FomObjectType.TABLE,
                    retrieveTableObject(theObject, theAttributes)));

        }
        if(classHandle.equals(dishClassHandle)) {
            builder.append("Dish: (time:  " + time + ")");
            federationEvents.add(new UpdateSubscribedObjects(
                    time,
                    EventType.OBJECT_UPDATE,
                    FomObjectType.DISH,
                    retrieveDishObject(theObject, theAttributes)));
        }

        log(builder.toString());

    }

    private Table retrieveTableObject(ObjectInstanceHandle handle, AttributeHandleValueMap attributes) {
        ByteWrapper tableNumberWrapper = attributes.getValueReference(tableNumberAttrHandle);
        ByteWrapper freeSeatsNowWrapper = attributes.getValueReference(freeSeatsNowAttrHandle);

        int tableNumber = tableNumberWrapper.getInt();
        int freeSeatsNow = freeSeatsNowWrapper.getInt();

        return new Table(handle, tableNumber, freeSeatsNow);
    }

    private Dish retrieveDishObject(ObjectInstanceHandle handle, AttributeHandleValueMap attributes) {
        ByteWrapper dishNumberWrapper = attributes.getValueReference(dishNumberAttrHandle);
        ByteWrapper consumptionTimeWrapper = attributes.getValueReference(consumptionTimeAttrHandle);

        int dishNumber = dishNumberWrapper.getInt();
        double consumptionTime = (double)consumptionTimeWrapper.getInt();

        return new Dish(handle, dishNumber, consumptionTime);
    }
}
