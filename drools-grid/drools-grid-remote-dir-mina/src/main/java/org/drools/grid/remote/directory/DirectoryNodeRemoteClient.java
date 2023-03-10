/*
 *  Copyright 2010 salaboy.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.drools.grid.remote.directory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.grid.ConnectorException;
import org.drools.grid.DirectoryNodeService;
import org.drools.grid.GenericConnectorFactory;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.NodeConnectionType;
import org.drools.grid.internal.Message;
import org.drools.grid.internal.MessageResponseHandler;
import org.drools.grid.internal.MessageSession;
import org.drools.grid.internal.commands.SimpleCommand;
import org.drools.grid.internal.commands.SimpleCommandName;
import org.drools.grid.internal.responsehandlers.BlockingMessageDirectoryMapRequestResponseHandler;
import org.drools.grid.internal.responsehandlers.BlockingMessageResponseHandler;
import org.drools.grid.remote.KnowledgeBaseRemoteClient;
import org.drools.grid.remote.mina.MinaIoHandler;

public class DirectoryNodeRemoteClient
    implements
    DirectoryNodeService {

    private GenericNodeConnector connector;

    DirectoryNodeRemoteClient(GenericNodeConnector connector) {
        this.connector = connector;
    }

    public void register(String executorId,
                         String resourceId) throws ConnectorException,
                                           RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 2 );
        args.add( executorId );
        args.add( resourceId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RegisterExecutor,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        write( msg,
               null );
        this.connector.disconnect();

    }

    public void register(String executorId,
                         GenericNodeConnector resourceConnector) throws ConnectorException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public GenericNodeConnector lookup(String resourceId) throws ConnectorException,
                                                         RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 1 );
        args.add( resourceId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestLookupSessionId,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        this.connector.disconnect();
        return GenericConnectorFactory.newConnector( (String) resultcmd.getArguments().get( 0 ) );
    }

    public void registerKBase(String kbaseId,
                              String resourceId) throws ConnectorException,
                                                RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 2 );
        args.add( kbaseId );
        args.add( resourceId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RegisterKBase,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        write( msg,
               null );
        this.connector.disconnect();
    }

    public KnowledgeBase lookupKBase(String kbaseId) throws ConnectorException,
                                                    RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 1 );
        args.add( kbaseId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestKBaseId,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        String connectorString = (String) resultcmd.getArguments().get( 0 );
        GenericNodeConnector currentConnector = GenericConnectorFactory.newConnector( connectorString );
        this.connector.disconnect();
        return new KnowledgeBaseRemoteClient( kbaseId,
                                              currentConnector,
                                              new MessageSession() );
    }

    public Map<String, String> getExecutorsMap() throws ConnectorException,
                                                RemoteException {
        this.connector.connect();
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestExecutorsMap,
                                               null );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        this.connector.disconnect();
        return (Map<String, String>) resultcmd.getArguments().get( 0 );
    }

    public void dispose() throws ConnectorException,
                         RemoteException {
        this.connector.disconnect();
    }

    public String lookupId(String resourceId) throws ConnectorException,
                                             RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 1 );
        args.add( resourceId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestLookupSessionId,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        this.connector.disconnect();
        return (String) resultcmd.getArguments().get( 0 );
    }

    private Message write(Message msg) throws ConnectorException,
                                      RemoteException {
        BlockingMessageResponseHandler responseHandler = new BlockingMessageResponseHandler();

        if ( responseHandler != null ) {
            addResponseHandler( msg.getResponseId(),
                                responseHandler );
        }
        ((RemoteMinaDirectoryConnector) this.connector).getSession().write( msg );

        Message returnMessage = responseHandler.getMessage();
        if ( responseHandler.getError() != null ) {
            throw responseHandler.getError();
        }

        return returnMessage;
    }

    private void write(Message msg,
                       MessageResponseHandler responseHandler) {
        if ( responseHandler != null ) {
            addResponseHandler( msg.getResponseId(),
                                responseHandler );
        }

        ((RemoteMinaDirectoryConnector) this.connector).getSession().write( msg );

    }

    private void addResponseHandler(int id,
                                    MessageResponseHandler responseHandler) {
        ((MinaIoHandler) ((RemoteMinaDirectoryConnector) this.connector).getSession().getHandler()).addResponseHandler( id,
                                                                                                                        responseHandler );

    }

    public String getId() throws ConnectorException {
        return "Remote:Directory";
    }

    public void unregister(String executorId) throws ConnectorException,
                                             RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 2 );
        args.add( executorId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.UnRegisterExecutor,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        write( msg,
               null );
        this.connector.disconnect();
    }

    public Map<String, String> getKBasesMap() throws ConnectorException,
                                             RemoteException {
        this.connector.connect();
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestKBasesMap,
                                               null );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        this.connector.disconnect();
        return (Map<String, String>) resultcmd.getArguments().get( 0 );
    }

    public void registerKBase(String kbaseId,
                              KnowledgeBase kbase) throws ConnectorException,
                                                  RemoteException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void unregisterKBase(String kbaseId) throws ConnectorException,
                                               RemoteException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public NodeConnectionType getNodeConnectionType() throws ConnectorException,
                                                     RemoteException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public ServiceType getServiceType() throws ConnectorException,
                                       RemoteException {
        return ServiceType.REMOTE;
    }

    public String lookupKBaseLocationId(String kbaseId) throws ConnectorException, RemoteException {
        this.connector.connect();
        List<Object> args = new ArrayList<Object>( 1 );
        args.add( kbaseId );
        SimpleCommand cmd = new SimpleCommand( this.connector.getCounter().getAndIncrement(),
                                               SimpleCommandName.RequestKBaseId,
                                               args );
        Message msg = new Message( this.connector.getSessionId(),
                                   this.connector.getCounter().incrementAndGet(),
                                   false,
                                   cmd );
        BlockingMessageDirectoryMapRequestResponseHandler handler = new BlockingMessageDirectoryMapRequestResponseHandler();
        write( msg,
               handler );
        SimpleCommand resultcmd = (SimpleCommand) handler.getMessage().getPayload();
        String connectorString = (String) resultcmd.getArguments().get( 0 );
        return connectorString;
    }
}
