package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.InteractionType;
import FomInteractions.TableInteraction;
import hla.rti1516e.*;
import hla.rti1516e.encoding.ByteWrapper;


public class TableAmbassador extends BasicAmbassador {

    public InteractionClassHandle seatTakenIHandle;
    public InteractionClassHandle seatFreedIHandle;

    public ParameterHandle tableNumberHandle;

    public TableAmbassador(BasicFederate federate) {
        super(federate);
        signature = "TableAmbassador";
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
        if(interactionClass.equals(seatFreedIHandle)) {
            builder.append("Seat freed, ");
            int tableNumber = retrieveTableNumber(theParameters);
            builder.append("Table number: " + tableNumber);
            TableInteraction interaction = new TableInteraction(InteractionType.SEAT_FREED, time, tableNumber);
            federationEvents.add(interaction);
        }
        if (interactionClass.equals(seatFreedIHandle)) {
            builder.append("Seat taken, ");
            int tableNumber = retrieveTableNumber(theParameters);
            builder.append("Table number: " + tableNumber);
            TableInteraction interaction = new TableInteraction(InteractionType.SEAT_TAKEN, time, tableNumber);
            federationEvents.add(interaction);
        }
    }

    private int retrieveTableNumber(ParameterHandleValueMap parameters) {
        ByteWrapper wrapper = parameters.getValueReference(tableNumberHandle);
        return wrapper.getInt();
    }
}
