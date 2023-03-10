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
package org.drools.grid.remote;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.drools.builder.DirectoryLookupFactoryService;
import org.drools.command.KnowledgeContextResolveFromContextCommand;
import org.drools.grid.ConnectorException;
import org.drools.grid.DirectoryNodeService;
import org.drools.grid.GenericConnection;
import org.drools.grid.GenericConnectorFactory;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.internal.Message;
import org.drools.grid.internal.MessageSession;
import org.drools.grid.internal.commands.LookupCommand;
import org.drools.grid.internal.commands.RegisterCommand;
import org.drools.grid.internal.commands.UnRegisterCommand;
import org.drools.runtime.CommandExecutor;
import org.drools.runtime.StatefulKnowledgeSession;

public class DirectoryLookupProviderRemoteClient
    implements
    DirectoryLookupFactoryService {

    private GenericNodeConnector connector;
    private GenericConnection    connection;
    private MessageSession       messageSession;

    public DirectoryLookupProviderRemoteClient(GenericNodeConnector connector,
                                               GenericConnection connection) {
        this.connector = connector;
        this.messageSession = new MessageSession();
        this.connection = connection;
    }

    public void register(String identifier,
                         CommandExecutor executor) {
        try {
            String commandId = "client.lookup" + this.messageSession.getNextId();
            String kresultsId = "kresults_" + this.messageSession.getSessionId();
            int type;
            if ( executor instanceof StatefulKnowledgeSession ) {
                type = 0;
            } else {
                throw new IllegalArgumentException( "Type is not supported for registration" );
            }

            DirectoryNodeService directoryNode = this.connection.getDirectoryNode().get( DirectoryNodeService.class );
            try {

                directoryNode.register( identifier,
                                        this.connector.getId() );
            } catch ( RemoteException ex ) {
                Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                             null,
                                                                                             ex );
            }

            Message msg = new Message( this.messageSession.getSessionId(),
                                       this.messageSession.getCounter().incrementAndGet(),
                                       false,
                                       new KnowledgeContextResolveFromContextCommand(
                                                                                      new RegisterCommand( identifier,
                                                                                                           ((StatefulKnowledgeSessionRemoteClient) executor).getInstanceId(),
                                                                                                           type ),
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null ) );

            try {
                this.connector.connect();
                Object object = this.connector.write( msg ).getPayload();
                //                if (!(object instanceof FinishedCommand)) {
                //                    throw new RuntimeException("Response was not correctly ended");
                //                }
                this.connector.disconnect();
            } catch ( Exception e ) {
                throw new RuntimeException( "Unable to execute message",
                                            e );
            }
            try {
                directoryNode.dispose();
            } catch ( RemoteException ex ) {
                Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                             null,
                                                                                             ex );
            }

        } catch ( ConnectorException ex ) {
            Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                         null,
                                                                                         ex );
        }

    }

    public CommandExecutor lookup(String identifier) {

        try {
            String commandId = "client.lookup" + this.messageSession.getNextId();
            String kresultsId = "kresults_" + this.messageSession.getSessionId();
            Message msg = new Message( this.messageSession.getSessionId(),
                                       this.messageSession.getCounter().incrementAndGet(),
                                       false,
                                       new KnowledgeContextResolveFromContextCommand( new LookupCommand( identifier,
                                                                                                         commandId ),
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      kresultsId ) );
            DirectoryNodeService directoryNode = this.connection.getDirectoryNode().get( DirectoryNodeService.class );
            String connectorString = "";
            try {
                connectorString = directoryNode.lookupId( identifier );
                directoryNode.dispose();
            } catch ( RemoteException ex ) {
                Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                             null,
                                                                                             ex );
            }

            this.connector = GenericConnectorFactory.newConnector( connectorString );

            //I need to add the new Connector to the connection to be able to clean it up/disconect it!
            this.connection.addExecutionNode( this.connector );

            try {
                this.connector.connect();
                Object object = this.connector.write( msg ).getPayload();
                if ( object == null ) {
                    throw new RuntimeException( "Response was not correctly received" );
                }
                String value = (String) object;
                String type = String.valueOf( value.charAt( 0 ) );
                String instanceId = value.substring( 2 );
                CommandExecutor executor = null;
                switch ( Integer.parseInt( type ) ) {
                    case 0 : {
                        executor = new StatefulKnowledgeSessionRemoteClient( instanceId,
                                                                             this.connector,
                                                                             this.messageSession );
                        break;
                    }
                    default : {
                    }
                }
                this.connector.disconnect();
                return executor;
            } catch ( Exception e ) {
                throw new RuntimeException( "Unable to execute message",
                                            e );
            }
        } catch ( ConnectorException ex ) {
            Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                         null,
                                                                                         ex );
        }
        return null;
    }

    public Map<String, String> getDirectoryMap() {
        Map<String, String> directory = null;
        try {
            DirectoryNodeService directoryNode = this.connection.getDirectoryNode().get( DirectoryNodeService.class );
            directory = directoryNode.getExecutorsMap();
        } catch ( RemoteException ex ) {
            Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                         null,
                                                                                         ex );

        } catch ( ConnectorException ex ) {
            Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                         null,
                                                                                         ex );
        }
        return directory;
    }

    public void registerId(String key,
                           String value) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public String lookupId(String key) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void unregister(String identifier) {
        try {
            String commandId = "client.lookup" + this.messageSession.getNextId();
            String kresultsId = "kresults_" + this.messageSession.getSessionId();

            DirectoryNodeService directoryNode = this.connection.getDirectoryNode().get( DirectoryNodeService.class );
            try {

                directoryNode.unregister( identifier );
            } catch ( RemoteException ex ) {
                Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                             null,
                                                                                             ex );
            }

            Message msg = new Message( this.messageSession.getSessionId(),
                                       this.messageSession.getCounter().incrementAndGet(),
                                       false,
                                       new KnowledgeContextResolveFromContextCommand(
                                                                                      new UnRegisterCommand( identifier ),
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null ) );

            try {
                this.connector.connect();
                Object object = this.connector.write( msg ).getPayload();
                //                if (!(object instanceof FinishedCommand)) {
                //                    throw new RuntimeException("Response was not correctly ended");
                //                }
                this.connector.disconnect();
            } catch ( Exception e ) {
                throw new RuntimeException( "Unable to execute message",
                                            e );
            }

        } catch ( ConnectorException ex ) {
            Logger.getLogger( DirectoryLookupProviderRemoteClient.class.getName() ).log( Level.SEVERE,
                                                                                         null,
                                                                                         ex );
        }
    }
}
