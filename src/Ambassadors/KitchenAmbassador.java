package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Interactions.DishOrderInteraction;
import hla.rti1516e.*;

public class KitchenAmbassador extends BasicAmbassador {

    public InteractionClassHandle orderPlacedHandle;
    public ParameterHandle clientNumberParamHandle;
    public ParameterHandle dishNumberParamHandle;

    public InteractionClassHandle finishHandle;

    public KitchenAmbassador(BasicFederate federate) {
        super(federate);
        signature = "KitchenAmbassador";
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
        if(interactionClass.equals(orderPlacedHandle)) {
            builder.append("Order placed, ");
            int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
            int dishNumber = theParameters.getValueReference(dishNumberParamHandle).getInt();
            builder.append("Table number: " + clientNumber + ", Dish number: " + dishNumber);
            DishOrderInteraction interaction = new DishOrderInteraction(time, EventType.ORDER_PLACED, clientNumber, dishNumber, -1);
            federationTimedEvents.add(interaction);
        }
        if(interactionClass.equals(finishHandle)) {
            FederationTimedEvent finish = new FederationTimedEvent(time, EventType.FINISH);
            federationTimedEvents.add(finish);
        }
        log(builder.toString());
    }
}
