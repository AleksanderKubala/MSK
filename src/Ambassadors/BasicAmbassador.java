package Ambassadors;

import Federates.BasicFederate;
import FomInteractions.Events.TimedEvent;
import hla.rti1516e.*;
import hla.rti1516e.FederateAmbassador;
import hla.rti1516e.time.HLAfloat64Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicAmbassador extends NullFederateAmbassador {

    protected double federateTime;
    protected double federateTimeStep;
    protected double federateLookahead;
    protected double grantedTime;

    protected boolean isRegulating;
    protected boolean isConstrained;
    protected boolean isAdvancing;

    protected boolean isAnnounced;
    protected boolean isReadyToRun;

    protected boolean running;

    protected String signature;

    protected BasicFederate federate;

    public List<TimedEvent> federationEvents;
    protected Map<ObjectInstanceHandle, ObjectClassHandle> instanceClassMap;

    BasicAmbassador(BasicFederate federate) {
        federateTime = 0.0;
        federateTimeStep = 1.0;
        federateLookahead = 1.0;
        isRegulating = false;
        isConstrained = false;
        isAdvancing = false;
        isAnnounced = false;
        isReadyToRun = false;
        running = true;
        signature = "BasicAmbassador";
        federationEvents = new ArrayList<>();
        instanceClassMap = new HashMap<>();
    }



    protected void log( String message )
    {
        System.out.println( signature + ": " + message );
    }

    @Override
    public void synchronizationPointRegistrationFailed( String label,
                                                        SynchronizationPointFailureReason reason )
    {
        log( "Failed to register sync point: " + label + ", reason="+reason );
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
    public void federationSynchronized( String label, FederateHandleSet failed )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(BasicFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    @Override
    public void timeRegulationEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant( LogicalTime time )
    {
        this.grantedTime = ((HLAfloat64Time)time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance( ObjectInstanceHandle theObject,
                                        ObjectClassHandle theObjectClass,
                                        String objectName )
    {
        log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrder,
                                        TransportationTypeHandle transport,
                                        FederateAmbassador.SupplementalReflectInfo reflectInfo )
    {
        reflectAttributeValues( theObject,
                theAttributes,
                tag,
                sentOrder,
                transport,
                null,
                sentOrder,
                reflectInfo );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrdering,
                                        TransportationTypeHandle theTransport,
                                        LogicalTime time,
                                        OrderType receivedOrdering,
                                        FederateAmbassador.SupplementalReflectInfo reflectInfo )
    {
        StringBuilder builder = new StringBuilder( "Reflection for object:" );

        // print the handle
        builder.append( " handle=" + theObject );
        // print the tag
        builder.append( ", tag=" + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
        }

        // print the attribute information
        builder.append( ", attributeCount=" + theAttributes.size() );
        builder.append( "\n" );
        for( AttributeHandle attributeHandle : theAttributes.keySet() )
        {
            // print the attibute handle
            builder.append( "\tattributeHandle=" );

            // if we're dealing with Flavor, decode into the appropriate enum value


            builder.append( "\n" );
        }

        log( builder.toString() );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    FederateAmbassador.SupplementalReceiveInfo receiveInfo )
    {
        this.receiveInteraction( interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    hla.rti1516e.LogicalTime time,
                                    OrderType receivedOrdering,
                                    FederateAmbassador.SupplementalReceiveInfo receiveInfo )
    {
        StringBuilder builder = new StringBuilder( "Interaction Received:" );

        // print the handle
        builder.append( " handle=" + interactionClass );

        // print the tag
        builder.append( ", tag=" + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
        }

        // print the parameer information
        builder.append( ", parameterCount=" + theParameters.size() );
        builder.append( "\n" );
        for( ParameterHandle parameter : theParameters.keySet() )
        {
            // print the parameter handle
            builder.append( "\tparamHandle=" );
            builder.append( parameter );
            // print the parameter value
            builder.append( ", paramValue=" );
            builder.append( theParameters.get(parameter).length );
            builder.append( " bytes" );
            builder.append( "\n" );
        }

        log( builder.toString() );
    }

    @Override
    public void removeObjectInstance( ObjectInstanceHandle theObject,
                                      byte[] tag,
                                      OrderType sentOrdering,
                                      FederateAmbassador.SupplementalRemoveInfo removeInfo )
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

    public double getGrantedTime() {
        return grantedTime;
    }

    public void setAdvancing(boolean advancing) {
        isAdvancing = advancing;
    }

    public void setFederateTime(double federateTime) {
        this.federateTime = federateTime;
    }
}
