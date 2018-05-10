package Ambassadors;

import Federates.BasicFederate;
import hla.rti.*;

import hla.rti.LogicalTime;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import org.portico.impl.hla13.types.DoubleTime;

public class BasicAmbassador extends NullFederateAmbassador {

    private double federateTime;
    private double federateTimeStep;
    private double federateLookahead;

    private boolean isRegulating;
    private boolean isConstrained;
    private boolean isAdvancing;

    private boolean isAnnounced;
    private boolean isReadyToRun;

    private boolean running;

    private String signature;

    BasicAmbassador() {
        federateTime = 0.0;
        federateTimeStep = 1.0;
        federateLookahead = 0.0;
        isRegulating = false;
        isConstrained = false;
        isAdvancing = false;
        isAnnounced = false;
        isReadyToRun = false;
        running = true;
        signature = "BasicAmbassador";
    }

    private double convertTime( LogicalTime logicalTime )
    {
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( signature + ": " + message );
    }

    @Override
    public void synchronizationPointRegistrationFailed( String label )
    {
        log( "Failed to register sync point: " + label );
    }

    @Override
    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    @Override
    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(BasicFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(BasicFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    @Override
    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }

    @Override
    public void reflectAttributeValues( int theObject,
                                        ReflectedAttributes theAttributes,
                                        byte[] tag )
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        reflectAttributeValues( theObject, theAttributes, tag, null, null );
    }

    @Override
    public void reflectAttributeValues( int theObject,
                                        ReflectedAttributes theAttributes,
                                        byte[] tag,
                                        LogicalTime theTime,
                                        EventRetractionHandle retractionHandle )
    {
        StringBuilder builder = new StringBuilder( "Reflection for object:" );

        // print the handle
        builder.append( " handle=" + theObject );
        // print the tag
        builder.append( ", tag=" + EncodingHelpers.decodeString(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( theTime != null )
        {
            builder.append( ", time=" + convertTime(theTime) );
        }

        // print the attribute information
        builder.append( ", attributeCount=" + theAttributes.size() );
        builder.append( "\n" );
        for( int i = 0; i < theAttributes.size(); i++ )
        {
            try
            {
                // print the attibute handle
                builder.append( "\tattributeHandle=" );
                builder.append( theAttributes.getAttributeHandle(i) );
                // print the attribute value
                builder.append( ", attributeValue=" );
                builder.append(
                        EncodingHelpers.decodeString(theAttributes.getValue(i)) );
                builder.append( "\n" );
            }
            catch( ArrayIndexOutOfBounds aioob )
            {
                // won't happen
            }
        }

        log( builder.toString() );
    }

    @Override
    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag )
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        receiveInteraction( interactionClass, theInteraction, tag, null, null );
    }

    @Override
    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag,
                                    LogicalTime theTime,
                                    EventRetractionHandle eventRetractionHandle )
    {
        StringBuilder builder = new StringBuilder( "Interaction Received:" );

        // print the handle
        builder.append( " handle=" + interactionClass );
        // print the tag
        builder.append( ", tag=" + EncodingHelpers.decodeString(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( theTime != null )
        {
            builder.append( ", time=" + convertTime(theTime) );
        }

        // print the parameer information
        builder.append( ", parameterCount=" + theInteraction.size() );
        builder.append( "\n" );
        for( int i = 0; i < theInteraction.size(); i++ )
        {
            try
            {
                // print the parameter handle
                builder.append( "\tparamHandle=" );
                builder.append( theInteraction.getParameterHandle(i) );
                // print the parameter value
                builder.append( ", paramValue=" );
                builder.append(
                        EncodingHelpers.decodeString(theInteraction.getValue(i)) );
                builder.append( "\n" );
            }
            catch( ArrayIndexOutOfBounds aioob )
            {
                // won't happen
            }
        }

        log( builder.toString() );
    }

    @Override
    public void removeObjectInstance( int theObject, byte[] userSuppliedTag )
    {
        log( "Object Removed: handle=" + theObject );
    }

    @Override
    public void removeObjectInstance( int theObject,
                                      byte[] userSuppliedTag,
                                      LogicalTime theTime,
                                      EventRetractionHandle retractionHandle )
    {
        log( "Object Removed: handle=" + theObject );
    }

    public double getFederateTime() {
        return federateTime;
    }

    public double getFederateTimeStep() {
        return federateTimeStep;
    }

    public double getFederateLookahead() {
        return federateLookahead;
    }

    public boolean isRegulating() {
        return isRegulating;
    }

    public boolean isConstrained() {
        return isConstrained;
    }

    public boolean isAdvancing() {
        return isAdvancing;
    }

    public boolean isAnnounced() {
        return isAnnounced;
    }

    public boolean isReadyToRun() {
        return isReadyToRun;
    }

    public boolean isRunning() {
        return running;
    }

    public void setAdvancing(boolean advancing) {
        isAdvancing = advancing;
    }
}
