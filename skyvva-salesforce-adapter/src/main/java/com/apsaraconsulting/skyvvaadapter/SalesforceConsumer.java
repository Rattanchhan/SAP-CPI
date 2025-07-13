/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apsaraconsulting.skyvvaadapter;

import com.apsaraconsulting.skyvvaadapter.internal.streaming.empconnector.TopicSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.dto.PlatformEvent;
import com.apsaraconsulting.skyvvaadapter.api.utils.JsonUtils;
import com.apsaraconsulting.skyvvaadapter.internal.client.RestClient;
import com.apsaraconsulting.skyvvaadapter.internal.dto.EventBodyExtractionStrategy;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.streaming.MessageKind;
import com.apsaraconsulting.skyvvaadapter.internal.streaming.PushTopicHelper;
import com.apsaraconsulting.skyvvaadapter.internal.streaming.SubscriptionHelper;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.ObjectHelper;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * The Salesforce consumer.
 */
public class SalesforceConsumer extends DefaultConsumer {

    private static final String CREATED_DATE_PROPERTY = "createdDate";
    private static final String EVENT_PROPERTY = "event";
    private static final double MINIMUM_VERSION = 52.0;
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.createObjectMapper();
    private static final String REPLAY_ID_PROPERTY = "replayId";
    private static final String EVENT_UUID_PROPERTY = "EventUuid";
    private static final String SOBJECT_PROPERTY = "sobject";
    private static final String TYPE_PROPERTY = "type";

    private final SalesforceEndpoint endpoint;
    private final MessageKind messageKind;
    private final ObjectMapper objectMapper;

    private final boolean rawPayload;
    private Class<?> sObjectClass;
    private boolean subscribed;
    private final SubscriptionHelper subscriptionHelper;
    private final String topicName;

    //private final EmpConnector empConnector;
    private TopicSubscription topicSubscription;

    private final CpiLoggingDecorator LOG;

    public SalesforceConsumer(final SalesforceEndpoint endpoint, final Processor processor, final SubscriptionHelper helper) throws Exception {

        super(endpoint, processor);
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(SalesforceConsumer.class),
            endpoint.getConfiguration().getLoggingOptionAsLogLevel()
        );
        LOG.trace();

        this.endpoint = endpoint;
        final ObjectMapper configuredObjectMapper = endpoint.getConfiguration().getObjectMapper();
        if (configuredObjectMapper != null) {
            objectMapper = configuredObjectMapper;
        } else {
            objectMapper = OBJECT_MAPPER;
        }

        // check minimum supported API version
        if (Double.parseDouble(endpoint.getConfiguration().getApiVersion()) < MINIMUM_VERSION) {
            throw new IllegalArgumentException("Minimum supported API version for consumer endpoints is " + 52.0);
        }

        topicName = endpoint.getTopicName();
        subscriptionHelper = helper;
        //empConnector = endpoint.getComponent().getEmpConnector(LOG.getLogLevel());

        messageKind = MessageKind.fromTopicName(topicName);
        rawPayload = endpoint.getConfiguration().isRawPayload();

