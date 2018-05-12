package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.EventType;
import FomInteractions.Interactions.TableInteraction;
import hla.rti1516e.*;
import hla.rti1516e.encoding.ByteWrapper;


public class TableAmbassador extends BasicAmbassador {

    public InteractionClassHandle seatTakenHandle;
    public InteractionClassHandle seatFreedHandle;

    public ParameterHandle tableNumberParamHandle;
    public ParameterHandle clientNumberParamHandle;

    public TableAmbassador(BasicFederate federate) {
        super(federate);
        signature = "TableAmbassador";
        federateLookahead = 1.0;
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
        int tableNumber = theParameters.getValueReference(tableNumberParamHandle).getInt();
        int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
        builder.append("Client " + clientNumber);
        if(interactionClass.equals(seatFreedHandle)) {
            builder.append(" freed seat at table " + tableNumber);
            TableInteraction interaction = new TableInteraction(time, EventType.SEAT_FREED, clientNumber, tableNumber);
            federationTimedEvents.add(interaction);
        }
        if (interactionClass.equals(seatTakenHandle)) {
            builder.append(" sat at table " + tableNumber);
            TableInteraction interaction = new TableInteraction(time, EventType.SEAT_TAKEN, clientNumber, tableNumber);
            federationTimedEvents.add(interaction);
        }

        log(builder.toString());
    }

}
