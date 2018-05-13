package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.EventType;
import FomInteractions.Interactions.ClientInteraction;
import hla.rti1516e.*;

public class WaiterAmbassador extends BasicAmbassador{

    public InteractionClassHandle clientWaitingHandle;
    public ParameterHandle clientNumberParamHandle;

    public WaiterAmbassador(BasicFederate federate) {
        super(federate);
        signature = "WaiterAmbassador";
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
        int clientNumber = theParameters.getValueReference(clientNumberParamHandle).getInt();
        builder.append("Client " + clientNumber);
        builder.append(" awaiting for service");
        ClientInteraction interaction = new ClientInteraction(time, EventType.CLIENT_WAITING, clientNumber);
        federationTimedEvents.add(interaction);
        log(builder.toString());
    }
}