        // get sObjectClass to convert to
        final String sObjectName = endpoint.getConfiguration().getSObjectName();
        if (sObjectName != null) {
            sObjectClass = endpoint.getComponent().getClassMap().get(sObjectName);
            if (sObjectClass == null) {
                throw new IllegalArgumentException(String.format("SObject Class not found for %s", sObjectName));
            }
        } else {
            final String className = endpoint.getConfiguration().getSObjectClass();
            if (className != null) {
                sObjectClass = endpoint.getComponent().getCamelContext().getClassResolver().resolveClass(className);
                if (sObjectClass == null) {
                    throw new IllegalArgumentException(String.format("SObject Class not found %s", className));
                }
            } else {
                LOG.warn("Property sObjectName or sObjectClass NOT set, messages will be of type java.lang.Map");
                sObjectClass = null;
            }
        }
    }

    public String getTopicName() {
        return topicName;
    }

    @Override
    public void handleException(String message, Throwable t) {
        super.handleException(message, t);
    }

    @Deprecated
    public void processMessage(final ClientSessionChannel channel, final Message message) {
        LOG.debug("Received event {} on channel {}", channel.getId(), channel.getChannelId());
        LOG.trace("message: {}", message);

        final Exchange exchange = endpoint.createExchange();
        final org.apache.camel.Message in = exchange.getIn();

        switch (messageKind) {
            case PUSH_TOPIC:
                createPushTopicMessage(message, in);
                break;
            case CDC_EVENT:
            case PLATFORM_EVENT:
                createPlatformEventOrCdcEventMessage(message, in);
                break;
            default:
                throw new IllegalStateException("Unknown message kind: " + messageKind);
        }

        try {
            getAsyncProcessor().process(exchange, new AsyncCallback() {
                @Override
                public void done(boolean doneSync) {
                    // noop
                    LOG.trace("Done processing event: {} {}", channel.getId(),
                            doneSync ? "synchronously" : "asynchronously");
                }
            });
        } catch (final Exception e) {
            final String msg = String.format("Error processing %s: %s", exchange, e);
            handleException(msg, new SalesforceException(msg, e));
        } finally {
            final Exception ex = exchange.getException();
            if (ex != null) {
                final String msg = String.format("Unhandled exception: %s", ex.getMessage());
                handleException(msg, new SalesforceException(msg, ex));
            }
        }
    }

    public void processMessage(final Message message) {
        LOG.trace("Received message from topic {}: {}", topicName, message);

        final Exchange exchange = endpoint.createExchange();
        final org.apache.camel.Message in = exchange.getIn();

        switch (messageKind) {
            case PUSH_TOPIC:
                createPushTopicMessage(message, in);
                break;
            case CDC_EVENT:
            case PLATFORM_EVENT:
                createPlatformEventOrCdcEventMessage(message, in);
                break;
            default:
                throw new IllegalStateException("Unknown message kind: " + messageKind);
        }

        try {
            getAsyncProcessor().process(exchange, doneSync -> {
                // noop
                LOG.trace("Done processing event on {} {}", topicName,
                    doneSync ? "synchronously" : "asynchronously");
            });
        } catch (final Exception e) {
            final String msg = String.format("Error processing %s: %s", exchange, e);
            handleException(msg, new SalesforceException(msg, e));
        } finally {
            final Exception ex = exchange.getException();
            if (ex != null) {
                final String msg = String.format("Unhandled exception: %s", ex.getMessage());
                handleException(msg, new SalesforceException(msg, ex));
            }
        }
    }

    void createPlatformEventOrCdcEventMessage(final Message message, final org.apache.camel.Message in) {
        setHeaders(in, message);

        final Map<String, Object> data = message.getDataAsMap();

        @SuppressWarnings("unchecked")
        final Map<String, Object> event = (Map<String, Object>) data.get(EVENT_PROPERTY);
        final Object replayId = event.get(REPLAY_ID_PROPERTY);
        final Object eventUuid = event.get(EVENT_UUID_PROPERTY);

        if (replayId != null) {
            setHeaderToCamelMessage(in, "CamelSalesforceReplayId", replayId);
        }
        if (eventUuid != null) {
            setHeaderToCamelMessage(in, "CamelSalesforceEventUuid", eventUuid);
        }

        setHeaderToCamelMessage(in, "CamelSalesforcePlatformEventSchema", data.get("schema"));
        setHeaderToCamelMessage(in, "CamelSalesforceEventType", topicName.substring(topicName.lastIndexOf('/') + 1));

        final Object payload = data.get("payload");

        try {
            if (messageKind.equals(MessageKind.PLATFORM_EVENT)) {
                final PlatformEvent platformEvent = objectMapper.convertValue(payload, PlatformEvent.class);
                setHeaderToCamelMessage(in, "CamelSalesforceCreatedDate", platformEvent.getCreated());
            }

            if (rawPayload) {
                LOG.trace("Setting {} map as exchange body", messageKind.name());
                if (EventBodyExtractionStrategy.EXTRACT_BUSINESS_DATA_AS_BODY.equals(endpoint.getConfiguration().getEventBodyExtractionStrategy())) {
                    in.setBody(objectMapper.writeValueAsString(payload));
                } else {
                    in.setBody(message.getJSON());
                }
            } else {
                LOG.trace("Setting {} object as exchange body", messageKind.name());
                in.setBody(payload);
            }
        } catch (Exception ex) {
            final String msg = String.format("Error parsing message [%s] from Topic %s [%s handling]: %s", message, topicName, messageKind.name(), ex.getMessage());
            handleException(msg, new SalesforceException(msg, ex));
        }

    }

    void createPushTopicMessage(final Message message, final org.apache.camel.Message in) {
        setHeaders(in, message);

        final Map<String, Object> data = message.getDataAsMap();

        @SuppressWarnings("unchecked")
        final Map<String, Object> event = (Map<String, Object>) data.get(EVENT_PROPERTY);
        final Object eventType = event.get(TYPE_PROPERTY);
        final Object createdDate = event.get(CREATED_DATE_PROPERTY);
        final Object replayId = event.get(REPLAY_ID_PROPERTY);
        final Object eventUuid = event.get(EVENT_UUID_PROPERTY);

        setHeaderToCamelMessage(in, "CamelSalesforceTopicName", topicName);
        setHeaderToCamelMessage(in, "CamelSalesforceEventType", eventType);
        setHeaderToCamelMessage(in, "CamelSalesforceCreatedDate", createdDate);
        if (replayId != null) {
            setHeaderToCamelMessage(in, "CamelSalesforceReplayId", replayId);
        }
        if (eventUuid != null) {
            setHeaderToCamelMessage(in, "CamelSalesforceEventUuid", eventUuid);
        }

        // get SObject
        @SuppressWarnings("unchecked")
        final Map<String, Object> sObject = (Map<String, Object>) data.get(SOBJECT_PROPERTY);
        try {

            final String sObjectString = objectMapper.writeValueAsString(sObject);
            LOG.trace("Received SObject: {}", sObjectString);

            if (rawPayload) {
                // return sobject bytes as exchange body
                LOG.trace("Setting sObject bytes as exchange body");
                if (EventBodyExtractionStrategy.EXTRACT_BUSINESS_DATA_AS_BODY.equals(endpoint.getConfiguration().getEventBodyExtractionStrategy())) {
                    in.setBody(sObjectString);
                } else {
                    in.setBody(message.getJSON());
                }
            } else if (sObjectClass == null) {
                // return sobject map as exchange body
                LOG.trace("Setting sObject map as exchange body");
                in.setBody(sObject);
            } else {
                // create the expected SObject
                LOG.trace("Setting sObject converted to DTO as exchange body");
                in.setBody(objectMapper.readValue(new StringReader(sObjectString), sObjectClass));
            }
        } catch (final IOException e) {
            final String msg = String.format("Error parsing message [%s] from Topic %s [Push topic event handling]: %s", message, topicName, e.getMessage());
            handleException(msg, new SalesforceException(msg, e));
        }
    }

    void setHeaders(final org.apache.camel.Message in, final Message message) {
        setHeaderToCamelMessage(in, "CamelSalesforceChannel", message.getChannel());
        final String clientId = message.getClientId();
        if (ObjectHelper.isNotEmpty(clientId)) {
            setHeaderToCamelMessage(in, "CamelSalesforceClientId", clientId);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final SalesforceEndpointConfig config = endpoint.getConfiguration();

        // is a query configured in the endpoint?
        if (messageKind == MessageKind.PUSH_TOPIC && ObjectHelper.isNotEmpty(config.getSObjectQuery())) {
            // Note that we don't lookup topic if the query is not specified
            // create REST client for PushTopic operations
            final SalesforceComponent salesforceComponent = endpoint.getComponent();
            final RestClient restClient = salesforceComponent.createRestClientFor(endpoint);

            // don't forget to start the client
            ServiceHelper.startService(restClient);

            try {
                final PushTopicHelper helper = new PushTopicHelper(config, topicName, restClient);
                helper.createOrUpdateTopic();
            } finally {
                // don't forget to stop the client
                ServiceHelper.stopService(restClient);
            }
        }

        // subscribe to topic
        subscriptionHelper.subscribe(topicName, this);
        //topicSubscription = empConnector.subscribe(
        //    SubscriptionHelper.getChannelName(topicName),
        //    -1,
        //    this::processMessage
        //).get(5, TimeUnit.SECONDS);

        subscribed = true;
    }

    @Override
    protected void doStop() throws Exception {
        LOG.trace();
        super.doStop();
        try {
            if (subscribed) {
                subscribed = false;
                // unsubscribe from topic
                subscriptionHelper.unsubscribe(topicName, this);
                //topicSubscription.cancel().get(5, TimeUnit.SECONDS);
                LOG.trace("Unsubscribed successfully from topic {}", topicName);
            }
        } catch (Throwable ex) {
            LOG.error(String.format("Couldn't unsubscribe from topic %s: %s", topicName, ex.getMessage()), ex);
        }
    }

    private void setHeaderToCamelMessage(final org.apache.camel.Message msg, String headerName, Object value) {
        msg.setHeader(headerName, value);
        LOG.trace("Added header {} with value {}", headerName, value);
    }

    // May be necessary to call from some unit tests.
    void determineSObjectClass() {
        // get sObjectClass to convert to
        if (!rawPayload) {
            final String sObjectName = endpoint.getConfiguration().getSObjectName();
            if (sObjectName != null) {
                sObjectClass = endpoint.getComponent().getClassMap().get(sObjectName);
                if (sObjectClass == null) {
                    throw new IllegalArgumentException(String.format("SObject Class not found for %s", sObjectName));
                }
            } else {
                final String className = endpoint.getConfiguration().getSObjectClass();
                if (className != null) {
                    sObjectClass = endpoint.getComponent().getCamelContext().getClassResolver().resolveClass(className);
                    if (sObjectClass == null) {
                        throw new IllegalArgumentException(String.format("SObject Class not found %s", className));
                    }
                } else {
                    LOG.warn("Property sObjectName or sObjectClass NOT set, messages will be of type java.lang.Map");
                    sObjectClass = null;
                }
            }
        } else {
            sObjectClass = null;
        }
    }
}
