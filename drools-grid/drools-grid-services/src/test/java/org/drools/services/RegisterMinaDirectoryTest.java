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
package org.drools.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.util.Map;

import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactoryService;
import org.drools.SystemEventListenerFactory;
import org.drools.builder.DirectoryLookupFactoryService;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactoryService;
import org.drools.builder.ResourceType;
import org.drools.grid.ConnectorException;
import org.drools.grid.DirectoryNodeService;
import org.drools.grid.ExecutionNode;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.internal.GenericMessageHandlerImpl;
import org.drools.grid.internal.NodeData;
import org.drools.grid.remote.directory.DirectoryServerMessageHandlerImpl;
import org.drools.grid.remote.mina.MinaAcceptor;
import org.drools.grid.remote.mina.MinaIoHandler;
import org.drools.grid.services.DirectoryInstance;
import org.drools.grid.services.ExecutionEnvironment;
import org.drools.grid.services.GridTopology;
import org.drools.grid.services.configuration.DirectoryInstanceConfiguration;
import org.drools.grid.services.configuration.ExecutionEnvironmentConfiguration;
import org.drools.grid.services.configuration.GridTopologyConfiguration;
import org.drools.grid.services.configuration.LocalProvider;
import org.drools.grid.services.configuration.MinaProvider;
import org.drools.grid.services.factory.GridTopologyFactory;
import org.drools.grid.services.strategies.DirectoryInstanceByPrioritySelectionStrategy;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegisterMinaDirectoryTest {

    private GridTopology grid;
    private MinaAcceptor serverDir;
    private MinaAcceptor serverNode;

    public RegisterMinaDirectoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws InterruptedException,
                       IOException {

        System.out.println( "Dir Server 1 Starting!" );
        // Directory Server configuration
        SocketAddress dirAddress = new InetSocketAddress( "127.0.0.1",
                                                          9123 );
        SocketAcceptor dirAcceptor = new NioSocketAcceptor();

        dirAcceptor.setHandler( new MinaIoHandler( SystemEventListenerFactory.getSystemEventListener(),
                                                   new DirectoryServerMessageHandlerImpl(
                                                                                          SystemEventListenerFactory.getSystemEventListener() ) ) );
        this.serverDir = new MinaAcceptor( dirAcceptor,
                                           dirAddress );
        this.serverDir.start();
        System.out.println( "Dir Server 1 Started! at = " + dirAddress.toString() );

        // End Execution Server

        //Execution Node related stuff

        System.out.println( "Exec Server 1 Starting!" );
        // the servers should be started in a different machine (jvm or physical) or in another thread
        SocketAddress address = new InetSocketAddress( "127.0.0.1",
                                                       9124 );
        NodeData nodeData = new NodeData();
        // setup Server
        SocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setHandler( new MinaIoHandler( SystemEventListenerFactory.getSystemEventListener(),
                                                new GenericMessageHandlerImpl( nodeData,
                                                                               SystemEventListenerFactory.getSystemEventListener() ) ) );
        this.serverNode = new MinaAcceptor( acceptor,
                                            address );
        this.serverNode.start();
        System.out.println( "Exec Server 1 Started! at = " + address.toString() );

    }

    @After
    public void tearDown() throws InterruptedException,
                          ConnectorException,
                          RemoteException {

        Thread.sleep( 3000 );

        Assert.assertEquals( 0,
                             this.serverNode.getCurrentSessions() );
        this.serverNode.stop();
        System.out.println( "Execution Server Stopped!" );

        Assert.assertEquals( 0,
                             this.serverDir.getCurrentSessions() );
        this.serverDir.stop();
        System.out.println( "Dir Server Stopped!" );

    }

    @Test
    public void directoryRemoteTest() throws ConnectorException,
                                     RemoteException {
        GridTopologyConfiguration gridTopologyConfiguration = new GridTopologyConfiguration( "MyTopology" );
        gridTopologyConfiguration.addDirectoryInstance( new DirectoryInstanceConfiguration( "MyMinaDir",
                                                                                            new MinaProvider( "127.0.0.1",
                                                                                                              9123 ) ) );
        gridTopologyConfiguration.addExecutionEnvironment( new ExecutionEnvironmentConfiguration( "MyLocalEnv",
                                                                                                  new LocalProvider() ) );
        gridTopologyConfiguration.addExecutionEnvironment( new ExecutionEnvironmentConfiguration( "MyRemoteEnv",
                                                                                                  new MinaProvider( "127.0.0.1",
                                                                                                                    9124 ) ) );

        this.grid = GridTopologyFactory.build( gridTopologyConfiguration );
        Assert.assertNotNull( this.grid );

        DirectoryInstance directory = this.grid.getBestDirectoryInstance( new DirectoryInstanceByPrioritySelectionStrategy() );
        Assert.assertNotNull( directory );

        DirectoryNodeService dir = directory.getDirectoryNode().get( DirectoryNodeService.class );
        Assert.assertNotNull( dir );
        Map<String, String> dirMap = dir.getExecutorsMap();

        Assert.assertNotNull( "Dir Null",
                              dirMap );

        Assert.assertEquals( 3,
                             dirMap.size() );

        Assert.assertEquals( 3,
                             dirMap.size() );
        Assert.assertEquals( 0,
                             this.serverNode.getCurrentSessions() );
        //Then we can get the registered Execution Environments by Name

        ExecutionEnvironment ee = this.grid.getExecutionEnvironment( "MyRemoteEnv" );
        Assert.assertNotNull( ee );

        // Give me an ExecutionNode in the selected environment
        // For the Mina we have just one Execution Node per server instance
        ExecutionNode node = ee.getExecutionNode();

        Assert.assertNotNull( node );

        // Do a basic Runtime Test that register a ksession and fire some rules.
        String str = "";
        str += "package org.drools \n";
        str += "global java.util.List list \n";
        str += "rule rule1 \n";
        str += "    dialect \"java\" \n";
        str += "when \n";
        str += "then \n";
        str += "    System.out.println( \"hello1!!!\" ); \n";
        str += "end \n";
        str += "rule rule2 \n";
        str += "    dialect \"java\" \n";
        str += "when \n";
        str += "then \n";
        str += "    System.out.println( \"hello2!!!\" ); \n";
        str += "end \n";

        KnowledgeBuilder kbuilder =
                node.get( KnowledgeBuilderFactoryService.class ).newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );

        if ( kbuilder.hasErrors() ) {
            System.out.println( "Errors: " + kbuilder.getErrors() );
        }

        KnowledgeBase kbase =
                node.get( KnowledgeBaseFactoryService.class ).newKnowledgeBase();
        Assert.assertNotNull( kbase );

        kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() );

        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        Assert.assertNotNull( ksession );

        node.get( DirectoryLookupFactoryService.class ).register( "ksession1",
                                                                  ksession );

        ksession = (StatefulKnowledgeSession) node.get( DirectoryLookupFactoryService.class ).lookup( "ksession1" );

        int fired = ksession.fireAllRules();

        Assert.assertEquals( 2,
                             fired );
        this.grid.dispose();

    }

    @Test
    public void directoryInstanceRetriveKSessionFromEE() throws ConnectorException,
                                                        RemoteException {

        GridTopologyConfiguration gridTopologyConfiguration = new GridTopologyConfiguration( "MyTopology" );
        gridTopologyConfiguration.addDirectoryInstance( new DirectoryInstanceConfiguration( "MyMinaDir",
                                                                                            new MinaProvider( "127.0.0.1",
                                                                                                              9123 ) ) );
        gridTopologyConfiguration.addExecutionEnvironment( new ExecutionEnvironmentConfiguration( "MyMinaExecutionEnv1",
                                                                                                  new MinaProvider( "127.0.0.1",
                                                                                                                    9124 ) ) );

        this.grid = GridTopologyFactory.build( gridTopologyConfiguration );
        Assert.assertNotNull( this.grid );
        //Then we can get the registered Execution Environments by Name

        ExecutionEnvironment ee = this.grid.getExecutionEnvironment( "MyMinaExecutionEnv1" );
        Assert.assertNotNull( ee );

        // Give me an ExecutionNode in the selected environment
        // For the Mina we have just one Execution Node per server instance
        ExecutionNode node = ee.getExecutionNode();

        Assert.assertNotNull( node );

        // Do a basic Runtime Test that register a ksession and fire some rules.
        String str = "";
        str += "package org.drools \n";
        str += "global java.util.List list \n";
        str += "rule rule1 \n";
        str += "    dialect \"java\" \n";
        str += "when \n";
        str += "then \n";
        str += "    System.out.println( \"hello1!!!\" ); \n";
        str += "end \n";
        str += "rule rule2 \n";
        str += "    dialect \"java\" \n";
        str += "when \n";
        str += "then \n";
        str += "    System.out.println( \"hello2!!!\" ); \n";
        str += "end \n";

        KnowledgeBuilder kbuilder =
                node.get( KnowledgeBuilderFactoryService.class ).newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newByteArrayResource( str.getBytes() ),
                      ResourceType.DRL );

        if ( kbuilder.hasErrors() ) {
            System.out.println( "Errors: " + kbuilder.getErrors() );
        }

        KnowledgeBase kbase =
                node.get( KnowledgeBaseFactoryService.class ).newKnowledgeBase();
        Assert.assertNotNull( kbase );

        kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() );

        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
        Assert.assertNotNull( ksession );

        node.get( DirectoryLookupFactoryService.class ).register( "sessionName",
                                                                  ksession );

        //We disconnect the grid in order to close all the active connections to remote services
        this.grid.disconnect();

        DirectoryInstance directoryInstance = this.grid.getDirectoryInstance();
        DirectoryNodeService directory = directoryInstance.getDirectoryNode().get( DirectoryNodeService.class );
        GenericNodeConnector connector = directory.lookup( "sessionName" );

        node = this.grid.getExecutionEnvironment( connector ).getExecutionNode();
        ksession = (StatefulKnowledgeSession) node.get( DirectoryLookupFactoryService.class ).lookup( "sessionName" );
        Assert.assertNotNull( ksession );

        this.grid.dispose();

    }
}
