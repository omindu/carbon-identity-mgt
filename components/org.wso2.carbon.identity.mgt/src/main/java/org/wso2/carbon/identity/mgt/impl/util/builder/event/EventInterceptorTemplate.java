/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.impl.util.builder.event;

import org.wso2.carbon.identity.common.base.event.EventContext;
import org.wso2.carbon.identity.common.base.event.model.Event;
import org.wso2.carbon.identity.common.base.exception.IdentityException;
import org.wso2.carbon.identity.event.EventService;
import org.wso2.carbon.identity.event.ResultReturningHandler;
import org.wso2.carbon.identity.mgt.constant.StoreConstants;
import org.wso2.carbon.identity.mgt.event.IdentityMgtMessageContext;
import org.wso2.carbon.identity.mgt.exception.IdentityStoreException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Template to be used for PRE-Event, main logic and POST-Event firing.
 * This template purpose is to reduce the number of lines in the intercepting identity stores.
 *
 * @param <T> The type to be returned from the intercepted method.
 * @param <X> Generic exception thrown. If any.
 */
public class EventInterceptorTemplate<T extends Object, X extends Exception> {

    private EventService eventService;
    private IdentityMgtMessageContext messageContext;
    private T result;

    public EventInterceptorTemplate(EventService eventService, IdentityMgtMessageContext messageContext) {
        this.messageContext = messageContext;
        this.eventService = eventService;
    }

    /**
     * Pushes the event to the event execution.
     * PRE and POST events can be pushed.
     *
     * @param eventId
     * @param binder
     * @return
     * @throws IdentityStoreException
     */
    public EventInterceptorTemplate<T, X> pushEvent(String eventId, PropertyBinder binder)
            throws IdentityStoreException {
        Map<String, Object> eventProperties = new HashMap<>();
        binder.bind(eventProperties);

        return pushEvent(eventId, eventProperties);
    }

    /**
     * Executes the main logic, whicj is the primary method being intercepted.
     *
     * @param delegate
     * @return
     * @throws IdentityStoreException
     */
    public EventInterceptorTemplate<T, X> executeWith(EventHandlerDelegate<T> delegate) throws X {

        ResultReturningHandler<T, X> resultReturningHandler = new ResultReturningHandler<T, X>() {

            @Override
            public T handleEventWithResult(EventContext eventContext) throws X {
                try {
                    return delegate.execute();
                } catch (Exception e) {
                    throw (X) e;
                }
            }
        };
        try {
            eventService.pushEvent(new Event("EventInterceptorTemplate", Collections.emptyMap()), messageContext,
                    resultReturningHandler);
            result = resultReturningHandler.getResult();
        } catch (IdentityException e) {
            throw (X) e;
        }

        return this;
    }

    /**
     * Returns the result of the primary(intercepted) method of execution.
     *
     * @return
     */
    public T getResult() {
        return result;
    }

    private EventInterceptorTemplate<T, X> pushEvent(String eventId, Map<String, Object> eventProperties)
            throws IdentityStoreException {
        Event event = new Event(StoreConstants.IdentityStoreInterceptorConstants.PRE_GET_USER_BY_ID, eventProperties);
        IdentityMgtMessageContext messageContext = new IdentityMgtMessageContext(event);
        try {
            eventService.handleEvent(messageContext);
        } catch (IdentityException e) {
            String message = String.format("Error while handling %s event.", eventId);
            throw new IdentityStoreException(message, e);
        }
        return this;
    }

}
