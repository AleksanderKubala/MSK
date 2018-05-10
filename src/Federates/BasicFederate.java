package Federates;

import Ambassadors.BasicAmbassador;
import hla.rti.*;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.File;
import java.net.MalformedURLException;

public abstract class BasicFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    protected RTIambassador rtiAmbassador;
    protected BasicAmbassador federateAmbassador;

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
        return new DoubleTime( time );
    }

    protected LogicalTimeInterval convertInterval(double time )
    {
        return new DoubleTimeInterval( time );
    }

    protected boolean createFederation() throws RTIexception{
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
        rtiAmbassador.joinFederationExecution( federateName, "ExampleFederation", federateAmbassador );
        log( "Joined Federation as " + federateName );
    }

    protected void registerSynchronizationPoint() throws RTIexception{
        rtiAmbassador.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while(!federateAmbassador.isAnnounced())
        {
            rtiAmbassador.tick();
        }
    }

    protected void awaitFederationSynchronization() throws RTIexception {
        rtiAmbassador.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while(!federateAmbassador.isReadyToRun())
        {
            rtiAmbassador.tick();
        }
    }

    protected void finish() throws RTIexception {
        rtiAmbassador.resignFederationExecution( ResignAction.NO_ACTION );
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

        LogicalTime currentTime = convertTime( federateAmbassador.getFederateTime() );
        LogicalTimeInterval lookahead = convertInterval( federateAmbassador.getFederateLookahead() );

        if(timeRegulating) {
            rtiAmbassador.enableTimeRegulation(currentTime, lookahead);
            while(!federateAmbassador.isRegulating())
            {
                rtiAmbassador.tick();
            }
        }

        if(timeConstrained) {
            rtiAmbassador.enableTimeConstrained();
            while(!federateAmbassador.isConstrained())            {
                rtiAmbassador.tick();
            }
        }
    }

    protected void advanceTime( double timestep ) throws RTIexception
    {
        federateAmbassador.setAdvancing(true);
        LogicalTime newTime = convertTime( federateAmbassador.getFederateTime() + timestep );
        rtiAmbassador.timeAdvanceRequest( newTime );

        while( federateAmbassador.isAdvancing() )
        {
            rtiAmbassador.tick();
        }
    }

    protected void deleteObject( int handle ) throws RTIexception
    {
        rtiAmbassador.deleteObjectInstance( handle, generateTag() );
    }

    protected double getLbts()
    {
        return federateAmbassador.getFederateTime() + federateAmbassador.getFederateLookahead();
    }

    protected byte[] generateTag()
    {
        return (""+System.currentTimeMillis()).getBytes();
    }

}
