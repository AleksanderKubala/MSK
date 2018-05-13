package Federates;

import Ambassadors.WaiterAmbassador;
import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Interactions.ClientInteraction;
import FomInteractions.Interactions.WaiterInteraction;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaiterFederate extends BasicFederate {

    public InteractionClassHandle clientServicedHandle;
    public ParameterHandle clientNumberParamHandke;
    public ParameterHandle waiterNumberParamHandle;

    private int minServicingTime;
    private int maxServicingTime;

    private List<Integer> waitersList;
    private List<Integer> clientsWaiting;

    protected WaiterFederate(String federateName,
                             int waitersCount,
                             int averageServicingTime,
                             int servicingDeviation) {
        super(federateName);
        signature = "WaiterFederate";
        waitersList = new ArrayList<>();
        clientsWaiting = new ArrayList<>();
        for(int i = 0; i < waitersCount; i++) {
            waitersList.add(i);
        }
        minServicingTime = averageServicingTime - servicingDeviation;
        maxServicingTime = averageServicingTime + servicingDeviation;
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {
        clientServicedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientServiced");
        clientNumberParamHandke = rtiAmbassador.getParameterHandle(clientServicedHandle, "clientNumber");
        waiterNumberParamHandle = rtiAmbassador.getParameterHandle(clientServicedHandle, "waiterNumber");

        rtiAmbassador.publishInteractionClass(clientServicedHandle);

        InteractionClassHandle clientWaitingHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientWaiting");
        ((WaiterAmbassador)federateAmbassador).clientWaitingHandle = clientWaitingHandle;
        ((WaiterAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberParamHandke;

        rtiAmbassador.subscribeInteractionClass(clientWaitingHandle);
    }

    @Override
    protected void setFederateAmbassador() throws RTIexception {
        federateAmbassador = new WaiterAmbassador(this);
        //federateAmbassador.setFederateLookahead(minServicingTime);
    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {

    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case CLIENT_WAITING:
                ClientInteraction clientWaiting = (ClientInteraction)event;
                clientsWaiting.add(clientWaiting.getClientNumber());
                break;
        }
    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case CLIENT_SERVICED:
                double time = convertLogicalTime(event.getTime());
                WaiterInteraction clientServiced = (WaiterInteraction)event;
                waitersList.add(clientServiced.getWaiterNumber());
                sendWaiterInteraction(getNextTime(), clientServiced.getClientNumber(), clientServiced.getWaiterNumber());
                //log("(time: " + time + "): Handling internal event: Service: Client " + clientServiced.getClientNumber() +  ", Waiter " + clientServiced.getWaiterNumber());
                break;
        }
    }

    @Override
    protected void afterSynchronization() throws RTIexception {

    }

    public void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();
        configurateFederate(timeConstrained, timeRegulating);
        afterSynchronization();

        double nextInternalEventTime = 0.0;
        boolean internalEventsPending = false;

        while(federateAmbassador.isRunning()) {

            double timeToAdvance = getNextTime();

            if(!clientsWaiting.isEmpty()) {
                int counter = 0;
                for(int i = 0, j = clientsWaiting.size(); (i < waitersList.size()) && (j > 0); i++, j--) {
                    serviceClient(clientsWaiting.get(i), waitersList.get(i));
                    counter++;
                }
                for(int i =0; i < counter; i++) {
                    clientsWaiting.remove(0);
                    waitersList.remove(0);
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
    }

    private void sendWaiterInteraction(double time, int clientNumber, int waiterNumber) throws RTIexception {
        double currentTime = getNextTime();
        ParameterHandleValueMap params = rtiAmbassador.getParameterHandleValueMapFactory().create(2);
        HLAinteger32BE clientNumberValue = encoderFactory.createHLAinteger32BE(clientNumber);
        HLAinteger32BE waiterNumberValue = encoderFactory.createHLAinteger32BE(waiterNumber);
        params.put(clientNumberParamHandke, clientNumberValue.toByteArray());
        params.put(waiterNumberParamHandle, waiterNumberValue.toByteArray());

        HLAfloat64Time timeValue = timeFactory.makeTime(currentTime);
        log("(time: " + time + "): Client " + clientNumber + " serviced by Waiter " + waiterNumber);
        rtiAmbassador.sendInteraction(clientServicedHandle, params, generateTag(), timeValue);
    }

    private void serviceClient(int clientNumber, int waiterNumber) throws RTIexception {
        Random random = new Random();
        double time =  getNextTime();
        double serviceFinishedTime = getNextTime() + minServicingTime + random.nextInt(maxServicingTime - minServicingTime);
        LogicalTime serviceFinishedEventTime = convertTime(serviceFinishedTime);
        WaiterInteraction clientServiced = new WaiterInteraction(
                serviceFinishedEventTime,
                EventType.CLIENT_SERVICED,
                clientNumber,
                waiterNumber);
        internalEvents.add(clientServiced);
        //log("(time: " + time + "): Created internal event: Service: Client " + clientNumber +  ", Waiter " + waiterNumber + " at time " + serviceFinishedTime);
        log("(time: " + time + "): Client " + clientNumber + " is being serviced by Waiter " + waiterNumber);
     }

     public static void main(String[] args) {

        String federateName = "WaiterFederate";
        int waitersCount = 2;
        int averageServicingTime = 25;
        int servicingDeviation = 2;
         try {
             new WaiterFederate(federateName, waitersCount, averageServicingTime, servicingDeviation).runFederate(true, true);
         } catch (RTIexception rtIexception) {
             rtIexception.printStackTrace();
         }
     }
}
