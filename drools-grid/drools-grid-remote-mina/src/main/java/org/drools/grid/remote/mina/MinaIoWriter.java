package org.drools.grid.remote.mina;

import org.apache.mina.core.session.IoSession;
import org.drools.grid.internal.GenericIoWriter;
import org.drools.grid.internal.Message;
import org.drools.grid.internal.MessageResponseHandler;

public class MinaIoWriter
    implements
    GenericIoWriter {

    private IoSession session;

    public MinaIoWriter(IoSession session) {
        this.session = session;
    }

    public void write(Message message,
                      MessageResponseHandler responseHandler) {
        this.session.write( message );
    }

}
