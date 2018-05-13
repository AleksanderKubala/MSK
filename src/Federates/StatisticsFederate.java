package Federates;

import Ambassadors.StatisticsAmbassador;
import Federates.FederatesInternal.ClientStatistics;
import FomInteractions.Events.EventType;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import FomInteractions.Interactions.ClientInteraction;
import FomInteractions.Interactions.TableInteraction;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.exceptions.RTIexception;

import java.util.*;

public class StatisticsFederate extends BasicFederate {

    private Map<Integer, ClientStatistics> clientStatistics;
    private Map<Integer, ClientStatistics> impatientClientStatistics;

    protected StatisticsFederate(String federateName) {
        super(federateName);
        clientStatistics = new HashMap<>();
        impatientClientStatistics = new HashMap<>();
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
            case SEAT_TAKEN:
                TableInteraction tableInteraction = (TableInteraction)event;
                updateStatisticsForClient(convertLogicalTime(event.getTime()), tableInteraction.getClientNumber());
                break;
            case CLIENT_LEFT_QUEUE:
                clientInteraction = (ClientInteraction)event;
                eraseStatisticsForClient(convertLogicalTime(event.getTime()), clientInteraction.getClientNumber());
                break;
            case FINISH:
                federateAmbassador.stop();
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
                List<FederationTimedEvent> arrivals = new ArrayList<>();
                List<FederationTimedEvent> awaitEnds = new ArrayList<>();
                for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                    if(event.getType() == EventType.CLIENT_ARRIVED) {
                        arrivals.add(event);
                    }
                    if((event.getType() == EventType.SEAT_TAKEN) || (event.getType() == EventType.CLIENT_LEFT_QUEUE)) {
                        awaitEnds.add(event);
                    }
                }
                for(FederationTimedEvent event: arrivals) {
                    processFederationTimedEvent(event);
                }
                for(FederationTimedEvent event: awaitEnds) {
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
        String federateName = "StatisticsFederate";
        boolean timeConstrained = true;
        boolean timeRegulating = true;

        StatisticsFederate statistics = new StatisticsFederate(federateName);
        try {
            statistics.runFederate(timeConstrained, timeRegulating);
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }

        statistics.calculateAverageAwaitTime(false);
        statistics.calculateAverageAwaitTime(true);
    }

    private void insertStatisticsForClient(double time, int clientNumber) {
        ClientStatistics statistics = new ClientStatistics(time);
        clientStatistics.put(clientNumber, statistics);
        log("Client " + clientNumber + " arrival time: " + time + " saved to statistics");
    }

    private void updateStatisticsForClient(double time, int clientNumber) {
        ClientStatistics statistics = clientStatistics.get(clientNumber);
        statistics.setAwaitEndTime(time);
        clientStatistics.put(clientNumber, statistics);
        log("Client " + clientNumber + " taking seat time: " + time + " saved to statistics");
    }

    private void eraseStatisticsForClient(double time, int clientNumber) {
        ClientStatistics statistics = clientStatistics.get(clientNumber);
        statistics.setAwaitEndTime(time);
        impatientClientStatistics.put(clientNumber, statistics);
        clientStatistics.remove(clientNumber);
        log("Client " + clientNumber + " moved into separate statistics list due to his impatience");
    }

    public void calculateAverageAwaitTime(boolean includeImpatientClients) {
        double timeSum = 0.0;
        int count = clientStatistics.size();
        Collection<ClientStatistics> statistics = clientStatistics.values();
        for(ClientStatistics statistic: statistics) {
            if(statistic.getAwaitEndTime() != null) {
                timeSum += (statistic.getAwaitEndTime() - statistic.getArrivalTime());
            }
        }
        if(includeImpatientClients) {
            count += impatientClientStatistics.size();
            Collection<ClientStatistics> impatientStatistics = impatientClientStatistics.values();
            for(ClientStatistics statistic: impatientStatistics) {
                if(statistic.getAwaitEndTime() != null) {
                    timeSum += (statistic.getAwaitEndTime() - statistic.getArrivalTime());
                }
            }
        }
        double averageAwaitTime = timeSum/((double)count);
        log("Average await time: " + averageAwaitTime);
    }

}
