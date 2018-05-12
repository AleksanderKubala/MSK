package Federates;

import Ambassadors.BasicAmbassador;
import FomInteractions.Events.FederationEvent;
import FomInteractions.Events.FederationTimedEvent;
import FomInteractions.Events.TimedEventComparator;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public abstract class BasicFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    protected RTIambassador rtiAmbassador;
    protected BasicAmbassador federateAmbassador;
    protected HLAfloat64TimeFactory timeFactory; // set when we join
    public EncoderFactory encoderFactory;

    protected List<FederationTimedEvent> internalEvents;

    protected String signature;

    protected BasicFederate(String federateName) {
        signature = federateName;
        internalEvents = new ArrayList<>();
    }

    protected abstract void publishAndSubscribe() throws RTIexception;
    protected abstract void setFederateAmbassador() throws RTIexception;
    protected abstract void processFederationNonTimedEvent(FederationEvent event) throws RTIexception;
    protected abstract void processFederationTimedEvent(FederationTimedEvent event) throws RTIexception;
    protected abstract void processNextInternalEvent(FederationTimedEvent event) throws RTIexception;
    protected abstract void afterSynchronization() throws RTIexception;

    /*
    protected void runFederate(boolean timeConstrained, boolean timeRegulating) throws RTIexception {
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        setFederateAmbassador();

        rtiAmbassador.connect(federateAmbassador, CallbackModel.HLA_EVOKED);
        createFederation();

        joinFederation(signature);

        timeFactory = (HLAfloat64TimeFactory) rtiAmbassador.getTimeFactory();

        registerSynchronizationPoint();
        waitForUser(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        awaitFederationSynchronization();

        setTimePolicy(timeConstrained, timeRegulating);
        publishAndSubscribe();
        afterSynchronization();

        while(federateAmbassador.isRunning()) {
            mainLoop();
        }
    }


    protected void mainLoop() throws RTIexception {

        boolean internalEventPending = false;
        double timeToAdvance = federateAmbassador.getFederateTime() + federateAmbassador.getFederateTimeStep();
        double nextInternalEventTime = 0.0;
        advanceTime(timeToAdvance);

        if(internalEvents.size() > 0) {
            internalEvents.sort(new TimedEventComparator());
            nextInternalEventTime = ((HLAfloat64Time) internalEvents.get(0).getTime()).getValue();
            timeToAdvance = nextInternalEventTime;
            nextEventRequest(timeToAdvance);
            internalEventPending = true;
        }

        if(federateAmbassador.federationNonTimedEvents.size() > 0) {
            for(FederationEvent event: federateAmbassador.federationNonTimedEvents) {
                processFederationNonTimedEvent(event);
            }
            federateAmbassador.federationNonTimedEvents.clear();
        }

        if(federateAmbassador.federationTimedEvents.size() > 0) {
            federateAmbassador.federationTimedEvents.sort(new TimedEventComparator());
            for(FederationTimedEvent event: federateAmbassador.federationTimedEvents) {
                processFederationTimedEvent(event);
            }
            int lastIndex = federateAmbassador.federationTimedEvents.size() - 1;
            LogicalTime logicalEventTime = federateAmbassador.federationTimedEvents.get(lastIndex).getTime();
            advanceTime(logicalEventTime);
            timeToAdvance = convertLogicalTime(logicalEventTime);
            federateAmbassador.federationTimedEvents.clear();
        }

        if( timeToAdvance > 0.0) {
            if (federateAmbassador.getGrantedTime() == timeToAdvance) {
                if(federateAmbassador.isRegulating()) {
                    timeToAdvance += federateAmbassador.getFederateLookahead();
                }
                federateAmbassador.setFederateTime(timeToAdvance);
                if(internalEventPending) {
                    if (federateAmbassador.getFederateTime() >= nextInternalEventTime) {
                        processNextInternalEvent(internalEvents.get(0));
                        internalEvents.remove(0);
                    }
                }
            }
        }
    }*/

    protected void log( String message )
    {
        System.out.println( signature + ": " + message );
    }

    protected LogicalTime convertTime(double time )
    {
        return timeFactory.makeTime(time);
    }

    protected double convertLogicalTime(LogicalTime time) {
        return ((HLAfloat64Time)time).getValue();
    }

    protected LogicalTimeInterval convertInterval(double time )
    {
        return timeFactory.makeInterval( time );
    }

    protected synchronized boolean createFederation() throws RTIexception{
        try
        {
            File fom = new File( "fom.fed" );
            rtiAmbassador.createFederationExecution( "Federation",
                    fom.toURI().toURL() );
            log( "Created Federation" );
            return true;
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
            return true;
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() );
            urle.printStackTrace();
            return false;
        }
    }

    protected void joinFederation(String federateName) throws RTIexception {
        rtiAmbassador.joinFederationExecution( federateName,            // name for the federate
                "BasicFederate",   // federate type
                "Federation" ); // name of federation


        log( "Joined Federation as " + federateName );
    }

    protected void registerSynchronizationPoint() throws RTIexception{
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while(!federateAmbassador.isAnnounced())
        {
            rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    protected void awaitFederationSynchronization() throws RTIexception {
        rtiAmbassador.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while(!federateAmbassador.isReadyToRun())
        {
            rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    protected void finish() throws RTIexception {
        rtiAmbassador.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );

        try
        {
            rtiAmbassador.destroyFederationExecution( "Federation" );
            log( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }
    }

    protected void setTimePolicy(boolean timeConstrained, boolean timeRegulating) throws RTIexception{

        LogicalTimeInterval lookahead = convertInterval( federateAmbassador.getFederateLookahead() );
        //LogicalTimeInterval interval = timeFactory.makeInterval()

        if(timeRegulating) {
            rtiAmbassador.enableTimeRegulation(lookahead);
            while(!federateAmbassador.isRegulating())
            {
                rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2);
            }
        }

        if(timeConstrained) {
            rtiAmbassador.enableTimeConstrained();
            while(!federateAmbassador.isConstrained())            {
                rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2);
            }
        }
    }

    protected void advanceTime( double timestep ) throws RTIexception
    {
        LogicalTime newTime = convertTime( timestep );
        advanceTime(newTime);
    }

    protected void advanceTime(LogicalTime timestep) throws RTIexception {
        federateAmbassador.setAdvancing(true);
        rtiAmbassador.timeAdvanceRequest( timestep );
        while( federateAmbassador.isAdvancing() )
        {
            rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }



    protected void nextEventRequest( double time ) throws RTIexception {
        federateAmbassador.setAdvancing(true);
        LogicalTime newTime = convertTime(time);
        rtiAmbassador.nextMessageRequest(newTime);

        while( federateAmbassador.isAdvancing() )
        {
            rtiAmbassador.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    protected void deleteObject( ObjectInstanceHandle handle ) throws RTIexception
    {
        rtiAmbassador.deleteObjectInstance( handle, generateTag() );
    }

    protected short getTimeAsShort()
    {
        return (short)federateAmbassador.getFederateTime();
    }

    protected byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    protected String waitForUser(String message)
    {
        log( message );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            return reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
            return null;
        }
    }

}
