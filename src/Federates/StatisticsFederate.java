package Federates;

import Ambassadors.StatisticsAmbassador;
import Federates.FederatesInternal.ClientStatistics;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Interactions.ClientInteraction;
import FomInteractions.Interactions.TableInteraction;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.exceptions.RTIexception;
import org.portico.lrc.model.Mom;

import java.util.HashMap;
import java.util.Map;

public class StatisticsFederate extends BasicFederate {

    private Map<Integer, ClientStatistics> clientStatistics;

    protected StatisticsFederate(String federateName) {
        super(federateName);
        clientStatistics = new HashMap<>();
    }

    @Override
    protected void publishAndSubscribe() throws RTIexception {

        InteractionClassHandle seatTakenHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.TableInteraction.SeatTaken");
        ParameterHandle tableNumberParamHandle = rtiAmbassador.getParameterHandle(seatTakenHandle, "tableNumber");

        InteractionClassHandle clientArrivedHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientArrived");
        InteractionClassHandle clientLeftQueueHandle = rtiAmbassador.getInteractionClassHandle("InteractionRoot.ClientInteraction.ClientLeftQueue");
        ParameterHandle clientNumberParamHandle = rtiAmbassador.getParameterHandle(clientArrivedHandle, "clientNumber");

        ((StatisticsAmbassador)federateAmbassador).seatTakenHandle = seatTakenHandle;
        ((StatisticsAmbassador)federateAmbassador).tableNumberParamHandle = tableNumberParamHandle;
        ((StatisticsAmbassador)federateAmbassador).clientArrivedHandle = clientArrivedHandle;
        ((StatisticsAmbassador)federateAmbassador).clientLeftQueueHandle = clientLeftQueueHandle;
        ((StatisticsAmbassador)federateAmbassador).clientNumberParamHandle = clientNumberParamHandle;

        rtiAmbassador.subscribeInteractionClass(seatTakenHandle);
        rtiAmbassador.subscribeInteractionClass(clientArrivedHandle);
        rtiAmbassador.subscribeInteractionClass(clientLeftQueueHandle);
    }

    @Override
    protected void setFederateAmbassador() throws RTIexception {
        federateAmbassador = new StatisticsAmbassador(this);
    }

    @Override
    protected void processFederationNonTimedEvent(FederationEvent event) throws RTIexception {

    }

    @Override
    protected void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception {
        switch(event.getType()) {
            case CLIENT_ARRIVED:
                ClientInteraction clientInteraction = (ClientInteraction)event;
                insertStatisticsForClient(convertLogicalTime(event.getTime()), clientInteraction.getClientNumber());
                break;
            case SEAT_FREED:
                TableInteraction tableInteraction = (TableInteraction)event;
                updateStatisticsForClient(convertLogicalTime(event.getTime()), tableInteraction.getClientNumber());
                break;
            case CLIENT_LEFT_QUEUE:
                clientInteraction = (ClientInteraction)event;
                eraseStatisticsForClient(clientInteraction.getClientNumber());
                break;
        }
    }

    @Override
    protected void processNextInternalEvent(FederationTimedEvent event) throws RTIexception {

    }

    @Override
    protected void afterSynchronization() throws RTIexception {

    }

    public void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        synchronizeWithFederation();
        configurateFederate(timeConstrained, timeRegulating);
        afterSynchronization();

        while(federateAmbassador.isRunning()) {

            double timeToAdvance = federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead();
            advanceTime(timeToAdvance);

            if(federateAmbassador.federationTimedEvents.size() > 0) {
                federateAmbassador.federationTimedEvents.sort(new TimedEventComparator());
                int lastEventIndex = federateAmbassador.federationTimedEvents.size() - 1;
                //timeToAdvance = convertLogicalTime(federateAmbassador.federationTimedEvents.get(lastEventIndex).getTime());
                for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                    processFederationTimedEvent(event);
                }
                federateAmbassador.federationTimedEvents.clear();
            }

            if(federateAmbassador.getGrantedTime() == timeToAdvance) {
                federateAmbassador.setFederateTime(timeToAdvance);
            }
        }
    }

    public static void main(String[] args) {
        try {
            String federateName = "StatisticsFederate";
            boolean timeConstrained = true;
            boolean timeRegulating = true;

            new StatisticsFederate(federateName).runFederate(timeConstrained, timeRegulating);
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

    private void insertStatisticsForClient(double time, int clientNumber) {
        ClientStatistics statistics = new ClientStatistics(time);
        clientStatistics.put(clientNumber, statistics);
        log("Client " + clientNumber + " arrival time: " + time + " saved to statistics");
    }

    private void updateStatisticsForClient(double time, int clientNumber) {
        ClientStatistics statistics = clientStatistics.get(clientNumber);
        statistics.setSeatTakenTime(time);
        clientStatistics.put(clientNumber, statistics);
        log("Client " + clientNumber + " taking seat time: " + time + " saved to statistics");
    }

    private void eraseStatisticsForClient(int clientNumber) {
        clientStatistics.remove(clientNumber);
        log("Client " + clientNumber + " removed from statistics due to his impatience");
    }
}
