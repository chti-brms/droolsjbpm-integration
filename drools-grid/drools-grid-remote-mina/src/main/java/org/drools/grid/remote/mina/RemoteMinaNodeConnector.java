package org.drools.grid.remote.mina;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;
import org.drools.grid.ConnectorException;
import org.drools.grid.ConnectorType;
import org.drools.grid.GenericConnection;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.GridConnection;
import org.drools.grid.NodeConnectionType;
import org.drools.grid.internal.Message;
import org.drools.grid.internal.MessageResponseHandler;
import org.drools.grid.internal.responsehandlers.BlockingMessageResponseHandler;

public class RemoteMinaNodeConnector
    implements
    GenericNodeConnector {

    protected IoSession           session;
    protected final String        name;
    protected AtomicInteger       counter;
    protected SocketConnector     connector;
    protected SocketAddress       address;
    protected SystemEventListener eventListener;
    protected GridConnection      connection;

    public RemoteMinaNodeConnector(String name,
                                   String providerAddress,
                                   Integer providerPort,
                                   SystemEventListener eventListener) {

        if ( name == null ) {
            throw new IllegalArgumentException( "Name can not be null" );
        }
        this.name = name;
        this.counter = new AtomicInteger();
        this.address = new InetSocketAddress( providerAddress,
                                              providerPort );

        this.eventListener = eventListener;
        this.connection = new GridConnection();
    }

    public void connect() throws ConnectorException {
        if ( this.session != null && this.session.isConnected() ) {
            throw new IllegalStateException( "Already connected. Disconnect first." );
        }

        try {
            this.connector = new NioSocketConnector();
            this.connector.setHandler( new MinaIoHandler( SystemEventListenerFactory.getSystemEventListener() ) );
            this.connector.getFilterChain().addLast( this.name + "codec" + UUID.randomUUID().toString(),
                                                     new ProtocolCodecFilter( new ObjectSerializationCodecFactory() ) );

            ConnectFuture future1 = this.connector.connect( this.address );
            future1.await( 2000 );
            if ( !future1.isConnected() ) {
                this.eventListener.info( "unable to connect : " + this.address + " : " + future1.getException() );
                Logger.getLogger( RemoteMinaNodeConnector.class.getName() ).log( Level.SEVERE,
                                                                                 null,
                                                                                 "The Node Connection Failed!" );
                throw new ConnectorException( "unable to connect : " + this.address + " : " + future1.getException() );
            }
            this.eventListener.info( "connected : " + this.address );
            this.session = future1.getSession();
        } catch ( Exception e ) {
            throw new ConnectorException( e );
        }
    }

    public void disconnect() throws ConnectorException {

        if ( this.session != null && this.session.isConnected() ) {

            CloseFuture future = this.session.getCloseFuture();

            future.addListener( new IoFutureListener<IoFuture>() {

                public void operationComplete(IoFuture future) {
                    System.out.println( "The remote node session is now closed" );
                }
            } );

            this.session.close( false );
            future.awaitUninterruptibly();

            this.connector.dispose();
        }

    }

    private void addResponseHandler(int id,
                                    MessageResponseHandler responseHandler) {
        ((MinaIoHandler) this.connector.getHandler()).addResponseHandler( id,
                                                                          responseHandler );
    }

    public void write(Message msg,
                      MessageResponseHandler responseHandler) {
        if ( responseHandler != null ) {
            addResponseHandler( msg.getResponseId(),
                                responseHandler );
        }
        this.session.write( msg );
    }

    public Message write(Message msg) throws ConnectorException {
        BlockingMessageResponseHandler responseHandler = new BlockingMessageResponseHandler();

        if ( responseHandler != null ) {
            addResponseHandler( msg.getResponseId(),
                                responseHandler );
        }
        this.session.write( msg );

        Message returnMessage = responseHandler.getMessage();
        if ( responseHandler.getError() != null ) {
            throw responseHandler.getError();
        }

        return returnMessage;
    }

    public String getId() {
        String hostName = ((InetSocketAddress) this.address).getHostName();
        int hostPort = ((InetSocketAddress) this.address).getPort();
        return "Remote:Mina:Node:" + hostName + ":" + hostPort;
    }

    public GenericConnection getConnection() {
        return this.connection;
    }

    public NodeConnectionType getNodeConnectionType() throws ConnectorException {
        return new RemoteMinaConnectionNode();
    }

    public ConnectorType getConnectorType() {
        return ConnectorType.REMOTE;
    }

    public int getSessionId() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public AtomicInteger getCounter() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
