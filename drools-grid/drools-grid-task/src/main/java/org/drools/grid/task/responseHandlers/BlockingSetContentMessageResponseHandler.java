/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.grid.task.responseHandlers;

import org.drools.grid.internal.Message;
import org.drools.grid.task.TaskClientMessageHandlerImpl.SetDocumentMessageResponseHandler;
import org.drools.task.service.Command;
import org.drools.task.service.responsehandlers.BlockingSetContentResponseHandler;

public class BlockingSetContentMessageResponseHandler extends BlockingSetContentResponseHandler
    implements
    SetDocumentMessageResponseHandler {

    public void receive(Message message) {
        Command cmd = (Command) message.getPayload();
        long contentId = (Long) cmd.getArguments().get( 0 );
        execute( contentId );
    }

}
