package org.drools.grid.remote;

import java.util.Collection;
import java.util.UUID;

import org.drools.KnowledgeBase;
import org.drools.command.KnowledgeBaseAddKnowledgePackagesCommand;
import org.drools.command.KnowledgeContextResolveFromContextCommand;
import org.drools.command.NewStatefulKnowledgeSessionCommand;
import org.drools.command.SetVariableCommand;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.definition.rule.Query;
import org.drools.definition.rule.Rule;
import org.drools.definition.type.FactType;
import org.drools.event.knowledgebase.KnowledgeBaseEventListener;
import org.drools.grid.GenericNodeConnector;
import org.drools.grid.internal.CollectionClient;
import org.drools.grid.internal.Message;
import org.drools.grid.internal.MessageSession;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.StatelessKnowledgeSession;

public class KnowledgeBaseRemoteClient
    implements
    KnowledgeBase {

    private GenericNodeConnector connector;
    private MessageSession       messageSession;
    private String               instanceId;

    public KnowledgeBaseRemoteClient(String instanceId,
                                     GenericNodeConnector connector,
                                     MessageSession messageSession) {
        this.instanceId = instanceId;
        this.connector = connector;
        this.messageSession = messageSession;
    }

    public void addKnowledgePackages(Collection<KnowledgePackage> kpackages) {
        String kresultsId = "kresults_" + this.messageSession.getSessionId();

        String kuilderInstanceId = ((CollectionClient<KnowledgePackage>) kpackages).getParentInstanceId();
        Message msg = new Message( this.messageSession.getSessionId(),
                                   this.messageSession.counter.incrementAndGet(),
                                   false,
                                   new KnowledgeContextResolveFromContextCommand( new KnowledgeBaseAddKnowledgePackagesCommand(),
                                                                                  kuilderInstanceId,
                                                                                  this.instanceId,
                                                                                  null,
                                                                                  kresultsId ) );

        try {
            this.connector.connect();
            Object object = this.connector.write( msg ).getPayload();

            //            if (!(object instanceof FinishedCommand)) {
            //                throw new RuntimeException("Response was not correctly ended");
            //            }
            this.connector.disconnect();
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to execute message",
                                        e );
        }
    }

    public FactType getFactType(String packageName,
                                String typeName) {
        // TODO Auto-generated method stub
        return null;
    }

    public KnowledgePackage getKnowledgePackage(String packageName) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<KnowledgePackage> getKnowledgePackages() {
        // TODO Auto-generated method stub
        return null;
    }

    public Process getProcess(String processId) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public Collection<Process> getProcesses() {
        // TODO Auto-generated method stub
        return null;
    }

    public Rule getRule(String packageName,
                        String ruleName) {
        // TODO Auto-generated method stub
        return null;
    }

    public StatefulKnowledgeSession newStatefulKnowledgeSession() {
        return newStatefulKnowledgeSession( null,
                                            null );
    }

    public StatefulKnowledgeSession newStatefulKnowledgeSession(KnowledgeSessionConfiguration conf,
                                                                Environment environment) {
        String kresultsId = "kresults_" + this.messageSession.getSessionId();

        String localId = UUID.randomUUID().toString();

        Message msg = new Message( this.messageSession.getSessionId(),
                                   this.messageSession.counter.incrementAndGet(),
                                   false,
                                   new SetVariableCommand( "__TEMP__",
                                                           localId,
                                                           new KnowledgeContextResolveFromContextCommand( new NewStatefulKnowledgeSessionCommand( conf ),
                                                                                                          null,
                                                                                                          this.instanceId,
                                                                                                          null,
                                                                                                          kresultsId ) ) );

        try {
            this.connector.connect();
            Object object = this.connector.write( msg ).getPayload();

            //            if (!(object instanceof FinishedCommand)) {
            //                throw new RuntimeException("Response was not correctly ended");
            //            }
            this.connector.disconnect();
        } catch ( Exception e ) {
            throw new RuntimeException( "Unable to execute message",
                                        e );
        }

        return new StatefulKnowledgeSessionRemoteClient( localId,
                                                         this.connector,
                                                         this.messageSession );
    }

    public StatelessKnowledgeSession newStatelessKnowledgeSession(KnowledgeSessionConfiguration conf) {
        // TODO Auto-generated method stub
        return null;
    }

    public StatelessKnowledgeSession newStatelessKnowledgeSession() {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeKnowledgePackage(String packageName) {
        // TODO Auto-generated method stub
    }

    public void removeProcess(String processId) {
        // TODO Auto-generated method stub
    }

    public void removeRule(String packageName,
                           String ruleName) {
        // TODO Auto-generated method stub
    }

    public void addEventListener(KnowledgeBaseEventListener listener) {
        // TODO Auto-generated method stub
    }

    public Collection<KnowledgeBaseEventListener> getKnowledgeBaseEventListeners() {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeEventListener(KnowledgeBaseEventListener listener) {
        // TODO Auto-generated method stub
    }

    public void removeFunction(String packageName,
                               String ruleName) {
        // TODO Auto-generated method stub
    }

    public Query getQuery(String packageName,
                          String queryName) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void removeQuery(String packageName,
                            String queryName) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Collection<StatefulKnowledgeSession> getStatefulKnowledgeSessions() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
