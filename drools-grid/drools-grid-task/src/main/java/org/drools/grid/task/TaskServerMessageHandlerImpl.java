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

package org.drools.grid.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.drools.SystemEventListener;
import org.drools.eventmessaging.EventKey;
import org.drools.grid.internal.GenericIoWriter;
import org.drools.grid.internal.GenericMessageHandler;
import org.drools.grid.internal.Message;
import org.drools.task.Attachment;
import org.drools.task.Comment;
import org.drools.task.Content;
import org.drools.task.Task;
import org.drools.task.query.TaskSummary;
import org.drools.task.service.Command;
import org.drools.task.service.CommandName;
import org.drools.task.service.ContentData;
import org.drools.task.service.Operation;
import org.drools.task.service.TaskService;
import org.drools.task.service.TaskServiceSession;

public class TaskServerMessageHandlerImpl
    implements
    GenericMessageHandler {
    private final TaskService                  service;
    private final Map<String, GenericIoWriter> clients;

    /**
     * Listener used for logging
     */
    private final SystemEventListener          systemEventListener;

    public TaskServerMessageHandlerImpl(TaskService service,
                                        SystemEventListener systemEventListener) {
        this.service = service;
        this.clients = new HashMap<String, GenericIoWriter>();
        this.systemEventListener = systemEventListener;
    }

    public void exceptionCaught(IoSession session,
                                Throwable cause) throws Exception {
        this.systemEventListener.exception( "Uncaught exception on Server",
                                            cause );
    }

    public void messageReceived(GenericIoWriter session,
                                Message msg) throws Exception {
        Command cmd = (Command) (msg).getPayload();
        TaskServiceSession taskSession = this.service.createSession();
        CommandName response = null;
        try {
            this.systemEventListener.debug( "Message received on server : " + cmd.getName() );
            this.systemEventListener.debug( "Arguments : " + Arrays.toString( cmd.getArguments().toArray() ) );
            switch ( cmd.getName() ) {
                case OperationRequest : {
                    response = CommandName.OperationResponse;
                    Operation operation = (Operation) cmd.getArguments().get( 0 );

                    this.systemEventListener.debug( "Command receieved on server was operation of type: " + operation );

                    long taskId = (Long) cmd.getArguments().get( 1 );
                    String userId = (String) cmd.getArguments().get( 2 );
                    String targetEntityId = null;
                    ContentData data = null;
                    List<String> groupIds = null;
                    if ( cmd.getArguments().size() > 3 ) {
                        targetEntityId = (String) cmd.getArguments().get( 3 );
                        if ( cmd.getArguments().size() > 4 ) {
                            data = (ContentData) cmd.getArguments().get( 4 );
                            if ( cmd.getArguments().size() > 5 ) {
                                groupIds = (List<String>) cmd.getArguments().get( 5 );
                            }
                        }
                    }
                    taskSession.taskOperation( operation,
                                               taskId,
                                               userId,
                                               targetEntityId,
                                               data,
                                               groupIds );

                    List args = Collections.emptyList();

                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.OperationResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case GetTaskRequest : {
                    response = CommandName.GetTaskResponse;
                    long taskId = (Long) cmd.getArguments().get( 0 );

                    Task task = taskSession.getTask( taskId );

                    List args = new ArrayList( 1 );
                    args.add( task );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.GetTaskResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );

                    break;
                }
                case AddTaskRequest : {
                    response = CommandName.AddTaskResponse;
                    Task task = (Task) cmd.getArguments().get( 0 );
                    ContentData content = (ContentData) cmd.getArguments().get( 1 );
                    taskSession.addTask( task,
                                         content );

                    List args = new ArrayList( 1 );
                    args.add( task.getId() );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.AddTaskResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case AddCommentRequest : {
                    response = CommandName.AddCommentResponse;
                    Comment comment = (Comment) cmd.getArguments().get( 1 );
                    taskSession.addComment( (Long) cmd.getArguments().get( 0 ),
                                            comment );

                    List args = new ArrayList( 1 );
                    args.add( comment.getId() );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.AddCommentResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case DeleteCommentRequest : {
                    response = CommandName.DeleteCommentResponse;
                    long taskId = (Long) cmd.getArguments().get( 0 );
                    long commentId = (Long) cmd.getArguments().get( 1 );
                    taskSession.deleteComment( taskId,
                                               commentId );

                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.DeleteCommentResponse,
                                                       Collections.emptyList() );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case AddAttachmentRequest : {
                    response = CommandName.AddAttachmentResponse;
                    Attachment attachment = (Attachment) cmd.getArguments().get( 1 );
                    Content content = (Content) cmd.getArguments().get( 2 );
                    taskSession.addAttachment( (Long) cmd.getArguments().get( 0 ),
                                               attachment,
                                               content );

                    List args = new ArrayList( 2 );
                    args.add( attachment.getId() );
                    args.add( content.getId() );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.AddAttachmentResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case DeleteAttachmentRequest : {
                    response = CommandName.DeleteAttachmentResponse;
                    long taskId = (Long) cmd.getArguments().get( 0 );
                    long attachmentId = (Long) cmd.getArguments().get( 1 );
                    long contentId = (Long) cmd.getArguments().get( 2 );
                    taskSession.deleteAttachment( taskId,
                                                  attachmentId,
                                                  contentId );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.DeleteAttachmentResponse,
                                                       Collections.emptyList() );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case SetDocumentContentRequest : {
                    response = CommandName.SetDocumentContentResponse;
                    long taskId = (Long) cmd.getArguments().get( 0 );
                    Content content = (Content) cmd.getArguments().get( 1 );
                    taskSession.setDocumentContent( taskId,
                                                    content );

                    List args = new ArrayList( 1 );
                    args.add( content.getId() );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.SetDocumentContentResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case GetContentRequest : {
                    response = CommandName.GetContentResponse;
                    long contentId = (Long) cmd.getArguments().get( 0 );
                    Content content = taskSession.getContent( contentId );
                    List args = new ArrayList( 1 );
                    args.add( content );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.GetContentResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksOwned : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksOwned( (String) cmd.getArguments().get( 0 ),
                                                                           (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsBusinessAdministrator : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsBusinessAdministrator( (String) cmd.getArguments().get( 0 ),
                                                                                                     (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsPotentialOwner : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsPotentialOwner( (String) cmd.getArguments().get( 0 ),
                                                                                              (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsPotentialOwnerWithGroup : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsPotentialOwner(
                                                                                              (String) cmd.getArguments().get( 0 ),
                                                                                              (List<String>) cmd.getArguments().get( 1 ),
                                                                                              (String) cmd.getArguments().get( 2 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsPotentialOwnerByGroup : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsPotentialOwnerByGroup( (String) cmd.getArguments().get( 0 ),
                                                                                                     (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QuerySubTasksAssignedAsPotentialOwner : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getSubTasksAssignedAsPotentialOwner( (Long) cmd.getArguments().get( 0 ),
                                                                                                 (String) cmd.getArguments().get( 1 ),
                                                                                                 (String) cmd.getArguments().get( 2 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryGetSubTasksByParentTaskId : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getSubTasksByParent( (Long) cmd.getArguments().get( 0 ),
                                                                                 (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }

                case QueryTasksAssignedAsTaskInitiator : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsTaskInitiator( (String) cmd.getArguments().get( 0 ),
                                                                                             (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsExcludedOwner : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsExcludedOwner( (String) cmd.getArguments().get( 0 ),
                                                                                             (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsRecipient : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsRecipient( (String) cmd.getArguments().get( 0 ),
                                                                                         (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case QueryTasksAssignedAsTaskStakeholder : {
                    response = CommandName.QueryTaskSummaryResponse;
                    List<TaskSummary> results = taskSession.getTasksAssignedAsTaskStakeholder( (String) cmd.getArguments().get( 0 ),
                                                                                               (String) cmd.getArguments().get( 1 ) );
                    List args = new ArrayList( 1 );
                    args.add( results );
                    Command resultsCmnd = new Command( cmd.getId(),
                                                       CommandName.QueryTaskSummaryResponse,
                                                       args );
                    session.write( new Message( msg.getSessionId(),
                                                msg.getResponseId(),
                                                msg.isAsync(),
                                                resultsCmnd ),
                                   null );
                    break;
                }
                case RegisterForEventRequest : {
                    response = CommandName.EventTriggerResponse;
                    EventKey key = (EventKey) cmd.getArguments().get( 0 );
                    boolean remove = (Boolean) cmd.getArguments().get( 1 );
                    String uuid = (String) cmd.getArguments().get( 2 );
                    this.clients.put( uuid,
                                      session );
                    GenericEventTransport transport = new GenericEventTransport( String.valueOf( msg.getSessionId() ), //this is wrong
                                                                                 msg.getResponseId(), //this is wrong
                                                                                 this.clients,
                                                                                 remove );
                    this.service.getEventKeys().register( key,
                                                          transport );
                    break;
                }
                case RegisterClient : {
                    String uuid = (String) cmd.getArguments().get( 0 );
                    this.clients.put( uuid,
                                      session );
                    break;
                }
                default : {
                    this.systemEventListener.debug( "Unknown command recieved on server" );
                }
            }
        } catch ( RuntimeException e ) {
            this.systemEventListener.exception( e.getMessage(),
                                                e );
            e.printStackTrace( System.err );
            List<Object> list = new ArrayList<Object>( 1 );
            list.add( e );
            Command resultsCmnd = new Command( cmd.getId(),
                                               response,
                                               list );
            session.write( new Message( msg.getSessionId(),
                                        msg.getResponseId(),
                                        msg.isAsync(),
                                        resultsCmnd ),
                           null );
        } finally {
            taskSession.dispose();
        }
    }

    public void sessionIdle(IoSession session,
                            IdleStatus status) throws Exception {
        this.systemEventListener.debug( "Server IDLE " + session.getIdleCount( status ) );
    }

}
