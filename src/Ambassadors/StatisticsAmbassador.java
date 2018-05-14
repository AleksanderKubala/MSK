package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Interactions.ClientInteraction;
import FomInteractions.Interactions.TableInteraction;
import hla.rti1516e.*;

public class StatisticsAmbassador extends BasicAmbassador {

    public InteractionClassHandle clientArrivedHandle;
    public InteractionClassHandle clientLeftQueueHandle;
    public ParameterHandle clientNumberParamHandle;

    public InteractionClassHandle seatTakenHandle;
    public ParameterHandle tableNumberParamHandle;

    public InteractionClassHandle finishHandle;

    public StatisticsAmbassador(BasicFederate federate) {
        super(federate);
        signature = "StatisticsAmbassador";
        //federateLookahead = 100.0;
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

            if (interactionClass.equals(clientArrivedHandle)) {
                builder.append("Client Arrived, ");
                int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
                builder.append("Client number: " + clientNumber);
                ClientInteraction interaction = new ClientInteraction(time, EventType.CLIENT_ARRIVED, clientNumber);
                federationTimedEvents.add(interaction);
            }
            if (interactionClass.equals(clientLeftQueueHandle)) {
                builder.append("Client Left Queue, ");
                int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
                builder.append("Client number: " + clientNumber);
                ClientInteraction interaction = new ClientInteraction(time, EventType.CLIENT_LEFT_QUEUE, clientNumber);
                federationTimedEvents.add(interaction);
            }
            if (interactionClass.equals(seatTakenHandle)) {
                builder.append("Seat taken, ");
                int tableNumber = theParameters.getValueReference(tableNumberParamHandle).getInt();
                int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
                builder.append("Table number: " + tableNumber);
                TableInteraction interaction = new TableInteraction(time, EventType.SEAT_TAKEN, clientNumber, tableNumber);
                federationTimedEvents.add(interaction);
            }
        if(interactionClass.equals(finishHandle)) {
            FederationTimedEvent finish = new FederationTimedEvent(time, EventType.FINISH);
            federationTimedEvents.add(finish);
        }
            log(builder.toString());

    }
}
