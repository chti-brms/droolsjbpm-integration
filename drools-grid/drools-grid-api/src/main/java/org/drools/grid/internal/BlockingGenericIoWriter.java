package org.drools.grid.internal;

public class BlockingGenericIoWriter
    implements
    GenericIoWriter {

    private Message msg;

    public void write(Message message) {
        this.msg = message;
    }

    public Message getMessage() {
        return this.msg;
    }

    public void write(Message msg,
                      MessageResponseHandler responseHandler) {
        if ( responseHandler == null ) {
            this.msg = msg;
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
