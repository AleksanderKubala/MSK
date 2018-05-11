package Federates;

import Ambassadors.BasicAmbassador;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

public abstract class BasicFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    protected RTIambassador rtiAmbassador;
    protected BasicAmbassador federateAmbassador;
    protected HLAfloat64TimeFactory timeFactory; // set when we join
    public EncoderFactory encoderFactory;

    protected String signature;

    BasicFederate() {
        signature = "BasicFederate";
    }

    protected abstract void publishAndSubscribe() throws RTIexception;
    protected abstract void runFederate(String federateName) throws RTIexception;

    protected void log( String message )
    {
        System.out.println( signature + ": " + message );
    }

    protected LogicalTime convertTime(double time )
    {
        return timeFactory.makeTime(time);
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
        federateAmbassador.setAdvancing(true);
        LogicalTime newTime = convertTime( timestep );
        rtiAmbassador.timeAdvanceRequest( newTime );

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

    public ObjectClassHandle retrieveClassHandle(ObjectInstanceHandle handle) throws RTIexception {
        ObjectClassHandle classHandle = rtiAmbassador.getKnownObjectClassHandle(handle);
        return classHandle;
    }

}
