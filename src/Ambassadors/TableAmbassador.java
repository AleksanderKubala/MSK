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
        if(interactionClass.equals(seatFreedHandle)) {
            builder.append("Seat freed, ");
            int tableNumber = retrieveTableNumber(theParameters);
            builder.append("Table number: " + tableNumber);
            TableInteraction interaction = new TableInteraction(time, EventType.SEAT_FREED, tableNumber);
            federationTimedEvents.add(interaction);
        }
        if (interactionClass.equals(seatTakenHandle)) {
            builder.append("Seat taken, ");
            int tableNumber = retrieveTableNumber(theParameters);
            builder.append("Table number: " + tableNumber);
            TableInteraction interaction = new TableInteraction(time, EventType.SEAT_TAKEN, tableNumber);
            federationTimedEvents.add(interaction);
        }

        log(builder.toString());
    }

    private int retrieveTableNumber(ParameterHandleValueMap parameters) {
        ByteWrapper wrapper = parameters.getValueReference(tableNumberParamHandle);
        return wrapper.getInt();
    }
}
