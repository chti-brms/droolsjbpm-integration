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

package org.drools.grid.remote.mina;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.drools.KnowledgeBaseFactoryService;
import org.drools.builder.DirectoryLookupFactoryService;
import org.drools.builder.KnowledgeBuilderFactoryService;
import org.drools.grid.ConnectorType;
import org.drools.grid.GenericConnection;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.NodeConnectionType;
import org.drools.grid.remote.DirectoryLookupProviderRemoteClient;
import org.drools.grid.remote.KnowledgeBaseProviderRemoteClient;
import org.drools.grid.remote.KnowledgeBuilderProviderRemoteClient;

public class RemoteMinaConnectionNode
    implements
    NodeConnectionType {
    private final Map<Class< ? >, Object> services = new ConcurrentHashMap<Class< ? >, Object>();
    private GenericNodeConnector          connector;
    private GenericConnection             connection;

    public RemoteMinaConnectionNode() {

    }

    public RemoteMinaConnectionNode(GenericNodeConnector connector,
                                    GenericConnection connection) {

        this.connector = connector;
        this.connection = connection;

    }

    public void init() {
        this.services.put( KnowledgeBuilderFactoryService.class,
                           new KnowledgeBuilderProviderRemoteClient( this.connector ) );
        this.services.put( KnowledgeBaseFactoryService.class,
                           new KnowledgeBaseProviderRemoteClient( this.connector ) );
        this.services.put( DirectoryLookupFactoryService.class,
                           new DirectoryLookupProviderRemoteClient( this.connector,
                                                                    this.connection ) );
    }

    public Set<Class< ? >> getServicesKeys() {
        return this.services.keySet();
    }

    public <T> T getServiceImpl(Class<T> clazz) {
        return (T) this.services.get( clazz );
    }

    public void setConnector(GenericNodeConnector connector) {
        this.connector = connector;
    }

    public void setConnection(GenericConnection connection) {
        this.connection = connection;
    }

    public ConnectorType getConnectorType() {
        return ConnectorType.REMOTE;
    }

}
